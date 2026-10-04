package com.example.adminbff.outbound;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "orchestrator")
public class OrchestratorServiceUrls {

    private String readBaseUrl = "http://orchestrator-read";
    private String writeBaseUrl = "http://orchestrator-write";

    public String getReadBaseUrl() {
        return readBaseUrl;
    }

    public void setReadBaseUrl(String readBaseUrl) {
        this.readBaseUrl = readBaseUrl;
    }

    public String getWriteBaseUrl() {
        return writeBaseUrl;
    }

    public void setWriteBaseUrl(String writeBaseUrl) {
        this.writeBaseUrl = writeBaseUrl;
    }
}
