import { CalendarIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Phone, Mail, FileText, Pencil } from "lucide-react";
import { Button } from '@/components/ui/button';
import { sanitizeHtml } from '@/utils/sanitizeHtml';

export interface Note {
    id: string;
    type: string;
    subject?: string; // Optional derived form "type" or specific subject
    content: string;
    createdAt: string;
    eventDate?: string;
}

interface ClientNoteCardProps {
    note: Note;
    readOnly?: boolean;
    onEdit: (note: Note) => void;
    onDelete: (id: string) => void;
}

const ClientNoteCard = ({ note, readOnly = false, onEdit, onDelete }: ClientNoteCardProps) => {

    // Helper to get icon based on type
    const getIcon = () => {
        switch ((note.type || "").toLowerCase()) {
            case 'call': return <Phone size={16} />;
            case 'email': return <Mail size={16} />;
            default: return <FileText size={16} />;
        }
    };

    const formatType = (value: string) => {
        const normalized = (value || "general").toLowerCase().replace(/[_\s]+/g, " ");
        return normalized.charAt(0).toUpperCase() + normalized.slice(1);
    };

    return (
        <div className="bg-white border border-gray-200 rounded-xl flex flex-col hover:shadow-sm transition-shadow overflow-hidden">
            <div className="p-5">
                <h4 className="mb-2 line-clamp-2 break-all text-base font-semibold text-gray-900">
                    {note.subject || "Lorem ipsum dolor sit amet"}
                </h4>

                <div
                    className="mb-2 line-clamp-3 break-all text-sm text-gray-500 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5"
                    dangerouslySetInnerHTML={{ __html: sanitizeHtml(note.content) }}
                />
            </div>

            <div className="mt-auto flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 bg-gray-50 px-5 py-3">
                <div className="flex min-w-0 flex-1 flex-wrap items-center gap-3 text-sm text-gray-600">
                    <div className="flex min-w-0 items-center gap-2">
                        <CalendarIcon size={18} className="text-gray-700" />
                        <span className="truncate text-[0.8125rem] text-gray-600">Created: {note.createdAt}</span>
                    </div>

                    <div className="h-4 w-[0.0625rem] bg-gray-200 mx-1"></div>

                    <div className="flex min-w-0 items-center gap-2">
                        {getIcon()}
                        <span className="truncate text-[0.8125rem] text-gray-600">
                            {formatType(note.type)}
                        </span>
                    </div>
                </div>

                {!readOnly ? (
                <div className="flex shrink-0 items-center gap-2">
                    <Button
                        variant="ghost"
                        size="icon"
                        className="h-9 w-9 bg-white border border-gray-200 text-gray-700 hover:text-gray-900 hover:bg-gray-50 rounded-xl shadow-sm cursor-pointer"
                        onClick={() => onEdit(note)}
                    >
                        <Pencil size={18} />
                    </Button>
                    <Button
                        variant="ghost"
                        size="icon"
                        className="h-9 w-9 bg-white border border-gray-200 text-red-500 hover:text-red-600 hover:bg-red-50 rounded-xl shadow-sm cursor-pointer"
                        onClick={() => onDelete(note.id)}
                    >
                        <TrashIcon size={18} />
                    </Button>
                </div>
                ) : null}
            </div>
        </div>
    );
};

export default ClientNoteCard;
