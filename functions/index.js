const { onDocumentCreated } = require("firebase-functions/v2/firestore");
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

    // Idempotency: Claim request in a Firestore transaction so it is never notified twice
    const db = admin.firestore();
    const docRef = snap.ref;

    const claimed = await db.runTransaction(async (tx) => {
      const docSnap = await tx.get(docRef);
      if (!docSnap.exists || docSnap.get("notifiedAt")) {
        return false;
      }
      tx.update(docRef, { notifiedAt: Date.now() });
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

    const urgency = request.urgency || "Routine";
    const hospital = request.hospital || request.hospitalName || "Hospital";
    const city = request.city ? `, ${request.city}` : "";
    const units = request.unitsNeeded || request.unitsRequired || 1;
    const lat = String(Number(request.latitude) || 0);
    const lon = String(Number(request.longitude) || 0);

    const payloadData = {
      title: `${urgency}: ${normalizedGroup} blood needed`,
      body: `${hospital}${city} - ${units} unit(s)`,
      requestId: String(requestId),
      requesterId: String(request.requesterId || ""),
      bloodGroup: String(normalizedGroup),
      urgency: String(urgency),
      latitude: lat,
      longitude: lon
    };

    const sendResults = await Promise.allSettled(
      topics.map((topic) =>
        admin.messaging().send({
          topic: topic,
          data: payloadData,
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
        console.log(`[sent] ${requestId} -> ${targetTopic}`);
      } else {
        const reason = result.reason && (result.reason.message || result.reason.code) || "Unknown error";
        console.error(`[fail] ${requestId} -> ${targetTopic}: ${reason}`);
      }
    });
  }
);
