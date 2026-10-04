package com.example.orchestrator.outbound;

import com.example.orchestrator.core.JobStorePort;
import com.example.orchestrator.core.JobView;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "job.store", name = "type", havingValue = "jdbc")
public class JdbcJobStore implements JobStorePort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcJobStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(JobView jobView) {
        jdbcTemplate.update(
                "INSERT INTO jobs(submission_id, partner_id, requested_at) VALUES (?, ?, ?)",
                jobView.submissionId(),
                jobView.partnerId(),
                jobView.requestedAt()
        );
    }

    @Override
    public List<JobView> findAll() {
        return jdbcTemplate.query(
                "SELECT submission_id, partner_id, requested_at FROM jobs ORDER BY id",
                (rs, rowNum) -> new JobView(
                        rs.getString("submission_id"),
                        rs.getString("partner_id"),
                        rs.getString("requested_at")
                )
        );
    }
}
