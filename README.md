# Orchestrator + Admin BFF POC

Monorepo containing three Spring Boot 4.1 services using Java 21 and Gradle:

- `orchestrator`: leader-elected orchestrator with read and write admin APIs
- `admin-bff`: minimal BFF that calls orchestrator read and write services
- `ingester`: ingestion service skeleton with hexagonal module structure

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
