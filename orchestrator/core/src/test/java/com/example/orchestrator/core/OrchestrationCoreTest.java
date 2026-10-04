package com.example.orchestrator.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrchestrationCoreTest {

    @Test
    void acceptsValidRequestForLeader() {
        TriggerCommand command = new TriggerCommand("sub-1", "partner-1", Instant.now());

        TriggerDecision decision = OrchestrationCore.validateTriggerCommand(command, true);

        assertTrue(decision.acceptedFlag());
        assertEquals("accepted", decision.reason());
    }

    @Test
    void rejectsWhenNotLeader() {
        TriggerCommand command = new TriggerCommand("sub-1", "partner-1", Instant.now());

        TriggerDecision decision = OrchestrationCore.validateTriggerCommand(command, false);

        assertFalse(decision.acceptedFlag());
        assertEquals("not_leader", decision.reason());
    }
}
