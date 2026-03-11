#!/bin/bash

# --- 1. CLEANUP & CLUSTER CREATION ---
echo "Deleting old cluster..."
kind delete cluster --name fitness-cluster

echo "Creating Kind cluster with Ingress support..."
# Ensure your kind-config.yaml has the extraPortMappings for 80/443
kind create cluster --config kind-config.yaml --name fitness-cluster

echo "Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml    

echo "Waiting for Ingress controller (approx 60-90s)..."
kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s

# --- 2. DATABASE PREP (The "Lead Engineer" approach) ---
echo "Ensuring Docker DB containers are running..."
# Force restart of containers for dbs and localstack
docker-compose up -d --remove-orphans --force-recreate

echo "------------------------------------------------------------"
echo "Setup Complete!"
echo "------------------------------------------------------------"