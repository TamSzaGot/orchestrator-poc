# Orchestrator + Admin BFF POC

Monorepo containing three Spring Boot 4.1 services using Java 21 and Gradle:

- `orchestrator`: leader-elected orchestrator with read and write admin APIs
- `admin-bff`: minimal BFF that calls orchestrator read and write services
- `ingester`: ingestion service skeleton with hexagonal module structure

Also includes a lightweight JavaScript mock partner service:

- `mock-partner`: minimal Node.js server for partner trigger/outcome callbacks and OpenAPI-driven code generation

For local Kubernetes deployment, orchestrator uses PostgreSQL shared state so read traffic can remain load balanced over all orchestrator pods.

## Build

```bash
brew install gradle
brew install openjdk@21
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
gradle wrapper
./gradlew clean test
```

Or without wrapper:

```bash
gradle clean test
```

## Run locally

```bash
./gradlew :orchestrator:application:bootRun
./gradlew :admin-bff:application:bootRun
./gradlew :ingester:application:bootRun
```

## Kubernetes manifests

Manifests are in `k8s/`.

- `k8s/orchestrator-postgres.yaml`: local PostgreSQL for shared job state
- `k8s/orchestrator-services.yaml`: read service (all pods) + write service (leader only)

The local PostgreSQL manifest includes a `PersistentVolumeClaim` (`orchestrator-postgres-data`) so data survives PostgreSQL pod restarts.

## Build container images

```bash
./gradlew :orchestrator:application:bootJar :admin-bff:application:bootJar
docker build -t orchestrator:latest ./orchestrator
docker build -t admin-bff:latest ./admin-bff
```

## One-command local k3d deploy

```bash
./scripts/deploy-local-k3d.sh
```

Optional args:

```bash
./scripts/deploy-local-k3d.sh <cluster-name> <namespace>
```

## One-command teardown

```bash
./scripts/teardown-local-k3d.sh
```

Optional args:

```bash
./scripts/teardown-local-k3d.sh <cluster-name> <namespace>
```

Keep cluster but remove workloads:

```bash
KEEP_CLUSTER=true ./scripts/teardown-local-k3d.sh
```

## Smoke test through BFF

```bash
kubectl port-forward svc/admin-bff 8088:80
curl -i http://localhost:8088/admin/jobs
curl -i -X POST http://localhost:8088/admin/trigger \
	-H 'Content-Type: application/json' \
	-d '{"submissionId":"sub-001","partnerId":"partner-a"}'
```

## One-command persistence smoke test

```bash
./scripts/smoke-persistence.sh
```

Optional args:

```bash
./scripts/smoke-persistence.sh <namespace> <submission-id> <partner-id> <local-port>
```

## Mock partner (lightweight)

```bash
cd mock-partner
npm install
npm run codegen
npm run dev
```

Code generation uses repository OpenAPI specs:

- `openapi/partner-orchestration-api.yaml` (partner API contract)
- `openapi/ingester-submission-api.yaml` (ingester client contract)

### Mock partner trigger metadata

The mock partner accepts deterministic behavior controls in the trigger request
`metadata` object.

Supported metadata fields:

- `metadata.mode`: fault injection mode (defaults to `happy`)
- `metadata.objectsPerDay`: positive integer used with date range sizing (defaults to `10`)

Payload sizing rule:

- If `submissionWindow.fromDate` and `submissionWindow.toDate` are provided,
	payload objects = inclusive day count * `objectsPerDay`
- If date range is missing/invalid, fallback payload object count is `2`

Supported `metadata.mode` values:

- `happy`: valid contiguous sequence and single `SUBMISSION_END`
- `malformed_line`: appends one invalid JSON line after valid stream
- `sequence_gap`: injects non-contiguous `sequenceNr`
- `count_mismatch`: `SUBMISSION_END.numberOfObjects` does not match payload count
- `missing_submission_end`: omits `SUBMISSION_END`
- `duplicate_submission_end`: appends `SUBMISSION_END` twice
- `token_replay`: sends the same submission twice with the same token

Example trigger request:

```json
{
	"submissionId": "sub-20261004-0001",
	"partnerId": "partner-a",
	"ingesterUrl": "http://localhost:8083/ingestions/records",
	"submissionToken": "<jwt>",
	"tokenExpiresAt": "2026-10-04T22:00:00Z",
	"requestedAt": "2026-10-04T21:00:00Z",
	"submissionWindow": {
		"fromDate": "2026-10-01",
		"toDate": "2026-10-04"
	},
	"metadata": {
		"mode": "count_mismatch",
		"objectsPerDay": 10
	}
}
```

### Curl examples for deterministic modes

Assumes mock-partner is running on `localhost:8082`.

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-happy-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-03" },
		"metadata": { "mode": "happy", "objectsPerDay": 10 }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-malformed-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "malformed_line" }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-seq-gap-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "sequence_gap" }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-count-mismatch-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "count_mismatch" }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-missing-end-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "missing_submission_end" }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-duplicate-end-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "duplicate_submission_end" }
	}'
```

```bash
curl -i -X POST http://localhost:8082/partner-submissions/trigger \
	-H 'Content-Type: application/json' \
	-d '{
		"submissionId": "sub-replay-001",
		"partnerId": "partner-a",
		"ingesterUrl": "http://localhost:8083/ingestions/records",
		"submissionToken": "<jwt>",
		"tokenExpiresAt": "2026-10-04T22:00:00Z",
		"requestedAt": "2026-10-04T21:00:00Z",
		"submissionWindow": { "fromDate": "2026-10-01", "toDate": "2026-10-01" },
		"metadata": { "mode": "token_replay" }
	}'
```
