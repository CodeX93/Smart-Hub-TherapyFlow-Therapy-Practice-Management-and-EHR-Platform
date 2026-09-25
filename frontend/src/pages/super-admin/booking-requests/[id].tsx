import { useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import { ArrowLeft, User, Phone, Mail, Building, Briefcase, Users, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import SemanticStatusBadge from "@/components/shared/SemanticStatusBadge";
import {
  useGetBookingRequestByIdQuery,
  useUpdateBookingRequestStatusMutation,
  useDeleteBookingRequestMutation
} from "@/store/api/superAdminApi";
import Toast from "@/components/shared/Toast";
import ConfirmationModal from "@/components/shared/ConfirmationModal";

function getStatusBadgeConfig(status: string) {
  switch (status) {
    case "PENDING": return { variant: "warning" as const, text: "Pending" };
    case "REVIEWED": return { variant: "info" as const, text: "Reviewed" };
    case "CONVERTED": return { variant: "success" as const, text: "Converted" };
    case "REJECTED": return { variant: "danger" as const, text: "Rejected" };
    default: return { variant: "default" as const, text: status };
  }
}

export default function BookingRequestDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const requestId = Number(id);
  const navigate = useNavigate();
  
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  
  const [modalState, setModalState] = useState<{ isOpen: boolean; action: 'CONVERTED' | 'REJECTED' | 'DELETE' | null }>({ isOpen: false, action: null });
  const [statusMessage, setStatusMessage] = useState("");

  const { data: request, isLoading } = useGetBookingRequestByIdQuery(requestId, { skip: !requestId });
  const [updateStatus, { isLoading: isUpdating }] = useUpdateBookingRequestStatusMutation();
  const [deleteRequest, { isLoading: isDeleting }] = useDeleteBookingRequestMutation();

  const handleConfirmAction = async () => {
    if (modalState.action === 'DELETE') {
      try {
        await deleteRequest(requestId).unwrap();
        setToastType("success");
        setToastMessage("Request deleted successfully");
        navigate("/super-admin/booking-requests");
      } catch {
        setToastType("error");
        setToastMessage("Failed to delete request");
        setModalState({ isOpen: false, action: null });
      }
      return;
    }

    try {
      await updateStatus({ id: requestId, status: modalState.action!, statusMessage }).unwrap();
      setToastType("success");
      setToastMessage("Status updated successfully");
      setModalState({ isOpen: false, action: null });
      setStatusMessage("");
    } catch {
      setToastType("error");
      setToastMessage("Failed to update status");
      setModalState({ isOpen: false, action: null });
    }
  };

  if (isLoading) {
    return (
      <SuperAdminPageShell title="Booking Request Details">
        <div className="p-8 text-center text-slate-500">Loading details...</div>
      </SuperAdminPageShell>
    );
  }

  if (!request) {
    return (
      <SuperAdminPageShell title="Booking Request Details">
        <div className="p-8 text-center text-red-500">Request not found.</div>
      </SuperAdminPageShell>
    );
  }

  const statusConfig = getStatusBadgeConfig(request.status);

  return (
    <SuperAdminPageShell title="Booking Request Details">
      <div className="w-full max-w-5xl mx-auto py-6 flex flex-col gap-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link to="/super-admin/booking-requests">
              <Button variant="ghost" size="icon" className="h-9 w-9 text-slate-500 hover:text-slate-900">
                <ArrowLeft className="h-5 w-5" />
              </Button>
            </Link>
            <div>
              <h1 className="text-2xl font-bold text-slate-900">
                {request.firstName} {request.lastName}
              </h1>
              <p className="text-sm text-slate-500">Submitted {new Date(request.createdAt).toLocaleString()}</p>
            </div>
          </div>
          <SemanticStatusBadge status={request.status} className="inline-flex h-6 w-fit items-center justify-center rounded-full border px-3 text-xs font-medium">
            {statusConfig.text}
          </SemanticStatusBadge>
        </div>

        {toastMessage && (
          <Toast
            type={toastType}
            message={toastMessage}
            onClose={() => setToastMessage(null)}
          />
        )}

        {/* Action Bar */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
          <div className="text-sm text-slate-600 font-medium">Update Lead Status:</div>
          <div className="flex gap-2">
            <Button 
              variant="outline" 
              className="border-slate-300 text-slate-700"
              disabled={isUpdating || isDeleting || request.status === 'REVIEWED'}
              onClick={async () => {
                try {
                  await updateStatus({ id: requestId, status: 'REVIEWED' }).unwrap();
                  setToastType("success");
                  setToastMessage("Status updated successfully");
                } catch {
                  setToastType("error");
                  setToastMessage("Failed to update status");
                }
              }}
            >
              Mark as Reviewed
            </Button>
            <Button 
              variant="default"
              disabled={isUpdating || isDeleting || request.status === 'CONVERTED'}
              onClick={() => setModalState({ isOpen: true, action: 'CONVERTED' })}
            >
              Accept
            </Button>
            <Button 
              variant="destructive"
              disabled={isUpdating || isDeleting || request.status === 'REJECTED'}
              onClick={() => setModalState({ isOpen: true, action: 'REJECTED' })}
            >
              Reject
            </Button>
            <div className="w-px h-8 bg-slate-200 mx-2 self-center"></div>
            <Button 
              variant="ghost"
              className="text-red-600 hover:text-red-700 hover:bg-red-50 px-3"
              disabled={isUpdating || isDeleting}
              onClick={() => setModalState({ isOpen: true, action: 'DELETE' })}
              title="Delete Request"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Details Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Contact Info */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 bg-slate-50">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <User className="h-4 w-4 text-slate-500" /> Contact Information
              </h3>
            </div>
            <div className="p-5 flex flex-col gap-4">
              <div>
                <div className="text-xs text-slate-500 uppercase font-semibold mb-1">Full Name</div>
                <div className="text-slate-900">{request.firstName} {request.lastName}</div>
              </div>
              <div className="flex gap-8">
                <div>
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1"><Mail className="h-3 w-3"/> Email</div>
                  <a href={`mailto:${request.email}`} className="text-(--primary-600) hover:underline">{request.email}</a>
                </div>
                <div>
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1"><Phone className="h-3 w-3"/> Phone</div>
                  <a href={`tel:${request.phone}`} className="text-(--primary-600) hover:underline">{request.phone}</a>
                </div>
              </div>
            </div>
          </div>

          {/* Practice Info */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-100 bg-slate-50">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <Building className="h-4 w-4 text-slate-500" /> Practice Details
              </h3>
            </div>
            <div className="p-5 flex flex-col gap-4">
              <div>
                <div className="text-xs text-slate-500 uppercase font-semibold mb-1">Practice / Hospital Name</div>
                <div className="text-slate-900">{request.practiceName}</div>
              </div>
              <div className="flex gap-8">
                <div>
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1"><Briefcase className="h-3 w-3"/> Services Offered</div>
                  <div className="text-slate-900">{request.servicesOffered}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1"><Users className="h-3 w-3"/> Practice Size</div>
                  <div className="text-slate-900">{request.practiceSize}</div>
                </div>
              </div>
              <div className="flex gap-8">
                <div className="flex-1">
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1">Country</div>
                  <div className="text-slate-900">{request.country || "-"}</div>
                </div>
                <div className="flex-1">
                  <div className="text-xs text-slate-500 uppercase font-semibold mb-1 flex items-center gap-1">Address</div>
                  <div className="text-slate-900">{request.address || "-"}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        {request.statusMessage && (
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-5 mt-2">
            <h3 className="text-sm font-semibold text-slate-700 mb-2">Status Message (Email Sent)</h3>
            <p className="text-sm text-slate-600">{request.statusMessage}</p>
          </div>
        )}
      </div>

      <ConfirmationModal
        type={modalState.action === 'CONVERTED' ? "activate" : "delete"}
        isOpen={modalState.isOpen}
        onClose={() => {
          setModalState({ isOpen: false, action: null });
          setStatusMessage("");
        }}
        onConfirm={handleConfirmAction}
        title={
          modalState.action === 'DELETE' 
            ? "Delete Request" 
            : modalState.action === 'CONVERTED' 
              ? "Accept Request" 
              : "Reject Request"
        }
        description={
          modalState.action === 'DELETE'
            ? "Are you sure you want to permanently delete this booking request? This action cannot be undone."
            : modalState.action === 'CONVERTED' 
              ? "Are you sure you want to accept this request and convert it? An email will be sent to the lead." 
              : "Are you sure you want to reject this request? An email will be sent to the lead."
        }
        confirmButtonText={
          modalState.action === 'DELETE'
            ? "Delete"
            : modalState.action === 'CONVERTED' 
              ? "Convert Request" 
              : "Reject Request"
        }
        confirmButtonLoading={isUpdating || isDeleting}
        reasonLabel={modalState.action === 'CONVERTED' ? "Custom Welcome Message" : "Rejection Reason"}
        reasonPlaceholder={modalState.action === 'CONVERTED' ? "Enter a custom welcome note (optional)..." : "Enter the reason for rejection..."}
        reasonValue={statusMessage}
        onReasonChange={modalState.action === 'DELETE' ? undefined : setStatusMessage}
      />
    </SuperAdminPageShell>
  );
}
