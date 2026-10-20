const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();

// Region must match your Firestore database location (asia-south1 = Mumbai)
const REGION = "asia-south1";

const COMPATIBLE_DONORS = {
  "O-": ["O-"],
  "O+": ["O+", "O-"],
  "A-": ["A-", "O-"],
  "A+": ["A+", "A-", "O+", "O-"],
  "B-": ["B-", "O-"],
  "B+": ["B+", "B-", "O+", "O-"],
  "AB-": ["AB-", "A-", "B-", "O-"],
  "AB+": ["AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"]
};

// Normalizes blood group string ("O+", "o+", "O_POSITIVE", "O_POS", "AB-") to standard "O+" format.
function normalizeGroup(value) {
  if (typeof value !== "string") return null;
  const v = value.trim().toUpperCase().replace(/\s+/g, "");
  const m = v.match(/^(AB|A|B|O)(\+|-|_POS(?:ITIVE)?|_NEG(?:ATIVE)?)$/);
  if (!m) return null;
  const sign = m[2] === "+" || m[2].startsWith("_POS") ? "+" : "-";
  return m[1] + sign;
}

// Converts standard blood group ("O+") to FCM topic name ("bg_O_POS").
function topicFor(group) {
  const g = normalizeGroup(group);
  if (!g) return null;
  return "bg_" + g.slice(0, -1) + (g.endsWith("+") ? "_POS" : "_NEG");
}

function buildPayload(requestId, request, radiusKm) {
  const normalizedGroup = normalizeGroup(request.bloodGroup);
  const urgency = request.urgency || "Routine";
  const hospital = request.hospital || request.hospitalName || "Hospital";
  const city = request.city ? `, ${request.city}` : "";
  const units = request.unitsNeeded || request.unitsRequired || 1;
  const lat = String(Number(request.latitude) || 0);
  const lon = String(Number(request.longitude) || 0);

  return {
    title: `${urgency}: ${normalizedGroup} blood needed`,
    body: `${hospital}${city} - ${units} unit(s)`,
    requestId: String(requestId),
    requesterId: String(request.requesterId || ""),
    bloodGroup: String(normalizedGroup || request.bloodGroup),
    urgency: String(urgency),
    latitude: lat,
    longitude: lon,
    radiusKm: String(radiusKm)
  };
}

async function sendToTopics(requestId, topics, data) {
  const sendResults = await Promise.allSettled(
    topics.map((topic) =>
      admin.messaging().send({
        topic: topic,
        data: data,
        android: {
          priority: "high",
          ttl: 3600000 // 1 hour
        }
      })
    )
  );

  sendResults.forEach((result, idx) => {
    const targetTopic = topics[idx];
    if (result.status === "fulfilled") {
      console.log(`[sent] ${requestId} -> ${targetTopic} (${data.radiusKm} km)`);
    } else {
      const reason = result.reason && (result.reason.message || result.reason.code) || "Unknown error";
      console.error(`[fail] ${requestId} -> ${targetTopic}: ${reason}`);
    }
  });
}

exports.sendEmergencyBloodRequestNotification = onDocumentCreated(
  {
    document: "blood_requests/{requestId}",
    region: REGION
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const request = snap.data();
    const requestId = event.params.requestId;

    if (!request) return;

    const rawStatus = request.status;
    if (typeof rawStatus !== "string" || rawStatus.trim().toLowerCase() !== "pending") {
      return;
    }

    const db = admin.firestore();
    const docRef = snap.ref;
    const now = Date.now();

    const claimed = await db.runTransaction(async (tx) => {
      const docSnap = await tx.get(docRef);
      if (!docSnap.exists || docSnap.get("notifiedAt")) {
        return false;
      }
      tx.update(docRef, {
        notifiedAt: now,
        broadcastStage: 1,
        radiusKm: 10,
        lastBroadcastAt: now
      });
      return true;
    });

    if (!claimed) return;

    const normalizedGroup = normalizeGroup(request.bloodGroup);
    if (!normalizedGroup) {
      console.warn(`[skip] ${requestId}: unknown blood group "${request.bloodGroup}"`);
      return;
    }

    const donorGroups = COMPATIBLE_DONORS[normalizedGroup] || [];
    const topics = donorGroups.map(topicFor).filter(Boolean);

    if (topics.length === 0) {
      console.warn(`[skip] ${requestId}: no compatible topics for group "${normalizedGroup}"`);
      return;
    }

    const payloadData = buildPayload(requestId, request, 10);
    await sendToTopics(requestId, topics, payloadData);
  }
);

// Scheduled cron job running every 5 minutes to widen radius for unpledged requests
exports.widenEmergencyRadius = onSchedule(
  {
    schedule: "every 5 minutes",
    region: REGION,
    timeZone: "Asia/Kolkata"
  },
  async (event) => {
    const db = admin.firestore();
    const now = Date.now();
    const fiveMinutes = 5 * 60 * 1000;
    const ladder = [10, 20, 30];

    try {
      const snapshot = await db.collection("blood_requests")
        .where("status", "in", ["Pending", "Partially Fulfilled"])
        .get();

      if (snapshot.empty) return;

      for (const doc of snapshot.docs) {
        const req = doc.data();
        const requestId = doc.id;
        const stage = req.broadcastStage;
        const lastBroadcast = req.lastBroadcastAt;

        // Skip requests created before the radius feature (no numeric broadcastStage or lastBroadcastAt)
        if (typeof stage !== "number" || typeof lastBroadcast !== "number") {
          continue;
        }

        const expiresAt = req.expiresAt || 0;

        // In-memory filters: stage 1 or 2, at least 5 mins elapsed, not expired
        if ((stage === 1 || stage === 2) && (now - lastBroadcast >= fiveMinutes) && (expiresAt === 0 || expiresAt > now)) {
          // Check pledge count
          const pledgeSnap = await doc.ref.collection("pledges").count().get();
          const pledgeCount = pledgeSnap.data().count;

          if (pledgeCount === 0) {
            const nextStage = stage + 1;
            const nextRadius = ladder[nextStage - 1] || 30;

            let updatedSuccessfully = false;
            let finalRadius = nextRadius;

            await db.runTransaction(async (tx) => {
              const freshSnap = await tx.get(doc.ref);
              if (!freshSnap.exists) return;
              const freshReq = freshSnap.data();
              const freshStage = freshReq.broadcastStage;

              if (freshStage === stage) {
                tx.update(doc.ref, {
                  broadcastStage: nextStage,
                  radiusKm: nextRadius,
                  lastBroadcastAt: now
                });
                updatedSuccessfully = true;
              }
            });

            if (updatedSuccessfully) {
              const normalizedGroup = normalizeGroup(req.bloodGroup);
              if (normalizedGroup) {
                const donorGroups = COMPATIBLE_DONORS[normalizedGroup] || [];
                const topics = donorGroups.map(topicFor).filter(Boolean);
                const payloadData = buildPayload(requestId, req, finalRadius);
                await sendToTopics(requestId, topics, payloadData);
                console.log(`[widen] ${requestId} -> ${finalRadius} km`);
              }
            }
          }
        }
      }
    } catch (e) {
      console.error("[error] widenEmergencyRadius:", e.message);
    }
  }
);
