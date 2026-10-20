const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

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

function calculateDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Radius of Earth in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

exports.sendEmergencyBloodRequestNotification = onDocumentCreated(
  {
    document: "blood_requests/{requestId}",
    region: "us-central1"
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const request = snap.data();
    const requestId = event.params.requestId;

    if (!request || request.status !== "Pending") {
      return;
    }

    const recipientGroup = request.bloodGroup;
    const compatibleGroups = COMPATIBLE_DONORS[recipientGroup] || [recipientGroup];

    const db = admin.firestore();
    const usersRef = db.collection("users");

    const groupChunks = [];
    for (let i = 0; i < compatibleGroups.length; i += 30) {
      groupChunks.push(compatibleGroups.slice(i, i + 30));
    }

    const matchingUsersMap = new Map();

    for (const chunk of groupChunks) {
      const querySnap = await usersRef
        .where("isAvailable", "==", true)
        .where("bloodGroup", "in", chunk)
        .get();

      querySnap.forEach((doc) => {
        const user = doc.data();
        const uid = doc.id;

        if (
          uid !== request.requesterId &&
          user.fcmToken &&
          typeof user.fcmToken === "string" &&
          user.fcmToken.trim().length > 0
        ) {
          const reqLat = Number(request.latitude) || 0;
          const reqLon = Number(request.longitude) || 0;
          const userLat = Number(user.latitude) || 0;
          const userLon = Number(user.longitude) || 0;

          if (reqLat !== 0 && reqLon !== 0 && userLat !== 0 && userLon !== 0) {
            const distance = calculateDistanceKm(reqLat, reqLon, userLat, userLon);
            if (distance <= 30) {
              matchingUsersMap.set(uid, { token: user.fcmToken, uid });
            }
          } else {
            matchingUsersMap.set(uid, { token: user.fcmToken, uid });
          }
        }
      });
    }

    const recipients = Array.from(matchingUsersMap.values());
    if (recipients.length === 0) {
      console.log(`No matching donors found within radius for request ${requestId}`);
      return;
    }

    const tokens = recipients.map((r) => r.token);
    const urgency = request.urgency || "Routine";
    const hospital = request.hospital || request.hospitalName || "Hospital";
    const city = request.city ? `, ${request.city}` : "";
    const units = request.unitsNeeded || request.unitsRequired || 1;

    const payload = {
      data: {
        title: `${urgency}: ${recipientGroup} blood needed`,
        body: `${hospital}${city} - ${units} unit(s)`,
        requestId: String(requestId),
        bloodGroup: String(recipientGroup),
        urgency: String(urgency),
        requesterId: String(request.requesterId || "")
      },
      android: {
        priority: "high"
      }
    };

    for (let i = 0; i < tokens.length; i += 500) {
      const batchTokens = tokens.slice(i, i + 500);
      const batchRecipients = recipients.slice(i, i + 500);

      const response = await admin.messaging().sendEachForMulticast({
        tokens: batchTokens,
        ...payload
      });

      response.responses.forEach(async (resp, idx) => {
        if (!resp.success) {
          const errCode = resp.error && resp.error.code;
          if (
            errCode === "messaging/registration-token-not-registered" ||
            errCode === "messaging/invalid-registration-token"
          ) {
            const invalidUid = batchRecipients[idx].uid;
            try {
              await usersRef.doc(invalidUid).update({
                fcmToken: admin.firestore.FieldValue.delete()
              });
              console.log(`Removed invalid token for user ${invalidUid}`);
            } catch (e) {
              console.error(`Failed to delete token for user ${invalidUid}:`, e);
            }
          }
        }
      });
    }

    console.log(`Multicast sent for request ${requestId} to ${tokens.length} donors.`);
  }
);
