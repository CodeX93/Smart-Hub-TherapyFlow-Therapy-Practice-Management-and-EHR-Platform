import { CalendarIcon, TrashIcon } from "@/components/icons/commonIcons";
import { Phone, Mail, FileText, Pencil } from "lucide-react";
import { Button } from '@/components/ui/button';
import { sanitizeHtml } from '@/utils/sanitizeHtml';

export interface Note {
    id: string;
    type: 'Call' | 'Email' | 'General Note';
    subject?: string; // Optional derived form "type" or specific subject
    content: string;
    createdAt: string;
}

interface ClientNoteCardProps {
    note: Note;
    onEdit: (note: Note) => void;
    onDelete: (id: string) => void;
}

const ClientNoteCard = ({ note, onEdit, onDelete }: ClientNoteCardProps) => {

    // Helper to get icon based on type
    const getIcon = () => {
        switch (note.type) {
            case 'Call': return <Phone size={16} />;
            case 'Email': return <Mail size={16} />;
            default: return <FileText size={16} />;
        }
    };

    return (
        <div className="bg-white border border-gray-200 rounded-xl flex flex-col hover:shadow-sm transition-shadow overflow-hidden">
            <div className="p-5">
                <h4 className="text-base font-semibold text-gray-900 mb-2">
                    {note.subject || "Lorem ipsum dolor sit amet"}
                </h4>

                <div
                    className="text-sm text-gray-500 line-clamp-3 mb-2 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5"
                    dangerouslySetInnerHTML={{ __html: sanitizeHtml(note.content) }}
                />
            </div>

            <div className="bg-gray-50 px-5 py-3 flex items-center justify-between border-t border-gray-100 mt-auto">
                <div className="flex items-center gap-3 text-sm text-gray-600">
                    <div className="flex items-center gap-2">
                        <CalendarIcon size={18} className="text-gray-700" />
                        <span className="text-gray-600 text-[0.8125rem]">Created: {note.createdAt}</span>
                    </div>

                    <div className="h-4 w-[0.0625rem] bg-gray-200 mx-1"></div>

                    <div className="flex items-center gap-2">
                        {getIcon()}
                        <span className="text-gray-600 text-[0.8125rem]">{note.type}</span>
                    </div>
                </div>

                <div className="flex items-center gap-2">
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
            </div>
        </div>
    );
};

export default ClientNoteCard;
