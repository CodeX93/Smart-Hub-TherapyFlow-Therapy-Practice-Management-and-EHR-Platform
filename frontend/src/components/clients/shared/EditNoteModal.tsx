import { useEffect, useRef, useState } from 'react';
import { Bold, Eraser, Italic, List, ListOrdered, Underline, X } from 'lucide-react';
import CustomDatePicker from '@/components/form/CustomDatePicker';
import CustomSelect, { type CustomSelectOption } from '@/components/form/CustomSelect';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export interface EditableNote {
    id: string;
    type: string;
    subject?: string;
    content: string;
    createdAt: string;
    eventDate?: string;
}

export interface EditNoteValues {
    type: string;
    subject: string;
    content: string;
    communicationDate: Date;
}

interface SharedEditNoteModalProps<TNote extends EditableNote> {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (values: EditNoteValues, note: TNote) => void;
    note: TNote | null;
    noteTypeOptions: CustomSelectOption[];
    defaultNoteType: string;
    getInitialCommunicationDate: (note: TNote) => Date;
    closeAfterSubmit: boolean;
    submittingState?: {
        isSubmitting: boolean;
        loadingLabel: string;
    };
}

interface EditNoteFormProps<TNote extends EditableNote>
    extends Omit<SharedEditNoteModalProps<TNote>, 'isOpen' | 'note'> {
    note: TNote;
}

