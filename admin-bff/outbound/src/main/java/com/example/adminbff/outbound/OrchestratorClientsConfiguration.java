package com.example.adminbff.outbound;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OrchestratorServiceUrls.class)
public class OrchestratorClientsConfiguration {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Qualifier("readClient")
    RestClient readClient(RestClient.Builder builder, OrchestratorServiceUrls urls) {
        return builder.baseUrl(urls.getReadBaseUrl()).build();
    }

    @Bean
    @Qualifier("writeClient")
    RestClient writeClient(RestClient.Builder builder, OrchestratorServiceUrls urls) {
        return builder.baseUrl(urls.getWriteBaseUrl()).build();
    }
}
