const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validDocPayload(uid, timestamp, overrides = {}) {
  return {
    userId: uid,
    title: "main.kt",
    content: "fun main() {\n    println(\"Hello CodePad\")\n}",
    language: "kotlin",
    isPinned: false,
    isOpenTab: true,
    cursorPosition: 0,
    lineEnding: "LF",
    createdAt: timestamp,
    updatedAt: timestamp,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
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

test("1. Unauthenticated user cannot read or list documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("2. Unauthenticated user cannot create documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("doc_1")
      .set(validDocPayload(ALICE_UID, new Date(Date.now() - 1000)))
  );
});

test("3. Authenticated owner can create and get a valid document", async () => {
  const aliceContext = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceContext.firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("doc_1")
      .set(validDocPayload(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("doc_1")
      .get()
  );
});

test("4. Cross-user isolation: Alice cannot read, update, or delete Bob's document", async () => {
  const seedTime = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("documents")
      .doc("bob_doc")
      .set(validDocPayload(BOB_UID, seedTime));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDocRef = aliceDb
    .collection("users")
    .doc(BOB_UID)
    .collection("documents")
    .doc("bob_doc");

  await assertFails(bobDocRef.get());
  await assertFails(
    bobDocRef.update({
      content: "hacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDocRef.delete());
});

test("5. Identity spoofing: Alice cannot create document with Bob's userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("spoof_doc")
      .set(validDocPayload(BOB_UID, new Date(Date.now() - 1000)))
  );
});

test("6. Shadow update test: Ghost field is rejected on create and update", async () => {
  const seedTime = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("ghost_doc")
      .set({
        ...validDocPayload(ALICE_UID, seedTime),
        isAdmin: true,
      })
  );

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("clean_doc")
      .set(validDocPayload(ALICE_UID, seedTime));
  });

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("clean_doc")
      .update({
        title: "updated.kt",
        isVerified: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("7. Value poisoning: Invalid language enum or oversized title fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("bad_lang")
      .set(validDocPayload(ALICE_UID, now, { language: "cobol" }))
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("long_title")
      .set(validDocPayload(ALICE_UID, now, { title: "a".repeat(150) }))
  );
});

test("8. Immutable fields: Cannot mutate createdAt or userId during update", async () => {
  const seedTime = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("imm_doc")
      .set(validDocPayload(ALICE_UID, seedTime));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("imm_doc")
      .update({
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("9. Valid update succeeds when allowed fields change", async () => {
  const seedTime = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("upd_doc")
      .set(validDocPayload(ALICE_UID, seedTime));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("upd_doc")
      .update({
        title: "script.py",
        language: "python",
        content: "print('Updated')",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("10. Client query alignment: Query with userId filter succeeds", async () => {
  const seedTime = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .doc("q_doc")
      .set(validDocPayload(ALICE_UID, seedTime));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("documents")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
