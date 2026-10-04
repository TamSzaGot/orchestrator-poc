package com.example.ingester.core;

import java.time.Instant;
import java.time.LocalDate;

public record SubmissionTokenClaims(
        String submissionId,
        String tokenId,
        Instant issuedAt,
        Instant expiresAt,
        LocalDate fromDate,
        LocalDate toDate
) {
}
