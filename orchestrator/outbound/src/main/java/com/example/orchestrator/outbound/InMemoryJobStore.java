package com.example.orchestrator.outbound;

import com.example.orchestrator.core.JobStorePort;
import com.example.orchestrator.core.JobView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "job.store", name = "type", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryJobStore implements JobStorePort {

    private final List<JobView> storage = new CopyOnWriteArrayList<>();

    @Override
    public void save(JobView jobView) {
        storage.add(jobView);
    }

    @Override
    public List<JobView> findAll() {
        return new ArrayList<>(storage);
    }
}
