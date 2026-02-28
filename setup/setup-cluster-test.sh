#!/bin/bash

# --- 1. DEFINE TARGETS ---
# Names match your infrastructure-services.yaml
services=(
  "accounts-db-service:5432"
  "fitness-db-service:5432"
  "auth-cache-service:6379"
  "notification-db-service:27017"
  "analytics-queue-service:4566"
  "cleanup-queue-service:4566"
  "notification-queue-service:4566"
)

echo "--------------------------------------------------"
echo "🚀 Starting Infrastructure Connectivity Test..."
echo "--------------------------------------------------"

# --- 2. EXECUTE TESTS ---
# We use a single 'kubectl run' to execute all tests inside one temporary pod
for entry in "${services[@]}"; do
  IFS=":" read -r name port <<< "$entry"
  
  echo -n "Testing $name on port $port... "
  
  # Run a temporary busybox pod to check the connection
  # --quiet hides the pod creation logs so we only see the result
  if kubectl run net-check-$RANDOM --image=busybox:1.28 --rm -it --restart=Never -- nc -zv -w 2 "$name" "$port" > /dev/null 2>&1; then
    echo "✅ PASSED"
  else
    echo "❌ FAILED"
  fi
done

echo "--------------------------------------------------"
echo "Test complete."