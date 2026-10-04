package com.example.orchestrator.core;

import java.time.Instant;

public record TriggerCommand(String submissionId, String partnerId, Instant requestedAt) {
}
