#!/bin/bash

# Define resource names
ANALYTICS_QUEUE_NAME="analytics-workout-events-queue"
CLEANUP_QUEUE_NAME="cleanup-workout-deletion-queue"
ANALYTICS_NOTIF_QUEUE_NAME="analytics-notification-alert-queue"
TOPIC_NAME="Workout-Domain-Events"
REGION="us-east-1" # Default LocalStack region

echo "Creating SQS queue: $ANALYTICS_QUEUE_NAME..."
ANALYTICS_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$ANALYTICS_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $ANALYTICS_QUEUE_URL"
ANALYTICS_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$ANALYTICS_QUEUE_URL" --attribute-names QueueArn --query 'Attributes.QueueArn' --region "$REGION" --output text)
echo "Queue ARN: $ANALYTICS_QUEUE_ARN"

echo "Creating SQS queue: $CLEANUP_QUEUE_NAME..."
CLEANUP_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$CLEANUP_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $CLEANUP_QUEUE_URL"
CLEANUP_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$CLEANUP_QUEUE_URL" --attribute-names QueueArn --query 'Attributes.QueueArn'  --region "$REGION" --output text)
echo "Queue ARN: $CLEANUP_QUEUE_ARN"

echo "Creating SQS queue: $ANALYTICS_NOTIF_QUEUE_NAME..."
ANALYTICS_NOTIF_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$ANALYTICS_NOTIF_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $ANALYTICS_NOTIF_QUEUE_URL"
ANALYTICS_NOTIF_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$ANALYTICS_NOTIF_QUEUE_URL" --attribute-names QueueArn --query 'Attributes.QueueArn'  --region "$REGION" --output text)
echo "Queue ARN: $ANALYTICS_NOTIF_QUEUE_ARN"

echo "Creating SNS topic: $TOPIC_NAME..."
TOPIC_ARN=$(awslocal sns create-topic --name "$TOPIC_NAME" --query 'TopicArn' --output text --region "$REGION")
echo "SNS Topic ARN: $TOPIC_ARN"

echo "Subscribing SQS queue to SNS topic..."
awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$ANALYTICS_QUEUE_ARN" \
  --region "$REGION" \
  --attributes '{"FilterPolicy": "{\"action\": [\"workout_completed\"]}"}'

echo "Setup complete. SQS queue '$ANALYTICS_QUEUE_NAME' is subscribed to SNS topic '$TOPIC_NAME'."

echo "Subscribing SQS queue to SNS topic..."
awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$CLEANUP_QUEUE_ARN" \
  --region "$REGION" \
  --attributes '{"FilterPolicy": "{\"action\": [\"workout_deleted\"]}"}'

echo "Setup complete. SQS queue '$CLEANUP_QUEUE_NAME' is subscribed to SNS topic '$TOPIC_NAME'."