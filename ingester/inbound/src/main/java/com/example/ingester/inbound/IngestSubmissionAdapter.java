package com.example.ingester.inbound;

import com.example.ingester.core.IngestionDecision;
import com.example.ingester.core.SubmissionTokenClaims;
import com.example.ingester.core.port.inbound.IngestSubmissionUseCase;
import java.util.stream.Stream;

public class IngestSubmissionAdapter {

    private final IngestSubmissionUseCase useCase;
    private final NdjsonLineParser parser;

    public IngestSubmissionAdapter(IngestSubmissionUseCase useCase, NdjsonLineParser parser) {
        this.useCase = useCase;
        this.parser = parser;
    }

    public IngestionDecision ingestRawLines(SubmissionTokenClaims tokenClaims, Stream<String> rawLines) {
        return useCase.ingest(tokenClaims, parser.parse(rawLines));
    }
}
