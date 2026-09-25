#!/usr/bin/env node
/**
 * Seed SmartHub Learning Hub with the 11 chapter guides (incl. Ch 2 payments sub-chapter).
 *
 * Reads: Therapy-Flow-Learning-Hub/content/guides/ch-*.md
 * Writes: public.cms_learning_hub_article (production/local Postgres via application-local.yml)
 *
 * Usage:
 *   node scripts/seed-chapter-learning-hub.mjs
 *   node scripts/seed-chapter-learning-hub.mjs ch-02-payments-and-subscription ch-02-clinic-setup
 *     — optional slugs: upsert ONLY those articles (no bulk unpublish of others)
 */
import { readFileSync, existsSync, readdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { Client } from "pg";

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, "..");
const guidesDir = join(root, "../../Therapy-Flow-Learning-Hub/content/guides");
const localYml = join(root, "src/main/resources/application-local.yml");
const schemaSql = readFileSync(
  join(root, "src/main/resources/db/migration/V73__cms_learning_hub_articles.sql"),
  "utf8",
);

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

function p(text) {
  return { type: "paragraph", children: textNodesFromMd(String(text)) };
}

function h(level, text) {
  return {
    type: "heading",
    level,
    children: textNodesFromMd(String(text)),
  };
}

/** Split markdown **bold** into Strapi-style text nodes. */
function textNodesFromMd(text) {
  const clean = String(text)
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/`([^`]+)`/g, "$1");
  const nodes = [];
  const re = /\*\*(.+?)\*\*/g;
  let last = 0;
  let m;
  while ((m = re.exec(clean))) {
    if (m.index > last) {
      nodes.push({ type: "text", text: clean.slice(last, m.index) });
    }
    nodes.push({ type: "text", text: m[1], bold: true });
    last = m.index + m[0].length;
  }
  if (last < clean.length) {
    nodes.push({ type: "text", text: clean.slice(last) });
  }
  if (!nodes.length) {
    nodes.push({ type: "text", text: clean });
  }
  return nodes.map((n) => ({
    ...n,
    text: n.text.replace(/\s+/g, " "),
  }));
}

function list(items, ordered = false) {
  return {
    type: "list",
    format: ordered ? "ordered" : "unordered",
    children: items.map((text) => ({
      type: "list-item",
      children: textNodesFromMd(String(text)),
    })),
  };
}

/** Videos placed after the matching heading (same positions as original live CMS). */
const VIDEO_PLACEMENTS = {
  "ch-01-getting-started": [
    {
      match: "1.1 Sign in correctly",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/chapter1-setupyour%20login.mp4",
    },
    {
      match: "First login for staff",
      level: 3,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Reset_Password_and_Log_In_to_SmartHub.mp4",
    },
  ],
  "ch-02-payments-and-subscription": [
    {
      match: "Payment Integration",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter2-Configure_Stripe_Payments_in_TherapyFlow.mp4",
    },
    {
      match: "Subscription",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter2-Set_Up_Enterprise_Billing_and_Payment_Methods_in_TherapyFlow.mp4",
    },
  ],
  "ch-03-people-and-access": [
    {
      match: "3.1 Roles",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/chapter%203-Create_and_Manage_Custom_Roles_in_TherapyFlow.mp4",
    },
    {
      match: "3.2 Users",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter3-Add_and_Configure_Therapist_Profiles_in_TherapyFlow.mp4",
    },
  ],
  "ch-04-clients-and-portal-access": [
    {
      match: "4.1",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/chapter4-Create_and_Manage_Complete_Client_Profiles_in_TherapyFlow.mp4",
    },
  ],
  "ch-05-scheduling-and-sessions": [
    {
      match: "5.1",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter5-Schedule_Individual_and_Recurring_Therapy_Sessions.mp4",
    },
  ],
  "ch-06-billing-and-payments": [
    {
      match: "6.1",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter6-Manage_Client_Billing__Discounts__and_Payments.mp4",
    },
  ],
  "ch-07-tasks-content-compliance": [
    {
      match: "7.1 Tasks",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/chapter7-Create_and_Manage_Client_Tasks_in_TherapyFlow.mp4",
    },
    {
      match: "7.2",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter6-Manage_Clinical_Content_and_Client_Assessments_in_TherapyFlow.mp4",
    },
  ],
  "ch-08-therapist-workspace": [
    {
      match: "8.1",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter8-Set_Up_Account_Security_and_Manage_Client_Operations_in_TherapyFlow.mp4",
    },
  ],
  "ch-09-client-portal": [
    {
      match: "9.1",
      level: 2,
      url: "https://pub-0169460c8309497191710ed2c35b3c5a.r2.dev/Chapter9-Complete_Your_SmartHub_Account_Setup_and_Booking.mp4",
    },
  ],
};

/** Related links for the Related links panel (clickable buttons). */
const RELATED_BY_SLUG = {
  "ch-05-scheduling-and-sessions": [
    {
      label: "Clinical Session Notes",
      url: "ch-11-clinical-session-notes",
      link_type: "article",
    },
  ],
  "ch-08-therapist-workspace": [
    {
      label: "Clinical Session Notes",
      url: "ch-11-clinical-session-notes",
      link_type: "article",
    },
  ],
  "ch-11-clinical-session-notes": [
    {
      label: "Scheduling & Sessions",
      url: "ch-05-scheduling-and-sessions",
      link_type: "article",
    },
    {
      label: "Therapist Workspace",
      url: "ch-08-therapist-workspace",
      link_type: "article",
    },
  ],
  "ch-02-clinic-setup": [
    {
      label: "Payments & Subscription",
      url: "ch-02-payments-and-subscription",
      link_type: "article",
    },
  ],
  "ch-02-payments-and-subscription": [
    { label: "Clinic Setup", url: "ch-02-clinic-setup", link_type: "article" },
    {
      label: "Billing & Payments",
      url: "ch-06-billing-and-payments",
      link_type: "article",
    },
    {
      label: "Client Portal",
      url: "ch-09-client-portal",
      link_type: "article",
    },
  ],
  "ch-06-billing-and-payments": [
    {
      label: "Payments & Subscription",
      url: "ch-02-payments-and-subscription",
      link_type: "article",
    },
    {
      label: "Clinic Setup",
      url: "ch-02-clinic-setup",
      link_type: "article",
    },
  ],
  "ch-09-client-portal": [
    {
      label: "Payments & Subscription",
      url: "ch-02-payments-and-subscription",
      link_type: "article",
    },
  ],
};

function headingText(block) {
  return (block.children || []).map((c) => c.text || "").join("");
}

/** Insert video URL paragraphs after their section headings (not all at the top). */
function injectVideos(blocks, slug) {
  const placements = VIDEO_PLACEMENTS[slug];
  if (!placements?.length) return blocks;

  const remaining = [...placements];
  const out = [];
  for (const block of blocks) {
    out.push(block);
    if (block.type !== "heading" || !remaining.length) continue;
    const text = headingText(block);
    const idx = remaining.findIndex(
      (pl) => block.level === pl.level && text.includes(pl.match),
    );
    if (idx === -1) continue;
    out.push(p(remaining[idx].url));
    remaining.splice(idx, 1);
  }
  // Do not dump leftovers at top/end (prevents wrong video stack).
  if (remaining.length) {
    console.warn(
      `  warn: ${slug}: ${remaining.length} video(s) could not be placed (heading match missed)`,
    );
  }
  return out;
}

function stripMd(text) {
  return text
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/\*\*(.+?)\*\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\s+/g, " ")
    .trim();
}

function estimateReadingTime(blocks) {
  const text = blocks
    .map((b) => {
      if (!b.children) return "";
      return b.children
        .map((c) =>
          c.children ? c.children.map((x) => x.text || "").join(" ") : c.text || "",
        )
        .join(" ");
    })
    .join(" ");
  const words = text.split(/\s+/).filter(Boolean).length;
  return Math.max(3, Math.ceil(words / 180));
}

/**
 * Convert chapter markdown body (after frontmatter-ish header) to Strapi blocks.
 */
function bodyToBlocks(body) {
  const lines = body.replace(/\r\n/g, "\n").split("\n");
  const blocks = [];
  let i = 0;
  let paraBuf = [];
  let listBuf = [];
  let listOrdered = false;
  let tableHeaders = [];
  let tableRows = [];
  let inTable = false;

  const flushPara = () => {
    if (!paraBuf.length) return;
    const text = paraBuf.join(" ").replace(/\s+/g, " ").trim();
    paraBuf = [];
    if (!text) return;
    const lower = text.toLowerCase();
    // These belong in Related_Links (or footers), not inline body text.
    if (
      lower.startsWith("related:") ||
      lower.startsWith("related paths") ||
      lower.startsWith("next:") ||
      lower.startsWith("product:")
    ) {
      return;
    }
    blocks.push(p(text));
  };
  const flushList = () => {
    if (!listBuf.length) return;
    blocks.push(list(listBuf, listOrdered));
    listBuf = [];
  };
  const flushTable = () => {
    if (!tableHeaders.length && !tableRows.length) {
      inTable = false;
      return;
    }
    flushPara();
    flushList();
    // Prefer clean bullets over "Header · Header" noise lines.
    // Two-column tables become: **Control** — Meaning
    const bullets = [];
    for (const row of tableRows) {
      const cells = row.map((c) => c.trim()).filter(Boolean);
      if (cells.length === 0) continue;
      if (cells.length === 2 || (tableHeaders.length === 2 && cells.length >= 1)) {
        const label = cells[0].replace(/\*\*/g, "");
        const value = cells
          .slice(1)
          .join(" — ")
          .replace(/\*\*/g, "");
        bullets.push(value ? `**${label}** — ${value}` : `**${label}**`);
      } else if (tableHeaders.length === cells.length && cells.length > 2) {
        const heads = tableHeaders.map((c) => c.trim().replace(/\*\*/g, ""));
        bullets.push(
          cells
            .map((c, i) => `**${heads[i]}:** ${c.replace(/\*\*/g, "")}`)
            .join(" · "),
        );
      } else {
        bullets.push(cells.join(" · "));
      }
    }
    if (bullets.length) {
      blocks.push(list(bullets, false));
    }
    tableHeaders = [];
    tableRows = [];
    inTable = false;
  };

  while (i < lines.length) {
    const raw = lines[i];
    const trimmed = raw.trim();

    if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
      flushPara();
      flushList();
      const cells = trimmed
        .slice(1, -1)
        .split("|")
        .map((c) => c.trim());
      const isDivider = cells.every((c) => /^[-:]+$/.test(c) || c === "");
      if (isDivider) {
        i += 1;
        continue;
      }
      if (!inTable) {
        inTable = true;
        tableHeaders = cells;
      } else {
        tableRows.push(cells);
      }
      i += 1;
      continue;
    }
    if (inTable) flushTable();

    if (!trimmed || trimmed === "---") {
      flushPara();
      flushList();
      i += 1;
      continue;
    }

    const hMatch = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (hMatch) {
      flushPara();
      flushList();
      blocks.push(h(hMatch[1].length, hMatch[2]));
      i += 1;
      continue;
    }

    const ol = trimmed.match(/^\d+\.\s+(.+)$/);
    if (ol) {
      flushPara();
      if (listBuf.length && !listOrdered) flushList();
      listOrdered = true;
      listBuf.push(ol[1]);
      i += 1;
      continue;
    }

    const ul = trimmed.match(/^[-*]\s+(.+)$/);
    if (ul) {
      flushPara();
      if (listBuf.length && listOrdered) flushList();
      listOrdered = false;
      listBuf.push(ul[1]);
      i += 1;
      continue;
    }

    flushList();
    paraBuf.push(trimmed);
    i += 1;
  }
  flushTable();
  flushPara();
  flushList();
  return blocks;
}

function parseGuide(filePath) {
  const raw = readFileSync(filePath, "utf8");
  const lines = raw.split("\n");
  let title = "";
  let slug = "";
  let audience = "Admin";
  let order = 0;
  let metaEnd = 0;

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i];
    if (line.startsWith("# ") && !title) {
      title = line.slice(2).trim();
      metaEnd = i + 1;
      continue;
    }
    const slugM = line.match(/^\*\*Slug:\*\*\s*`([^`]+)`/);
    if (slugM) {
      slug = slugM[1];
      metaEnd = i + 1;
      continue;
    }
    const audM = line.match(/^\*\*Audience:\*\*\s*(.+)$/);
    if (audM) {
      audience = audM[1].trim();
      metaEnd = i + 1;
      continue;
    }
    const ordM = line.match(/^\*\*Order:\*\*\s*(\d+)/);
    if (ordM) {
      order = Number(ordM[1]);
      metaEnd = i + 1;
      continue;
    }
    if (/^\*\*Parent:\*\*/i.test(line)) {
      metaEnd = i + 1;
      continue;
    }
    // Body starts at the first non-meta content line after the header block.
    // Do not treat later "---" section dividers as meta (they used to drop the intro).
    if (
      title &&
      slug &&
      line.trim() &&
      !line.startsWith("# ") &&
      !/^\*\*(Slug|Audience|Order|Parent):\*\*/i.test(line)
    ) {
      metaEnd = i;
      break;
    }
  }

  let body = lines.slice(metaEnd).join("\n").trim();
  // Drop trailing product footer
  body = body.replace(/\n---\s*\nProduct:[\s\S]*$/i, "").trim();
  body = body.replace(/\nProduct:[\s\S]*$/i, "").trim();

  if (!title || !slug) {
    throw new Error(`Missing title/slug in ${filePath}`);
  }

  // Chapter name without "Chapter N - " prefix for chapter field if present
  const chapterLabel =
    title.replace(/^Chapter\s+\d+\s+[—\-–:]+\s*/i, "").trim() || title;
  const firstUsefulLine =
    body
      .split("\n")
      .map((l) => l.trim())
      .find(
        (l) =>
          l &&
          !l.startsWith("#") &&
          !l.startsWith("|") &&
          l !== "---" &&
          !l.startsWith("http"),
      ) || "";
  const description =
    stripMd(firstUsefulLine).slice(0, 220) ||
    `${title}. SmartHub Learning Hub guide.`;

  // Section is optional metadata for browse filters — keep short (first H2), not a crumb trail
  const firstH2 = body.match(/^##\s+(.+)$/m)?.[1]?.trim();
  const Section = firstH2 || chapterLabel;

  let category = "Admin";
  const a = audience.toLowerCase();
  if (a.includes("everyone") || a.includes("all")) category = "All Roles";
  else if (a.includes("therapist")) category = "Therapist";
  else if (a.includes("client")) category = "Client";
  else if (a.includes("staff")) category = "Staff";

  const Content = injectVideos(bodyToBlocks(body), slug);
  const related = RELATED_BY_SLUG[slug] || [];

  return {
    title,
    slug,
    Order: order,
    Chapter: chapterLabel,
    Section,
    Category_Tag: category,
    Description: description,
    Is_Featured: order === 1000,
    Reading_Time: estimateReadingTime(Content),
    Publish_Date: new Date().toISOString().slice(0, 10),
    breadcrumb: slug,
    Content,
    ...(related.length ? { Related_Links: related } : {}),
  };
}

