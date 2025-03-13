#!/bin/bash

services=("admin-basic-info" "admin-order" "admin-route" "admin-travel" "admin-user" "assurance" "auth" "basic" "cancel" "config" "consign-price" "consign" "contacts" "execute" "food-map" "food" "inside-payment" "notification" "order-other" "order" "payment" "preserve-other" "preserve" "price" "rebook" "route-plan" "route" "seat" "security" "station" "ticketinfo" "train" "travel2" "travel-plan" "travel" "user" "verification-code")

# Build Docker images for each service
for service in "${services[@]}"
do
    cd "ts-${service}-service"
    echo "Building Docker image for service: ${service}"
    docker build -t "local/ts-${service}-service:0.1" .
    cd ..
done