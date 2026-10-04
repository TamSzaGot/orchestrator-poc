import express from "express";
import {
  Configuration as IngesterClientConfiguration,
  DefaultApi as IngesterApi,
  EndOfSubmissionObjectTypeEnum,
  NdjsonStreamObjectToJSON,
  type NdjsonStreamObject,
  type StreamPayloadObject
} from "../generated/ingester-api/index";
import {
  TriggerSubmissionRequestFromJSON,
  TriggerSubmissionResponseStatusEnum,
  type TriggerSubmissionRequest,
  type TriggerSubmissionResponse
} from "../generated/partner-api/index";

const app = express();
app.use(express.json({ limit: "2mb" }));

const port = Number(process.env.PORT ?? 8082);

const DEFAULT_OBJECTS_PER_DAY = 10;
const FALLBACK_OBJECT_COUNT = 2;
const MAX_OBJECT_COUNT = 5_000;

type FailureMode =
  | "happy"
  | "malformed_line"
  | "sequence_gap"
  | "count_mismatch"
  | "missing_submission_end"
  | "duplicate_submission_end"
  | "token_replay";

app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

app.post("/partner-submissions/trigger", (req, res) => {
  const triggerRequest = TriggerSubmissionRequestFromJSON(req.body);

  if (!isValidTriggerRequest(triggerRequest)) {
    res.status(400).json({
      code: "invalid_trigger_request",
      message: "Required trigger request fields are missing"
    });
    return;
  }

  void submitMockStreamToIngester(triggerRequest).catch((error: unknown) => {
    // eslint-disable-next-line no-console
    console.error("mock-partner submission failed", {
      submissionId: triggerRequest.submissionId,
      error
    });
  });

  const response: TriggerSubmissionResponse = {
    submissionId: triggerRequest.submissionId,
    status: TriggerSubmissionResponseStatusEnum.Accepted,
    acceptedAt: new Date(),
    message: "Mock partner accepted trigger"
  };

  res.status(202).json(response);
});

app.post("/partner-submissions/:submissionId/outcome", (_req, res) => {
  // Non-idempotence behavior can be added in next iteration.
  res.status(204).send();
});

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`mock-partner listening on port ${port}`);
});

function isValidTriggerRequest(request: TriggerSubmissionRequest): boolean {
  return Boolean(
    request.submissionId &&
      request.partnerId &&
      request.ingesterUrl &&
      request.submissionToken
  );
}

function resolveIngesterBasePath(ingesterUrl: string): string {
  const url = new URL(ingesterUrl);
  const endpointSuffix = "/ingestions/records";
  const pathPrefix = url.pathname.endsWith(endpointSuffix)
    ? url.pathname.slice(0, url.pathname.length - endpointSuffix.length)
    : "";
  return `${url.origin}${pathPrefix}`;
}

function buildMockNdjsonObjects(request: TriggerSubmissionRequest): NdjsonStreamObject[] {
  const mode = getFailureMode(request);
  const objectCount = resolvePayloadObjectCount(request);

  const payloadLines: StreamPayloadObject[] = Array.from(
    { length: objectCount },
    (_value, index) => {
      const sequenceNr = index + 1;
      return {
        sequenceNr,
        payload: {
          submissionId: request.submissionId,
          partnerId: request.partnerId,
          objectType: "BUSINESS_EVENT",
          event: sequenceNr % 2 === 0 ? "UPDATED" : "CREATED",
          objectKey: `${request.submissionId}-${sequenceNr}`
        }
      };
    }
  );

  if (mode === "sequence_gap" && payloadLines.length >= 2) {
    payloadLines[1] = {
      ...payloadLines[1],
      sequenceNr: payloadLines[1].sequenceNr + 1
    };
  }

  if (mode === "sequence_gap" && payloadLines.length === 1) {
    payloadLines[0] = {
      ...payloadLines[0],
      sequenceNr: 2
    };
  }

  const endLine: NdjsonStreamObject = {
    type: EndOfSubmissionObjectTypeEnum.SubmissionEnd,
    numberOfObjects:
      mode === "count_mismatch" ? payloadLines.length + 1 : payloadLines.length,
    sentAt: new Date()
  };

  if (mode === "missing_submission_end") {
    return [...payloadLines];
  }

  if (mode === "duplicate_submission_end") {
    return [...payloadLines, endLine, endLine];
  }

  return [...payloadLines, endLine];
}

function toNdjsonBody(lines: NdjsonStreamObject[], mode: FailureMode): string {
  const serialized = lines
    .map((line) => JSON.stringify(NdjsonStreamObjectToJSON(line)))
    .join("\n");

  if (mode === "malformed_line") {
    return `${serialized}\n{"sequenceNr": this-is-invalid-json}\n`;
  }

  return `${serialized}\n`;
}

async function submitMockStreamToIngester(request: TriggerSubmissionRequest): Promise<void> {
  const mode = getFailureMode(request);
  const ingesterBasePath = resolveIngesterBasePath(request.ingesterUrl);
  const ingesterApi = new IngesterApi(
    new IngesterClientConfiguration({ basePath: ingesterBasePath })
  );

  const ndjsonBody = toNdjsonBody(buildMockNdjsonObjects(request), mode);

  await ingesterApi.ingestSubmissionRecords({
    xSubmissionToken: request.submissionToken,
    body: ndjsonBody
  });

  if (mode === "token_replay") {
    try {
      await ingesterApi.ingestSubmissionRecords({
        xSubmissionToken: request.submissionToken,
        body: ndjsonBody
      });
    } catch (error: unknown) {
      // eslint-disable-next-line no-console
      console.log("mock-partner replay produced expected error", {
        submissionId: request.submissionId,
        error
      });
    }
  }
}

function getFailureMode(request: TriggerSubmissionRequest): FailureMode {
  const modeValue = request.metadata?.mode;
  if (typeof modeValue !== "string") {
    return "happy";
  }

  const validModes: FailureMode[] = [
    "happy",
    "malformed_line",
    "sequence_gap",
    "count_mismatch",
    "missing_submission_end",
    "duplicate_submission_end",
    "token_replay"
  ];

  return validModes.includes(modeValue as FailureMode)
    ? (modeValue as FailureMode)
    : "happy";
}

function resolvePayloadObjectCount(request: TriggerSubmissionRequest): number {
  const override = request.metadata?.objectsPerDay;
  const objectsPerDay =
    typeof override === "number" && Number.isInteger(override) && override > 0
      ? override
      : DEFAULT_OBJECTS_PER_DAY;

  const fromDate = request.submissionWindow?.fromDate;
  const toDate = request.submissionWindow?.toDate;

  if (!fromDate || !toDate) {
    return FALLBACK_OBJECT_COUNT;
  }

  const from = new Date(`${fromDate}T00:00:00Z`);
  const to = new Date(`${toDate}T00:00:00Z`);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || to < from) {
    return FALLBACK_OBJECT_COUNT;
  }

  const dayCount = Math.floor((to.getTime() - from.getTime()) / 86_400_000) + 1;
  const count = dayCount * objectsPerDay;
  return Math.min(Math.max(count, 1), MAX_OBJECT_COUNT);
}
