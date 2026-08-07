import { readFileSync, writeFileSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");

const BUMP_TYPES = {
  major: "major",
  ma: "major",
  minor: "minor",
  mi: "minor",
  patch: "patch",
  p: "patch",
};

function usage() {
  console.error("Usage: bump <major|ma|minor|mi|patch|p>");
  process.exit(1);
}

const arg = process.argv[2]?.toLowerCase();
const bumpType = BUMP_TYPES[arg];
if (!bumpType) {
  usage();
}

function parseVersion(version) {
  const match = version.match(/^(\d+)\.(\d+)\.(\d+)$/);
  if (!match) {
    throw new Error(`Invalid semver: ${version}`);
  }
  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
  };
}

function bumpVersion(version, type) {
  const v = parseVersion(version);
  switch (type) {
    case "major":
      return `${v.major + 1}.0.0`;
    case "minor":
      return `${v.major}.${v.minor + 1}.0`;
    case "patch":
      return `${v.major}.${v.minor}.${v.patch + 1}`;
    default:
      throw new Error(`Unknown bump type: ${type}`);
  }
}

function replaceRegex(path, pattern, replacement) {
  const content = readFileSync(path, "utf8");
  if (!pattern.test(content)) {
    throw new Error(`${path} does not match expected version pattern`);
  }
  pattern.lastIndex = 0;
  writeFileSync(path, content.replace(pattern, replacement));
}

const androidBuildGradlePath = join(root, "app", "build.gradle.kts");
const androidGradle = readFileSync(androidBuildGradlePath, "utf8");

const versionNameMatch = androidGradle.match(/versionName\s*=\s*"([^"]+)"/);
if (!versionNameMatch) {
  throw new Error(`${androidBuildGradlePath} does not contain versionName`);
}

const versionCodeMatch = androidGradle.match(/versionCode\s*=\s*(\d+)/);
if (!versionCodeMatch) {
  throw new Error(`${androidBuildGradlePath} does not contain versionCode`);
}

const currentVersion = versionNameMatch[1];
const newVersion = bumpVersion(currentVersion, bumpType);
const currentVersionCode = Number(versionCodeMatch[1]);
const newVersionCode = currentVersionCode + 1;

replaceRegex(
  androidBuildGradlePath,
  /versionCode\s*=\s*\d+/,
  `versionCode = ${newVersionCode}`
);

replaceRegex(
  androidBuildGradlePath,
  /versionName\s*=\s*"[^"]+"/,
  `versionName = "${newVersion}"`
);

const updatedFiles = ["app/build.gradle.kts"];

console.log(`Bumped ${currentVersion} -> ${newVersion} (${bumpType})`);
console.log(`Android versionCode ${currentVersionCode} -> ${newVersionCode}`);
console.log(`Android versionName ${currentVersion} -> ${newVersion}`);
console.log("");
console.log("Updated:");
for (const file of updatedFiles) {
  console.log(`  ${file}`);
}
