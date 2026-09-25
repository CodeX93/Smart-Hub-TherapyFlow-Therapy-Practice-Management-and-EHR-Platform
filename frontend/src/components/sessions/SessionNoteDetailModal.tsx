
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo, useState } from "react";
import { X } from "lucide-react";
import { Button } from "../ui/button";
import CustomInput from "../form/CustomInput";
import CustomTextarea from "../form/CustomTextarea";
import ConfirmationModal from "../shared/ConfirmationModal";
import {
  useDeleteSessionNoteMutation,
  useGetSessionNoteByIdQuery,
  useGetSessionNotesBySessionIdQuery,
  useLazyGetSessionNotePdfQuery,
  useUpdateSessionNoteMutation,
} from "@/store/api/admin/sessionNotes.api";
import { getApiErrorMessage } from "@/utils/apiError";

interface SessionNoteDetailModalProps {
  isOpen: boolean;
  sessionId: number | null;
  onClose: () => void;
  onChanged?: () => void;
}

const SessionNoteDetailModalContent = ({ isOpen, sessionId, onClose, onChanged }: SessionNoteDetailModalProps) => {
  const [selectedNoteId, setSelectedNoteId] = useState<number | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [form, setForm] = useState({
    date: "",
    sessionFocus: "",
    symptoms: "",
    shortTermGoals: "",
    intervention: "",
    progress: "",
    remarks: "",
    recommendations: "",
  });

  const {
    data: sessionNotes = [],
    isLoading: isLoadingSessionNotes,
    isFetching: isFetchingSessionNotes,
    isError: isSessionNotesError,
    error: sessionNotesError,
    refetch: refetchSessionNotes,
  } = useGetSessionNotesBySessionIdQuery(sessionId ?? 0, { skip: !isOpen || !sessionId });

  const {
    currentData: selectedNote,
    isLoading: isLoadingSelectedNote,
    isFetching: isFetchingSelectedNote,
    isError: isSelectedNoteError,
    error: selectedNoteError,
    refetch: refetchSelectedNote,
  } = useGetSessionNoteByIdQuery(selectedNoteId ?? 0, { skip: !isOpen || !selectedNoteId });

  const [updateSessionNote, { isLoading: isUpdating }] = useUpdateSessionNoteMutation();
  const [deleteSessionNote, { isLoading: isDeleting }] = useDeleteSessionNoteMutation();
  const [getSessionNotePdf, { isFetching: isDownloadingPdf }] = useLazyGetSessionNotePdfQuery();

  if (isOpen && sessionNotes.length > 0 && !selectedNoteId) setSelectedNoteId(sessionNotes[0].id);

  const draftKey = selectedNote?.id;
  const [draftSource, setDraftSource] = useState(selectedNote);
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (selectedNote && (seededDraftKey !== draftKey || (!isEditing && draftSource !== selectedNote))) {
    setDraftSource(selectedNote);
    setSeededDraftKey(draftKey);
    setForm({
      date: selectedNote.date ? new Date(selectedNote.date).toISOString().slice(0, 16) : "",
      sessionFocus: selectedNote.sessionFocus ?? "",
      symptoms: selectedNote.symptoms ?? "",
      shortTermGoals: selectedNote.shortTermGoals ?? "",
      intervention: selectedNote.intervention ?? "",
      progress: selectedNote.progress ?? "",
      remarks: selectedNote.remarks ?? "",
      recommendations: selectedNote.recommendations ?? "",
    });
  }

  const canRenderLoader = isLoadingSessionNotes || isFetchingSessionNotes || isLoadingSelectedNote || isFetchingSelectedNote;
  const errorMessage = useMemo(() => {
    if (isSessionNotesError) return getApiErrorMessage(sessionNotesError);
    if (isSelectedNoteError) return getApiErrorMessage(selectedNoteError);
    return null;
  }, [isSessionNotesError, isSelectedNoteError, sessionNotesError, selectedNoteError]);

  const handleUpdate = async () => {
    if (!selectedNoteId) return;
    await updateSessionNote({
      id: selectedNoteId,
      body: {
        date: form.date ? new Date(form.date).toISOString() : undefined,
        sessionFocus: form.sessionFocus,
        symptoms: form.symptoms,
        shortTermGoals: form.shortTermGoals,
        intervention: form.intervention,
        progress: form.progress,
        remarks: form.remarks,
        recommendations: form.recommendations,
      },
    }).unwrap();
    await refetchSelectedNote();
    await refetchSessionNotes();
    setIsEditing(false);
    onChanged?.();
  };

  const handleDelete = async () => {
    if (!selectedNoteId) return;
    await deleteSessionNote(selectedNoteId).unwrap();
    setShowDeleteConfirm(false);
    setSelectedNoteId(null);
    await refetchSessionNotes();
    onChanged?.();
  };

  const handleDownloadPdf = async () => {
    if (!selectedNoteId) return;
    const result = await getSessionNotePdf(selectedNoteId).unwrap();
    if (result) {
      window.open(result, "_blank", "noopener,noreferrer");
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/50 p-4">
      <div className="flex max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-2xl bg-white">
        <div className="w-72 shrink-0 border-r border-(--neutral-100) overflow-y-auto">
          <div className="p-4 font-semibold text-(--text-primary-dark)">Session Notes</div>
          {isLoadingSessionNotes ? (
            <div className="p-4 text-sm text-(--text-neutral-600)">Loading notes...</div>
          ) : sessionNotes.length === 0 ? (
            <div className="p-4 text-sm text-(--text-neutral-600)">No notes found.</div>
          ) : (
            sessionNotes.map((note) => (
              <button
                key={note.id}
                onClick={() => {
                  setSelectedNoteId(note.id);
                  setIsEditing(false);
                }}
                className={`w-full border-t border-(--neutral-100) px-4 py-3 text-left text-sm cursor-pointer ${
                  selectedNoteId === note.id ? "bg-(--neutral-100)" : "bg-white"
                }`}
              >
                <p className="font-medium text-(--text-primary-dark)">{note.sessionFocus || "Session Note"}</p>
                <p className="text-(--text-neutral-600)">{note.date ? new Date(note.date).toLocaleString() : "-"}</p>
              </button>
            ))
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          <div className="flex items-center justify-between border-b border-(--neutral-100) p-4">
            <h3 className="text-lg font-semibold text-(--text-primary-dark)">View Session Note</h3>
            <button onClick={onClose} className="rounded-full p-1 hover:bg-(--neutral-100)">
              <X size={20} />
            </button>
          </div>

          {canRenderLoader ? (
            <div className="p-6 text-sm text-(--text-neutral-600) flex items-center gap-2">
              <ContentLoader variant="inline" size="sm" />
            </div>
          ) : errorMessage ? (
            <div className="p-6 text-sm text-(--status-denied)">{errorMessage}</div>
          ) : selectedNote ? (
            <div className="space-y-4 p-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <CustomInput label="Date" type="datetime-local" value={form.date} onChange={(e) => setForm((p) => ({ ...p, date: e.target.value }))} disabled={!isEditing} />
                <CustomInput label="Session Focus" value={form.sessionFocus} onChange={(e) => setForm((p) => ({ ...p, sessionFocus: e.target.value }))} disabled={!isEditing} />
                <CustomInput label="Symptoms" value={form.symptoms} onChange={(e) => setForm((p) => ({ ...p, symptoms: e.target.value }))} disabled={!isEditing} />
                <CustomInput label="Short-term Goals" value={form.shortTermGoals} onChange={(e) => setForm((p) => ({ ...p, shortTermGoals: e.target.value }))} disabled={!isEditing} />
                <CustomInput label="Intervention" value={form.intervention} onChange={(e) => setForm((p) => ({ ...p, intervention: e.target.value }))} disabled={!isEditing} />
                <CustomInput label="Progress" value={form.progress} onChange={(e) => setForm((p) => ({ ...p, progress: e.target.value }))} disabled={!isEditing} />
              </div>
              <CustomTextarea label="Remarks" value={form.remarks} onChange={(e) => setForm((p) => ({ ...p, remarks: e.target.value }))} disabled={!isEditing} />
              <CustomTextarea label="Recommendations" value={form.recommendations} onChange={(e) => setForm((p) => ({ ...p, recommendations: e.target.value }))} disabled={!isEditing} />

              <div className="flex flex-wrap justify-end gap-2">
                <Button onClick={() => void handleDownloadPdf()} disabled={isDownloadingPdf} loading={isDownloadingPdf} loadingLabel="Downloading..." className="h-9 rounded-full bg-transparent text-(--text-primary-500) hover:bg-(--neutral-100)">
                  Download PDF
                </Button>
                {isEditing ? (
                  <>
                    <Button onClick={() => { setIsEditing(false); setSeededDraftKey(null); }} className="h-9 rounded-full bg-transparent text-(--text-neutral-600) hover:bg-(--neutral-100)">Cancel</Button>
                    <Button onClick={() => void handleUpdate()} disabled={isUpdating} loading={isUpdating} loadingLabel="Saving..." className="h-9 rounded-full bg-(--bg-primary-dark) text-white">
                      Save
                    </Button>
                  </>
                ) : (
                  <>
                    <Button onClick={() => setIsEditing(true)} className="h-9 rounded-full bg-transparent text-(--text-primary-500) hover:bg-(--neutral-100)">Edit</Button>
                    <Button onClick={() => setShowDeleteConfirm(true)} className="h-9 rounded-full bg-transparent text-(--status-denied) hover:bg-(--neutral-100)">Delete</Button>
                  </>
                )}
              </div>
            </div>
          ) : (
            <div className="p-6 text-sm text-(--text-neutral-600)">No note selected.</div>
          )}
        </div>
      </div>

      <ConfirmationModal
        type="delete"
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={() => void handleDelete()}
        title="Delete session note?"
        description="Are you sure you want to delete this session note? This action cannot be undone."
        items={[]}
        confirmButtonText={isDeleting ? "Deleting..." : "Delete"}
        confirmButtonLoading={isDeleting}
      />
    </div>
  );
};

const SessionNoteDetailModal = (props: SessionNoteDetailModalProps) => props.isOpen ? <SessionNoteDetailModalContent key={props.sessionId} {...props} /> : null;

export default SessionNoteDetailModal;
