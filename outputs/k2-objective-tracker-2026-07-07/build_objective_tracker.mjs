import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const repo = "/Users/steve/gemp-swccg-public";
const outDir = path.join(repo, "outputs/k2-objective-tracker-2026-07-07");
const wipPath = path.join(repo, "resources/Objective_Playbook_Facts_Codex_WIP.json");
const inventoryPath = path.join(repo, "resources/Objective_Blueprint_Inventory_2026-07-07.json");
const outputPath = path.join(outDir, "Objective_Playbook_Tracker_2026-07-07.xlsx");

const wip = JSON.parse(await fs.readFile(wipPath, "utf8"));
const inventory = JSON.parse(await fs.readFile(inventoryPath, "utf8"));

function truncate(value, max = 900) {
  const s = value == null ? "" : String(value);
  return s.length > max ? `${s.slice(0, max - 3)}...` : s;
}

function titleOf(ref) {
  if (!ref || typeof ref !== "object") return "";
  return ref.label || ref.text || ref.title || ref.runtimeFilter || JSON.stringify(ref);
}

function summarizeList(list, maxItems = 4) {
  if (!Array.isArray(list) || list.length === 0) return "";
  return truncate(list.map(titleOf).filter(Boolean).slice(0, maxItems).join("; "));
}

function summarizeObject(value) {
  if (value == null) return "";
  if (typeof value === "string") return truncate(value);
  if (Array.isArray(value)) return summarizeList(value, 5);
  return truncate(JSON.stringify(value));
}

function getChecks(entry) {
  const checks = [];
  for (const item of entry.verification?.forK2ToCheck || []) {
    checks.push({ type: "Current", item });
  }
  for (const item of entry.verification?.priorPilotForK2ToCheck || []) {
    checks.push({ type: "Prior pilot", item });
  }
  return checks;
}

function countMapping(entry) {
  const mapping = entry.randoMapping;
  if (!mapping) return 0;
  if (Array.isArray(mapping)) return mapping.length;
  if (typeof mapping === "object") {
    return Object.values(mapping).reduce((sum, v) => sum + (Array.isArray(v) ? v.length : v ? 1 : 0), 0);
  }
  return 1;
}

const entries = [...wip.entries].sort((a, b) => a.inventoryRow - b.inventoryRow);
const inventoryByBp = new Map(inventory.rows.map((row, i) => [row.frontBp, { ...row, inventoryRow: i }]));

const objectiveRows = entries.map((entry) => {
  const inv = inventoryByBp.get(entry.frontBp) || {};
  const checks = getChecks(entry);
  const sourceCount = entry.verification?.sourceFilesRead?.length || 0;
  return [
    entry.inventoryRow,
    entry.side,
    entry.abbreviation || inv.frontAbbrev || "",
    entry.objectiveTitleFront || inv.frontTitle || "",
    entry.objectiveTitleBack || inv.backTitle || "",
    entry.frontBp,
    entry.backBp,
    inv.isVirtual ? "Y" : "",
    sourceCount >= 2 ? "Source verified" : "Source gap",
    checks.length ? "Needs K-2 review" : "Ready",
    checks.length,
    entry.verification?.confidence || "",
    summarizeList(entry.namedCards, 5),
    summarizeList(entry.sites || entry.locationRequirements, 4),
    summarizeList(entry.characters || entry.characterRequirements, 4),
    summarizeList(entry.pullChain || entry.pullOrDeployActions, 4),
    summarizeObject(entry.flip || entry.flipRequirements),
    summarizeObject(entry.flipBack),
    countMapping(entry),
    truncate((entry.verification?.forK2ToCheck || [])[0] || ""),
  ];
});

const actionRows = [];
for (const entry of entries) {
  for (const check of getChecks(entry)) {
    actionRows.push([
      entry.inventoryRow,
      entry.side,
      entry.abbreviation || "",
      entry.frontBp,
      entry.objectiveTitleFront || "",
      check.type,
      truncate(check.item, 1200),
    ]);
  }
}

