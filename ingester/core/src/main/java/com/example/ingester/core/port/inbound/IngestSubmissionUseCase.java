package com.example.ingester.core.port.inbound;

import com.example.ingester.core.IngestionLine;
import com.example.ingester.core.IngestionDecision;
import com.example.ingester.core.SubmissionTokenClaims;
import java.util.stream.Stream;

public interface IngestSubmissionUseCase {
    IngestionDecision ingest(SubmissionTokenClaims tokenClaims, Stream<IngestionLine> lines);
}
