package com.example.orchestrator.inbound;

import com.example.orchestrator.core.JobView;
import com.example.orchestrator.core.ReadJobsUseCase;
import com.example.orchestrator.core.TriggerSubmissionRequest;
import com.example.orchestrator.core.TriggerSubmissionResponse;
import com.example.orchestrator.core.TriggerSubmissionUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TriggerSubmissionUseCase triggerSubmissionUseCase;
    private final ReadJobsUseCase readJobsUseCase;

    public AdminController(TriggerSubmissionUseCase triggerSubmissionUseCase, ReadJobsUseCase readJobsUseCase) {
        this.triggerSubmissionUseCase = triggerSubmissionUseCase;
        this.readJobsUseCase = readJobsUseCase;
    }

    @GetMapping("/jobs")
    public List<JobView> jobs() {
        return readJobsUseCase.readJobs();
    }

    @PostMapping("/trigger")
    public ResponseEntity<TriggerSubmissionResponse> trigger(@RequestBody TriggerSubmissionRequest request) {
        TriggerSubmissionResponse response = triggerSubmissionUseCase.trigger(request);
        if (!response.accepted() && "not_leader".equals(response.status())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        if (!response.accepted()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.accepted().body(response);
    }
}