const issueRows = [
  ["HIGH", "K2 draft rows40-57", "13_73 duplicated", "K-2 WIP has 18 entries but duplicates 13_73, displacing 112_15.", "Open in K-2 draft, Codex WIP corrected"],
  ["HIGH", "K2 draft rows40-57", "112_15 missing", "Inventory row 45 is My Kind Of Scum. Codex WIP includes 112_15; K-2 draft omitted it.", "Open in K-2 draft, Codex WIP corrected"],
  ["RESOLVED", "Codex WIP row 56", "226_12 Special Edition Bespin id", "Corrected [Special Edition] Bespin from 5_164 to 223_8 because Java requires Icon.SPECIAL_EDITION.", "Fixed in Codex WIP and rows54_57 shard"],
  ["CAVEAT", "All rows", "Candidate lists are snapshots", "Runtime Filters from Java remain the source of truth. Candidate IDs are DB/source snapshots for Rando playbook support.", "Expected"],
];

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Summary");
const tracker = workbook.worksheets.add("Objective Tracker");
const objectives = workbook.worksheets.add("Objective Details");
const actionItems = workbook.worksheets.add("K2 Check Queue");
const issues = workbook.worksheets.add("Verifier Notes");

const darkBody = {
  fill: "#2F343D",
  font: { color: "#FFFFFF" },
};
const darkHeader = {
  fill: "#111827",
  font: { bold: true, color: "#FFFFFF" },
};
const darkPanel = {
  fill: "#3B414C",
  font: { bold: true, color: "#FFFFFF" },
};
const subtleBorder = { preset: "inside", style: "thin", color: "#4B5563" };

for (const sheet of [summary, tracker, objectives, actionItems, issues]) {
  sheet.showGridLines = false;
}

const total = entries.length;
const light = entries.filter((e) => e.side === "LIGHT").length;
const dark = entries.filter((e) => e.side === "DARK").length;
const checkTotal = actionRows.length;
const needsReview = entries.filter((e) => getChecks(e).length > 0).length;

summary.getRange("A1:F1").values = [["Objective Playbook Tracker", "", "", "", "", ""]];
summary.getRange("A1:F1").merge();
summary.getRange("A1:F16").format = darkBody;
summary.getRange("A1").format = {
  fill: "#111827",
  font: { bold: true, color: "#FFFFFF", size: 16 },
};
summary.getRange("A3:B10").values = [
  ["Generated", "2026-07-07"],
  ["Source JSON", "resources/Objective_Playbook_Facts_Codex_WIP.json"],
  ["Inventory", "resources/Objective_Blueprint_Inventory_2026-07-07.json"],
  ["Total objectives", total],
  ["Light objectives", light],
  ["Dark objectives", dark],
  ["Objectives needing K-2 review", needsReview],
  ["K-2 check items", checkTotal],
];
summary.getRange("A3:B10").format = darkBody;
summary.getRange("A3:A10").format = darkPanel;
summary.getRange("A12:E16").values = [
  ["Severity", "Where", "Issue", "Summary", "Status"],
  ...issueRows,
];
summary.getRange("A12:E16").format = darkBody;
summary.getRange("A12:E12").format = darkHeader;
summary.getRange("A12:E16").format.borders = subtleBorder;
summary.getRange("A3:B10").format.borders = subtleBorder;
summary.getRange("A:A").format.columnWidth = 26;
summary.getRange("B:B").format.columnWidth = 72;
summary.getRange("C:E").format.columnWidth = 34;
summary.getRange("A12:E16").format.wrapText = true;

const trackerHeaders = [
  "Row", "Side", "Abbrev", "Front Objective", "Back Objective", "Front BP", "Back BP",
  "Virtual", "Source", "Review", "K2 Checks", "Top Note",
];
const trackerRows = objectiveRows.map((row) => [
  row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7],
  row[8], row[9], row[10], row[19],
]);
tracker.getRangeByIndexes(0, 0, trackerRows.length + 1, trackerHeaders.length).values = [trackerHeaders, ...trackerRows];
tracker.tables.add(`A1:L${trackerRows.length + 1}`, true, "ObjectiveTrackerTable");
tracker.freezePanes.freezeRows(1);
tracker.getRange(`A1:L${trackerRows.length + 1}`).format = darkBody;
tracker.getRange(`A1:L${trackerRows.length + 1}`).format.borders = subtleBorder;
tracker.getRange("A1:L1").format = darkHeader;
tracker.getRange(`A1:L${trackerRows.length + 1}`).format.wrapText = true;
tracker.getRange("A:A").format.columnWidth = 7;
tracker.getRange("B:C").format.columnWidth = 11;
tracker.getRange("D:E").format.columnWidth = 30;
tracker.getRange("F:G").format.columnWidth = 13;
tracker.getRange("H:K").format.columnWidth = 14;
tracker.getRange("L:L").format.columnWidth = 64;

