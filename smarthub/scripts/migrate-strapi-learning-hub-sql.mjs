#!/usr/bin/env node
/**
 * Migrate Strapi learning-hub articles → Postgres cms_learning_hub_article.
 *
 * Usage: node scripts/migrate-strapi-learning-hub-sql.mjs
 */

import { readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { Client } from "pg";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, "..");
const websiteEnv = join(root, "../../therapyflow-website/.env");
const hubEnv = join(root, "../../Therapy-Flow-Learning-Hub/.env");
const localYml = join(root, "src/main/resources/application-local.yml");
const schemaSql = readFileSync(join(root, "src/main/resources/db/migration/V73__cms_learning_hub_articles.sql"), "utf8");

function loadDotEnv(path) {
  if (!existsSync(path)) return {};
  const out = {};
  for (const line of readFileSync(path, "utf8").split("\n")) {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/);
    if (!m) continue;
    let v = m[2].trim();
    if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
    out[m[1]] = v;
  }
  return out;
}

function parseLocalYmlDb() {
  const text = readFileSync(localYml, "utf8");
  return {
    url: text.match(/url:\s*\$\{DB_URL:(jdbc:postgresql:\/\/[^}]+)\}/)?.[1],
    username: text.match(/username:\s*\$\{DB_USERNAME:([^}]+)\}/)?.[1],
    password: text.match(/password:\s*\$\{DB_PASSWORD:([^}]+)\}/)?.[1],
  };
}

function jdbcToPgConfig(jdbcUrl, username, password) {
  const m = jdbcUrl.match(/^jdbc:postgresql:\/\/([^:/]+)(?::(\d+))?\/([^?]+)/);
  if (!m) throw new Error("Cannot parse JDBC URL");
  return {
    host: m[1],
    port: Number(m[2] || 5432),
    database: m[3],
    user: username,
    password,
    ssl: { rejectUnauthorized: false },
  };
}

const env = { ...loadDotEnv(websiteEnv), ...loadDotEnv(hubEnv) };
const STRAPI_URL = (process.env.STRAPI_URL || env.VITE_STRAPI_API_URL || "https://strapi-admin.therapyflow.pro").replace(/\/$/, "");
const STRAPI_TOKEN = process.env.STRAPI_TOKEN || env.VITE_STRAPI_API_TOKEN || "";
const ymlDb = parseLocalYmlDb();
const jdbcUrl = process.env.DB_URL || ymlDb.url;
const dbUser = process.env.DB_USERNAME || ymlDb.username;
const dbPass = process.env.DB_PASSWORD || ymlDb.password;

const POPULATE =
  "populate[Card_Image]=true&populate[SEO][populate]=OG_Image&populate[Content]=true&populate[Related_Links]=true&pagination[pageSize]=100&sort=Order:asc";

function absoluteMediaUrl(url) {
  if (!url) return url;
  if (/^https?:\/\//i.test(url)) return url;
  return `${STRAPI_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}

function normalizeMedia(node) {
  if (node == null) return node;
  if (Array.isArray(node)) return node.map(normalizeMedia);
  if (typeof node !== "object") return node;
  const out = {};
  for (const [key, value] of Object.entries(node)) {
    if (key === "url" && typeof value === "string") out[key] = absoluteMediaUrl(value);
    else out[key] = normalizeMedia(value);
  }
  return out;
}

function stripMeta(node) {
  if (node == null) return node;
  if (Array.isArray(node)) return node.map(stripMeta);
  if (typeof node !== "object") return node;
  const skip = new Set(["id", "documentId", "createdAt", "updatedAt", "publishedAt", "locale", "localizations", "createdBy", "updatedBy"]);
  const out = {};
  for (const [key, value] of Object.entries(node)) {
    if (skip.has(key)) continue;
    out[key] = stripMeta(value);
  }
  return out;
}

function normalizeSlug(slug, title) {
  const raw = (slug || title || "article").toString();
  return raw
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "") || "article";
}

async function main() {
  console.log("── Strapi Learning Hub → Postgres migration ──");
  console.log("Strapi:", STRAPI_URL);

  const headers = { Accept: "application/json" };
  if (STRAPI_TOKEN) headers.Authorization = `Bearer ${STRAPI_TOKEN}`;

  const res = await fetch(`${STRAPI_URL}/api/learning-hubs?${POPULATE}`, { headers });
  if (!res.ok) throw new Error(`Strapi fetch failed: ${res.status}`);
  const json = await res.json();
  const items = Array.isArray(json.data) ? json.data : [];
  console.log(`Fetched ${items.length} articles`);

  const client = new Client(jdbcToPgConfig(jdbcUrl, dbUser, dbPass));
  await client.connect();
  try {
    await client.query(schemaSql);
    let upserted = 0;
    for (const item of items) {
      const attrs = item.attributes ? item.attributes : item;
      const title = attrs.title || "Untitled";
      const slug = normalizeSlug(attrs.slug, title);
      const content = stripMeta(normalizeMedia(attrs));
      content.title = title;
      content.slug = slug;
      const sortOrder = Number(attrs.Order ?? attrs.order ?? 0) || 0;
      const chapter = attrs.Chapter || attrs.chapter || null;
      const section = attrs.Section || attrs.section || null;
      const featured = Boolean(attrs.Is_Featured);
      const body = JSON.stringify(content);

      await client.query(
        `INSERT INTO cms_learning_hub_article
          (slug, title, draft_content, published_content, published_at, sort_order, chapter, section_name, is_featured, is_published, created_at, updated_at)
         VALUES ($1,$2,$3::jsonb,$3::jsonb,NOW(),$4,$5,$6,$7,TRUE,NOW(),NOW())
         ON CONFLICT (slug) DO UPDATE SET
           title = EXCLUDED.title,
           draft_content = EXCLUDED.draft_content,
           published_content = EXCLUDED.published_content,
           published_at = NOW(),
           sort_order = EXCLUDED.sort_order,
           chapter = EXCLUDED.chapter,
           section_name = EXCLUDED.section_name,
           is_featured = EXCLUDED.is_featured,
           is_published = TRUE,
           updated_at = NOW()`,
        [slug, title, body, sortOrder, chapter, section, featured],
      );
      upserted += 1;
      console.log(`  ✓ ${slug}`);
    }
    const count = await client.query(`SELECT COUNT(*)::int AS c FROM cms_learning_hub_article WHERE is_published = TRUE`);
    console.log(`\nDone. Upserted ${upserted}. Published rows: ${count.rows[0].c}`);
  } finally {
    await client.end();
  }
}

main().catch((err) => {
  console.error("Migration failed:", err.message || err);
  process.exit(1);
});
