package com.example.ingester.core;

import java.util.Iterator;
import java.util.stream.Stream;

public final class IngestionCore {

    private IngestionCore() {
    }

    public static IngestionDecision validateStream(Stream<IngestionLine> lines) {
        if (lines == null) {
            return IngestionDecision.rejected("stream_missing");
        }

        Iterator<IngestionLine> iterator = lines.iterator();
        long expectedSequenceNr = 1;
        long payloadCount = 0;
        boolean seenEnd = false;

        while (iterator.hasNext()) {
            IngestionLine line = iterator.next();

            if (line instanceof ParseFailureLine) {
                return IngestionDecision.rejected("line_parse_error");
            }

            if (!(line instanceof ParsedSubmissionObjectLine parsedLine)) {
                return IngestionDecision.rejected("unknown_stream_line");
            }

            SubmissionStreamObject next = parsedLine.object();

            if (next instanceof StreamPayloadObject payloadObject) {
                if (seenEnd) {
                    return IngestionDecision.rejected("payload_after_submission_end");
                }
                if (payloadObject.sequenceNr() != expectedSequenceNr) {
                    return IngestionDecision.rejected("sequence_not_contiguous");
                }
                expectedSequenceNr++;
                payloadCount++;
                continue;
            }

            if (next instanceof SubmissionEndObject endObject) {
                if (seenEnd) {
                    return IngestionDecision.rejected("multiple_submission_end_objects");
                }
                seenEnd = true;
                if (endObject.numberOfObjects() != payloadCount) {
                    return IngestionDecision.rejected("number_of_objects_mismatch");
                }
                continue;
            }

            return IngestionDecision.rejected("unknown_stream_object");
        }

        if (!seenEnd) {
            return IngestionDecision.rejected("submission_end_missing");
        }

        return IngestionDecision.accepted(payloadCount);
    }
}