const objectiveHeaders = [
  "Row", "Side", "Abbrev", "Front Title", "Back Title", "Front BP", "Back BP", "Virtual",
  "Source Status", "Review Status", "K2 Checks", "Confidence", "Named Cards",
  "Locations", "Characters", "Pulls / Deploys", "Flip", "Flip Back", "Rando Map Count", "Top K2 Note",
];
objectives.getRangeByIndexes(0, 0, objectiveRows.length + 1, objectiveHeaders.length).values = [objectiveHeaders, ...objectiveRows];
objectives.tables.add(`A1:T${objectiveRows.length + 1}`, true, "ObjectivesTable");
objectives.freezePanes.freezeRows(1);
objectives.getRange(`A1:T${objectiveRows.length + 1}`).format = darkBody;
objectives.getRange(`A1:T${objectiveRows.length + 1}`).format.borders = subtleBorder;
objectives.getRange("A1:T1").format = darkHeader;
objectives.getRange(`A1:T${objectiveRows.length + 1}`).format.wrapText = true;
objectives.getRange("A:A").format.columnWidth = 7;
objectives.getRange("B:C").format.columnWidth = 11;
objectives.getRange("D:E").format.columnWidth = 28;
objectives.getRange("F:G").format.columnWidth = 13;
objectives.getRange("H:L").format.columnWidth = 15;
objectives.getRange("M:P").format.columnWidth = 34;
objectives.getRange("Q:R").format.columnWidth = 38;
objectives.getRange("S:S").format.columnWidth = 15;
objectives.getRange("T:T").format.columnWidth = 50;

const actionHeaders = ["Row", "Side", "Abbrev", "Front BP", "Objective", "Check Type", "Check Item"];
actionItems.getRangeByIndexes(0, 0, actionRows.length + 1, actionHeaders.length).values = [actionHeaders, ...actionRows];
actionItems.tables.add(`A1:G${actionRows.length + 1}`, true, "K2CheckQueueTable");
actionItems.freezePanes.freezeRows(1);
actionItems.getRange(`A1:G${actionRows.length + 1}`).format = darkBody;
actionItems.getRange(`A1:G${actionRows.length + 1}`).format.borders = subtleBorder;
actionItems.getRange("A1:G1").format = { fill: "#7C2D12", font: { bold: true, color: "#FFFFFF" } };
actionItems.getRange(`A1:G${actionRows.length + 1}`).format.wrapText = true;
actionItems.getRange("A:A").format.columnWidth = 7;
actionItems.getRange("B:D").format.columnWidth = 12;
actionItems.getRange("E:E").format.columnWidth = 28;
actionItems.getRange("F:F").format.columnWidth = 14;
actionItems.getRange("G:G").format.columnWidth = 92;

issues.getRangeByIndexes(0, 0, issueRows.length + 1, 5).values = [["Severity", "Where", "Issue", "Evidence", "Status"], ...issueRows];
issues.tables.add(`A1:E${issueRows.length + 1}`, true, "VerifierNotesTable");
issues.freezePanes.freezeRows(1);
issues.getRange(`A1:E${issueRows.length + 1}`).format = darkBody;
issues.getRange(`A1:E${issueRows.length + 1}`).format.borders = subtleBorder;
issues.getRange("A1:E1").format = darkHeader;
issues.getRange(`A1:E${issueRows.length + 1}`).format.wrapText = true;
issues.getRange("A:A").format.columnWidth = 14;
issues.getRange("B:C").format.columnWidth = 26;
issues.getRange("D:E").format.columnWidth = 60;

const summaryInspect = await workbook.inspect({
  kind: "table",
  range: "Summary!A1:F16",
  include: "values",
  tableMaxRows: 20,
  tableMaxCols: 8,
});
console.log(summaryInspect.ndjson);

const errorScan = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 100 },
  summary: "formula error scan",
});
console.log(errorScan.ndjson);

const previewRanges = {
  Summary: "A1:F16",
  "Objective Tracker": "A1:L28",
  "Objective Details": "A1:T18",
  "K2 Check Queue": "A1:G24",
  "Verifier Notes": "A1:E5",
};
for (const [sheetName, range] of Object.entries(previewRanges)) {
  const preview = await workbook.render({ sheetName, range, scale: 1, format: "png" });
  await fs.writeFile(path.join(outDir, `${sheetName.replaceAll(" ", "_")}.png`), new Uint8Array(await preview.arrayBuffer()));
}

await fs.mkdir(outDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(outputPath);
console.log(`saved ${outputPath}`);
