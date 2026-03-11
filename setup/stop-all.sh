#!/bin/bash
echo "🛑 Shutting down Fitness Tracker environment..."

# 1. Stop Kubernetes (KinD)
# This removes the control plane and worker nodes
echo "Stopping KinD cluster..."
kind delete cluster --name fitness-cluster

# 2. Stop Infrastructure (Docker Compose)
# 'stop' keeps the containers/volumes but halts the processes
# 'down' removes the containers and the internal network
echo "Stopping Databases and LocalStack..."
docker-compose stop 

echo "✅ Environment paused. Run ./setup-cluster.sh to resume."