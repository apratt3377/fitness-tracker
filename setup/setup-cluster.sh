#!/bin/bash

# --- 1. CLEANUP & CLUSTER CREATION ---
echo "Forcefully clearing stale network endpoints..."
# Disconnects containers even if the network is "ghosted"
for container in user-service-db workout-management-service-db auth-service-cache notification-service-db fitness-infra-localstack; do
  docker network disconnect -f kind $container 2>/dev/null
done
echo "Deleting old cluster and pruning networks..."
kind delete cluster --name fitness-cluster
docker network prune -f
sleep 2

echo "Creating Kind cluster and Ingress..."
kind create cluster --config kind-config.yaml --name fitness-cluster
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml    

echo "Waiting for Ingress controller (approx 60-90s)..."
kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=90s

# --- 2. NETWORK BRIDGING ---
echo "Bridging external Docker containers to 'kind' network..."
docker network connect kind user-service-db
docker network connect kind workout-management-service-db
docker network connect kind auth-service-cache
docker network connect kind notification-service-db
docker network connect kind fitness-infra-localstack

# --- 3. IP EXTRACTION ---
U_DB_IP=$(docker inspect user-service-db -f '{{.NetworkSettings.Networks.kind.IPAddress}}')
W_DB_IP=$(docker inspect workout-management-service-db -f '{{.NetworkSettings.Networks.kind.IPAddress}}')
A_C_IP=$(docker inspect auth-service-cache -f '{{.NetworkSettings.Networks.kind.IPAddress}}')
N_DB_IP=$(docker inspect notification-service-db -f '{{.NetworkSettings.Networks.kind.IPAddress}}')
LS_IP=$(docker inspect fitness-infra-localstack -f '{{.NetworkSettings.Networks.kind.IPAddress}}')

echo "Infrastructure IPs Captured:"
echo "  User DB:         $U_DB_IP"
echo "  Workout DB:      $W_DB_IP"
echo "  Auth Cache:      $A_C_IP"
echo "  Notification DB: $N_DB_IP"
echo "  LocalStack:      $LS_IP"

# --- 4. AUTOMATED DNS PATCHING ---
# This replaces the manual 'kubectl edit' step for better Fault Tolerance and speed
echo "Generating CoreDNS Patch..."

# Create a temporary file for the new CoreDNS data
cat <<EOF > coredns-patch.yaml
data:
  Corefile: |
    .:53 {
        errors
        health {
           lameduck 5s
        }
        ready
        hosts {
           $U_DB_IP postgres-accounts.internal
           $W_DB_IP postgres-fitness.internal
           $A_C_IP redis-cache.internal
           $N_DB_IP mongodb.internal
           $LS_IP localstack.internal
           fallthrough
        }
        kubernetes cluster.local in-addr.arpa ip6.arpa {
           pods insecure
           fallthrough in-addr.arpa ip6.arpa
           ttl 30
        }
        prometheus :9153
        forward . /etc/resolv.conf {
           max_concurrent 1000
        }
        cache 30
        loop
        reload
        loadbalance
    }
EOF

echo "Applying DNS Patch to Cluster..."
kubectl patch configmap coredns -n kube-system --patch-file coredns-patch.yaml

# Clean up the temp file
rm coredns-patch.yaml

# Restart DNS to apply changes
kubectl delete pod -n kube-system -l k8s-app=kube-dns

# --- 5. SERVICE ABSTRACTION ---
echo "Applying Kubernetes Service objects..."
kubectl apply -f infrastructure-services.yaml

echo "Done!"