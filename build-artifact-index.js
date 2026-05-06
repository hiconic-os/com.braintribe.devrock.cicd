/**
 *
 * Node 18+ (fetch builtin)
 * Token scopes: read:packages (and possibly repo, depending on your setup)
 *
 * Usage:
 *  node build-artifact-index.js [-v] --token=... --owner=hiconic-os --ownerType=org --repo=some-repo --out=dist --version=1.2.3
 */

const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const args = process.argv.slice(2);
const verbose = args.includes("-v");
const getArg = (name) => args.find(a => a.startsWith(`--${name}=`))?.split("=").slice(1).join("=");

const token = getArg("token") || process.env.GITHUB_TOKEN;
const owner = getArg("owner"); // org/user for packages listing
const ownerType = (getArg("ownerType") || "org").toLowerCase(); // org|user
const repo = getArg("repo"); // REQUIRED: repo name only (e.g. "some-repo")
const outDirArg = getArg("out"); // REQUIRED output directory for artifact-index files
const indexVersion = getArg("version"); // REQUIRED version for artifact-index files
const packageType = "maven";
const perPage = Number(getArg("perPage") || 100);

if (!token) {
  console.error("Missing: --token=... or environment variable GITHUB_TOKEN");
  process.exit(2);
}
if (!owner) {
  console.error("Missing: --owner=ORG_OR_USER");
  process.exit(2);
}
if (!["org", "user"].includes(ownerType)) {
  console.error("Invalid: --ownerType=org|user");
  process.exit(2);
}
if (!repo) {
  console.error("Missing: --repo=REPO_NAME (owner will be taken from --owner)");
  process.exit(2);
}
if (!outDirArg) {
  console.error("Missing: --out=OUTPUT_DIR");
  process.exit(2);
}
if (!indexVersion) {
  console.error("Missing: --version=VERSION (used for artifact-index files)");
  process.exit(2);
}

const repoFullName = `${owner}/${repo}`; // OWNER/REPO
const outDir = path.resolve(process.cwd(), outDirArg);

const apiBase = "https://api.github.com";
const headersApi = {
  "Authorization": `Bearer ${token}`,
  "Accept": "application/vnd.github+json",
  "X-GitHub-Api-Version": "2026-03-10",
};

const headersMaven = {
  // maven.pkg.github.com typically accepts Basic auth:
  // username can be arbitrary, token is the password
  "Authorization": "Basic " + Buffer.from(`x-access-token:${token}`).toString("base64"),
  "Accept": "application/xml,text/xml,*/*",
};

function log(msg) {
  process.stderr.write(`[determine-all-versioned-artifacts] ${msg}\n`);
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function artifactIndexPomXml(version) {
  return [
    `<?xml version="1.0" encoding="UTF-8"?>`,
    `<project xmlns="http://maven.apache.org/POM/4.0.0"`,
    `         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"`,
    `         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">`,
    `  <modelVersion>4.0.0</modelVersion>`,
    `  <groupId>meta</groupId>`,
    `  <artifactId>artifact-index</artifactId>`,
    `  <version>${version}</version>`,
    `  <packaging>pom</packaging>`,
    `  <name>artifact-index</name>`,
    `  <description>Generated artifact version index</description>`,
    `</project>`,
    ``,
  ].join("\n");
}

async function httpGetText(url, headers) {
  const res = await fetch(url, { headers });
  const text = await res.text().catch(() => "");
  if (!res.ok) {
    const msg = text ? `\n${text}` : "";
    throw new Error(`HTTP ${res.status} ${res.statusText} for ${url}${msg}`);
  }
  return text;
}

async function ghGetJson(url) {
  const res = await fetch(url, { headers: headersApi });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`GitHub API ${res.status} ${res.statusText} for ${url}\n${text}`);
  }
  return res.json();
}

async function paginateJson(endpointUrl) {
  const out = [];
  for (let page = 1; ; page++) {
    const sep = endpointUrl.includes("?") ? "&" : "?";
    const url = `${endpointUrl}${sep}per_page=${perPage}&page=${page}`;
    const items = await ghGetJson(url);
    if (!Array.isArray(items) || items.length === 0) break;
    out.push(...items);
    if (items.length < perPage) break;
  }
  return out;
}

function coordsFromPackageName(packageName) {
  const name = String(packageName || "");
  const idx = name.lastIndexOf(".");
  if (idx <= 0 || idx === name.length - 1) return null;
  return { groupId: name.slice(0, idx), artifactId: name.slice(idx + 1) };
}

function groupIdToPath(groupId) {
  return String(groupId).split(".").filter(Boolean).join("/");
}

