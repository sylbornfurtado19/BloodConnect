# BloodConnect Firebase Cloud Functions

Serverless Cloud Functions (2nd Gen) triggering on Firestore document creation (`blood_requests/{requestId}`) and scheduled radius widening via Cloud Scheduler (`widenEmergencyRadius` every 5 minutes).

## Requirements
- Firebase **Blaze Plan** (Pay-as-you-go).
- Cloud Scheduler API (automatically enabled upon deployment on the Blaze plan).

## Deployment Instructions

Run the following commands in your terminal:

```bash
cd functions
npm install
firebase login
firebase deploy --only functions --project bloodconnect-7d5dd
```
