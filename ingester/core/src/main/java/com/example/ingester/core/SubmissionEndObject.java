package com.example.ingester.core;

import java.time.Instant;

public record SubmissionEndObject(long numberOfObjects, Instant sentAt) implements SubmissionStreamObject {
}
