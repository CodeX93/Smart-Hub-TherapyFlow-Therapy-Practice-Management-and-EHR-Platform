import { useState } from "react";
import { Link } from "react-router-dom";
import { Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import Pagination from "@/components/shared/Pagination";
import { useGetBookingRequestsQuery } from "@/store/api/superAdminApi";

function getStatusBadgeConfig(status: string) {
  switch (status) {
    case "PENDING":
      return { variant: "warning" as const, text: "Pending" };
    case "REVIEWED":
      return { variant: "info" as const, text: "Reviewed" };
    case "CONVERTED":
      return { variant: "success" as const, text: "Converted" };
    case "REJECTED":
      return { variant: "danger" as const, text: "Rejected" };
    default:
      return { variant: "default" as const, text: status };
  }
}

export default function BookingRequestsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useGetBookingRequestsQuery({ page, size: 20 });

  return (
    <SuperAdminPageShell 
      title="Trial Sign-Up Requests"
      description="Manage leads coming from the public website form."
    >
      <div className="flex flex-col gap-6 w-full py-6">
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">

          <div className="w-full overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider border-b border-slate-200">
                  <th className="px-6 py-4">Name</th>
                  <th className="px-6 py-4">Practice</th>
                  <th className="px-6 py-4">Size</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Date</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading && (
                  <tr>
                    <td colSpan={6} className="px-6 py-8 text-center text-slate-500">
                      Loading requests...
                    </td>
                  </tr>
                )}
                {!isLoading && (!data || data.content.length === 0) && (
                  <tr>
                    <td colSpan={6} className="px-6 py-16 text-center">
                      <div className="flex flex-col items-center justify-center">
                        <div className="h-16 w-16 rounded-full bg-slate-50 flex items-center justify-center mb-4 border border-slate-100">
                          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-slate-400"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                        </div>
                        <h3 className="text-[15px] font-semibold text-slate-900">No booking requests found</h3>
                        <p className="text-sm text-slate-500 mt-1 max-w-sm">
                          There are currently no trial sign-up requests to review. New requests from the public website will appear here.
                        </p>
                      </div>
                    </td>
                  </tr>
                )}
                {!isLoading && data?.content.map((req) => {
                  const statusConfig = getStatusBadgeConfig(req.status);
                  return (
                    <tr key={req.id} className="hover:bg-slate-50 transition-colors group">
                      <td className="px-6 py-4">
                        <div className="text-sm font-medium text-slate-900">{req.firstName} {req.lastName}</div>
                        <div className="text-sm text-slate-500">{req.email}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-slate-900">{req.practiceName}</div>
                        <div className="text-sm text-slate-500">{req.servicesOffered}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-slate-700">{req.practiceSize}</div>
                      </td>
                      <td className="px-6 py-4">
                        <SemanticStatusBadge 
                          status={req.status}
                          className="inline-flex h-5 w-fit items-center justify-center rounded-full border px-2.5 text-[0.6875rem] font-medium leading-4"
                        >
                          {statusConfig.text}
                        </SemanticStatusBadge>
                      </td>
                      <td className="px-6 py-4 text-sm text-slate-600">
                        {new Date(req.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link to={`/super-admin/booking-requests/${req.id}`}>
                          <Button variant="ghost" size="sm" className="h-8 px-2 text-slate-600 hover:text-(--primary-600)">
                            <Eye className="h-4 w-4 mr-2" />
                            View Details
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between p-4 border-t border-slate-200">
              <Pagination
                currentPage={page + 1}
                totalPages={data.totalPages}
                totalItems={data.totalElements}
                itemsPerPage={data.size}
                onPageChange={(p) => setPage(p - 1)}
              />
            </div>
          )}
        </div>
      </div>
    </SuperAdminPageShell>
  );
}
