const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_01";
const BOB_UID = "bob_creator_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    title: "Viral Podcast Hook #1",
    youtubeUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    youtubeVideoId: "dQw4w9WgXcQ",
    channelName: "Creator Lab",
    coverThumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    aspectRatio: "RATIO_19_6",
    exportQuality: "PRO_IPHONE_MASTER",
    exportResolution: "RES_4K",
    exportBitrateTier: "MASTER",
    exportFps: 60,
    proClarityEnabled: true,
    targetLengthSec: 60,
    autoCaptionsEnabled: true,
    subtitleStyle: "VIRAL_POP",
    subtitleFontSizeSp: 20,
    subtitleYFraction: 0.78,
    filterPreset: "CINEMATIC_TEAL",
    filterIntensity: 0.85,
    effectPreset: "ZOOM_PULSE",
    effectIntensity: 0.65,
    transitionPreset: "CROSSFADE",
    transitionDurationSec: 0.6,
    musicPreset: "PHONK_DRIVE",
    musicVolume: 0.45,
    clips: [
      {
        id: "clip_1",
        title: "Opening Hook",
        sourceType: "YOUTUBE_SEGMENT",
        sourceUri: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
        durationMs: 60000,
        trimStartMs: 0,
        trimEndMs: 60000,
        viralScore: 95,
        hookReason: "High retention hook",
        youtubeStartLabel: "00:10",
        youtubeEndLabel: "01:10",
      },
    ],
    subtitles: [
      { id: "sub_1", startMs: 0, endMs: 2500, text: "Wait till the end!" },
    ],
    textOverlays: [
      {
        id: "ov_1",
        text: "PRO QUALITY",
        xFraction: 0.5,
        yFraction: 0.2,
        fontSizeSp: 22,
        textColorArgb: -1,
        bgColorArgb: -855687846,
        fontStyle: "BOLD_DISPLAY",
        startMs: 0,
        endMs: 60000,
      },
    ],
    ttsSettings: {
      enabled: false,
      script: "Wait till the end!",
      languageTag: "en-US",
      speechRate: 1.05,
      pitch: 1.0,
      volume: 0.9,
    },
    socialCaptions: {
      viralHook: "Wait till the end!",
      instagramCaption: "Edited in ClipPulse",
      youtubeShortsCaption: "Must watch #Shorts",
      tiktokCaption: "Watch till the end!",
      twitterCaption: "New cut dropped",
      hashtags: ["#Reels", "#Shorts"],
    },
    createdAt,
    updatedAt,
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

test("Unauthenticated user: denied all reads and writes on users & projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID, now, now))
  );
});

test("UserProfile: owner can create, read, update; cannot list users or spoof another UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Creator",
      photoUrl: "https://example.com/alice.jpg",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Owner partial update preserving createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Pro",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Listing /users is forbidden
  await assertFails(aliceDb.collection("users").get());

  // Cross-user read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Projects: owner can create, list, get, partially update, and delete; Kotlin payload parity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const projectRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("550e8400-e29b-41d4-a716-446655440000");

  // Empty subcollection list must succeed without referencing existing()
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );

  // Create matching Kotlin map (NO @DocumentId `id` field inside body)
  await assertSucceeds(projectRef.set(validProjectPayload(ALICE_UID, created, created)));

  // List query matching Kotlin repository (orderBy updatedAt desc)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .orderBy("updatedAt", "desc")
      .get()
  );

  // Partial update preserving immutable userId & createdAt
  await assertSucceeds(
    projectRef.update({
      title: "Updated Viral Cut",
      exportQuality: "FULL_HD_1080P",
      exportResolution: "RES_2K",
      exportBitrateTier: "HIGH",
      exportFps: 30,
      proClarityEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    projectRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId `id` key on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_with_id_field")
      .set({
        ...validProjectPayload(ALICE_UID, created, created),
        id: "proj_with_id_field",
      })
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_future_ts")
      .set(validProjectPayload(ALICE_UID, new Date(Date.now() + 60000), new Date(Date.now() + 60000)))
  );

  // Reject invalid enum values, invalid fps, & oversized title
  await assertFails(
    projectRef.update({
      exportQuality: "INVALID_8K",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    projectRef.update({
      exportResolution: "RES_8K",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    projectRef.update({
      exportFps: 120,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    projectRef.update({
      title: "A".repeat(121),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner delete succeeds
  await assertSucceeds(projectRef.delete());
});

test("Projects: cross-user isolation blocks Bob from reading, listing, modifying, or deleting Alice's projects", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("alice_proj_1")
      .set(validProjectPayload(ALICE_UID, created, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj_1").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("alice_proj_1")
      .update({ title: "Hijacked", updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("alice_proj_1")
      .delete()
  );
});
