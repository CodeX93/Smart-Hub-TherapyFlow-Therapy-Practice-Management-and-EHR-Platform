
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, Plus, Search } from "lucide-react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useGetCmsLearningHubArticlesQuery } from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

const CmsLearningHubList = () => {
  const { data = [], isLoading, isError, error, refetch } = useGetCmsLearningHubArticlesQuery();
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return data;
    return data.filter(
      (a) =>
        a.title.toLowerCase().includes(q) ||
        a.slug.toLowerCase().includes(q) ||
        (a.chapter || "").toLowerCase().includes(q) ||
        (a.sectionName || "").toLowerCase().includes(q),
    );
  }, [data, search]);

  return (
    <SuperAdminPageShell
      title="Learning Hub"
      description="Create and publish guides for learninghub.therapyflow.pro."
      toolbar={
        <div className="flex flex-wrap items-center gap-2">
          <Button type="button" variant="ghost" size="sm" asChild>
            <Link to="/super-admin/cms">
              <ArrowLeft size={14} className="mr-1" />
              CMS
            </Link>
          </Button>
          <Button type="button" size="sm" asChild>
            <Link to="/super-admin/cms/learning-hub/new">
              <Plus size={14} className="mr-1" />
              New article
            </Link>
          </Button>
        </div>
      }
    >
      <div className="relative max-w-md">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#97a3b1]" />
        <Input
          className="pl-9"
          placeholder="Search by title, slug, chapter…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {isLoading ? (
        <div className="flex items-center gap-2 text-sm text-[#697584]">
          <ContentLoader variant="inline" size="sm" />…
        </div>
      ) : isError ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {getApiErrorMessage(error)}
          <Button type="button" className="mt-3" variant="outline" onClick={() => void refetch()}>
            Retry
          </Button>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-[#e6e9ee] bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-[#f7f8fa] text-[#697584]">
              <tr>
                <th className="px-4 py-3 font-medium">Title</th>
                <th className="px-4 py-3 font-medium">Chapter</th>
                <th className="px-4 py-3 font-medium">Order</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium" />
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-[#97a3b1]">
                    No articles found.
                  </td>
                </tr>
              ) : (
                filtered.map((article) => (
                  <tr key={article.id} className="border-t border-[#eef1f5]">
                    <td className="px-4 py-3">
                      <div className="font-medium text-[#2f3945]">{article.title}</div>
                      <div className="text-xs text-[#97a3b1]">{article.slug}</div>
                    </td>
                    <td className="px-4 py-3 text-[#697584]">
                      {article.chapter || "—"}
                      {article.sectionName ? (
                        <div className="text-xs text-[#97a3b1]">{article.sectionName}</div>
                      ) : null}
                    </td>
                    <td className="px-4 py-3 text-[#697584]">{article.sortOrder}</td>
                    <td className="px-4 py-3">
                      <span
                        className={
                          article.isPublished
                            ? "rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"
                            : "rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-700"
                        }
                      >
                        {article.isPublished
                          ? article.hasUnpublishedChanges
                            ? "Published · draft changes"
                            : "Published"
                          : "Draft"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button type="button" variant="outline" size="sm" asChild>
                        <Link to={`/super-admin/cms/learning-hub/${article.id}`}>Edit</Link>
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </SuperAdminPageShell>
  );
};

export default CmsLearningHubList;
