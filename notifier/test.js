"use strict";
const assert = require("assert");
const L = require("./lib");

// normalize
assert.strictEqual(L.normalizeGroup("O+"), "O+");
assert.strictEqual(L.normalizeGroup(" ab- "), "AB-");
assert.strictEqual(L.normalizeGroup("O_POSITIVE"), "O+");
assert.strictEqual(L.normalizeGroup("A_NEG"), "A-");
assert.strictEqual(L.normalizeGroup("C+"), null);
assert.strictEqual(L.normalizeGroup(null), null);

// topics (must match Android TopicManager: bg_A_POS ... bg_O_NEG, no + or -)
assert.strictEqual(L.topicFor("O+"), "bg_O_POS");
assert.strictEqual(L.topicFor("AB-"), "bg_AB_NEG");
assert.strictEqual(L.topicFor("A+"), "bg_A_POS");
for (const g of Object.keys(L.COMPATIBLE_DONORS)) {
  assert.ok(/^bg_[A-Z]+_(POS|NEG)$/.test(L.topicFor(g)), g);
}

// compatibility (donor must be able to give to recipient)
assert.deepStrictEqual(L.topicsForRequest("O-"), ["bg_O_NEG"]);
assert.deepStrictEqual(L.topicsForRequest("A+").sort(), ["bg_A_NEG", "bg_A_POS", "bg_O_NEG", "bg_O_POS"]);
assert.strictEqual(L.topicsForRequest("AB+").length, 8);
assert.deepStrictEqual(L.topicsForRequest("zzz"), []);
// O- donors can give to everyone; AB+ donors only to AB+
const all = Object.keys(L.COMPATIBLE_DONORS);
assert.ok(all.every((r) => L.topicsForRequest(r).includes("bg_O_NEG")));
assert.deepStrictEqual(all.filter((r) => L.topicsForRequest(r).includes("bg_AB_POS")), ["AB+"]);

// status
assert.ok(L.isActiveStatus("Pending"));
assert.ok(L.isActiveStatus("PENDING"));
assert.ok(L.isActiveStatus("Active"));
assert.ok(L.isActiveStatus("Pending (Demo)"));
assert.ok(!L.isActiveStatus("Fulfilled"));
assert.ok(!L.isActiveStatus("Cancelled"));
assert.ok(!L.isActiveStatus(undefined));

// createdAt
assert.strictEqual(L.createdMillis({ createdAt: 123 }), 123);
assert.strictEqual(L.createdMillis({ timestamp: { toMillis: () => 456 } }), 456);
assert.strictEqual(L.createdMillis({}), null);

// payload: all strings
const p = L.buildData("abc", {
  bloodGroup: "O+", urgency: "Critical", hospital: "City Hospital",
  city: "Mumbai", unitsNeeded: 2, requesterId: "u1", latitude: 19.07, longitude: 72.87,
});
assert.ok(Object.values(p).every((v) => typeof v === "string"));
assert.strictEqual(p.title, "Critical: O+ blood needed");
assert.strictEqual(p.body, "City Hospital, Mumbai - 2 unit(s)");
const q = L.buildData("x", { bloodGroup: "A-" });
assert.strictEqual(q.latitude, "0");
assert.strictEqual(q.urgency, "Routine");

console.log("All notifier logic tests passed.");
