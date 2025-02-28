// Command-line arguments
const args = process.argv.slice(2);
const GITHUB_TOKEN = args.find(arg => arg.startsWith("--token="))?.split("=")[1];
const DRY_RUN = args.includes("--dry");

const GITHUB_USERNAME = "hiconic-os"; // Or organization name
const PACKAGE_NAME = "meta.artifact-index"; // Case-sensitive package name
const PACKAGE_TYPE = "maven"; // Example: 'npm', 'maven', 'docker'
const IS_ORG = true; // Set to true if it's an org package

if (!GITHUB_TOKEN) {
  console.error("⚠️  GitHub Token is missing! Use --token=YOUR_TOKEN");
  process.exit(1);
}

const GITHUB_API = IS_ORG
  ? `https://api.github.com/orgs/${GITHUB_USERNAME}/packages/${PACKAGE_TYPE}/${PACKAGE_NAME}/versions`
  : `https://api.github.com/users/${GITHUB_USERNAME}/packages/${PACKAGE_TYPE}/${PACKAGE_NAME}/versions`;

async function fetchVersions() {
    let versions = [];
    let page = 1;
    const perPage = 100; // Maximum allowed by GitHub API

    try {
        while (true) {
            const response = await fetch(`${GITHUB_API}?per_page=${perPage}&page=${page}`, {
                headers: { Authorization: `token ${GITHUB_TOKEN}` },
            });

            if (!response.ok) {
                throw new Error(`GitHub API error: ${response.status} ${response.statusText}`);
            }

            const pageVersions = await response.json();
            if (pageVersions.length === 0) break; // Stop when no more results

            versions = versions.concat(pageVersions);
            page++;
        }

        return versions;
    } catch (error) {
        console.error("❌ Error fetching package versions:", error.message);
        return [];
    }
}
  
async function deleteVersion(versionId) {
  const deleteUrl = `${GITHUB_API}/${versionId}`;
  if (DRY_RUN) {
    console.log(`🔹 [DRY] Would delete: ${deleteUrl}`);
    return;
  }

  try {
    const response = await fetch(deleteUrl, {
      method: "DELETE",
      headers: { Authorization: `token ${GITHUB_TOKEN}` },
    });

    if (!response.ok) {
      throw new Error(`Failed to delete version ${versionId}: ${response.status} ${response.statusText}`);
    }

    console.log(`✅ Deleted version ID: ${versionId}`);
  } catch (error) {
    console.error(`❌ Error deleting version ${versionId}:`, error.message);
  }
}

async function cleanupVersions() {
  const versions = await fetchVersions();

  if (versions.length <= 1) {
    console.log("✅ No need to delete anything. Only one version exists.");
    return;
  }

  // Sort versions by creation date (newest first)
  versions.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

  const latestVersion = versions[0];
  const oldVersions = versions.slice(1);

  console.log(`🔍 Keeping latest version: ID ${latestVersion.id}, created at ${latestVersion.created_at}`);

  for (const version of oldVersions) {
    await deleteVersion(version.id);
  }

  console.log(DRY_RUN ? "🛑 Dry run complete! No versions were actually deleted." : "🎉 Cleanup complete!");
}

cleanupVersions();
