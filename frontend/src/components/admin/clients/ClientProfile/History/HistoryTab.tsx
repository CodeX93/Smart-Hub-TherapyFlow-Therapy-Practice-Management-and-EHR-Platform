import type { AdminClientHistoryEvent } from "@/store/api/admin/clients.api";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Search, Plus, X } from "lucide-react";
import { Letter } from "@solar-icons/react-perf/category/messages/Linear/Letter";
import { ClockCircle } from "@solar-icons/react-perf/category/time/Linear/ClockCircle";
import { Button } from "@/components/ui/button";
import TimeInStageCard from "./TimeInStageCard";
import ClientTimeline from "./ClientTimeline";
import ClientNoteCard, { type Note } from "./ClientNoteCard";
import AddNoteModal from "./AddNoteModal";
import EditNoteModal from "./EditNoteModal";
import ClientEmailHistory from "./ClientEmailHistory";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import NotesEmptyState from "@/components/shared/NotesEmptyState";
import { ContentLoader } from "@/components/shared/ContentLoader";
import Toast from "@/components/shared/Toast";
import { cn } from "../../../../../lib/utils";
import { format } from "date-fns";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import RangeCalendar from "@/components/shared/RangeCalendar";
import CustomInput from "@/components/form/CustomInput";
import CustomMultiSelect from "@/components/form/CustomMultiSelect";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import {
  useCreateClientNoteMutation,
  useDeleteClientNoteMutation,
  useGetClientEmailHistoryQuery,
  useGetClientHistoryQuery,
  useGetClientNotesQuery,
  useGetClientStageDurationsQuery,
  useLazyGetClientNoteByIdQuery,
  useUpdateClientNoteMutation,
} from "@/store/api/admin/clients.api";
import { getApiErrorMessage } from "@/utils/apiError";

interface HistoryTabProps {
  clientId: number;
  isActive?: boolean;
  readOnly?: boolean;
}

const normalizeNoteType = (noteType?: string | null): string => {
  const normalized = (noteType || "").trim().toLowerCase();
  if (!normalized) return "general";
  if (normalized === "note") return "general";
  return normalized;
};