function extractVersionsFromMavenMetadataXml(xml) {
  // Simple extraction of everything between <version>...</version>
  const versions = [];
  const re = /<version>\s*([^<\s]+)\s*<\/version>/g;
  let m;
  while ((m = re.exec(xml)) !== null) versions.push(m[1]);
  // de-dupe
  return [...new Set(versions)];
}

function isPreReleaseCandidate(version) {
  return /-(rc|pc)$/i.test(String(version || "").trim());
}

async function listPackages() {
  const base =
    ownerType === "org"
      ? `${apiBase}/orgs/${encodeURIComponent(owner)}/packages`
      : `${apiBase}/users/${encodeURIComponent(owner)}/packages`;

  const qp = new URLSearchParams({ package_type: packageType });
  return paginateJson(`${base}?${qp.toString()}`);
}

async function main() {
  const repoLower = repoFullName.toLowerCase();

  log(`Starting. ownerType=${ownerType}, owner=${owner}, repo=${repo} (full: ${repoFullName}), packageType=${packageType}`);
  log(`Output directory: ${outDir}`);
  log(`artifact-index version: ${indexVersion}`);
  log(`Listing packages via GitHub API...`);
  const packages = await listPackages();
  log(`Found ${packages.length} ${packageType} packages visible to token.`);

  const repoPackages = packages.filter(p =>
    String(p?.repository?.full_name || "").toLowerCase() === repoLower
  );
  log(`After repo filter (${repoFullName}): ${repoPackages.length} packages.`);

  const rows = [];
  let skippedInvalidCoords = 0;
  let metadataFetchOk = 0;
  let metadataFetchFailed = 0;

  for (const pkg of repoPackages) {
    const packageName = String(pkg?.name || "");
    const coords = coordsFromPackageName(packageName);
    if (!coords) {
      skippedInvalidCoords++;
      log(`Skipping package "${packageName}" (cannot derive groupId/artifactId from last '.').`);
      continue;
    }

    const groupPath = groupIdToPath(coords.groupId);
    const metadataUrl = `https://maven.pkg.github.com/${repoFullName}/${groupPath}/${coords.artifactId}/maven-metadata.xml`;

    try {
      log(`Fetching metadata: ${coords.groupId}:${coords.artifactId} -> ${metadataUrl}`);
      const xml = await httpGetText(metadataUrl, headersMaven);
      const allVersions = extractVersionsFromMavenMetadataXml(xml);
      const versions = allVersions.filter(v => !isPreReleaseCandidate(v));
      metadataFetchOk++;
      const filtered = allVersions.length - versions.length;
      log(`  -> ${versions.length} versions${filtered ? ` (filtered ${filtered} ending with -rc/-pc)` : ""}`);
      for (const v of versions) {
        rows.push({ ...coords, version: v });
      }
    } catch (e) {
      metadataFetchFailed++;
      log(`  -> FAILED (${e?.message || String(e)})`);
      continue;
    }
  }

  rows.sort((a, b) => {
    const ga = `${a.groupId}:${a.artifactId}`;
    const gb = `${b.groupId}:${b.artifactId}`;
    if (ga !== gb) return ga.localeCompare(gb);
    return a.version.localeCompare(b.version);
  });

  const artifactCount = new Set(rows.map(r => `${r.groupId}:${r.artifactId}`)).size;
  const startIndex = rows.length + 10;
  log(`Done collecting. artifacts(distinct)=${artifactCount}, rows(artifact#version)=${rows.length}, metadata ok=${metadataFetchOk}, failed=${metadataFetchFailed}, skippedInvalidCoords=${skippedInvalidCoords}`);
  log(`Printing results with start index = rows + 10 = ${startIndex}`);

  const lines = [];
  let i = startIndex;
  for (const r of rows) {
    lines.push(`${i} ${r.groupId}:${r.artifactId}#${r.version} U`);
    i++;
  }

  const resultText = lines.join("\n") + (lines.length ? "\n" : "");
  process.stdout.write(resultText);

  ensureDir(outDir);
  const baseName = `artifact-index-${indexVersion}`;
  const pomPath = path.join(outDir, `${baseName}.pom`);
  const txtPath = path.join(outDir, `${baseName}.txt`);
  const gzPath = path.join(outDir, `${baseName}.gz`);

  fs.writeFileSync(pomPath, artifactIndexPomXml(indexVersion), "utf8");
  fs.writeFileSync(txtPath, resultText, "utf8");
  fs.writeFileSync(gzPath, zlib.gzipSync(Buffer.from(resultText, "utf8")));

  log(`Wrote files:`);
  log(`  - ${pomPath}`);
  log(`  - ${txtPath}`);
  log(`  - ${gzPath}`);
}

main().catch(err => {
  console.error(err?.stack || String(err));
  process.exit(1);
});
