import { CalendarIcon } from "@/components/icons/commonIcons";
import { useState } from "react";
import { Search, Plus } from "lucide-react";
import { Letter } from "@solar-icons/react-perf/category/messages/Linear/Letter";
import { ClockCircle } from "@solar-icons/react-perf/category/time/Linear/ClockCircle";
import { Button } from "@/components/ui/button";
import CustomMultiSelect from "@/components/form/CustomMultiSelect";
import TimeInStageCard from "./TimeInStageCard";
import ClientTimeline from "./ClientTimeline";
import ClientNoteCard, { type Note } from "./ClientNoteCard";
import AddNoteModal from "./AddNoteModal";
import EditNoteModal from "./EditNoteModal";
import ClientEmailHistory from "./ClientEmailHistory";
import ConfirmationModal from "@/components/shared/ConfirmationModal";
import NotesEmptyState from "@/components/shared/NotesEmptyState";
import { cn } from "../../../../lib/utils";
import { format } from "date-fns";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import RangeCalendar from "@/components/shared/RangeCalendar";
import CustomInput from "@/components/form/CustomInput";

// Default empty to match screenshot
const MOCK_NOTES: Note[] = [];

const HistoryTab = () => {
  const [activeTab, setActiveTab] = useState<"communications" | "email">(
    "communications",
  );
  // ... existing state ...
  const [notes, setNotes] = useState<Note[]>(MOCK_NOTES);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTypes, setSelectedTypes] = useState<string[]>([]);
  const [date, setDate] = useState<
    { from: Date | undefined; to: Date | undefined } | undefined
  >();
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date());
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedNoteId, setSelectedNoteId] = useState<string | null>(null);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // ... existing handlers ...
  const filteredNotes = notes.filter((note) => {
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

  const handleAddNote = (newNoteData: Omit<Note, "id" | "createdAt">) => {
    const newNote: Note = {
      id: Math.random().toString(36).substr(2, 9),
      createdAt: "Dec 18, 2025 - 2:06 PM", // Mock date for now
      ...newNoteData,
    };
    setNotes([newNote, ...notes]);
  };

  const handleDeleteClick = (id: string) => {
    setSelectedNoteId(id);
    setIsDeleteModalOpen(true);
  };

  const handleEditNote = (note: Note) => {
    setSelectedNote(note);
    setIsEditModalOpen(true);
  };

  const handleUpdateNote = (updatedNote: Note) => {
    setNotes((prev) =>
      prev.map((n) => (n.id === updatedNote.id ? updatedNote : n)),
    );
    setIsEditModalOpen(false);
    setSelectedNote(null);
  };

  const handleConfirmDelete = () => {
    if (selectedNoteId) {
      setNotes(notes.filter((n) => n.id !== selectedNoteId));
      setIsDeleteModalOpen(false);
      setSelectedNoteId(null);
    }
  };

  return (
    <>
      <div className="animate-in fade-in space-y-5 p-4 pt-2 duration-300 sm:p-6 sm:pt-2">
        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-3">
            <h3 className="text-base leading-6 font-semibold text-(--neutral-950)">
              Time in Each Stage
            </h3>
            <TimeInStageCard />
          </div>

          <ClientTimeline />
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
              {notes.length > 0 ||
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
                  <Popover>
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
                    <PopoverContent
                      className="w-80 rounded-xl p-4"
                      align="start"
                    >
                      <RangeCalendar
                        currentMonth={currentMonth}
                        setCurrentMonth={setCurrentMonth}
                        selectedRange={date}
                        onSelectRange={setDate}
                      />
                    </PopoverContent>
                  </Popover>
                  <Button
                    onClick={() => setIsAddModalOpen(true)}
                    className="h-10 w-full shrink-0 cursor-pointer rounded-full px-5 font-medium shadow-none sm:ml-auto sm:w-auto"
                  >
                    <Plus size={18} className="mr-0.5 size-[1.125rem]" />
                    Add Note
                  </Button>
                </div>
              ) : null}

              {filteredNotes.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  {filteredNotes.map((note) => (
                    <ClientNoteCard
                      key={note.id}
                      note={note}
                      onEdit={handleEditNote}
                      onDelete={handleDeleteClick}
                    />
                  ))}
                </div>
              ) : notes.length > 0 ||
                Boolean(searchQuery.trim()) ||
                selectedTypes.length > 0 ||
                Boolean(date?.from || date?.to) ? (
                <div className="py-10 text-center text-sm text-(--text-neutral-600)">
                  No notes found.
                </div>
              ) : (
                <NotesEmptyState onAddNote={() => setIsAddModalOpen(true)} />
              )}
            </div>
          ) : (
            <div className="py-2">
              <ClientEmailHistory />
            </div>
          )}
        </div>

        <AddNoteModal
          isOpen={isAddModalOpen}
          onClose={() => setIsAddModalOpen(false)}
          onAdd={handleAddNote}
        />

        <EditNoteModal
          isOpen={isEditModalOpen}
          onClose={() => setIsEditModalOpen(false)}
          onSave={handleUpdateNote}
          note={selectedNote}
        />

        <ConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={handleConfirmDelete}
          title="Delete Note"
          description="Are you sure you want to delete this note? This action cannot be undone."
          confirmButtonText="Delete"
          type="delete"
          items={[]}
        />
      </div>
    </>
  );
};

export default HistoryTab;
