#!/bin/bash

# Define resource names
ANALYTICS_QUEUE_NAME="analytics-process-workout"
RECOMMENDATION_QUEUE_NAME="recommendation-process-workout"
ANALYTICS_NOTIF_QUEUE_NAME="analytics-to-notification"
REC_NOTIF_QUEUE_NAME="recommendation-to-notification"
TOPIC_NAME="workout-complete"
REGION="us-east-1" # Default LocalStack region

echo "Creating SQS queue: $ANALYTICS_QUEUE_NAME..."
ANALYTICS_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$ANALYTICS_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $ANALYTICS_QUEUE_URL"
ANALYTICS_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$ANALYTICS_QUEUE_URL" --attribute-names QueueArn --query 'Attributes.QueueArn' --region "$REGION" --output text)
echo "Queue ARN: $ANALYTICS_QUEUE_ARN"

echo "Creating SQS queue: $RECOMMENDATION_QUEUE_NAME..."
RECOMMENDATION_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$RECOMMENDATION_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $RECOMMENDATION_QUEUE_URL"
RECOMMENDATION_QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$RECOMMENDATION_QUEUE_URL" --attribute-names QueueArn --query 'Attributes.QueueArn'  --region "$REGION" --output text)
echo "Queue ARN: $ANALYTICS_QUEUE_ARN"

echo "Creating SQS queue: $ANALYTICS_NOTIF_QUEUE_NAME..."
ANALYTICS_NOTIF_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$ANALYTICS_NOTIF_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $QUEUE_URL"

echo "Creating SQS queue: $REC_NOTIF_QUEUE_NAME..."
REC_NOTIF_QUEUE_URL=$(awslocal sqs create-queue --queue-name "$REC_NOTIF_QUEUE_NAME" --query 'QueueUrl' --output text --region "$REGION")
echo "SQS Queue URL: $QUEUE_URL"

echo "Creating SNS topic: $TOPIC_NAME..."
TOPIC_ARN=$(awslocal sns create-topic --name "$TOPIC_NAME" --query 'TopicArn' --output text --region "$REGION")
echo "SNS Topic ARN: $TOPIC_ARN"

echo "Subscribing SQS queue to SNS topic..."
awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$ANALYTICS_QUEUE_ARN" \
  --region "$REGION"

echo "Setup complete. SQS queue '$ANALYTICS_QUEUE_NAME' is subscribed to SNS topic '$TOPIC_NAME'."

echo "Subscribing SQS queue to SNS topic..."
awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$RECOMMENDATION_QUEUE_ARN" \
  --region "$REGION"

echo "Setup complete. SQS queue '$RECOMMENDATION_QUEUE_NAME' is subscribed to SNS topic '$TOPIC_NAME'."