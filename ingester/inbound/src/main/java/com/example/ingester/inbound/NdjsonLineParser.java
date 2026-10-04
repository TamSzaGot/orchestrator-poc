package com.example.ingester.inbound;

import com.example.ingester.core.IngestionLine;
import com.example.ingester.core.ParseFailureLine;
import com.example.ingester.core.ParsedSubmissionObjectLine;
import com.example.ingester.core.StreamPayloadObject;
import com.example.ingester.core.SubmissionEndObject;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class NdjsonLineParser {

    private final ObjectMapper objectMapper;

    public NdjsonLineParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Stream<IngestionLine> parse(Stream<String> rawLines) {
        if (rawLines == null) {
            return Stream.of(new ParseFailureLine(0, null, "stream_missing"));
        }

        AtomicLong lineCounter = new AtomicLong(0);
        return rawLines
                .filter(line -> line != null && !line.isBlank())
                .map(line -> parseLine(lineCounter.incrementAndGet(), line));
    }

    private IngestionLine parseLine(long lineNumber, String rawLine) {
        try {
            JsonNode root = objectMapper.readTree(rawLine);

            JsonNode typeNode = root.get("type");
            if (typeNode != null && "SUBMISSION_END".equals(typeNode.asText())) {
                JsonNode numberOfObjectsNode = root.get("numberOfObjects");
                if (numberOfObjectsNode == null || !numberOfObjectsNode.canConvertToLong()) {
                    return new ParseFailureLine(lineNumber, rawLine, "number_of_objects_missing_or_invalid");
                }

                Instant sentAt = null;
                JsonNode sentAtNode = root.get("sentAt");
                if (sentAtNode != null && !sentAtNode.isNull()) {
                    try {
                        sentAt = Instant.parse(sentAtNode.asText());
                    } catch (DateTimeParseException ex) {
                        return new ParseFailureLine(lineNumber, rawLine, "sent_at_invalid");
                    }
                }

                SubmissionEndObject endObject = new SubmissionEndObject(numberOfObjectsNode.asLong(), sentAt);
                return new ParsedSubmissionObjectLine(lineNumber, endObject);
            }

            JsonNode sequenceNrNode = root.get("sequenceNr");
            JsonNode payloadNode = root.get("payload");
            if (sequenceNrNode == null || !sequenceNrNode.canConvertToLong()) {
                return new ParseFailureLine(lineNumber, rawLine, "sequence_nr_missing_or_invalid");
            }
            if (payloadNode == null || !payloadNode.isObject()) {
                return new ParseFailureLine(lineNumber, rawLine, "payload_missing_or_invalid");
            }

            Map<String, Object> payload = objectMapper.convertValue(payloadNode, new TypeReference<>() {
            });
            StreamPayloadObject payloadObject = new StreamPayloadObject(sequenceNrNode.asLong(), payload);
            return new ParsedSubmissionObjectLine(lineNumber, payloadObject);
        } catch (Exception ex) {
            return new ParseFailureLine(lineNumber, rawLine, "json_parse_failed");
        }
    }
}
