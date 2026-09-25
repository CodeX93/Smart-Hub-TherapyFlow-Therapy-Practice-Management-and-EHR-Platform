#!/usr/bin/env node
/**
 * Migrate TherapyFlow landing page + global settings from Strapi → therapyflow-backend CMS.
 *
 * Usage:
 *   node scripts/migrate-strapi-landing.mjs
 *
 * Env:
 *   STRAPI_URL              default https://strapi-admin.therapyflow.pro
 *   STRAPI_TOKEN            optional Bearer token for Strapi
 *   CMS_API_URL             backend origin, e.g. https://api.therapyflow.pro
 *   CMS_ADMIN_TOKEN         Super Admin JWT (PLATFORM_SUPER_ADMIN)
 *   DRY_RUN=true            fetch + transform only, do not write
 *   SKIP_PUBLISH=true       save draft only (do not publish)
 */

const STRAPI_URL = (process.env.STRAPI_URL || "https://strapi-admin.therapyflow.pro").replace(/\/$/, "");
const STRAPI_TOKEN = process.env.STRAPI_TOKEN || "";
const CMS_API_URL = (process.env.CMS_API_URL || "http://localhost:8080").replace(/\/$/, "");
const CMS_ADMIN_TOKEN = process.env.CMS_ADMIN_TOKEN || "";
const DRY_RUN = process.env.DRY_RUN === "true";
const SKIP_PUBLISH = process.env.SKIP_PUBLISH === "true";

const LANDING_POPULATE = [
  "populate[Header_Logo]=true",
  "populate[Header_Nav_Links]=true",
  "populate[Hero_Image]=true",
  "populate[Features_Tabs_List][populate][Preview_Image]=true",
  "populate[Customization_Cards][populate][Graphic_Image]=true",
  "populate[Communication_Cards]=true",
  "populate[Security_Cards][populate][Image]=true",
  "populate[Benefits_Cards]=true",
  "populate[Faq_List]=true",
  "populate[Footer_Logo]=true",
  "populate[Footer_Nav_Links]=true",
  "populate[Footer_Socials]=true",
  "populate[Footer_Legal_Links]=true",
  "populate[SEO][populate][OG_Image]=true",
].join("&");

const GLOBAL_POPULATE = [
  "populate[Default_OG_Image]=true",
  "populate[Organization_Logo]=true",
  "populate[Landing_SEO][populate][OG_Image]=true",
  "populate[LearningHub_SEO][populate][OG_Image]=true",
].join("&");

async function fetchJson(url, headers = {}) {
  const res = await fetch(url, { headers: { Accept: "application/json", ...headers } });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`GET ${url} → ${res.status} ${res.statusText}\n${body}`);
  }
  return res.json();
}

async function putJson(url, body, token) {
  const res = await fetch(url, {
    method: "PUT",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`PUT ${url} → ${res.status} ${res.statusText}\n${text}`);
  }
  return res.json();
}

async function postJson(url, body, token) {
  const res = await fetch(url, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(`POST ${url} → ${res.status} ${res.statusText}\n${text}`);
  }
  return res.json();
}

