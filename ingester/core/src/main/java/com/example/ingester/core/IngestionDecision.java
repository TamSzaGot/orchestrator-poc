package com.example.ingester.core;

public record IngestionDecision(boolean accepted, String code, long payloadObjectCount) {

    public static IngestionDecision accepted(long payloadObjectCount) {
        return new IngestionDecision(true, "accepted", payloadObjectCount);
    }

    public static IngestionDecision rejected(String code) {
        return new IngestionDecision(false, code, 0);
    }
}
