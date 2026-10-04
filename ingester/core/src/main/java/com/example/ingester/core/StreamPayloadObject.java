package com.example.ingester.core;

import java.util.Map;

public record StreamPayloadObject(long sequenceNr, Map<String, Object> payload) implements SubmissionStreamObject {
}
