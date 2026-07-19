import fs from "node:fs/promises";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = process.cwd();
const base = process.argv[2] ?? "HEAD";
const expected = [
  "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/ActionTextEvaluator.java",
  "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java",
];

function git(args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" });
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
      }
      continue;
    }
    if (state === "block") {
      if (current === "*" && next === "/") {
        index++;
        state = "code";
      } else if (current === "\n") {
        output += "\n";
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
      index++;
      state = "line";
    } else if (current === "/" && next === "*") {
      index++;
      state = "block";
    } else {
      output += current;
      if (current === "\"") state = "string";
      else if (current === "'") state = "char";
    }
  }
  return output.replace(/^[ \t]*\n/gm, "").replace(/[ \t]+$/gm, "");
}

const changedProduction = git(["diff", "--name-only", base, "--", "src"])
  .trim()
  .split("\n")
  .filter(Boolean)
  .sort();
if (JSON.stringify(changedProduction) !== JSON.stringify([...expected].sort())) {
  throw new Error(`Unexpected V243 production paths: ${changedProduction.join(", ")}`);
}

for (const file of expected) {
  const before = git(["show", `${base}:${file}`]);
  const after = await fs.readFile(path.join(root, file), "utf8");
  if (stripJavaComments(before) !== stripJavaComments(after)) {
    throw new Error(`${file} changed executable Java, not comments only`);
  }
  for (const tag of ["V95", "V97", "V100"]) {
    const breadcrumb = new RegExp(`^\\s*// ${tag}:`, "m");
    if (!breadcrumb.test(after)) throw new Error(`${file} is missing the ${tag} breadcrumb`);
  }
  if (/feedback_comment_out_old_rules[\s\S]{0,1200}V(?:95|97|100)/.test(after)) {
    throw new Error(`${file} retains a retired executable comment block`);
  }
}

console.log(JSON.stringify({ base, changedProduction, executableJavaChanged: false, retiredBlocksRemoved: ["V95", "V97", "V100"] }));