async function main() {
  if (!existsSync(guidesDir)) {
    throw new Error(`Guides dir missing: ${guidesDir}`);
  }

  // Optional CLI args: only seed these slugs (e.g. ch-02-payments-and-subscription)
  const onlySlugs = process.argv
    .slice(2)
    .map((s) => s.replace(/\.md$/i, "").trim())
    .filter(Boolean);
  const selective = onlySlugs.length > 0;

  let files = readdirSync(guidesDir)
    .filter((f) => /^ch-\d{2}-.+\.md$/.test(f))
    .sort();

  if (selective) {
    files = files.filter((f) => {
      const base = f.replace(/\.md$/i, "");
      return onlySlugs.includes(base);
    });
    const missing = onlySlugs.filter(
      (s) => !files.some((f) => f.replace(/\.md$/i, "") === s),
    );
    if (missing.length) {
      throw new Error(`Guide file(s) not found for slug(s): ${missing.join(", ")}`);
    }
  } else if (files.length !== 11) {
    console.warn(`Expected 11 chapter files, found ${files.length}`);
  }

  const articles = files.map((f) => parseGuide(join(guidesDir, f)));
  console.log(
    selective
      ? "── Seed SELECTED Learning Hub articles only ──"
      : "── Seed ALL chapter Learning Hub articles ──",
  );
  for (const a of articles) {
    console.log(`  ${a.Order}  ${a.slug}  (${a.Chapter})`);
  }

  const ymlDb = parseLocalYmlDb();
  const jdbcUrl = process.env.DB_URL || ymlDb.url;
  const dbUser = process.env.DB_USERNAME || ymlDb.username;
  const dbPass = process.env.DB_PASSWORD || ymlDb.password;
  if (!jdbcUrl || !dbUser || !dbPass) {
    throw new Error("Missing DB connection (DB_URL / application-local.yml)");
  }

  const client = new Client(jdbcToPgConfig(jdbcUrl, dbUser, dbPass));
  await client.connect();
  try {
    await client.query(schemaSql);

    // Full seed only: unpublish non-chapter curriculum. Selective seed never touches others.
    if (!selective) {
      const unpub = await client.query(
        `UPDATE cms_learning_hub_article
         SET is_published = FALSE, updated_at = NOW()
         WHERE slug NOT LIKE 'ch-%' AND is_published = TRUE`,
      );
      console.log(`Unpublished other articles: ${unpub.rowCount}`);
    } else {
      console.log("Selective mode: leaving all other published articles unchanged.");
    }

    let upserted = 0;
    for (const article of articles) {
      const body = JSON.stringify(article);
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
        [
          article.slug,
          article.title,
          body,
          article.Order,
          article.Chapter,
          article.Section,
          article.Is_Featured,
        ],
      );
      upserted += 1;
      console.log(`  ✓ ${article.slug}`);
    }

    console.log(`\nDone. Upserted ${upserted} article(s).`);
    if (selective) {
      const rows = await client.query(
        `SELECT sort_order, slug, title, is_published, updated_at
         FROM cms_learning_hub_article
         WHERE slug = ANY($1::text[])
         ORDER BY sort_order ASC`,
        [onlySlugs],
      );
      for (const r of rows.rows) {
        console.log(
          `  ${r.sort_order}\t${r.slug}\tpublished=${r.is_published}\tupdated=${r.updated_at?.toISOString?.() || r.updated_at}`,
        );
      }
    } else {
      const count = await client.query(
        `SELECT COUNT(*)::int AS c FROM cms_learning_hub_article WHERE is_published = TRUE`,
      );
      const rows = await client.query(
        `SELECT sort_order, slug, title FROM cms_learning_hub_article
         WHERE is_published = TRUE ORDER BY sort_order ASC, id ASC`,
      );
      console.log(`Published total: ${count.rows[0].c}`);
      for (const r of rows.rows) {
        console.log(`  ${r.sort_order}\t${r.slug}`);
      }
    }
  } finally {
    await client.end();
  }
}

main().catch((err) => {
  console.error("Seed failed:", err.message || err);
  process.exit(1);
});