const RichTextEditor = ({ content, onChange }: { content: string; onChange: (html: string) => void }) => {
    const editorRef = useRef<HTMLDivElement>(null);
    const hasInitialized = useRef(false);

    useEffect(() => {
        if (content && editorRef.current && !hasInitialized.current) {
            editorRef.current.innerHTML = content;
            hasInitialized.current = true;
        }

        if (!content && editorRef.current && editorRef.current.innerHTML !== '') {
            // Preserve the editor's existing content while it has focus.
        }
    }, [content]);

    const execCmd = (command: string, value: string | undefined = undefined) => {
        document.execCommand(command, false, value);
        if (editorRef.current) {
            onChange(editorRef.current.innerHTML);
        }
        editorRef.current?.focus();
    };

    return (
        <div className="border border-gray-200 rounded-xl overflow-hidden flex flex-col h-[17.5rem]">
            <div className="flex items-center gap-2 p-3 border-b border-gray-100 bg-white">
                <button
                    type="button"
                    onClick={() => execCmd('bold')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    title="Bold"
                    aria-label="Bold"
                >
                    <Bold size={18} strokeWidth={2.5} />
                </button>
                <button
                    type="button"
                    onClick={() => execCmd('italic')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    aria-label="Italic"
                >
                    <Italic size={18} strokeWidth={2.5} />
                </button>
                <button
                    type="button"
                    onClick={() => execCmd('underline')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    aria-label="Underline"
                >
                    <Underline size={18} strokeWidth={2.5} />
                </button>

                <div className="w-[0.0625rem] h-5 bg-gray-200 mx-1"></div>

                <button
                    type="button"
                    onClick={() => execCmd('insertUnorderedList')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    aria-label="Bulleted list"
                >
                    <List size={18} strokeWidth={2.5} />
                </button>
                <button
                    type="button"
                    onClick={() => execCmd('insertOrderedList')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    aria-label="Numbered list"
                >
                    <ListOrdered size={18} strokeWidth={2.5} />
                </button>

                <div className="w-[0.0625rem] h-5 bg-gray-200 mx-1"></div>

                <button
                    type="button"
                    onClick={() => execCmd('removeFormat')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    aria-label="Clear formatting"
                >
                    <Eraser size={18} strokeWidth={2.5} />
                </button>
            </div>

            <div
                ref={editorRef}
                contentEditable
                className="flex-1 w-full p-4 overflow-y-auto outline-none text-base text-gray-600 focus:bg-gray-50/10 transition-colors [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5"
                onInput={(event) => onChange(event.currentTarget.innerHTML)}
                style={{ minHeight: '9.375rem' }}
            />
        </div>
    );
};

const EditNoteForm = <TNote extends EditableNote>({
    onClose,
    onSubmit,
    note,
    noteTypeOptions,
    defaultNoteType,
    getInitialCommunicationDate,
    closeAfterSubmit,
    submittingState,
}: EditNoteFormProps<TNote>) => {
    const [type, setType] = useState(note.type);
    const [communicationDate, setCommunicationDate] = useState<Date | null>(() => {
        const parsedDate = getInitialCommunicationDate(note);
        return isNaN(parsedDate.getTime()) ? new Date() : parsedDate;
    });
    const [subject, setSubject] = useState(note.subject || '');
    const [content, setContent] = useState(note.content || '');

    const isSubmitting = submittingState?.isSubmitting ?? false;
    const isValid = !!type && !!communicationDate;

    const handleSubmit = () => {
        if (!communicationDate) return;

        onSubmit(
            {
                type: type || defaultNoteType,
                subject,
                content,
                communicationDate,
            },
            note,
        );

        if (closeAfterSubmit) {
            onClose();
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-200 p-4">
            <div className="bg-white rounded-2xl w-full max-w-[40.625rem] shadow-xl animate-in zoom-in-95 duration-200 flex max-h-[95vh] flex-col overflow-hidden">
                <div className="flex shrink-0 items-center justify-between border-b border-gray-100 px-6 py-5 sm:px-8">
                    <h2 className="text-xl font-semibold text-gray-900">Edit Client Note</h2>
                    {submittingState ? (
                        <Button
                            type="button"
                            variant="ghost"
                            size="icon-sm"
                            onClick={onClose}
                            disabled={isSubmitting}
                            aria-label="Close edit note"
                        >
                            <X size={20} aria-hidden="true" />
                        </Button>
                    ) : (
                        <button
                            type="button"
                            onClick={onClose}
                            aria-label="Close edit note"
                            className="text-gray-900 hover:text-gray-600 transition-colors cursor-pointer"
                        >
                            <X size={20} />
                        </button>
                    )}
                </div>

                <div className="flex-1 min-h-0 overflow-y-auto px-6 py-5 sm:px-8">
                    <div className="space-y-5">
                        <CustomSelect
                            label="Type of note"
                            placeholder=""
                            required
                            options={noteTypeOptions}
                            value={type}
                            onChange={setType}
                        />

                        <CustomDatePicker
                            label="Communication Date"
                            required
                            date={communicationDate}
                            onDateChange={setCommunicationDate}
                        />
                        <p className="text-[0.8125rem] text-gray-400 ml-1 -mt-3 mb-2">
                            When did this communication happen?
                        </p>

                        <Input
                            placeholder="Subject"
                            value={subject}
                            onChange={(event) => setSubject(event.target.value)}
                            className="h-14 rounded-xl border-gray-200 text-base px-4 focus-visible:ring-0 focus-visible:border-gray-400 placeholder:text-gray-400"
                        />

                        <div className="relative">
                            <RichTextEditor content={content} onChange={setContent} />
                        </div>
                    </div>
                </div>

                <div className="flex shrink-0 justify-end gap-3 border-t border-gray-100 px-6 py-5 sm:px-8">
                    <Button
                        variant="secondary"
                        size="lg"
                        onClick={onClose}
                        disabled={submittingState ? isSubmitting : undefined}
                    >
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        size="lg"
                        onClick={handleSubmit}
                        disabled={!isValid || isSubmitting}
                        loading={submittingState ? isSubmitting : undefined}
                        loadingLabel={submittingState?.loadingLabel}
                    >
                        Save
                    </Button>
                </div>
            </div>
        </div>
    );
};

const SharedEditNoteModal = <TNote extends EditableNote>({
    isOpen,
    note,
    ...formProps
}: SharedEditNoteModalProps<TNote>) => {
    if (!isOpen || !note) return null;

    const formKey = [
        note.id,
        note.type,
        note.subject,
        note.content,
        note.createdAt,
        note.eventDate,
    ].join('\u0000');

    return <EditNoteForm key={formKey} note={note} {...formProps} />;
};

export default SharedEditNoteModal;
