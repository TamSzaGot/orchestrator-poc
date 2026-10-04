package com.example.orchestrator.inbound;

import com.example.orchestrator.core.JobStorePort;
import com.example.orchestrator.core.JobView;
import com.example.orchestrator.core.LeadershipPort;
import com.example.orchestrator.core.OrchestrationCore;
import com.example.orchestrator.core.ReadJobsUseCase;
import com.example.orchestrator.core.TriggerCommand;
import com.example.orchestrator.core.TriggerDecision;
import com.example.orchestrator.core.TriggerSubmissionRequest;
import com.example.orchestrator.core.TriggerSubmissionResponse;
import com.example.orchestrator.core.TriggerSubmissionUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrchestratorService implements TriggerSubmissionUseCase, ReadJobsUseCase {

    private final LeadershipPort leadershipPort;
    private final JobStorePort jobStorePort;

    public OrchestratorService(LeadershipPort leadershipPort, JobStorePort jobStorePort) {
        this.leadershipPort = leadershipPort;
        this.jobStorePort = jobStorePort;
    }

    @Override
    public TriggerSubmissionResponse trigger(TriggerSubmissionRequest request) {
        TriggerCommand command = new TriggerCommand(request.submissionId(), request.partnerId(), Instant.now());
        TriggerDecision decision = OrchestrationCore.validateTriggerCommand(command, leadershipPort.isLeader());

        if (!decision.acceptedFlag()) {
            return new TriggerSubmissionResponse(false, decision.reason());
        }

        jobStorePort.save(new JobView(command.submissionId(), command.partnerId(), command.requestedAt().toString()));
        return new TriggerSubmissionResponse(true, "accepted");
    }

    @Override
    public List<JobView> readJobs() {
        return jobStorePort.findAll();
    }
}
