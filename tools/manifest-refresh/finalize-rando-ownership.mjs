import fs from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = process.cwd();
const inputPath = path.resolve(process.argv[2] ?? "resources/Rando_Section_Manifest_2026-07-06.xlsx");
const outputPath = path.resolve(process.argv[3] ?? "resources/Rando_Section_Manifest_2026-07-19.xlsx");
const previewDir = path.resolve(process.argv[4] ?? "/tmp/codex-rando-final-manifest-preview");
const builtDate = process.env.RANDO_MANIFEST_DATE ?? new Date().toISOString().slice(0, 10);
const v243Gate = process.env.V243_GATE_STATUS ?? "PHASE-BOUNDARY GATE PENDING";

const headerFormat = {
  fill: "#17365D",
  font: { bold: true, color: "#FFFFFF" },
  wrapText: true,
  verticalAlignment: "middle",
};
const bodyFormat = {
  font: { color: "#1F2937" },
  wrapText: true,
  verticalAlignment: "top",
  borders: { preset: "inside", style: "thin", color: "#D9E1F2" },
};

async function walk(directory) {
  const entries = await fs.readdir(directory, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...await walk(absolute));
    else files.push(absolute);
  }
  return files;
}

function stripJavaComments(source) {
  let output = "";
  let state = "code";
  for (let index = 0; index < source.length; index++) {
    const current = source[index];
    const next = source[index + 1];
    if (state === "line") {
      if (current === "\n") {
        output += current;
        state = "code";
      } else {
        output += " ";
      }
      continue;
    }
    if (state === "block") {
      if (current === "*" && next === "/") {
        output += "  ";
        index++;
        state = "code";
      } else {
        output += current === "\n" ? "\n" : " ";
      }
      continue;
    }
    if (state === "string" || state === "char") {
      output += current;
      if (current === "\\" && next != null) {
        output += next;
        index++;
      } else if ((state === "string" && current === "\"") || (state === "char" && current === "'")) {
        state = "code";
      }
      continue;
    }
    if (current === "/" && next === "/") {
      output += "  ";
      index++;
      state = "line";
    } else if (current === "/" && next === "*") {
      output += "  ";
      index++;
      state = "block";
    } else {
      output += current;
      if (current === "\"") state = "string";
      else if (current === "'") state = "char";
    }
  }
  return output;
}

function versionKey(value) {
  const match = String(value ?? "").match(/^V\d+(?:\.\d+)?[A-Za-z]*/i);
  return match ? match[0].toUpperCase() : "";
}

function basenameWithoutExtension(file) {
  return path.basename(file, path.extname(file));
}

function ownerBasenames(grepHits) {
  return [...String(grepHits ?? "").matchAll(/([A-Za-z0-9_]+\.java):\d+/g)].map((match) => match[1]);
}

const sectionPatterns = new Map([
  ["SETUP", [/common\/phase\/SetupPolicy\.java$/]],
  ["ACTIVATE", [/common\/phase\/Activate(?:Action|Amount)Policy\.java$/]],
  ["CONTROL", [/common\/phase\/Control(?:ActionPolicy|DrainAssessment)\.java$/]],
  ["DEPLOY-1", [/common\/phase\/Deploy(?:Budget|Plan|Sequencing)Policy\.java$/]],
  ["DEPLOY-2", [/common\/phase\/Deploy(?:FormationSiting|ObjectiveSiting|Siting|Tactical)Policy\.java$/]],
  ["DEPLOY-3", [/common\/phase\/Deploy(?:PilotShip|Weapon)Policy\.java$/]],
  ["BATTLE-1", [/common\/phase\/BattleDecisionPolicy\.java$/]],
  ["BATTLE-2", [/common\/phase\/BattleWeaponsPolicy\.java$/]],
  ["BATTLE-3", [/common\/phase\/BattleForfeitPolicy\.java$/]],
  ["MOVE", [/common\/phase\/Move[A-Za-z]+Policy\.java$/]],
  ["DRAW", [/common\/phase\/DrawPhasePolicy\.java$/]],
  ["PULL-ENGINE", [/common\/phase\/Pull[A-Za-z]+Policy\.java$/]],
  ["FORCE-LOSS", [/common\/phase\/ForceLossPolicy\.java$/]],
  ["SHIELDS", [/common\/phase\/ShieldPolicy\.java$/]],
  ["RESPONSE", [/common\/phase\/ResponsePolicy\.java$/]],
  ["PLAYBOOKS", [/common\/strategy\/ObjectiveAnalyzer\.java$/, /common\/playbook\/ObjectiveProgressAssessment\.java$/]],
  ["SVC-INTEL", [/common\/strategy\/ObjectiveAnalyzer\.java$/, /rando\/strategy\/DeckOracle\.java$/]],
  ["SVC-SAFETY", [/common\/strategy\/(?:FormationSafety|ForceReserveService)\.java$/, /rando\/DecisionSafety\.java$/]],
  ["TARGETING", [/common\/phase\/BattleTargetResolver\.java$/, /rando\/evaluators\/CardSelectionEvaluator\.java$/]],
  ["INFRA", [/rando\/evaluators\/CombinedEvaluator\.java$/]],
]);

