const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const OWNER_EMAIL = "analynlandingin77@gmail.com";
const ATTACKER_EMAIL = "hacker@evil.com";

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. UNAUTHENTICATED ACCESS
test("1. Unauthenticated user: cannot read /admin_settings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("admin_settings").get());
});

test("2. Unauthenticated user: cannot read /users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("3. Unauthenticated user: cannot create user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice",
    rating: 1500,
    solvedCount: 10,
    currentPuzzleId: 11,
  }));
});

// 2. CROSS-USER ACCESS & ROLE ELEVATION
test("4. Authenticated user (Alice): cannot write to Bob's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).set({
    userId: BOB_UID,
    displayName: "Bob Fake",
    rating: 2000,
    solvedCount: 50,
    currentPuzzleId: 51,
  }));
});

test("5. Authenticated user (Alice): cannot inject 'role' on profile create", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Hacker",
    rating: 1500,
    solvedCount: 10,
    currentPuzzleId: 11,
    role: "admin",
  }));
});

test("6. Authenticated user (Alice): can create own valid profile without role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Player",
    rating: 1500,
    solvedCount: 10,
    currentPuzzleId: 11,
  }));
});

test("7. Authenticated user (Alice): cannot update own profile to add 'role: admin'", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Player",
      rating: 1500,
      solvedCount: 10,
      currentPuzzleId: 11,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    role: "admin",
  }));
});

// 3. FAKE LEADERBOARD SUBMISSIONS
test("8. Authenticated ordinary user (Alice): cannot write to /leaderboard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("leaderboard").doc("fake_entry").set({
    username: "Alice Exploiter",
    rating: 3999,
  }));
});

test("9. Public / unauthenticated user: can read /leaderboard", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("leaderboard").doc("rank_1").set({
      username: "Magnus",
      rating: 2850,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("leaderboard").doc("rank_1").get());
});

// 4. MATCH HISTORY MANIPULATION
test("10. Authenticated user (Alice): cannot forge match history for Bob", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("match_history").doc("match_fake_1").set({
    userId: BOB_UID,
    result: "DEFEAT",
    opponentName: "Alice",
  }));
});

test("11. Authenticated user (Alice): can write own match history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("match_history").doc("match_alice_1").set({
    userId: ALICE_UID,
    result: "VICTORY",
    opponentName: "Challenger 1",
  }));
});

test("12. Authenticated user (Bob): cannot read Alice's match history", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("match_history").doc("match_alice_1").set({
      userId: ALICE_UID,
      result: "VICTORY",
      opponentName: "Challenger 1",
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("match_history").doc("match_alice_1").get());
});

// 5. ADMINISTRATIVE COLLECTIONS
test("13. Ordinary user (Alice): cannot access /admin_settings or /owner_audit", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("admin_settings").doc("config").get());
  await assertFails(aliceDb.collection("admin_settings").doc("config").set({ test: true }));
  await assertFails(aliceDb.collection("owner_audit").doc("log_1").set({ log: "hacked" }));
});

test("14. Attacker with spoofed email header but unverified password auth: cannot access /admin_settings", async () => {
  const attackerDb = testEnv.authenticatedContext("attacker_uid", {
    email: OWNER_EMAIL,
    email_verified: false,
    firebase: { sign_in_provider: "password" },
  }).firestore();

  await assertFails(attackerDb.collection("admin_settings").doc("config").set({ test: true }));
});

test("15. Real App Owner (analynlandingin77@gmail.com via google.com): can write to admin_settings and leaderboard", async () => {
  const ownerDb = testEnv.authenticatedContext("owner_uid_777", {
    email: OWNER_EMAIL,
    firebase: { sign_in_provider: "google.com" },
  }).firestore();

  await assertSucceeds(ownerDb.collection("admin_settings").doc("system_config").set({
    maintenance: false,
    version: "1.0.0",
  }));
  await assertSucceeds(ownerDb.collection("leaderboard").doc("global_rank_1").set({
    username: "Top Grandmaster",
    rating: 2900,
  }));
});

test("16. Unauthorized document deletion: Alice cannot delete Bob's user profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      rating: 1800,
      solvedCount: 20,
      currentPuzzleId: 21,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).delete());
});
