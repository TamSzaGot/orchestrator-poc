package com.example.adminbff.core;

public interface OrchestratorGatewayPort {
    String readJobs();
    String trigger(TriggerRequest request);
}
