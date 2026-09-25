import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useNavigate } from "react-router-dom";

const FeatureCatalogHeader = () => {
  const navigate = useNavigate();

  return (
    <div className="w-full flex items-start justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-(--text-gray-900) text-3xl font-semibold leading-10">
          Feature Catalog
        </h1>
        <p className="mt-1 text-(--text-neutral-600) text-sm font-normal leading-5.5">
          Central registry for core and custom feature toggles, usage limits, and
          rollouts.
        </p>
      </div>

      <div className="flex items-center gap-3 shrink-0">
        <Button
          variant="outline"
          className={cn(
            "h-10 rounded-lg border-(--neutral-100) bg-(--surface-white)",
            "text-(--text-gray-900) text-sm font-medium leading-5 hover:bg-(--bg-primary-50)"
          )}
        >
          Plan Entitlements
        </Button>
        <Button
          className={cn(
            "h-10 rounded-lg px-4 text-sm font-medium leading-5",
            "bg-(--admin-primary-dark) text-(--surface-white) hover:opacity-95"
          )}
          onClick={() => navigate("/super-admin/feature-flags/create")}
        >
          Create Feature
        </Button>
      </div>
    </div>
  );
};

export default FeatureCatalogHeader;
