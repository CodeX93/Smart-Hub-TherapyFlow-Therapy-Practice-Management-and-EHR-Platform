
import { ContentLoader } from "@/components/shared/ContentLoader";
import { X } from "lucide-react";
import type { AdminFormAssignment } from "@/store/api/admin/clients.api";
import { Button } from "@/components/ui/button";

interface FormAssignmentDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  assignment: AdminFormAssignment | null;
  isLoading: boolean;
}

const FormAssignmentDetailsModal = ({
  isOpen,
  onClose,
  assignment,
  isLoading,
}: FormAssignmentDetailsModalProps) => {
  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="w-full max-w-3xl rounded-2xl bg-white shadow-lg max-h-[85vh] overflow-hidden flex flex-col">
          <div className="flex items-center justify-between px-5 py-4 border-b border-(--neutral-100)">
            <h2 className="text-lg font-semibold text-(--text-primary-dark)">
              Form Assignment Details
            </h2>
            <button
              onClick={onClose}
              className="p-1 rounded-full hover:bg-(--neutral-100) cursor-pointer"
            >
              <X size={18} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-5">
            {isLoading ? (
              <div className="py-16 flex flex-col items-center justify-center">
                <ContentLoader size="lg" />
                <p className="mt-2 text-sm text-(--text-neutral-600)">
                  Loading assignment...
                </p>
              </div>
            ) : assignment ? (
              <div className="space-y-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                  <div>
                    <p className="text-(--text-neutral-600)">Template</p>
                    <p className="font-medium">{assignment.templateName || "-"}</p>
                  </div>
                  <div>
                    <p className="text-(--text-neutral-600)">Status</p>
                    <p className="font-medium">{assignment.status || "-"}</p>
                  </div>
                  <div>
                    <p className="text-(--text-neutral-600)">Due Date</p>
                    <p className="font-medium">
                      {assignment.dueDate
                        ? new Date(assignment.dueDate).toLocaleString()
                        : "-"}
                    </p>
                  </div>
                </div>

                <div className="text-sm">
                  <p className="text-(--text-neutral-600)">Instructions</p>
                  <p className="mt-1 font-medium break-words whitespace-pre-wrap leading-relaxed">
                    {assignment.instructions?.trim() || "-"}
                  </p>
                </div>

                {(assignment.responses || []).length > 0 ? (
                <div>
                  <h3 className="font-semibold text-(--text-primary-dark) mb-2">
                    Responses
                  </h3>
                  <div className="border border-(--neutral-100) rounded-xl divide-y divide-(--neutral-100)">
                      {assignment.responses?.map((response) => (
                        <div key={response.id || `${response.fieldLabel}-${response.value}`} className="p-3">
                          <p className="text-sm font-medium text-(--text-primary-dark)">
                            {response.fieldLabel || "-"}
                          </p>
                          <p className="text-sm text-(--text-neutral-600) break-words whitespace-pre-wrap">
                            {response.value || "-"}
                          </p>
                        </div>
                      ))}
                  </div>
                </div>
                ) : null}

                <div>
                  <h3 className="font-semibold text-(--text-primary-dark) mb-2">
                    Signatures
                  </h3>
                  <div className="border border-(--neutral-100) rounded-xl divide-y divide-(--neutral-100)">
                    {(assignment.signatures || []).length ? (
                      assignment.signatures?.map((signature) => (
                        <div key={signature.id || `${signature.signerName}-${signature.signedAt}`} className="p-3">
                          <p className="text-sm font-medium text-(--text-primary-dark)">
                            {signature.signerName || "-"}
                          </p>
                          <p className="text-xs text-(--text-neutral-600)">
                            {(signature.signerRole || "-") +
                              (signature.signedAt
                                ? ` • ${new Date(signature.signedAt).toLocaleString()}`
                                : "")}
                          </p>
                          {signature.signatureData && (
                            <div className="mt-4 bg-slate-50 border border-slate-200 rounded-lg p-4 flex justify-center items-center">
                              <img
                                src={signature.signatureData}
                                alt={`Signature from ${signature.signerName}`}
                                className="max-h-24 w-auto object-contain"
                              />
                            </div>
                          )}
                        </div>
                      ))
                    ) : (
                      <p className="p-3 text-sm text-(--text-neutral-600)">
                        No signatures found.
                      </p>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="py-16 text-center text-sm text-(--text-neutral-600)">
                Unable to load assignment details.
              </div>
            )}
          </div>

          <div className="px-5 py-4 border-t border-(--neutral-100) flex justify-end">
            <Button onClick={onClose} className="rounded-full">
              Close
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};

export default FormAssignmentDetailsModal;