function absoluteMediaUrl(url) {
  if (!url) return url;
  if (/^https?:\/\//i.test(url)) return url;
  return `${STRAPI_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}

/** Normalize Strapi media objects so url fields are absolute (keep formats for website compatibility). */
function normalizeMedia(node) {
  if (node == null) return node;
  if (Array.isArray(node)) return node.map(normalizeMedia);
  if (typeof node !== "object") return node;

  const out = {};
  for (const [key, value] of Object.entries(node)) {
    if (key === "url" && typeof value === "string") {
      out[key] = absoluteMediaUrl(value);
    } else if (key === "formats" && value && typeof value === "object") {
      out[key] = normalizeMedia(value);
    } else {
      out[key] = normalizeMedia(value);
    }
  }
  return out;
}

/** Strip Strapi bookkeeping fields not needed by the frontend. */
function stripMeta(node) {
  if (node == null) return node;
  if (Array.isArray(node)) return node.map(stripMeta);
  if (typeof node !== "object") return node;

  const skip = new Set([
    "id",
    "documentId",
    "createdAt",
    "updatedAt",
    "publishedAt",
    "locale",
    "localizations",
    "createdBy",
    "updatedBy",
  ]);
  const out = {};
  for (const [key, value] of Object.entries(node)) {
    if (skip.has(key)) continue;
    out[key] = stripMeta(value);
  }
  return out;
}

function transformLanding(strapiData) {
  if (!strapiData) throw new Error("Strapi landing-page returned empty data");
  return stripMeta(normalizeMedia(strapiData));
}

function transformGlobal(strapiData) {
  if (!strapiData) throw new Error("Strapi global-site-settings returned empty data");
  return stripMeta(normalizeMedia(strapiData));
}

async function main() {
  console.log("── Strapi → Backend CMS migration ──");
  console.log(`Strapi:  ${STRAPI_URL}`);
  console.log(`Backend: ${CMS_API_URL}`);
  console.log(`Dry run: ${DRY_RUN}`);

  const strapiHeaders = {};
  if (STRAPI_TOKEN) strapiHeaders.Authorization = `Bearer ${STRAPI_TOKEN}`;

  console.log("Fetching landing page from Strapi…");
  const landingRes = await fetchJson(
    `${STRAPI_URL}/api/landing-page?${LANDING_POPULATE}`,
    strapiHeaders,
  );
  const landing = transformLanding(landingRes.data);
  console.log(`  Landing fields: ${Object.keys(landing).join(", ")}`);

  console.log("Fetching global settings from Strapi…");
  const globalRes = await fetchJson(
    `${STRAPI_URL}/api/global-site-settings?${GLOBAL_POPULATE}`,
    strapiHeaders,
  );
  const globalSettings = transformGlobal(globalRes.data);
  console.log(`  Global fields: ${Object.keys(globalSettings).join(", ")}`);

  if (DRY_RUN) {
    console.log("\nDRY_RUN=true — writing sample JSON to stdout (truncated):");
    console.log(JSON.stringify({ landing: Object.keys(landing), globalSettings: Object.keys(globalSettings) }, null, 2));
    console.log("Done (no writes).");
    return;
  }

  if (!CMS_ADMIN_TOKEN) {
    throw new Error("CMS_ADMIN_TOKEN is required (Super Admin JWT). Set DRY_RUN=true to skip writes.");
  }

  console.log("Saving landing page draft…");
  await putJson(
    `${CMS_API_URL}/api/v1/super-admin/cms/landing-page`,
    { draftContent: landing },
    CMS_ADMIN_TOKEN,
  );

  console.log("Saving global settings…");
  await putJson(
    `${CMS_API_URL}/api/v1/super-admin/cms/global-settings`,
    { content: globalSettings },
    CMS_ADMIN_TOKEN,
  );

  if (!SKIP_PUBLISH) {
    console.log("Publishing landing page…");
    await postJson(
      `${CMS_API_URL}/api/v1/super-admin/cms/landing-page/publish`,
      null,
      CMS_ADMIN_TOKEN,
    );
  } else {
    console.log("SKIP_PUBLISH=true — draft saved, not published.");
  }

  console.log("\nVerifying public API…");
  const pubLanding = await fetchJson(`${CMS_API_URL}/api/v1/public/cms/landing-page`);
  const pubGlobal = await fetchJson(`${CMS_API_URL}/api/v1/public/cms/global-settings`);
  console.log(`  Public landing keys: ${Object.keys(pubLanding.data || {}).length}`);
  console.log(`  Public global keys:  ${Object.keys(pubGlobal.data || {}).length}`);
  console.log("\nMigration complete.");
}

main().catch((err) => {
  console.error("\nMigration failed:", err.message || err);
  process.exit(1);
});
