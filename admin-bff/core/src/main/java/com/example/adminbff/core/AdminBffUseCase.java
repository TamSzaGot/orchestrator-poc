package com.example.adminbff.core;

public interface AdminBffUseCase {
    String getJobs();
    String trigger(TriggerRequest request);
}
