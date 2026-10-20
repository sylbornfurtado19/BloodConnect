# Walkthrough: Cloud Functions FCM Topic Messaging Rewrite

Successfully rewritten `functions/index.js` to dispatch high-priority data-only notifications to matching blood group topics (`bg_O_POS`, `bg_A_NEG`, etc.), enforced transactional idempotency, updated Node 22 package specifications, and configured deployment documentation.

---

## ⚡ 1. Cloud Function Rewrite (`functions/index.js`)
- **Trigger & Region**: Uses `onDocumentCreated` from `firebase-functions/v2/firestore` on `blood_requests/{requestId}` in `asia-south1`.
- **Status & Idempotency**:
  - Validates `request.status` (trimmed, case-insensitive) == `"pending"`.
  - Runs a Firestore transaction to claim `notifiedAt: Date.now()`, ensuring no request receives duplicate notifications upon function retries.
- **Blood Group Normalization & Topic Mapping**:
  - Normalizes input (`"O+"`, `"O_POSITIVE"`, `"o+"` -> `"O+"`).
  - Maps recipient group to compatible donor blood groups and topic strings (`"bg_O_POS"`, `"bg_AB_NEG"`, etc.).
- **Data-Only Multicast Payload**:
  - Calls `admin.messaging().send({ topic, data, android: { priority: "high", ttl: 3600000 } })` via `Promise.allSettled`.
  - All data values formatted as strings (`title`, `body`, `requestId`, `requesterId`, `bloodGroup`, `urgency`, `latitude`, `longitude`).
  - Backward compatibility for `hospital` || `hospitalName` and `unitsNeeded` || `unitsRequired` || `1`.
  - No `notification` key included.
  - Logs `"[sent] <requestId> -> <topic>"` on success or failure error reasons.
- **Cleanup**: Removed all `users` collection queries, `fcmToken`, and distance calculations from backend code (distance filtering stays on client phone).

---

## ⚙️ 2. Package Configuration (`functions/package.json`)
- Updated Node runtime engine to `"22"`.
- Updated dependencies to `"firebase-admin": "^13.0.0"` and `"firebase-functions": "^6.0.0"`.

---

## 🧹 3. Git Ignore & Cleanup
- Added `firebase-debug.log`, `firebase-debug.*.log`, `/functions/node_modules` to `.gitignore`.
- Removed `firebase-debug.log` from git tracking (`git rm --cached firebase-debug.log`).

---

## 📄 4. Deployment Documentation (`functions/README.md`)
- Created `functions/README.md` with the 4 deploy commands:
  1. `cd functions`
  2. `npm install`
  3. `firebase login`
  4. `firebase deploy --only functions --project bloodconnect-7d5dd`

---

## 🧪 Build & Test Results
- **Command Executed:** `./gradlew clean testDebugUnitTest assembleDebug`
- **Result:** **131 unit tests passed successfully (0 failed)** ✅
- **Android Files Changed:** **0** (No Android/Kotlin files modified)
- **Git Commit:** Committed and pushed to `origin/main`.
