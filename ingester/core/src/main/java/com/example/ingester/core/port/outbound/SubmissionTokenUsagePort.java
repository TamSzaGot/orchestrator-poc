package com.example.ingester.core.port.outbound;

public interface SubmissionTokenUsagePort {
    boolean markAsUsed(String tokenId);
}
