import { Link } from "react-router-dom";
import { ArrowRight, BookOpen, LayoutTemplate } from "lucide-react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";

const CmsHub = () => {
  return (
    <SuperAdminPageShell
      title="CMS"
      description="Edit TherapyFlow marketing website content. Changes go live when you publish."
    >
      <div className="grid gap-4 md:grid-cols-2">
        <Link
          to="/super-admin/cms/landing-page"
          className="group flex flex-col gap-3 rounded-2xl border border-[#e6e9ee] bg-white p-6 shadow-[0_1px_2px_rgba(15,23,42,0.04)] transition hover:border-[#c5ccd6] hover:shadow-md"
        >
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#eef2f7] text-[#2f3945]">
            <LayoutTemplate size={22} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[#2f3945]">Landing Page</h2>
            <p className="mt-1 text-sm leading-6 text-[#697584]">
              Edit hero, features, FAQs, footer, SEO, and more for therapyflow.pro.
            </p>
          </div>
          <span className="mt-auto inline-flex items-center gap-1 text-sm font-medium text-[#2f3945]">
            Open editor
            <ArrowRight size={16} className="transition group-hover:translate-x-0.5" />
          </span>
        </Link>

        <Link
          to="/super-admin/cms/learning-hub"
          className="group flex flex-col gap-3 rounded-2xl border border-[#e6e9ee] bg-white p-6 shadow-[0_1px_2px_rgba(15,23,42,0.04)] transition hover:border-[#c5ccd6] hover:shadow-md"
        >
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#eef2f7] text-[#2f3945]">
            <BookOpen size={22} />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-[#2f3945]">Learning Hub</h2>
            <p className="mt-1 text-sm leading-6 text-[#697584]">
              Manage articles and guides for learninghub.therapyflow.pro.
            </p>
          </div>
          <span className="mt-auto inline-flex items-center gap-1 text-sm font-medium text-[#2f3945]">
            Open articles
            <ArrowRight size={16} className="transition group-hover:translate-x-0.5" />
          </span>
        </Link>
      </div>
    </SuperAdminPageShell>
  );
};

export default CmsHub;
