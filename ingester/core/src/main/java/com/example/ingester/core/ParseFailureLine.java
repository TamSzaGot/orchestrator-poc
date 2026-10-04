package com.example.ingester.core;

public record ParseFailureLine(long lineNumber, String rawLine, String reason) implements IngestionLine {
}