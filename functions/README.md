# BloodConnect Firebase Cloud Functions

Serverless Cloud Functions (2nd Gen) triggering on Firestore document creation (`blood_requests/{requestId}`).
Sends data-only FCM emergency notifications directly to matching blood group topics (`bg_O_POS`, `bg_A_NEG`, etc.).

## Deployment Instructions

Run the following commands in your terminal:

```bash
cd functions
npm install
firebase login
firebase deploy --only functions --project bloodconnect-7d5dd
```
