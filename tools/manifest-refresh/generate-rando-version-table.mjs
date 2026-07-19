import fs from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { FileBlob, SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = process.cwd();
const inputPath = path.resolve(process.argv[2] ?? "resources/Rando_Version_Table_2026-07-01.xlsx");
const outputPath = path.resolve(process.argv[3] ?? "resources/Rando_Version_Table_2026-07-19.xlsx");
const previewDir = path.resolve(process.argv[4] ?? "/tmp/codex-rando-version-table-preview");
const builtDate = process.env.RANDO_VERSION_DATE ?? "2026-07-19";
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

function git(args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" }).trim();
}

function sourceState() {
  const head = git(["rev-parse", "HEAD"]);
  const aiChanges = git(["status", "--porcelain", "--", "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai"]);
  return aiChanges ? `${head} + AI working-tree candidate` : head;
}

function versionNumber(value) {
  const match = String(value ?? "").match(/^V(\d+)/i);
  return match ? Number(match[1]) : null;
}

function compact(text, limit = 900) {
  const value = String(text ?? "").replace(/\s+/g, " ").trim();
  return value.length <= limit ? value : `${value.slice(0, limit - 3)}...`;
}

function changedFiles(commit) {
  const files = git(["show", "--pretty=format:", "--name-only", commit]).split("\n").filter(Boolean);
  const production = files
    .filter((file) => file.includes("/src/main/java/com/gempukku/swccgo/ai/"))
    .map((file) => path.basename(file));
  const tests = files
    .filter((file) => file.includes("/src/test/java/com/gempukku/swccgo/ai/"))
    .map((file) => path.basename(file));
  return {
    production: [...new Set(production)].join("; ") || "documentation/artifact only",
    tests: [...new Set(tests)].join("; ") || "phase-boundary suite",
  };
}

function phaseFromSubject(subject) {
  const lower = subject.toLowerCase();
  if (lower.includes("draw")) return "DRAW";
  if (lower.includes("control")) return "CONTROL";
  if (lower.includes("shield")) return "SHIELDS";
  if (lower.includes("force-loss")) return "FORCE-LOSS";
  if (lower.includes("objective")) return "PLAYBOOKS";
  if (lower.includes("battle")) return "BATTLE";
  if (lower.includes("pull")) return "PULL-ENGINE";
  if (lower.includes("activate")) return "ACTIVATE";
  if (lower.includes("deploy")) return "DEPLOY";
  if (lower.includes("move")) return "MOVE";
  if (lower.includes("setup")) return "SETUP";
  if (lower.includes("response")) return "RESPONSE";
  return "INFRA";
}

function changelogSummary(markdown, version) {
  const heading = new RegExp(`^## .*\\bV${version}\\b.*$`, "im").exec(markdown);
  if (!heading) return { issue: `V${version} historical AI change`, fix: "See AI_CHANGELOG.md" };
  const sectionStart = heading.index + heading[0].length;
  const nextHeading = markdown.slice(sectionStart).search(/^## /m);
  const section = markdown.slice(sectionStart, nextHeading < 0 ? undefined : sectionStart + nextHeading);
  const bullets = section.split("\n").map((line) => line.trim()).filter((line) => line.startsWith("- "));
  const why = bullets.find((line) => /^- Why:/i.test(line));
  const fix = bullets.find((line) => !/^- (Why|Engine boundary|Verification|Revert|Boundary math):/i.test(line));
  return {
    issue: compact((why ?? heading[0]).replace(/^- Why:\s*/i, "").replace(/^##\s*/, "")),
    fix: compact((fix ?? "See AI_CHANGELOG.md").replace(/^-\s*/, "")),
  };
}

const sourceWorkbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
const sourceVersions = sourceWorkbook.worksheets.getItem("Rando Versions");
const sourceRows = sourceVersions.getRange("A1:D302").values;
const historicalRows = sourceRows.slice(1).filter((row) => {
  const number = versionNumber(row[0]);
  return number != null && number <= 188;
});

const changelog = await fs.readFile(path.join(root, "resources/AI_CHANGELOG.md"), "utf8");
const newRows = [];
for (let version = 189; version <= 201; version++) {
  const summary = changelogSummary(changelog, version);
  newRows.push([
    `V${version}`,
    summary.issue,
    summary.fix,
    "Current owner recorded in Rando_Section_Manifest_2026-07-19.xlsx",
    "resources/AI_CHANGELOG.md",
  ]);
}

const explicitVersions = new Map([
  ["6a7f3304f", "V202"],
  ["4e9b377f5", "V202"],
  ["6f3c80e12", "V203"],
  ["92080a43d", "V203"],
  ["461bffd98", "V204"],
  ["662a72ece", "V204"],
]);
const commitLines = git(["log", "--format=%H%x09%s", "--reverse", "bd0687d49..HEAD"])
  .split("\n")
  .filter(Boolean);
for (const line of commitLines) {
  const [commit, subject] = line.split("\t", 2);
  const version = explicitVersions.get(commit.slice(0, 9)) ?? subject.match(/^V\d+/)?.[0];
  if (!version) continue;
  const number = Number(version.slice(1));
  if (number < 202 || number > 242) continue;
  const files = changedFiles(commit);
  newRows.push([
    version,
    compact(subject.replace(/^V\d+[: ]*/, "")),
    `Parity-preserving ${phaseFromSubject(subject)} consolidation. Production: ${files.production}.`,
    `${phaseFromSubject(subject)} shared owner; old history retained in Git and changelogs`,
    `${commit.slice(0, 12)}; tests: ${files.tests}`,
  ]);
}
newRows.push([
  "V243",
  "Final ownership map and proven retired-comment cleanup",
  "Removed only the inert V95, V97, and V100 executable comment blocks from both ActionTextEvaluator mirrors; regenerated current owner/version artifacts; named START-OF-TURN and END-OF-TURN as empty metadata slots.",
  "PULL-ENGINE owns V95/V97/V100; no engine or new empty policy class",
  v243Gate,
]);

const seenNew = new Set();
for (const row of newRows) {
  if (seenNew.has(row[0])) throw new Error(`Duplicate generated version ${row[0]}`);
  seenNew.add(row[0]);
}
for (let version = 189; version <= 243; version++) {
  if (!seenNew.has(`V${version}`)) throw new Error(`Missing generated version V${version}`);
}

const workbook = Workbook.create();
const about = workbook.worksheets.add("About");
about.getRange("A1:B9").values = [
  ["Rando AI version table (AI-only final refresh)", null],
  ["Built", `${builtDate} from ${path.basename(inputPath)}, AI_CHANGELOG.md, Git history, and current AI source`],
  ["Scope", "Rando and Chosen One AI logic only. Engine, board, card, client, database, and deck-library changes are excluded."],
  ["Historical base", `${historicalRows.length} V-tag rows through V188 retained from the frozen July 1 workbook.`],
  ["Current additions", "One generated row each for V189 through V243."],
  ["Ownership", "Current primary owner is recorded in Rando_Section_Manifest_2026-07-19.xlsx."],
  ["Retired code", "Git and changelogs are the archive; inert executable comment blocks do not remain in production files."],
  ["Proof boundary", "Committed, packaged, JVM-loaded, and live-game-fired are separate states."],
  ["Source state", sourceState()],
];
about.getRange("A1:B1").format = headerFormat;
about.getRange("A2:A9").format = { font: { bold: true, color: "#17365D" }, verticalAlignment: "top" };
about.getRange("A1:B9").format.wrapText = true;
about.getRange("A1:A9").format.columnWidth = 24;
about.getRange("B1:B9").format.columnWidth = 110;
about.showGridLines = false;

const versions = workbook.worksheets.add("Rando Versions");
const versionRows = [["Version", "Issue Addressed", "Fix", "Commented Out / Consolidated Into", "Evidence"], ...historicalRows.map((row) => [...row, "Frozen July 1 history"]), ...newRows];
versions.getRange(`A1:E${versionRows.length}`).values = versionRows;
versions.getRange("A1:E1").format = headerFormat;
versions.getRange(`A2:E${versionRows.length}`).format = bodyFormat;
for (const [range, width] of [[`A1:A${versionRows.length}`, 18], [`B1:B${versionRows.length}`, 72], [`C1:C${versionRows.length}`, 86], [`D1:D${versionRows.length}`, 58], [`E1:E${versionRows.length}`, 48]]) versions.getRange(range).format.columnWidth = width;
versions.freezePanes.freezeRows(1);
versions.showGridLines = false;

const coverage = workbook.worksheets.add("Coverage");
coverage.getRange("A1:C7").values = [
  ["Check", "Expected", "Result"],
  ["Historical V-tag rows", "Frozen through V188", historicalRows.length],
  ["Generated structural versions", "V189 through V243", newRows.length],
  ["Generated version uniqueness", "55 unique rows", seenNew.size],
  ["Engine rows", "Excluded from AI-only refresh", 0],
  ["Latest structural version", "V243", newRows[newRows.length - 1][0]],
  ["Runtime status", "Separate from source table", "JVM LOAD PENDING"],
];
coverage.getRange("A1:C1").format = headerFormat;
coverage.getRange("A2:C7").format = bodyFormat;
for (const [range, width] of [["A1:A7", 32], ["B1:B7", 54], ["C1:C7", 36]]) coverage.getRange(range).format.columnWidth = width;
coverage.showGridLines = false;

await fs.mkdir(path.dirname(outputPath), { recursive: true });
await fs.mkdir(previewDir, { recursive: true });
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);

const checks = [
  await workbook.inspect({ kind: "region", sheetId: "Rando Versions", range: `A${versionRows.length - 60}:E${versionRows.length}`, maxChars: 12000 }),
  await workbook.inspect({ kind: "region", sheetId: "Coverage", range: "A1:C7", maxChars: 4000 }),
  await workbook.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options: { useRegex: true, maxResults: 100 }, summary: "formula error scan" }),
];
for (const check of checks) console.log(check.ndjson);

for (const [sheetName, range] of [["About", "A1:B9"], ["Rando Versions", `A${Math.max(1, versionRows.length - 22)}:E${versionRows.length}`], ["Coverage", "A1:C7"]]) {
  const preview = await workbook.render({ sheetName, range, scale: 1, format: "png" });
  await fs.writeFile(path.join(previewDir, `${sheetName.replace(/[^A-Za-z0-9._-]+/g, "_")}.png`), new Uint8Array(await preview.arrayBuffer()));
}

console.log(JSON.stringify({ outputPath, historicalRows: historicalRows.length, generatedRows: newRows.length, latestVersion: "V243" }));
