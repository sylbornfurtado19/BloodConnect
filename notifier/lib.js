"use strict";

// Pure helpers (no Firebase dependency) so they can be unit-tested.

// Recipient blood group -> donor groups that can give to them.
const COMPATIBLE_DONORS = {
  "O-": ["O-"],
  "O+": ["O+", "O-"],
  "A-": ["A-", "O-"],
  "A+": ["A+", "A-", "O+", "O-"],
  "B-": ["B-", "O-"],
  "B+": ["B+", "B-", "O+", "O-"],
  "AB-": ["AB-", "A-", "B-", "O-"],
  "AB+": ["AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"],
};

// Accepts "O+", "o+", "O_POSITIVE", "O_POS", "AB-" ... returns "O+" form or null.
function normalizeGroup(value) {
  if (typeof value !== "string") return null;
  const v = value.trim().toUpperCase().replace(/\s+/g, "");
  const m = v.match(/^(AB|A|B|O)(\+|-|_POS(?:ITIVE)?|_NEG(?:ATIVE)?)$/);
  if (!m) return null;
  const sign = m[2] === "+" || m[2].startsWith("_POS") ? "+" : "-";
  return m[1] + sign;
}

// "O+" -> "bg_O_POS" (FCM topic names cannot contain + or -).
function topicFor(group) {
  const g = normalizeGroup(group);
  if (!g) return null;
  return "bg_" + g.slice(0, -1) + (g.endsWith("+") ? "_POS" : "_NEG");
}

// Topics that should receive a request for `recipientGroup`.
function topicsForRequest(recipientGroup) {
  const g = normalizeGroup(recipientGroup);
  if (!g) return [];
  return COMPATIBLE_DONORS[g].map(topicFor);
}

function isActiveStatus(status) {
  if (typeof status !== "string") return false;
  const s = status.trim().toLowerCase();
  return s.startsWith("pending") || s.startsWith("active");
}

// createdAt is epoch millis (Long); timestamp is a Firestore Timestamp.
function createdMillis(data) {
  if (typeof data.createdAt === "number") return data.createdAt;
  if (data.timestamp && typeof data.timestamp.toMillis === "function") {
    return data.timestamp.toMillis();
  }
  return null;
}

// FCM data payload: every value MUST be a string.
function buildData(requestId, d) {
  const urgency = d.urgency || "Routine";
  const bloodGroup = normalizeGroup(d.bloodGroup) || String(d.bloodGroup || "");
  const hospital = d.hospital || d.hospitalName || "a hospital";
  const city = d.city ? `, ${d.city}` : "";
  const units = d.unitsNeeded || d.unitsRequired || 1;
  return {
    title: `${urgency}: ${bloodGroup} blood needed`,
    body: `${hospital}${city} - ${units} unit(s)`,
    requestId: String(requestId),
    requesterId: String(d.requesterId || ""),
    bloodGroup: String(bloodGroup),
    urgency: String(urgency),
    latitude: String(d.latitude || 0),
    longitude: String(d.longitude || 0),
  };
}

module.exports = {
  COMPATIBLE_DONORS,
  normalizeGroup,
  topicFor,
  topicsForRequest,
  isActiveStatus,
  createdMillis,
  buildData,
};
