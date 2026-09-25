
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from 'react';
import { Button } from "../../../../ui/button";
import FormAssignmentDropdown from './FormAssignmentDropdown';
import AssignedFormsList from './AssignedFormsList';
import EmptyFormsState from './EmptyFormsState';
import {
  useCreateFormAssignmentMutation,
  useDeleteFormAssignmentMutation,
  useGetFormAssignmentsQuery,
  useLazyGetFormAssignmentByIdQuery,
  useGetFormTemplatesQuery,
} from "@/store/api/admin/clients.api";
import type { Client } from "@/types/client.type";

import Toast from "../../../../shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import FormAssignmentDetailsModal from "./FormAssignmentDetailsModal";
import type { AdminFormAssignment } from "@/store/api/admin/clients.api";

export interface FormTemplate {
    id: string;
    title: string;
}

interface ClinicalFormsProps {
    client: Client;
    readOnly?: boolean;
}

const ClinicalForms = ({ client, readOnly = false }: ClinicalFormsProps) => {
    const [selectedForms, setSelectedForms] = useState<string[]>([]);
    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
    const [selectedAssignment, setSelectedAssignment] = useState<AdminFormAssignment | null>(null);
    const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
    const [deleteAssignmentId, setDeleteAssignmentId] = useState<string | null>(null);
    const [viewingAssignmentId, setViewingAssignmentId] = useState<string | null>(null);
    const [deletingAssignmentId, setDeletingAssignmentId] = useState<string | null>(null);
    const { data: formTemplatesResponse } = useGetFormTemplatesQuery();
    const formTemplates = formTemplatesResponse?.items ?? [];
    const {
        data: assignedForms = [],
        isLoading: isLoadingAssignedForms,
        isFetching: isFetchingAssignedForms,
        refetch: refetchAssignedForms,
    } = useGetFormAssignmentsQuery({ clientId: Number(client.id) });
    const [createFormAssignment, { isLoading: isAssigning }] =
        useCreateFormAssignmentMutation();
    const [triggerGetFormAssignmentById, { isFetching: isFetchingAssignment }] =
        useLazyGetFormAssignmentByIdQuery();
    const [deleteFormAssignment] = useDeleteFormAssignmentMutation();

    const templateOptions: FormTemplate[] = formTemplates
        .filter((template) => template.isActive !== false)
        .map((template) => ({
            id: String(template.id),
            title: template.name || "Untitled Form",
        }));

    const handleAssign = async () => {
        if (selectedForms.length === 0) return;
        try {
            const promises = selectedForms.map((templateId) =>
                createFormAssignment({
                    templateId: Number(templateId),
                    clientId: Number(client.id),
                    dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
                }).unwrap(),
            );
            await Promise.all(promises);
            await refetchAssignedForms();
            setSelectedForms([]);
            setToastType("success");
            setToastMessage("Form(s) assigned successfully.");
        } catch (error) {
            setToastType("error");
            setToastMessage(getApiErrorMessage(error));
        }
    };

    const handleView = async (id: string) => {
        const parsedId = Number(id);
        if (!parsedId) return;
        setViewingAssignmentId(id);
        setIsDetailsModalOpen(true);
        setSelectedAssignment(null);
        try {
            const detail = await triggerGetFormAssignmentById(parsedId).unwrap();
            setSelectedAssignment(detail);
        } catch (error) {
            setToastType("error");
            setToastMessage(getApiErrorMessage(error));
            setIsDetailsModalOpen(false);
        } finally {
            setViewingAssignmentId(null);
        }
    };

    const handleDelete = (id: string) => {
        setDeleteAssignmentId(id);
    };

    const handleConfirmDelete = async () => {
        if (!deleteAssignmentId) return;
        const parsedId = Number(deleteAssignmentId);
        if (!parsedId) return;
        setDeletingAssignmentId(deleteAssignmentId);
        try {
            await deleteFormAssignment(parsedId).unwrap();
            await refetchAssignedForms();
            setToastType("success");
            setToastMessage("Assigned form deleted successfully.");
            setDeleteAssignmentId(null);
        } catch (error) {
            setToastType("error");
            setToastMessage(getApiErrorMessage(error));
        } finally {
            setDeletingAssignmentId(null);
        }
    };

    return (
        <div className="space-y-8 animate-in fade-in duration-300">
            {/* Assignment Area */}
            {!readOnly ? (
            <div className="flex min-w-0 items-start gap-4">
                <div className="min-w-0 flex-1">
                    <FormAssignmentDropdown
                        options={templateOptions}
                        selectedValues={selectedForms}
                        onChange={setSelectedForms}
                    />
                </div>
                <Button
                    onClick={handleAssign}
                    disabled={selectedForms.length === 0 || isAssigning}
                    className="h-[3rem] shrink-0 px-8 rounded-full bg-[var(--bg-primary-dark)] hover:bg-[var(--bg-primary-dark)]/90 text-white font-semibold disabled:opacity-50 disabled:cursor-not-allowed text-sm shadow-sm transition-all"
                  loading={isAssigning}
                  loadingLabel="Assigning form to client..."
                >
                    Assign form(s) to client
                </Button>
            </div>
            ) : null}

            {/* List Area */}
            <div>
                <h3 className="text-sm font-semibold text-gray-900 mb-4">Assigned Forms</h3>

                {isLoadingAssignedForms || (isFetchingAssignedForms && assignedForms.length === 0) ? (
                    <ContentLoader size="lg" className="py-12" />
                ) : assignedForms.length > 0 ? (
                    <AssignedFormsList
                        forms={assignedForms}
                        readOnly={readOnly}
                        onDelete={handleDelete}
                        onView={(id) => {
                            void handleView(id);
                        }}
                        deletingId={deletingAssignmentId}
                        viewingId={viewingAssignmentId}
                    />
                ) : (
                    <EmptyFormsState />
                )}
            </div>

            {toastMessage ? (
                <Toast
                    message={toastMessage}
                    type={toastType}
                    onClose={() => setToastMessage(null)}
                />
            ) : null}

            <FormAssignmentDetailsModal
                isOpen={isDetailsModalOpen}
                onClose={() => {
                    setIsDetailsModalOpen(false);
                    setSelectedAssignment(null);
                }}
                assignment={selectedAssignment}
                isLoading={isFetchingAssignment}
            />

            <ConfirmationModal
                isOpen={Boolean(deleteAssignmentId)}
                onClose={() => setDeleteAssignmentId(null)}
                onConfirm={() => {
                    void handleConfirmDelete();
                }}
                title="Delete assigned form?"
                description="Are you sure you want to delete this assigned form? This action cannot be undone."
                type="delete"
                items={[]}
                confirmButtonText={deletingAssignmentId ? "Deleting..." : "Delete"}
                confirmButtonLoading={Boolean(deletingAssignmentId)}
            />
        </div>
    );
};

export default ClinicalForms;
