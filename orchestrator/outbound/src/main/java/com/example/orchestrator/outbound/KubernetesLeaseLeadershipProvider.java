package com.example.orchestrator.outbound;

import com.example.orchestrator.core.LeadershipPort;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KubernetesLeaseLeadershipProvider implements LeadershipPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesLeaseLeadershipProvider.class);

    private final KubernetesClient kubernetesClient;
    private final LeaderElectionProperties properties;
    private final String podName;
    private final String namespace;
    private final AtomicBoolean leader = new AtomicBoolean(false);

    public KubernetesLeaseLeadershipProvider(LeaderElectionProperties properties) {
        this.properties = properties;
        this.podName = System.getenv().getOrDefault("POD_NAME", "local-dev");
        this.namespace = System.getenv().getOrDefault("POD_NAMESPACE", properties.getNamespace());
        this.kubernetesClient = properties.isEnabled() ? new KubernetesClientBuilder().build() : null;
    }

    @Override
    public boolean isLeader() {
        if (!properties.isEnabled()) {
            return true;
        }
        return leader.get();
    }

    @Scheduled(fixedDelayString = "${leader.election.retry-ms:2000}")
    public void electionLoop() {
        if (!properties.isEnabled() || kubernetesClient == null) {
            return;
        }
        try {
            Lease lease = kubernetesClient.resources(Lease.class)
                    .inNamespace(namespace)
                    .withName(properties.getLeaseName())
                    .get();

            if (lease == null) {
                createLease();
                becomeLeader();
                return;
            }

            LeaseSpec spec = lease.getSpec();
            String holder = spec != null ? spec.getHolderIdentity() : null;

            if (podName.equals(holder)) {
                renewLease(lease);
                becomeLeader();
                return;
            }

            if (isExpired(spec)) {
                claimExpiredLease(lease);
                becomeLeader();
                return;
            }

            becomeFollower();
        } catch (KubernetesClientException ex) {
            LOGGER.warn("Leader election iteration failed", ex);
        }
    }

    private void createLease() {
        ZonedDateTime now = OffsetDateTime.now().toZonedDateTime();
        Lease lease = new LeaseBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(properties.getLeaseName())
                        .withNamespace(namespace)
                        .build())
                .withNewSpec()
                .withHolderIdentity(podName)
                .withLeaseDurationSeconds(properties.getLeaseDurationSeconds())
                .withAcquireTime(now)
                .withRenewTime(now)
                .endSpec()
                .build();
        kubernetesClient.resource(lease).create();
    }

    private void renewLease(Lease lease) {
        ZonedDateTime now = OffsetDateTime.now().toZonedDateTime();
        Lease renewed = new LeaseBuilder(lease)
                .editSpec()
                .withHolderIdentity(podName)
                .withLeaseDurationSeconds(properties.getLeaseDurationSeconds())
                .withRenewTime(now)
                .endSpec()
                .build();
        sanitizeForUpdate(renewed);
        kubernetesClient.resources(Lease.class)
                .inNamespace(namespace)
                .resource(renewed)
                .update();
    }

    private void claimExpiredLease(Lease lease) {
        ZonedDateTime now = OffsetDateTime.now().toZonedDateTime();
        Lease claimed = new LeaseBuilder(lease)
                .editSpec()
                .withHolderIdentity(podName)
                .withLeaseDurationSeconds(properties.getLeaseDurationSeconds())
                .withAcquireTime(now)
                .withRenewTime(now)
                .endSpec()
                .build();
        sanitizeForUpdate(claimed);
        kubernetesClient.resources(Lease.class)
                .inNamespace(namespace)
                .resource(claimed)
                .update();
    }

    private void sanitizeForUpdate(Lease lease) {
        if (lease.getMetadata() != null) {
            lease.getMetadata().setManagedFields(null);
        }
    }

    private boolean isExpired(LeaseSpec spec) {
        if (spec == null || spec.getRenewTime() == null) {
            return true;
        }
        int duration = spec.getLeaseDurationSeconds() == null
                ? properties.getLeaseDurationSeconds()
                : spec.getLeaseDurationSeconds();
        Instant renewTime = spec.getRenewTime().toInstant();
        return renewTime.plusSeconds(duration).isBefore(Instant.now());
    }

    private void becomeLeader() {
        if (leader.compareAndSet(false, true)) {
            patchPodLeaderLabel("true");
            LOGGER.info("Pod {} became leader", podName);
        }
    }

    private void becomeFollower() {
        if (leader.compareAndSet(true, false)) {
            patchPodLeaderLabel("false");
            LOGGER.info("Pod {} became follower", podName);
        }
    }

    private void patchPodLeaderLabel(String value) {
        try {
            String patchBody = "{\"metadata\":{\"labels\":{\"leader\":\"" + value + "\"}}}";
            kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .patch(patchBody);
        } catch (Exception ex) {
            LOGGER.warn("Failed to patch leader label on pod {}: {}", podName, ex.getMessage());
        }
    }
}
