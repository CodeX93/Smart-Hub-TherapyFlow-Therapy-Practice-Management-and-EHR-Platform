import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { ContentLoader } from "@/components/shared/ContentLoader";

/**
 * Route pages are lazy chunks; suspending here rather than above the layout
 * keeps the sidebar and topbar on screen while the next page loads.
 */
export function SuspenseOutlet() {
  return (
    <Suspense fallback={<ContentLoader className="py-16" />}>
      <Outlet />
    </Suspense>
  );
}
