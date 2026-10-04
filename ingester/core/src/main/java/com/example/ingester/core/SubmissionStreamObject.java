package com.example.ingester.core;

public sealed interface SubmissionStreamObject permits StreamPayloadObject, SubmissionEndObject {
}
