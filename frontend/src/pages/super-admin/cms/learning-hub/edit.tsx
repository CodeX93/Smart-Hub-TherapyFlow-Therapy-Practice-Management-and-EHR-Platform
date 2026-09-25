import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, ExternalLink, Plus } from "lucide-react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  type CmsLearningHubArticleContent,
  useCreateCmsLearningHubArticleMutation,
  useDeleteCmsLearningHubArticleMutation,
  useGetCmsLearningHubArticleQuery,
  usePublishCmsLearningHubArticleMutation,
  useUpdateCmsLearningHubArticleMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import CmsImageUpload from "../landing-page/components/CmsImageUpload";
import ArticleBodyEditor from "./ArticleBodyEditor";
import {
  type BodyBlock,
  blocksFromContent,
  contentFromBlocks,
} from "./articleBody";

const LIVE_HUB = "https://learninghub.therapyflow.pro";

function slugify(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

const emptyDraft = (): CmsLearningHubArticleContent => ({
  title: "",
  slug: "",
  Description: "",
  Chapter: "",
  Section: "",
  Order: 0,
  Category_Tag: "",
  Is_Featured: false,
  Reading_Time: 5,
  Related_Links: [],
  Content: [],
  SEO: { Meta_Title: "", Meta_Description: "" },
});

const CmsLearningHubEditor = () => {
  const { id } = useParams();
  const isNew = !id || id === "new";
  const numericId = !isNew ? Number(id) : 0;
  const navigate = useNavigate();

  const { currentData: data, isLoading, isError, error } = useGetCmsLearningHubArticleQuery(numericId, {
    skip: isNew || !Number.isFinite(numericId) || numericId <= 0,
  });
  const [createArticle, { isLoading: creating }] = useCreateCmsLearningHubArticleMutation();
  const [updateArticle, { isLoading: updating }] = useUpdateCmsLearningHubArticleMutation();
  const [publishArticle, { isLoading: publishing }] = usePublishCmsLearningHubArticleMutation();
  const [deleteArticle, { isLoading: deleting }] = useDeleteCmsLearningHubArticleMutation();

  const [draft, setDraft] = useState<CmsLearningHubArticleContent>(emptyDraft());
  const [bodyBlocks, setBodyBlocks] = useState<BodyBlock[]>(() => blocksFromContent([]));
  const [slugManual, setSlugManual] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("success");

  const [seededArticle, setSeededArticle] = useState<string | null>(null);
  if ((isNew || data?.draftContent) && seededArticle !== (id ?? "new")) {
    setSeededArticle(id ?? "new");
    setDraft(isNew ? emptyDraft() : { ...emptyDraft(), ...data?.draftContent, title: data?.title ?? "", slug: data?.slug ?? "" });
    setBodyBlocks(blocksFromContent(isNew ? [] : data?.draftContent?.Content));
    setSlugManual(!isNew);
  }

  const patch = (next: Partial<CmsLearningHubArticleContent>) => {
    setDraft((prev) => ({ ...prev, ...next }));
  };

  const showToast = (message: string, type: "success" | "error" | "info" = "success") => {
    setToastType(type);
    setToastMessage(message);
  };

  const builtContent = useMemo((): CmsLearningHubArticleContent => {
    return {
      ...draft,
      title: draft.title || "",
      slug: draft.slug || slugify(draft.title || "article"),
      Content: contentFromBlocks(bodyBlocks),
      Related_Links: (draft.Related_Links || []).filter((l) => l.label && l.url),
    };
  }, [draft, bodyBlocks]);

  const handleSave = async () => {
    try {
      if (!builtContent.title?.trim()) {
        showToast("Title is required.", "error");
        return;
      }
      if (isNew) {
        const created = await createArticle({
          title: builtContent.title!,
          slug: builtContent.slug!,
          draftContent: builtContent,
        }).unwrap();
        showToast("Article created.");
        navigate(`/super-admin/cms/learning-hub/${created.id}`, { replace: true });
      } else {
        await updateArticle({
          id: numericId,
          title: builtContent.title!,
          slug: builtContent.slug!,
          draftContent: builtContent,
        }).unwrap();
        showToast("Draft saved.");
      }
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  const handlePublish = async () => {
    try {
      if (isNew) {
        showToast("Save the article first, then publish.", "error");
        return;
      }
      await updateArticle({
        id: numericId,
        title: builtContent.title!,
        slug: builtContent.slug!,
        draftContent: builtContent,
      }).unwrap();
      await publishArticle(numericId).unwrap();
      showToast("Published. Live hub will show this on next load.");
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  const handleDelete = async () => {
    if (isNew) return;
    if (!window.confirm("Delete this article permanently?")) return;
    try {
      await deleteArticle(numericId).unwrap();
      navigate("/super-admin/cms/learning-hub");
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  if (!isNew && isLoading) {
    return (
      <SuperAdminPageShell title="Article" description="Loading…">
        <div className="flex items-center gap-2 text-sm text-[#697584]">
          <ContentLoader variant="inline" size="sm" />
        </div>
      </SuperAdminPageShell>
    );
  }

  if (!isNew && isError) {
    return (
      <SuperAdminPageShell title="Article" description="Could not load article.">
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {getApiErrorMessage(error)}
        </div>
      </SuperAdminPageShell>
    );
  }

  return (
    <SuperAdminPageShell
      title={isNew ? "New article" : "Edit article"}
      description="Write paragraphs, headings, and paste video URLs — preview updates instantly."
      toolbar={
        <div className="flex flex-wrap items-center gap-2">
          <Button type="button" variant="ghost" size="sm" asChild>
            <Link to="/super-admin/cms/learning-hub">
              <ArrowLeft size={14} className="mr-1" />
              Articles
            </Link>
          </Button>
          {!isNew ? (
            <Button type="button" variant="outline" size="sm" asChild>
              <a href={`${LIVE_HUB}/${builtContent.slug}`} target="_blank" rel="noopener noreferrer">
                View live
                <ExternalLink size={14} className="ml-1" />
              </a>
            </Button>
          ) : null}
          {!isNew ? (
            <Button type="button" variant="outline" size="sm" onClick={() => void handleDelete()} disabled={deleting}>
              {deleting ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : <TrashIcon size={14} className="mr-1" />}
              Delete
            </Button>
          ) : null}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => void handleSave()}
            disabled={creating || updating}
          >
            {creating || updating ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
            Save draft
          </Button>
          <Button type="button" size="sm" onClick={() => void handlePublish()} disabled={publishing || isNew}>
            {publishing ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
            Publish
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        <section className="space-y-5 rounded-2xl border border-[#e6e9ee] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
          <div>
            <h2 className="text-base font-semibold text-[#2f3945]">Basics</h2>
            <p className="mt-1 text-sm text-[#697584]">Title, URL slug, and how this article appears in the hub.</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-sm font-medium text-[#2f3945]">Title</label>
              <Input
                value={draft.title || ""}
                onChange={(e) => {
                  const title = e.target.value;
                  patch({
                    title,
                    slug: slugManual ? draft.slug : slugify(title),
                    SEO: {
                      ...(draft.SEO || {}),
                      Meta_Title: (draft.SEO?.Meta_Title || title).slice(0, 60),
                    },
                  });
                }}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Slug (URL)</label>
              <Input
                value={draft.slug || ""}
                onChange={(e) => {
                  setSlugManual(true);
                  patch({ slug: slugify(e.target.value) });
                }}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Category tag</label>
              <Input
                value={draft.Category_Tag || ""}
                onChange={(e) => patch({ Category_Tag: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Chapter</label>
              <Input value={draft.Chapter || ""} onChange={(e) => patch({ Chapter: e.target.value })} />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Section</label>
              <Input value={draft.Section || ""} onChange={(e) => patch({ Section: e.target.value })} />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Order</label>
              <Input
                type="number"
                value={draft.Order ?? 0}
                onChange={(e) => patch({ Order: Number(e.target.value) || 0 })}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#2f3945]">Reading time (minutes)</label>
              <Input
                type="number"
                value={draft.Reading_Time ?? 5}
                onChange={(e) => patch({ Reading_Time: Number(e.target.value) || 1 })}
              />
            </div>
            <div className="flex items-center gap-3 md:col-span-2">
              <Switch
                checked={Boolean(draft.Is_Featured)}
                onCheckedChange={(checked) => patch({ Is_Featured: checked })}
              />
              <span className="text-sm text-[#2f3945]">Featured on hub homepage</span>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-sm font-medium text-[#2f3945]">Short description</label>
            <Textarea
              rows={3}
              value={draft.Description || ""}
              onChange={(e) =>
                patch({
                  Description: e.target.value,
                  SEO: {
                    ...(draft.SEO || {}),
                    Meta_Description: e.target.value.slice(0, 160),
                  },
                })
              }
            />
          </div>

          <CmsImageUpload
            label="Card image"
            value={draft.Card_Image}
            onChange={(media) => patch({ Card_Image: media })}
          />
        </section>

        <section className="rounded-2xl border border-[#e6e9ee] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
          <ArticleBodyEditor value={bodyBlocks} onChange={setBodyBlocks} />
        </section>

        <section className="space-y-4 rounded-2xl border border-[#e6e9ee] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold text-[#2f3945]">Related links</h2>
              <p className="mt-1 text-sm text-[#697584]">Optional links shown beside the article.</p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                patch({
                  Related_Links: [...(draft.Related_Links || []), { label: "", url: "", link_type: "article" }],
                })
              }
            >
              <Plus size={14} className="mr-1" />
              Add link
            </Button>
          </div>
          {(draft.Related_Links || []).length === 0 ? (
            <p className="rounded-xl border border-dashed border-[#d7dde5] bg-[#f8fafc] px-4 py-6 text-center text-sm text-[#97a3b1]">
              No related links yet.
            </p>
          ) : null}
          {(draft.Related_Links || []).map((link, index) => (
            <div
              key={index}
              className="grid gap-2 rounded-xl border border-[#e6e9ee] p-3 md:grid-cols-[1fr_1.2fr_160px_auto]"
            >
              <Input
                placeholder="Label"
                value={link.label || ""}
                onChange={(e) => {
                  const next = [...(draft.Related_Links || [])];
                  next[index] = { ...next[index], label: e.target.value };
                  patch({ Related_Links: next });
                }}
              />
              <Input
                placeholder="URL"
                value={link.url || ""}
                onChange={(e) => {
                  const next = [...(draft.Related_Links || [])];
                  next[index] = { ...next[index], url: e.target.value };
                  patch({ Related_Links: next });
                }}
              />
              <Select
                value={link.link_type || "article"}
                onValueChange={(v) => {
                  const next = [...(draft.Related_Links || [])];
                  next[index] = { ...next[index], link_type: v };
                  patch({ Related_Links: next });
                }}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="article">Article</SelectItem>
                  <SelectItem value="product">Product</SelectItem>
                  <SelectItem value="external">External</SelectItem>
                </SelectContent>
              </Select>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() =>
                  patch({
                    Related_Links: (draft.Related_Links || []).filter((_, i) => i !== index),
                  })
                }
              >
                <TrashIcon size={14} />
              </Button>
            </div>
          ))}
        </section>

        <section className="space-y-4 rounded-2xl border border-[#e6e9ee] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
          <div>
            <h2 className="text-base font-semibold text-[#2f3945]">SEO</h2>
            <p className="mt-1 text-sm text-[#697584]">Search title and description for this article page.</p>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-sm font-medium text-[#2f3945]">
                SEO title ({(draft.SEO?.Meta_Title || "").length}/60)
              </label>
              <Input
                maxLength={60}
                value={draft.SEO?.Meta_Title || ""}
                onChange={(e) => patch({ SEO: { ...(draft.SEO || {}), Meta_Title: e.target.value } })}
              />
            </div>
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-sm font-medium text-[#2f3945]">
                SEO description ({(draft.SEO?.Meta_Description || "").length}/160)
              </label>
              <Textarea
                maxLength={160}
                rows={2}
                value={draft.SEO?.Meta_Description || ""}
                onChange={(e) =>
                  patch({ SEO: { ...(draft.SEO || {}), Meta_Description: e.target.value } })
                }
              />
            </div>
          </div>
        </section>
      </div>

      {toastMessage ? (
        <Toast message={toastMessage} type={toastType} onClose={() => setToastMessage(null)} />
      ) : null}
    </SuperAdminPageShell>
  );
};

export default CmsLearningHubEditor;
