package com.example.adminbff.inbound;

import com.example.adminbff.core.AdminBffUseCase;
import com.example.adminbff.core.BffCore;
import com.example.adminbff.core.OrchestratorGatewayPort;
import com.example.adminbff.core.TriggerRequest;
import org.springframework.stereotype.Service;

@Service
public class AdminBffService implements AdminBffUseCase {

    private final OrchestratorGatewayPort orchestratorGatewayPort;

    public AdminBffService(OrchestratorGatewayPort orchestratorGatewayPort) {
        this.orchestratorGatewayPort = orchestratorGatewayPort;
    }

    @Override
    public String getJobs() {
        return orchestratorGatewayPort.readJobs();
    }

    @Override
    public String trigger(TriggerRequest request) {
        if (!BffCore.isValidTrigger(request.submissionId(), request.partnerId())) {
            return "{\"error\":\"invalid_request\"}";
        }
        return orchestratorGatewayPort.trigger(request);
    }
}
