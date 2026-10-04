package com.example.ingester.core;

public record ParsedSubmissionObjectLine(long lineNumber, SubmissionStreamObject object) implements IngestionLine {
}