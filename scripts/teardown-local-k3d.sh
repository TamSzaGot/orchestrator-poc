#!/usr/bin/env bash
set -euo pipefail

CLUSTER_NAME="${1:-local-dev}"
NAMESPACE="${2:-default}"
KEEP_CLUSTER="${KEEP_CLUSTER:-false}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1"
    exit 1
  fi
}

require_cmd kubectl
require_cmd k3d

if k3d cluster list | awk '{print $1}' | grep -qx "$CLUSTER_NAME"; then
  echo "Deleting manifests from namespace: $NAMESPACE"
  kubectl -n "$NAMESPACE" delete -f ./k8s/admin-bff-deployment.yaml --ignore-not-found
  kubectl -n "$NAMESPACE" delete -f ./k8s/orchestrator-services.yaml --ignore-not-found
  kubectl -n "$NAMESPACE" delete -f ./k8s/orchestrator-deployment.yaml --ignore-not-found
  kubectl -n "$NAMESPACE" delete -f ./k8s/orchestrator-rbac.yaml --ignore-not-found
  kubectl -n "$NAMESPACE" delete -f ./k8s/orchestrator-postgres.yaml --ignore-not-found

  if [[ "$NAMESPACE" != "default" ]]; then
    echo "Deleting namespace: $NAMESPACE"
    kubectl delete namespace "$NAMESPACE" --ignore-not-found
  fi

  if [[ "$KEEP_CLUSTER" == "true" ]]; then
    echo "Keeping k3d cluster '$CLUSTER_NAME' (KEEP_CLUSTER=true)"
  else
    echo "Deleting k3d cluster: $CLUSTER_NAME"
    k3d cluster delete "$CLUSTER_NAME"
  fi
else
  echo "Cluster '$CLUSTER_NAME' not found. Nothing to tear down."
fi

echo "Teardown complete"
