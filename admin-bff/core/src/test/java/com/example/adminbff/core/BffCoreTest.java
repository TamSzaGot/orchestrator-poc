package com.example.adminbff.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BffCoreTest {

    @Test
    void acceptsValidTriggerRequest() {
        assertTrue(BffCore.isValidTrigger("sub-1", "partner-1"));
    }

    @Test
    void rejectsBlankValues() {
        assertFalse(BffCore.isValidTrigger("", "partner-1"));
        assertFalse(BffCore.isValidTrigger("sub-1", ""));
    }
}
