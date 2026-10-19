"use strict";

/**
 * BloodConnect notifier
 * Watches Firestore "blood_requests" and pushes each NEW pending request to the
 * FCM topics of every compatible donor blood group (e.g. bg_O_POS).
 * Runs on any machine with Node 18+. Free (Spark plan): uses the Admin SDK only.
 *
 * Auth: set GOOGLE_APPLICATION_CREDENTIALS to the path of a service-account JSON.
 */

const admin = require("firebase-admin");
const {
  topicsForRequest,
  isActiveStatus,
  createdMillis,
  buildData,
} = require("./lib");

if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error(
    "Missing GOOGLE_APPLICATION_CREDENTIALS. Point it to your service-account JSON (see README.md)."
  );
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();

const START_TIME = Date.now();
const GRACE_MS = 60 * 1000; // ignore anything created before we started (minus 1 min)

// Atomically claim a request so it is never sent twice (restarts, 2 instances).
async function claim(ref) {
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists || snap.get("notifiedAt")) return false;
    tx.update(ref, { notifiedAt: Date.now() });
    return true;
  });
}

async function handleNewRequest(doc) {
  const d = doc.data();

  if (d.notifiedAt) return;
  if (!isActiveStatus(d.status)) return;

  const created = createdMillis(d);
  if (created === null || created < START_TIME - GRACE_MS) return; // old doc from first snapshot

  const topics = topicsForRequest(d.bloodGroup);
  if (topics.length === 0) {
    console.warn(`[skip] ${doc.id}: unknown blood group "${d.bloodGroup}"`);
    return;
  }

  if (!(await claim(doc.ref))) return;

  const data = buildData(doc.id, d);
  const results = await Promise.allSettled(
    topics.map((topic) =>
      admin.messaging().send({
        topic,
        data,
        android: { priority: "high", ttl: 60 * 60 * 1000 }, // 1 hour
      })
    )
  );

  results.forEach((r, i) => {
    if (r.status === "fulfilled") {
      console.log(`[sent] ${doc.id} -> ${topics[i]} (${r.value})`);
    } else {
      console.error(`[fail] ${doc.id} -> ${topics[i]}: ${r.reason && r.reason.message}`);
    }
  });
}

let unsubscribe = null;

function listen() {
  console.log("Listening for new blood requests... (Ctrl+C to stop)");
  unsubscribe = db.collection("blood_requests").onSnapshot(
    (snapshot) => {
      snapshot.docChanges().forEach((change) => {
        if (change.type === "added") {
          handleNewRequest(change.doc).catch((e) =>
            console.error(`[error] ${change.doc.id}:`, e.message)
          );
        }
      });
    },
    (err) => {
      console.error("Listener error, retrying in 5s:", err.message);
      if (unsubscribe) unsubscribe();
      setTimeout(listen, 5000);
    }
  );
}

listen();

process.on("SIGINT", () => {
  if (unsubscribe) unsubscribe();
  console.log("\nStopped.");
  process.exit(0);
});
