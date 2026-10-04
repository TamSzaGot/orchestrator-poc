#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${1:-local-dev}"
NAMESPACE="${2:-default}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1"
    exit 1
  fi
}

require_cmd kubectl
require_cmd k3d
require_cmd docker

if [[ ! -f "./gradlew" ]]; then
  echo "Gradle wrapper missing. Run: gradle wrapper"
  exit 1
fi

if ! k3d cluster list | awk '{print $1}' | grep -qx "$CLUSTER_NAME"; then
  echo "Creating k3d cluster: $CLUSTER_NAME"
  k3d cluster create "$CLUSTER_NAME" -p "8080:30080@loadbalancer" -p "8081:30081@loadbalancer"
fi

echo "Building Spring Boot jars"
./gradlew :orchestrator:application:bootJar :admin-bff:application:bootJar

echo "Building Docker images"
docker build -t orchestrator:latest ./orchestrator
docker build -t admin-bff:latest ./admin-bff

echo "Importing images into k3d"
k3d image import -c "$CLUSTER_NAME" orchestrator:latest admin-bff:latest

echo "Applying Kubernetes manifests"
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$NAMESPACE" apply -f ./k8s/orchestrator-postgres.yaml
kubectl -n "$NAMESPACE" apply -f ./k8s/orchestrator-rbac.yaml
kubectl -n "$NAMESPACE" apply -f ./k8s/orchestrator-deployment.yaml
kubectl -n "$NAMESPACE" apply -f ./k8s/orchestrator-services.yaml
kubectl -n "$NAMESPACE" apply -f ./k8s/admin-bff-deployment.yaml

echo "Restarting deployments to pick up latest :latest images"
kubectl -n "$NAMESPACE" rollout restart deployment/orchestrator deployment/admin-bff

echo "Waiting for rollouts"
kubectl -n "$NAMESPACE" rollout status deployment/orchestrator-postgres --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/orchestrator --timeout=180s
kubectl -n "$NAMESPACE" rollout status deployment/admin-bff --timeout=180s

echo "Local access hints"
echo "- Admin BFF service: kubectl -n $NAMESPACE port-forward svc/admin-bff 8088:80"
echo "- Orchestrator read service: kubectl -n $NAMESPACE port-forward svc/orchestrator-read 8089:80"
echo "- Orchestrator write service: kubectl -n $NAMESPACE port-forward svc/orchestrator-write 8090:80"
