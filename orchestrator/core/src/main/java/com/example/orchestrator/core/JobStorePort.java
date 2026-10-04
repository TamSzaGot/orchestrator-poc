package com.example.orchestrator.core;

import java.util.List;

public interface JobStorePort {
    void save(JobView jobView);
    List<JobView> findAll();
}
