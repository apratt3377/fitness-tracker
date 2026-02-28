// Switch to the notification database
db = db.getSiblingDB('notification_db');

// 1. Create the primary collection
db.createCollection('notifications');

// 2. Create an index for fast lookups by user
db.notifications.createIndex({ "user_id": 1 });

// 3. Create a TTL Index (Data Expiration)
// This automatically deletes documents after 30 days (2592000 seconds)
// based on the 'createdAt' field from your screenshot.
db.notifications.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 2592000 });

print("Notification DB initialized with TTL indexes.");