package com.example.orchestrator.core;

public record TriggerDecision(boolean acceptedFlag, String reason) {
    public static TriggerDecision accepted() {
        return new TriggerDecision(true, "accepted");
    }

    public static TriggerDecision rejected(String reason) {
        return new TriggerDecision(false, reason);
    }
}