/** Turn API keys like `file_created` / `portal-activated` into "File Created". */
const formatEventLabel = (value?: string | null, fallback = "History Event"): string => {
  const trimmed = (value || "").trim();
  if (!trimmed) return fallback;

  return trimmed
    .replace(/[_-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/\b\w/g, (char) => char.toUpperCase());
};

const HistoryTab = ({ clientId, isActive = false, readOnly = false }: HistoryTabProps) => {
  const [activeTab, setActiveTab] = useState<"communications" | "email">(
    "communications",
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [date, setDate] = useState<
    { from: Date | undefined; to: Date | undefined } | undefined
  >();
  const [draftDate, setDraftDate] = useState<
    { from: Date | undefined; to: Date | undefined } | undefined
  >();
  const [isDatePopoverOpen, setIsDatePopoverOpen] = useState(false);
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date());
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [deletingNoteId, setDeletingNoteId] = useState<string | null>(null);
  const [isCreatingNote, setIsCreatingNote] = useState(false);
  const [isUpdatingNote, setIsUpdatingNote] = useState(false);
  const [historyItems, setHistoryItems] = useState<AdminClientHistoryEvent[]>([]);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyTotalPages, setHistoryTotalPages] = useState(1);
  const [isLoadingMoreHistory, setIsLoadingMoreHistory] = useState(false);
  const [emailHistoryItems, setEmailHistoryItems] = useState<AdminClientHistoryEvent[]>([]);
  const [emailHistoryPage, setEmailHistoryPage] = useState(1);
  const [emailHistoryTotalPages, setEmailHistoryTotalPages] = useState(1);
  const [isLoadingMoreEmailHistory, setIsLoadingMoreEmailHistory] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("error");
  const {
    data: stageDurations,
    isLoading: isStageDurationsLoading,
    isFetching: isStageDurationsFetching,
    refetch: refetchStageDurations,
  } = useGetClientStageDurationsQuery(clientId, {
    skip: !clientId || clientId <= 0,
  });
  const {
    data: clientHistoryResponse,
    isLoading: isHistoryLoading,
    isFetching: isHistoryFetching,
    refetch: refetchClientHistory,
  } = useGetClientHistoryQuery(
    { id: clientId, page: historyPage, pageSize: 20 },
    { skip: !isActive || !clientId || clientId <= 0 },
  );
  const {
    data: clientEmailHistoryResponse,
    isLoading: isEmailHistoryLoading,
    isFetching: isEmailHistoryFetching,
    isError: isEmailHistoryError,
    error: emailHistoryError,
    refetch: refetchClientEmailHistory,
  } = useGetClientEmailHistoryQuery(
    { id: clientId, page: emailHistoryPage, pageSize: 20 },
    { skip: !isActive || activeTab !== "email" || !clientId || clientId <= 0 },
  );
  const {
    data: clientNotesResponse = [],
    isLoading: isNotesLoading,
    isFetching: isNotesFetching,
    refetch: refetchClientNotes,
  } = useGetClientNotesQuery(
    {
      clientId,
      noteType:
        selectedTypes.length === 1 ? selectedTypes[0].toLowerCase() : undefined,
      startDate: date?.from?.toISOString(),
      endDate: date?.to?.toISOString(),
    },
    { skip: !isActive || activeTab !== "communications" || !clientId || clientId <= 0 },
  );
  const [createClientNote] = useCreateClientNoteMutation();
  const [updateClientNote] = useUpdateClientNoteMutation();
  const [deleteClientNote] = useDeleteClientNoteMutation();
  const [getClientNoteById] = useLazyGetClientNoteByIdQuery();

  useEffect(() => {
    if (!isActive || !clientId || clientId <= 0) return;
    void refetchStageDurations();
    setHistoryPage(1);
    setHistoryTotalPages(1);
    setHistoryItems([]);
    void refetchClientHistory();
  }, [isActive, clientId, refetchStageDurations, refetchClientHistory]);

  useEffect(() => {
    if (!isActive || activeTab !== "email" || !clientId || clientId <= 0) return;
    setEmailHistoryPage(1);
    setEmailHistoryTotalPages(1);
    setEmailHistoryItems([]);
    void refetchClientEmailHistory();
  }, [isActive, activeTab, clientId, refetchClientEmailHistory]);

  useEffect(() => {
    if (!isActive || activeTab !== "communications" || !clientId || clientId <= 0) return;
    void refetchClientNotes();
  }, [isActive, activeTab, clientId, refetchClientNotes]);

  useEffect(() => {
    if (!isActive) return;
    if (!clientHistoryResponse) return;
    setHistoryTotalPages(Math.max(clientHistoryResponse.totalPages || 1, 1));
    setHistoryItems((prev) =>
      historyPage <= 1 ? clientHistoryResponse.items : [...prev, ...clientHistoryResponse.items],
    );
    setIsLoadingMoreHistory(false);
  }, [isActive, clientHistoryResponse, historyPage]);

  useEffect(() => {
    if (!isActive || activeTab !== "email") return;
    if (!clientEmailHistoryResponse) return;
    const pageSize = 20;
    const totalPages = Math.max(
      1,
      Math.ceil((clientEmailHistoryResponse.count || 0) / pageSize),
    );
    setEmailHistoryTotalPages(totalPages);
    setEmailHistoryItems((prev) =>
      emailHistoryPage <= 1
        ? clientEmailHistoryResponse.history
        : [...prev, ...clientEmailHistoryResponse.history],
    );
    setIsLoadingMoreEmailHistory(false);
  }, [isActive, activeTab, clientEmailHistoryResponse, emailHistoryPage]);

  useEffect(() => {
    if (!isEmailHistoryError) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(emailHistoryError));
    setIsLoadingMoreEmailHistory(false);
  }, [isEmailHistoryError, emailHistoryError]);

  const loadMoreHistory = useCallback(() => {
    if (isLoadingMoreHistory || isHistoryLoading || isHistoryFetching) return;
    if (historyPage >= historyTotalPages) return;
    setIsLoadingMoreHistory(true);
    setHistoryPage((prev) => prev + 1);
  }, [
    isLoadingMoreHistory,
    isHistoryLoading,
    isHistoryFetching,
    historyPage,
    historyTotalPages,
  ]);

  const { observerTarget: historyObserverTarget } = useInfiniteScroll({
    onLoadMore: loadMoreHistory,
    hasMore: historyPage < historyTotalPages,
    isLoading: isLoadingMoreHistory || isHistoryLoading || isHistoryFetching,
  });

  const loadMoreEmailHistory = useCallback(() => {
    if (isLoadingMoreEmailHistory || isEmailHistoryLoading || isEmailHistoryFetching) return;
    if (emailHistoryPage >= emailHistoryTotalPages) return;
    setIsLoadingMoreEmailHistory(true);
    setEmailHistoryPage((prev) => prev + 1);
  }, [
    isLoadingMoreEmailHistory,
    isEmailHistoryLoading,
    isEmailHistoryFetching,
    emailHistoryPage,
    emailHistoryTotalPages,
  ]);

  const { observerTarget: emailHistoryObserverTarget } = useInfiniteScroll({
    onLoadMore: loadMoreEmailHistory,
    hasMore: emailHistoryPage < emailHistoryTotalPages,
    isLoading: isLoadingMoreEmailHistory || isEmailHistoryLoading || isEmailHistoryFetching,
  });

  const mappedTimelineEvents = useMemo(() => {
    return historyItems.map((event) => {
      const source = (event.eventSource || "").toLowerCase();
      const type = source.includes("file")
        ? "file"
        : source.includes("portal")
          ? "portal"
          : "general";
      const createdAt = event.createdAt
        ? new Date(event.createdAt)
        : null;
      const dateLabel =
        createdAt && !Number.isNaN(createdAt.getTime())
          ? new Intl.DateTimeFormat("en-US", {
              month: "short",
              day: "2-digit",
              year: "numeric",
              hour: "numeric",
              minute: "2-digit",
            }).format(createdAt)
          : "-";

      return {
        id: String(event.id),
        title: formatEventLabel(event.eventType, "History Event"),
        description:
          event.changeSummary ||
          event.description ||
          [event.fromValue, event.toValue].filter(Boolean).join(" -> ") ||
          "No details available.",
        author: event.createdByName || undefined,
        date: dateLabel,
        type,
      } as const;
    });
  }, [historyItems]);

  const mappedEmailEvents = useMemo(() => {
    return emailHistoryItems.map((event) => {
      const createdAt = event.createdAt ? new Date(event.createdAt) : null;
      const dateLabel =
        createdAt && !Number.isNaN(createdAt.getTime())
          ? new Intl.DateTimeFormat("en-US", {
              month: "short",
              day: "2-digit",
              year: "numeric",
              hour: "numeric",
              minute: "2-digit",
            }).format(createdAt)
          : "-";

      return {
        id: String(event.id),
        title: event.eventType?.trim() || "Email Event",
        status: formatEventLabel(event.eventSource, "Communication"),
        date: dateLabel,
        details: {
          recipient: "-",
          trigger: event.changeSummary || event.description || "No details available.",
          relatedTo: formatEventLabel(event.fromValue, "Client"),
          id: event.toValue ? `#${event.toValue}` : `#${event.id}`,
        },
      };
    });
  }, [emailHistoryItems]);

  const mappedNotes = useMemo<Note[]>(
    () =>
      clientNotesResponse.map((note) => ({
        id: String(note.id),
        type: normalizeNoteType(note.noteType),
        subject: note.title || undefined,
        content: note.content || "",
        createdAt: note.eventDate
          ? new Intl.DateTimeFormat("en-US", {
              month: "short",
              day: "2-digit",
              year: "numeric",
              hour: "numeric",
              minute: "2-digit",
            }).format(new Date(note.eventDate))
          : "-",
        eventDate: note.eventDate,
      })),
    [clientNotesResponse],
  );

  const filteredNotes = mappedNotes.filter((note) => {
    const matchesSearch =
      note.subject?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      note.content.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesType =
      selectedTypes.length === 0 ||
      selectedTypes.some(
        (type) => type.toLowerCase() === note.type.toLowerCase(),
      );
    return matchesSearch && matchesType;
  });

  const TYPE_OPTIONS = [
    { id: "call", label: "Call" },
    { id: "email", label: "Email" },
    { id: "note", label: "Note" },
  ];

  const handleDateRangeChange = (
    range: { from: Date | undefined; to: Date | undefined } | undefined,
  ) => {
    setDraftDate(range);
    if (range?.from && range?.to) {
      setDate(range);
      setIsDatePopoverOpen(false);
    }
  };

  const clearDateRange = () => {
    setDate(undefined);
    setDraftDate(undefined);
    setIsDatePopoverOpen(false);
  };

  const handleAddNote = async (newNoteData: {
    type: string;
    subject?: string;
    content: string;
    communicationDate: Date;
  }) => {
    try {
      setIsCreatingNote(true);
      await createClientNote({
        clientId,
        title: newNoteData.subject?.trim() || undefined,
        content: newNoteData.content,
        noteType: newNoteData.type.toLowerCase(),
        eventDate: newNoteData.communicationDate.toISOString(),
        isPrivate: false,
      }).unwrap();
      await refetchClientNotes();
      setIsAddModalOpen(false);
      setToastType("success");
      setToastMessage("Note created successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsCreatingNote(false);
    }
  };

  const handleDeleteClick = (id: string) => {
    setSelectedNoteId(id);
    setIsDeleteModalOpen(true);
  };

  const handleEditNote = async (note: Note) => {
    try {
      const detail = await getClientNoteById(Number(note.id)).unwrap();
      setSelectedNote({
        id: String(detail.id),
        type: normalizeNoteType(detail.noteType),
        subject: detail.title || undefined,
        content: detail.content || "",
        createdAt: detail.eventDate
          ? new Intl.DateTimeFormat("en-US", {
              month: "short",
              day: "2-digit",
              year: "numeric",
              hour: "numeric",
              minute: "2-digit",
            }).format(new Date(detail.eventDate))
          : "-",
        eventDate: detail.eventDate,
      });
      setIsEditModalOpen(true);
    } catch (error) {
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleUpdateNote = async (updatedNote: {
    id: string;
    type: string;
    subject?: string;
    content: string;
    communicationDate: Date;
  }) => {
    try {
      setIsUpdatingNote(true);
      await updateClientNote({
        id: Number(updatedNote.id),
        body: {
          title: updatedNote.subject?.trim() || undefined,
          content: updatedNote.content,
          noteType: updatedNote.type.toLowerCase(),
          eventDate: updatedNote.communicationDate.toISOString(),
          isPrivate: false,
        },
      }).unwrap();
      await refetchClientNotes();
      setIsEditModalOpen(false);
      setSelectedNote(null);
      setToastType("success");
      setToastMessage("Note updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setIsUpdatingNote(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!selectedNoteId) return;
    try {
      setDeletingNoteId(selectedNoteId);
      await deleteClientNote(Number(selectedNoteId)).unwrap();
      await refetchClientNotes();
      setIsDeleteModalOpen(false);
      setSelectedNoteId(null);
      setToastType("success");
      setToastMessage("Note deleted successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setDeletingNoteId(null);
    }
  };

  return (
    <div className="animate-in fade-in space-y-5 p-4 duration-300 sm:space-y-5 sm:p-6">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex flex-col gap-5">
        <div className="flex flex-col gap-3">
          <h3 className="text-base leading-6 font-semibold text-(--neutral-950)">
            Time in Each Stage
          </h3>
          <TimeInStageCard
            currentStage={stageDurations?.currentStage}
            durations={stageDurations?.durations}
            isLoading={isStageDurationsLoading || isStageDurationsFetching}
          />
        </div>

        <ClientTimeline
          events={mappedTimelineEvents}
          isLoading={(isHistoryLoading || isHistoryFetching) && historyPage === 1}
          isLoadingMore={isLoadingMoreHistory}
          observerTarget={historyObserverTarget}
        />
      </div>

      <div>
        <div className="mb-6 flex items-center border-b border-(--neutral-100) px-0.5">
          <button
            type="button"
            onClick={() => setActiveTab("communications")}
            className={cn(
              "relative flex cursor-pointer items-center justify-center gap-2 px-3 py-[0.4375rem] text-sm leading-[1.375rem] transition-all sm:px-4",
              activeTab === "communications"
                ? "border-b-2 border-(--neutral-950) font-semibold text-(--neutral-950)"
                : "font-normal text-(--text-neutral-600) hover:text-(--neutral-950)",
            )}
          >
            <Letter size={20} className="size-5 shrink-0" />
            Communications
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("email")}
            className={cn(
              "relative flex cursor-pointer items-center justify-center gap-2 px-3 py-[0.4375rem] text-sm leading-[1.375rem] transition-all sm:px-4",
              activeTab === "email"
                ? "border-b-2 border-(--neutral-950) font-semibold text-(--neutral-950)"
                : "font-normal text-(--text-neutral-600) hover:text-(--neutral-950)",
            )}
          >
            <ClockCircle size={20} className="size-5 shrink-0" />
            Email History
          </button>
        </div>

        {activeTab === "communications" ? (
          <div className="space-y-6">
            {(isNotesLoading || isNotesFetching) &&
            mappedNotes.length === 0 &&
            !searchQuery.trim() &&
            selectedTypes.length === 0 &&
            !date?.from &&
            !date?.to ? (
              <div className="flex justify-center py-10">
                <ContentLoader size="md" className="min-h-0" />
              </div>
            ) : null}

            {mappedNotes.length > 0 ||
            Boolean(searchQuery.trim()) ||
            selectedTypes.length > 0 ||
            Boolean(date?.from || date?.to) ? (
              <div className="flex flex-wrap items-center gap-2">
                <CustomInput
                  placeholder="Search..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  icon={
                    <Search className="size-4.5 text-(--text-neutral-600)" />
                  }
                  className="min-h-10 w-full min-w-0 rounded-full pb-0 pt-1.75 sm:w-[14rem] md:w-64 lg:w-70"
                />
                <div className="w-full min-w-0 sm:w-[13rem] md:w-[15rem]">
                  <CustomMultiSelect
                    options={TYPE_OPTIONS}
                    value={selectedTypes}
                    onChange={setSelectedTypes}
                    placeholder="Filter by Type"
                    isSearch={false}
                    className="min-h-10"
                  />
                </div>
                <Popover
                  open={isDatePopoverOpen}
                  onOpenChange={(open) => {
                    if (open) {
                      setDraftDate(date);
                      setIsDatePopoverOpen(true);
                      return;
                    }
                    if (draftDate?.from && !draftDate?.to) {
                      return;
                    }
                    setIsDatePopoverOpen(false);
                  }}
                >
                  <PopoverTrigger asChild>
                    <button
                      className={cn(
                        "flex h-10 w-full cursor-pointer items-center justify-between gap-2 rounded-full border border-(--neutral-100) bg-white px-4 text-sm text-(--text-neutral-600) transition-colors hover:border-(--neutral-950) sm:w-auto sm:min-w-[12.5rem]",
                        !date && "text-(--text-neutral-600)",
                      )}
                    >
                      <span className="truncate">
                        {date?.from ? (
                          date.to ? (
                            <>
                              {format(date.from, "LLL dd, y")} -{" "}
                              {format(date.to, "LLL dd, y")}
                            </>
                          ) : (
                            format(date.from, "LLL dd, y")
                          )
                        ) : (
                          "Choose date range"
                        )}
                      </span>
                      <CalendarIcon
                        size={16}
                        className="size-4 shrink-0 text-(--text-neutral-600)"
                      />
                    </button>
                  </PopoverTrigger>
                  <PopoverContent className="w-80 rounded-xl p-4" align="start">
                    <RangeCalendar
                      currentMonth={currentMonth}
                      setCurrentMonth={setCurrentMonth}
                      selectedRange={draftDate}
                      onSelectRange={handleDateRangeChange}
                    />
                  </PopoverContent>
                </Popover>
                {date?.from || date?.to ? (
                  <Button
                    type="button"
                    variant="ghost"
                    className="h-10 cursor-pointer rounded-full px-3 text-(--text-neutral-600) hover:bg-(--neutral-100) hover:text-(--neutral-950)"
                    onClick={clearDateRange}
                  >
                    <X size={16} className="mr-1 size-4" />
                    Clear
                  </Button>
                ) : null}
                {!readOnly ? (
                  <Button
                    onClick={() => setIsAddModalOpen(true)}
                    className="h-10 w-full shrink-0 cursor-pointer rounded-full px-5 font-medium shadow-none sm:ml-auto sm:w-auto"
                  >
                    <Plus size={18} className="mr-0.5 size-[1.125rem]" />
                    Add Note
                  </Button>
                ) : null}
              </div>
            ) : null}

            {filteredNotes.length > 0 ? (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {filteredNotes.map((note) => (
                  <ClientNoteCard
                    key={note.id}
                    note={note}
                    readOnly={readOnly}
                    onEdit={handleEditNote}
                    onDelete={handleDeleteClick}
                  />
                ))}
              </div>
            ) : !(isNotesLoading || isNotesFetching) ? (
              mappedNotes.length > 0 ||
              Boolean(searchQuery.trim()) ||
              selectedTypes.length > 0 ||
              Boolean(date?.from || date?.to) ? (
                <div className="py-10 text-center text-sm text-(--text-neutral-600)">
                  No notes found.
                </div>
              ) : (
                <NotesEmptyState
                  showAddButton={!readOnly}
                  onAddNote={() => setIsAddModalOpen(true)}
                />
              )
            ) : null}
          </div>
        ) : (
          <div className="py-2">
            <ClientEmailHistory
              logs={mappedEmailEvents}
              isLoading={
                isEmailHistoryLoading ||
                (isEmailHistoryFetching && emailHistoryPage === 1)
              }
              isLoadingMore={isLoadingMoreEmailHistory}
              observerTarget={emailHistoryObserverTarget}
            />
          </div>
        )}
      </div>

      <AddNoteModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onAdd={(payload) => {
          void handleAddNote(payload);
        }}
        isSubmitting={isCreatingNote}
      />

      <EditNoteModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSave={(payload) => {
          void handleUpdateNote(payload);
        }}
        note={selectedNote}
        isSubmitting={isUpdatingNote}
      />

      <ConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Delete Note"
        description="Are you sure you want to delete this note? This action cannot be undone."
        confirmButtonText={deletingNoteId ? "Deleting..." : "Delete"}
        confirmButtonLoading={Boolean(deletingNoteId)}
        type="delete"
        items={[]}
      />
    </div>
  );
};

export default HistoryTab;
