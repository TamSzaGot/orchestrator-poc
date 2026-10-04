package com.example.adminbff.outbound;

import com.example.adminbff.core.OrchestratorGatewayPort;
import com.example.adminbff.core.TriggerRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

@Component
public class OrchestratorRestGateway implements OrchestratorGatewayPort {

    private final RestClient readClient;
    private final RestClient writeClient;

    public OrchestratorRestGateway(
            @Qualifier("readClient") RestClient readClient,
            @Qualifier("writeClient") RestClient writeClient
    ) {
        this.readClient = readClient;
        this.writeClient = writeClient;
    }

    @Override
    public String readJobs() {
        return readClient.get()
                .uri("/admin/jobs")
                .retrieve()
                .body(String.class);
    }

    @Override
    public String trigger(TriggerRequest request) {
        return writeClient.post()
                .uri("/admin/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
    }
}