const defaultOwners = new Map([
  ["SETUP", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/SetupPolicy.java"],
  ["ACTIVATE", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ActivateActionPolicy.java"],
  ["CONTROL", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ControlActionPolicy.java"],
  ["DEPLOY-1", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DeploySequencingPolicy.java"],
  ["DEPLOY-2", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DeploySitingPolicy.java"],
  ["DEPLOY-3", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DeployPilotShipPolicy.java"],
  ["BATTLE-1", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleDecisionPolicy.java"],
  ["BATTLE-2", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleWeaponsPolicy.java"],
  ["BATTLE-3", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleForfeitPolicy.java"],
  ["MOVE", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/MoveDestinationPolicy.java"],
  ["DRAW", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DrawPhasePolicy.java"],
  ["PULL-ENGINE", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/PullActionPolicy.java"],
  ["FORCE-LOSS", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ForceLossPolicy.java"],
  ["SHIELDS", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ShieldPolicy.java"],
  ["RESPONSE", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ResponsePolicy.java"],
  ["PLAYBOOKS", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/ObjectiveAnalyzer.java"],
  ["SVC-INTEL", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/ObjectiveAnalyzer.java"],
  ["SVC-SAFETY", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/FormationSafety.java"],
  ["TARGETING", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleTargetResolver.java"],
  ["INFRA", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CombinedEvaluator.java"],
]);

const baselineRows = [
  ["V194", "DRAW recovery", "DrawEvaluator; RandoCalAi; TheChosenOneAi; HeuristicAiBase", "DrawReserveAssessment; DrawReserveLegacyReader", "direct reserve amount plus legacy adapter branches", "DrawReserveAssessmentTest; DrawReserveLegacyReaderTest; PassEvaluatorIntegerGuardTest; AbstractBattleActionTextParityTest", "PARITY BASELINE", "Extract remaining DRAW arithmetic and keep failed-search/reset ownership explicit."],
  ["V195", "ACTIVATE + CONTROL", "ForceActivationEvaluator; ActionTextEvaluator", "ActivateAmountPolicy; ControlDrainAssessment; ControlDrainFacts", "direct amount plus ordered additive operations", "ActivateAmountPolicyTest; ControlDrainAssessmentTest", "PARITY BASELINE", "Move remaining ACTIVATE/CONTROL recognition and rule ownership into phase packages."],
  ["V196", "DEPLOY identity", "DeployEvaluator; DeployPhasePlanner; DeploymentPlan", "DeploymentInstruction physical identity", "facts only", "DeploymentPlanAssessmentCopyPurityTest; DeploymentPlanPhysicalIdentityTest", "PARITY BASELINE", "DEPLOY scoring remains distributed across planner and evaluators."],
  ["V197", "ACTIVATE routing", "RandoCalAi; TheChosenOneAi; ForceActivationEvaluator", "ActivateDecisionRouting", "AI-only latch and label-first response", "ActivateDecisionRoutingTest; ForceActivationRoutingParityTest; RandoCalAiTraceHookTest; TheChosenOneAiTraceHookTest", "PARITY BASELINE", "Activation lifecycle and intent still span entry points and evaluators."],
  ["V198", "BATTLE facts", "BattleEvaluator; ActionTextEvaluator", "BattleTargetResolver; BattleWeaponProfile", "facts only", "BattleTargetResolverTest; BattleWeaponProfileTest", "PARITY BASELINE", "BATTLE-1, BATTLE-2, and BATTLE-3 rule arithmetic remains split."],
  ["V199", "MOVE facts", "CardSelectionEvaluator", "MovePhysicalCardResolver", "facts only", "MovePhysicalCardResolverTest", "PARITY BASELINE", "MOVE ladder, parent, destination, and overlays remain split."],
  ["V200", "OBJECTIVE sides", "ObjectiveAnalyzer", "ObjectiveSideBlueprints", "facts only; enables existing back-side rules", "ObjectiveAnalyzerSideParsingTest; ObjectiveSideBlueprintsTest; live replay 8dzc0wa0yd7t2pt1", "G8 PASS 2026-07-18", "Migrate objective rule arms toward facts-only playbook ownership without changing scores."],
  ["V201", "DEPLOY safety", "ActionTextEvaluator; CardSelectionEvaluator; CombinedEvaluator; EvaluatedAction; DeploymentPlan", "FormationSafety deploy verdict", "structural ADMISSIBLE / DEFER / HARD_BLOCK", "CombinedEvaluatorTieTest; DeploymentPlanCompanionTest; FormationSafetyDeployTest", "PARITY BASELINE", "Preserve V172 dominant-solo admissibility while consolidating remaining deploy callers."],
];

function applicationMode(kind) {
  if (kind === "VETO") return "veto contract (structural where extracted; preserved additive otherwise)";
  if (kind === "ORDERING") return "ordered policy operation";
  return "banded score operation";
}

function ownershipStatus(selection) {
  if (selection.evidence === "active shared tag") return "LIVE; SHARED OWNER CANDIDATE";
  if (selection.evidence === "retained primary scorer") return "LIVE; LEGACY OWNER AUDIT";
  if (selection.evidence === "section fallback") return "REVIEW; SECTION FALLBACK";
  return "REVIEW; OWNER UNRESOLVED";
}

function selectOwner(section, tag, activeFiles, historicalOwner, productionFileSet) {
  const patterns = sectionPatterns.get(section) ?? [];
  const exact = [...(activeFiles.get(tag) ?? [])].filter((file) => patterns.some((pattern) => pattern.test(file)));
  exact.sort((left, right) => {
    const leftPolicy = left.endsWith("Policy.java") ? 0 : 1;
    const rightPolicy = right.endsWith("Policy.java") ? 0 : 1;
    return leftPolicy - rightPolicy || left.localeCompare(right);
  });
  const retainedOwner = String(historicalOwner ?? "")
    .split(";")
    .map((file) => file.trim())
    .find((file) => productionFileSet.has(file));
  if (exact.length > 0) return { owner: exact[0], evidence: "active shared tag", candidates: exact };
  if (retainedOwner) return { owner: retainedOwner, evidence: "retained primary scorer", candidates: [] };
  const defaultOwner = defaultOwners.get(section);
  if (defaultOwner) return { owner: defaultOwner, evidence: "section fallback", candidates: [] };
  return { owner: "OWNER REVIEW REQUIRED", evidence: "unresolved", candidates: [] };
}

function git(args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" }).trim();
}

function sourceState() {
  const head = git(["rev-parse", "HEAD"]);
  const aiChanges = git(["status", "--porcelain", "--", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai"]);
  return aiChanges ? `${head} + AI working-tree candidate` : head;
}

function changedFiles(commit) {
  const files = git(["show", "--pretty=format:", "--name-only", commit]).split("\n").filter(Boolean);
  const production = files.filter((file) => file.includes("/src/main/java/com/gempukku/swccgo/ai/"));
  const tests = files.filter((file) => file.includes("/src/test/java/com/gempukku/swccgo/ai/"));
  return {
    production: production.map((file) => path.basename(file)).join("; ") || "documentation/artifact only",
    tests: tests.map((file) => path.basename(file)).join("; ") || "phase-boundary suite",
  };
}

const sourceJsonPath = process.env.RANDO_MANIFEST_SOURCE_JSON;
const auditOnly = process.argv.includes("--audit-json");
let FileBlob;
let SpreadsheetFile;
let Workbook;
let historicalRows;
if (sourceJsonPath) {
  historicalRows = JSON.parse(await fs.readFile(path.resolve(sourceJsonPath), "utf8"));
  if (!auditOnly) ({ FileBlob, SpreadsheetFile, Workbook } = await import("@oai/artifact-tool"));
} else {
  ({ FileBlob, SpreadsheetFile, Workbook } = await import("@oai/artifact-tool"));
  const sourceWorkbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
  historicalRows = sourceWorkbook.worksheets.getItem("Manifest").getRange("A1:H341").values;
}
if (historicalRows.length !== 341) throw new Error(`Expected 341 manifest rows, found ${historicalRows.length}`);

const productionRoot = path.join(root, "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models");
const productionFiles = (await walk(productionRoot))
  .filter((file) => file.endsWith(".java") && !file.includes("/chosenone/"));
const productionFileSet = new Set(productionFiles.map((file) => path.relative(root, file)));
const productionFilesByBasename = new Map();
for (const file of productionFileSet) {
  const name = path.basename(file);
  if (!productionFilesByBasename.has(name)) productionFilesByBasename.set(name, []);
  productionFilesByBasename.get(name).push(file);
}
const activeFiles = new Map();
for (const file of productionFiles) {
  const relative = path.relative(root, file);
  const source = await fs.readFile(file, "utf8");
  const extractedOwner = relative.includes("/models/common/phase/")
    || relative.includes("/models/common/playbook/")
    || relative.endsWith("/models/common/strategy/ObjectiveAnalyzer.java")
    || relative.endsWith("/models/common/strategy/FormationSafety.java")
    || relative.endsWith("/models/common/strategy/ForceReserveService.java")
    || relative.endsWith("/models/rando/strategy/DeckOracle.java");
  const code = extractedOwner ? source : stripJavaComments(source);
  for (const match of code.matchAll(/\bV\d+(?:\.\d+)?[A-Za-z]*\b/g)) {
    const tag = match[0].toUpperCase();
    if (!activeFiles.has(tag)) activeFiles.set(tag, new Set());
    activeFiles.get(tag).add(relative);
  }
}

const manifestRows = [[...historicalRows[0], "Current owner file", "Extracted policy", "Application mode", "Fixture ID", "Retirement status"]];
const ownershipAudit = [];
for (const historical of historicalRows.slice(1)) {
  const row = [...historical];
  const section = String(row[2] ?? "").toUpperCase();
  const tag = versionKey(row[0] || row[1]);
  const historicalOwner = ownerBasenames(row[5])
    .flatMap((name) => productionFilesByBasename.get(name) ?? [])
    .find((file) => productionFileSet.has(file));
  const selection = selectOwner(section, tag, activeFiles, historicalOwner, productionFileSet);
  const owner = selection.owner;
  ownershipAudit.push({ arm: row[0], tag, section, owner, evidence: selection.evidence, candidates: selection.candidates });
  row[8] = owner;
  row[9] = selection.evidence === "active shared tag"
    ? basenameWithoutExtension(owner)
    : selection.evidence === "retained primary scorer"
      ? "NOT YET EXTRACTED"
      : "REVIEW REQUIRED";
  row[10] = applicationMode(String(row[3] ?? ""));
  row[11] = row[11] || "phase parity suite";
  row[12] = ownershipStatus(selection);
  manifestRows.push(row);
}

const unresolvedOwnerCount = ownershipAudit.filter((row) => row.evidence === "unresolved").length;
if (unresolvedOwnerCount !== 0) throw new Error(`Manifest has ${unresolvedOwnerCount} unresolved owners`);

const commitLines = git(["log", "--format=%H%x09%s", "--reverse", "bd0687d49..HEAD"])
  .split("\n")
  .filter(Boolean);
const explicitVersions = new Map([
  ["6a7f3304f", "V202"],
  ["4e9b377f5", "V202"],
  ["6f3c80e12", "V203"],
  ["92080a43d", "V203"],
  ["461bffd98", "V204"],
  ["662a72ece", "V204"],
]);
const consolidationRows = baselineRows.map((row) => [...row]);
for (const line of commitLines) {
  const [commit, subject] = line.split("\t", 2);
  const version = explicitVersions.get(commit.slice(0, 9)) ?? subject.match(/^V\d+/)?.[0];
  if (!version || Number(version.slice(1)) < 202 || Number(version.slice(1)) > 242) continue;
  const files = changedFiles(commit);
  consolidationRows.push([
    version,
    subject.replace(/^V\d+[: ]*/, ""),
    files.production,
    "shared AI phase policy or verified retirement",
    "parity-preserving consolidation",
    files.tests,
    "COMMITTED; VERIFIED; JVM LOAD PENDING",
    "Runtime fire proof after safe reload",
  ]);
}
consolidationRows.push([
  "V243",
  "ownership audit tooling and retired-comment cleanup",
  "ActionTextEvaluator comments; generated audit artifacts",
  "reproducible ownership candidate audit and version table",
  "comments-only production cleanup",
  "mirror parity; source boundary; full reactor; package artifact",
  v243Gate,
  "Runtime reload only; no strategy changes",
]);

if (auditOnly) {
  console.log(JSON.stringify({
    ruleArms: manifestRows.length - 1,
    unresolvedOwnerCount,
    legacyOwnerAuditCount: ownershipAudit.filter((row) => row.evidence === "retained primary scorer").length,
    sectionFallbackReviewCount: ownershipAudit.filter((row) => row.evidence === "section fallback").length,
    consolidationVersions: consolidationRows.length,
    evidenceCounts: Object.fromEntries([...new Set(ownershipAudit.map((row) => row.evidence))]
      .map((evidence) => [evidence, ownershipAudit.filter((row) => row.evidence === evidence).length])),
    retainedBySection: Object.fromEntries([...new Set(ownershipAudit.map((row) => row.section))]
      .map((section) => [section, ownershipAudit.filter((row) => row.section === section && row.evidence === "retained primary scorer").length])),
    retainedArms: ownershipAudit.filter((row) => row.evidence === "retained primary scorer"),
    ambiguousActive: ownershipAudit.filter((row) => row.candidates.length > 1),
    sectionFallbacks: ownershipAudit.filter((row) => row.evidence === "section fallback"),
  }, null, 2));
  process.exit(0);
}

const workbook = Workbook.create();

const about = workbook.worksheets.add("About");
about.getRange("A1:B10").values = [
  ["Rando ownership audit candidate", null],
  ["Built", `${builtDate} from ${path.basename(inputPath)} and current AI source`],
  ["Rule arms", "340 historical manifest rows. Shared-owner, legacy-owner, fallback, and unresolved states remain visibly distinct until migration is complete."],
  ["Current scope", "Rando and Chosen One AI only. No engine, board, card, client, database, or deck-library ownership."],
  ["Comment handling", "Legacy evaluator comments are stripped before V-tag indexing, so breadcrumbs never become owners. Shared policy comments may supply ownership tags within their section."],
  ["Named slots", "START-OF-TURN and END-OF-TURN are recorded as empty metadata slots. No policy classes were invented."],
  ["Retirement", "V95, V97, and V100 executable comment blocks were replaced by one-line breadcrumbs after shared PULL ownership proof."],
  ["Historical files", "The July 6 source manifest remains frozen and unchanged. This workbook is generated output."],
  ["Runtime status", "Committed source and packaged artifacts are separate from JVM load and live-game fire proof."],
  ["Source state", sourceState()],
];
about.getRange("A1:B1").format = headerFormat;
about.getRange("A2:A10").format = { font: { bold: true, color: "#17365D" }, verticalAlignment: "top" };
about.getRange("A1:B10").format.wrapText = true;
about.getRange("A1:A10").format.columnWidth = 24;
about.getRange("B1:B10").format.columnWidth = 110;
about.showGridLines = false;

const manifest = workbook.worksheets.add("Manifest");
manifest.getRange(`A1:M${manifestRows.length}`).values = manifestRows;
manifest.getRange("A1:M1").format = headerFormat;
manifest.getRange(`A2:M${manifestRows.length}`).format = bodyFormat;
for (const [range, width] of [
  [`A1:A${manifestRows.length}`, 24], [`B1:B${manifestRows.length}`, 18],
  [`C1:C${manifestRows.length}`, 20], [`D1:D${manifestRows.length}`, 14],
  [`E1:E${manifestRows.length}`, 30], [`F1:F${manifestRows.length}`, 32],
  [`G1:G${manifestRows.length}`, 22], [`H1:H${manifestRows.length}`, 76],
  [`I1:I${manifestRows.length}`, 72], [`J1:J${manifestRows.length}`, 34],
  [`K1:K${manifestRows.length}`, 34], [`L1:L${manifestRows.length}`, 48],
  [`M1:M${manifestRows.length}`, 22],
]) manifest.getRange(range).format.columnWidth = width;
manifest.freezePanes.freezeRows(1);
manifest.showGridLines = false;

const slots = workbook.worksheets.add("Named Slots");
const slotRows = [
  ["Slot", "Runtime shape", "Primary owner", "Scorer", "Status", "Boundary"],
  ["START-OF-TURN", "engine trigger window before ACTIVATE", "NONE", "NONE", "NAMED EMPTY SLOT", "No Phase.START_OF_TURN exists; add policy only when a real AI decision branch exists."],
  ["END-OF-TURN", "engine END_OF_TURN phase/result before BETWEEN_TURNS", "NONE", "NONE", "NAMED EMPTY SLOT", "No AI scoring branch exists; add policy only when a real AI decision branch exists."],
];
slots.getRange("A1:F3").values = slotRows;
slots.getRange("A1:F1").format = headerFormat;
slots.getRange("A2:F3").format = bodyFormat;
for (const [range, width] of [["A1:A3", 24], ["B1:B3", 42], ["C1:C3", 18], ["D1:D3", 18], ["E1:E3", 24], ["F1:F3", 90]]) slots.getRange(range).format.columnWidth = width;
slots.freezePanes.freezeRows(1);
slots.showGridLines = false;

const releases = workbook.worksheets.add("Consolidation 194-243");
const releaseRows = [["Version", "Scope", "Current owner", "Extracted component", "Application mode", "Verification fixtures", "Gate status", "Remaining work"], ...consolidationRows];
releases.getRange(`A1:H${releaseRows.length}`).values = releaseRows;
releases.getRange("A1:H1").format = headerFormat;
releases.getRange(`A2:H${releaseRows.length}`).format = bodyFormat;
for (const [range, width] of [[`A1:A${releaseRows.length}`, 12], [`B1:B${releaseRows.length}`, 34], [`C1:C${releaseRows.length}`, 58], [`D1:D${releaseRows.length}`, 42], [`E1:E${releaseRows.length}`, 34], [`F1:F${releaseRows.length}`, 54], [`G1:G${releaseRows.length}`, 34], [`H1:H${releaseRows.length}`, 46]]) releases.getRange(range).format.columnWidth = width;
releases.freezePanes.freezeRows(1);
releases.showGridLines = false;

const gates = workbook.worksheets.add("Gate Summary");
gates.getRange("A1:C8").values = [
  ["Gate", "Required result", "V243 result"],
  ["Production scope", "Only AI paths; V243 Java edits are comments only", process.env.V243_SCOPE_RESULT ?? "PENDING"],
  ["Forbidden symbols", "Zero production matches", process.env.V243_FORBIDDEN_RESULT ?? "PENDING"],
  ["Bot mirror", "Normalized Rando/Chosen One source parity", process.env.V243_MIRROR_RESULT ?? "PENDING"],
  ["Full reactor", "Zero failures and errors", process.env.V243_TEST_RESULT ?? "PENDING"],
  ["Package", "Async reactor package succeeds", process.env.V243_PACKAGE_RESULT ?? "PENDING"],
  ["Artifact bytes", "Server and web JAR SHA-256 captured", process.env.V243_ARTIFACT_RESULT ?? "PENDING"],
  ["Runtime", "Reload only when Docker is available and hall is empty", "JVM LOAD PENDING"],
];
gates.getRange("A1:C1").format = headerFormat;
gates.getRange("A2:C8").format = bodyFormat;
for (const [range, width] of [["A1:A8", 24], ["B1:B8", 74], ["C1:C8", 60]]) gates.getRange(range).format.columnWidth = width;
gates.showGridLines = false;

await fs.mkdir(path.dirname(outputPath), { recursive: true });
await fs.mkdir(previewDir, { recursive: true });
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);

const checks = [
  await workbook.inspect({ kind: "region", sheetId: "Manifest", range: "A1:M12", maxChars: 7000 }),
  await workbook.inspect({ kind: "region", sheetId: "Named Slots", range: "A1:F3", maxChars: 4000 }),
  await workbook.inspect({ kind: "region", sheetId: "Consolidation 194-243", range: `A1:H${releaseRows.length}`, maxChars: 9000 }),
  await workbook.inspect({ kind: "region", sheetId: "Gate Summary", range: "A1:C8", maxChars: 4000 }),
  await workbook.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options: { useRegex: true, maxResults: 100 }, summary: "formula error scan" }),
];
for (const check of checks) console.log(check.ndjson);

for (const [sheetName, range] of [
  ["About", "A1:B10"], ["Manifest", "A1:M24"], ["Named Slots", "A1:F3"],
  ["Consolidation 194-243", `A1:H${releaseRows.length}`], ["Gate Summary", "A1:C8"],
]) {
  const preview = await workbook.render({ sheetName, range, scale: 1, format: "png" });
  await fs.writeFile(path.join(previewDir, `${sheetName.replace(/[^A-Za-z0-9._-]+/g, "_")}.png`), new Uint8Array(await preview.arrayBuffer()));
}

console.log(JSON.stringify({ outputPath, ruleArms: manifestRows.length - 1, unresolvedOwnerCount, consolidationVersions: consolidationRows.length, namedSlots: 2 }));
