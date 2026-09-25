import { useEffect, useState, useRef } from 'react';
import { X, Bold, Italic, Underline, List, ListOrdered, Eraser } from 'lucide-react';
import { Button } from '@/components/ui/button';
import CustomDatePicker from '@/components/form/CustomDatePicker';
import CustomSelect, { type CustomSelectOption } from '@/components/form/CustomSelect';
import { Input } from '@/components/ui/input';

interface AddNoteModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (note: {
      type: string;
      subject?: string;
      content: string;
      communicationDate: Date;
    }) => void;
    isSubmitting?: boolean;
}

const NOTE_TYPE_OPTIONS: CustomSelectOption[] = [
    { label: 'Phone call', value: 'call' },
    { label: 'Email', value: 'email' },
    { label: 'General Note', value: 'general' },
];

const RichTextEditor = ({ content, onChange }: { content: string, onChange: (html: string) => void }) => {
    const editorRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (editorRef.current && editorRef.current.innerHTML !== content) {
            editorRef.current.innerHTML = content;
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
            {/* Toolbar */}
            <div className="flex items-center gap-2 p-3 border-b border-gray-100 bg-white">
                <button
                    onClick={() => execCmd('bold')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                    title="Bold"
                >
                    <Bold size={18} strokeWidth={2.5} />
                </button>
                <button
                    onClick={() => execCmd('italic')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                >
                    <Italic size={18} strokeWidth={2.5} />
                </button>
                <button
                    onClick={() => execCmd('underline')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                >
                    <Underline size={18} strokeWidth={2.5} />
                </button>

                {/* Divider */}
                <div className="w-[0.0625rem] h-5 bg-gray-200 mx-1"></div>

                <button
                    onClick={() => execCmd('insertUnorderedList')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                >
                    <List size={18} strokeWidth={2.5} />
                </button>
                <button
                    onClick={() => execCmd('insertOrderedList')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                >
                    <ListOrdered size={18} strokeWidth={2.5} />
                </button>

                {/* Divider */}
                <div className="w-[0.0625rem] h-5 bg-gray-200 mx-1"></div>

                <button
                    onClick={() => execCmd('removeFormat')}
                    className="p-1 hover:bg-gray-100 rounded text-gray-700 transition-colors cursor-pointer"
                >
                    <Eraser size={18} strokeWidth={2.5} />
                </button>
            </div>

            {/* Editor Area */}
            <div
                ref={editorRef}
                contentEditable
                className="flex-1 w-full p-4 overflow-y-auto outline-none text-base text-gray-600 focus:bg-gray-50/10 transition-colors [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:list-decimal [&_ol]:pl-5"
                onInput={(e) => onChange(e.currentTarget.innerHTML)}
                style={{ minHeight: '9.375rem' }}
            >
            </div>
            {/* Placeholder emulation if needed, or simple text check */}
            {!content && (
                <div className="absolute top-[4.375rem] left-6 pointer-events-none text-gray-400">
                    Enter details here
                </div>
            )}
        </div>
    );
};

const AddNoteModalContent = ({ isOpen, onClose, onAdd, isSubmitting = false }: AddNoteModalProps) => {
    const [type, setType] = useState<string>('');
    const [communicationDate, setCommunicationDate] = useState<Date | null>(new Date());
    const [subject, setSubject] = useState('');
    const [content, setContent] = useState('');

    const resetForm = () => {
        setType('');
        setCommunicationDate(new Date());
        setSubject('');
        setContent('');
    };



    if (!isOpen) return null;

    const handleClose = () => {
        resetForm();
        onClose();
    };

    const handleSubmit = () => {
        if (!communicationDate) return;
        onAdd({
            type: type || 'general',
            subject,
            content: content,
            communicationDate,
        });
    };

    const isValid = !!type && !!communicationDate;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-200 p-4">
            <div className="bg-white rounded-2xl w-full max-w-[40.625rem] shadow-xl animate-in zoom-in-95 duration-200 flex max-h-[95vh] flex-col overflow-hidden">
                <div className="flex shrink-0 items-center justify-between border-b border-gray-100 px-6 py-5 sm:px-8">
                    <h2 className="text-xl font-semibold text-gray-900">Add Client Note</h2>
                    <button
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="text-gray-900 hover:text-gray-600 transition-colors cursor-pointer"
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="flex-1 min-h-0 overflow-y-auto px-6 py-5 sm:px-8">
                    <div className="space-y-5">
                        {/* Type Dropdown */}
                        <CustomSelect
                            label="Type of note"
                            placeholder=""
                            required
                            options={NOTE_TYPE_OPTIONS}
                            value={type}
                            onChange={setType}
                        />

                        {/* Communication Date */}
                        <CustomDatePicker
                            label="Communication Date"
                            required
                            date={communicationDate}
                            onDateChange={setCommunicationDate}
                        />
                        <p className="text-[0.8125rem] text-gray-400 ml-1 -mt-3 mb-2">When did this communication happen?</p>

                        {/* Subject */}
                        <Input
                            placeholder="Subject"
                            value={subject}
                            onChange={(e) => setSubject(e.target.value)}
                            className="h-14 rounded-xl border-gray-200 text-base px-4 focus-visible:ring-0 focus-visible:border-gray-400 placeholder:text-gray-400"
                        />

                        {/* Rich Text Editor */}
                        <div className="relative">
                            <RichTextEditor content={content} onChange={setContent} />

                        </div>
                    </div>
                </div>

                <div className="flex shrink-0 justify-end gap-3 border-t border-gray-100 px-6 py-5 sm:px-8">
                    <Button
                        variant="outline"
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="rounded-full h-11 px-8 font-medium text-gray-700 border-gray-200 hover:bg-gray-50 hover:text-gray-900 text-base cursor-pointer"
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={handleSubmit}
                        disabled={!isValid || isSubmitting}
                        className="rounded-full h-11 px-8 font-medium shadow-none text-base transition-colors"
                        style={{
                            backgroundColor: isValid ? '#344054' : '#E2E4E9',
                            color: isValid ? 'white' : '#9CA3AF',
                            cursor: isValid ? 'pointer' : 'not-allowed'
                        }}
                      loading={isSubmitting}
                      loadingLabel="Adding..."
                    >
                        Add note
                    </Button>
                </div>
            </div>
        </div>
    );
};

const AddNoteModal = (props: AddNoteModalProps) => props.isOpen ? <AddNoteModalContent {...props} /> : null;

export default AddNoteModal;
