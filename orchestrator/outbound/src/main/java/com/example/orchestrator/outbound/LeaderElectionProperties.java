package com.example.orchestrator.outbound;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "leader.election")
public class LeaderElectionProperties {

    private boolean enabled = false;
    private String leaseName = "orchestrator-leader";
    private int leaseDurationSeconds = 15;
    private long retryMs = 2000;
    private String namespace = "default";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLeaseName() {
        return leaseName;
    }

    public void setLeaseName(String leaseName) {
        this.leaseName = leaseName;
    }

    public int getLeaseDurationSeconds() {
        return leaseDurationSeconds;
    }

    public void setLeaseDurationSeconds(int leaseDurationSeconds) {
        this.leaseDurationSeconds = leaseDurationSeconds;
    }

    public long getRetryMs() {
        return retryMs;
    }

    public void setRetryMs(long retryMs) {
        this.retryMs = retryMs;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
