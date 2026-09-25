import type { Note } from './ClientNoteCard';
import type { CustomSelectOption } from '@/components/form/CustomSelect';
import SharedEditNoteModal, {
    type EditNoteValues,
} from '@/components/clients/shared/EditNoteModal';

interface EditNoteModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (updatedNote: Note) => void;
    note: Note | null;
}

const NOTE_TYPE_OPTIONS: CustomSelectOption[] = [
    { label: 'Phone call', value: 'Call' },
    { label: 'Email', value: 'Email' },
    { label: 'General Note', value: 'General Note' },
];

const getInitialCommunicationDate = (note: Note) => new Date(note.createdAt.split(' - ')[0]);

const EditNoteModal = ({ isOpen, onClose, onSave, note }: EditNoteModalProps) => {
    const handleSubmit = (values: EditNoteValues, currentNote: Note) => {
        onSave({
            ...currentNote,
            type: values.type as Note['type'],
            subject: values.subject,
            content: values.content,
            createdAt:
                values.communicationDate.toLocaleDateString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                }) +
                ' - ' +
                new Date().toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }),
        });
    };

    return (
        <SharedEditNoteModal
            isOpen={isOpen}
            onClose={onClose}
            onSubmit={handleSubmit}
            note={note}
            noteTypeOptions={NOTE_TYPE_OPTIONS}
            defaultNoteType="General Note"
            getInitialCommunicationDate={getInitialCommunicationDate}
            closeAfterSubmit
        />
    );
};

export default EditNoteModal;
