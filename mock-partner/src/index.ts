import express from "express";

const app = express();
app.use(express.json({ limit: "2mb" }));

const port = Number(process.env.PORT ?? 8082);

app.get("/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

app.post("/partner-submissions/trigger", (req, res) => {
  const body = req.body as Record<string, unknown>;

  // Lightweight placeholder. Next iteration will map request/response strictly to generated partner API types
  // and invoke generated ingester client with an NDJSON stream payload.
  res.status(202).json({
    submissionId: body.submissionId ?? "unknown",
    status: "ACCEPTED",
    acceptedAt: new Date().toISOString(),
    message: "Mock partner accepted trigger"
  });
});

app.post("/partner-submissions/:submissionId/outcome", (_req, res) => {
  // Non-idempotence behavior can be added in next iteration.
  res.status(204).send();
});

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`mock-partner listening on port ${port}`);
});
