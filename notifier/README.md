# BloodConnect notifier

Watches Firestore `blood_requests`. When a NEW pending request appears, it sends an FCM
data message to the topics of every compatible donor group (`bg_O_POS`, `bg_A_NEG`, ...).
The Android app subscribes to the topic of its user's own blood group and filters by
distance (30 km) on the device.

## One-time setup
1. Install Node.js LTS (https://nodejs.org) and check: `node -v`
2. Firebase Console -> Project settings -> Service accounts -> **Generate new private key**.
   Save the JSON OUTSIDE the project, e.g. `C:\Users\Dell\keys\bloodconnect-admin.json`.
   NEVER commit it (your GitHub repo is public).
3. In this folder: `npm install`
4. `npm test` (should print "All notifier logic tests passed.")

## Run (PowerShell)
```
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\Users\Dell\keys\bloodconnect-admin.json"
node index.js
```
Keep the window open. Create a request in the app -> you should see `[sent] ...` lines.

## Notes
- Requests created before the notifier started are ignored. A `notifiedAt` field is written
  to each request so restarts never double-send.
- Add to the repo `.gitignore`:
  ```
  *firebase-adminsdk*.json
  *service-account*.json
  /notifier/node_modules
  ```
