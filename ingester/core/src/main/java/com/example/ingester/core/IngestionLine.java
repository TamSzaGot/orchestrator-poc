package com.example.ingester.core;

public sealed interface IngestionLine permits ParsedSubmissionObjectLine, ParseFailureLine {
}