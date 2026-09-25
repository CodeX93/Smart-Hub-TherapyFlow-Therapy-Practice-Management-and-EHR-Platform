import type { Note } from './ClientNoteCard';
import type { CustomSelectOption } from '@/components/form/CustomSelect';
import SharedEditNoteModal, {
    type EditNoteValues,
} from '@/components/clients/shared/EditNoteModal';

interface EditNoteModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (updatedNote: {
      id: string;
      type: string;
      subject?: string;
      content: string;
      communicationDate: Date;
    }) => void;
    note: Note | null;
    isSubmitting?: boolean;
}

const NOTE_TYPE_OPTIONS: CustomSelectOption[] = [
    { label: 'Phone call', value: 'call' },
    { label: 'Email', value: 'email' },
    { label: 'General Note', value: 'general' },
];

const getInitialCommunicationDate = (note: Note) =>
    note.eventDate ? new Date(note.eventDate) : new Date(note.createdAt);

const EditNoteModal = ({ isOpen, onClose, onSave, note, isSubmitting = false }: EditNoteModalProps) => {
    const handleSubmit = (values: EditNoteValues, currentNote: Note) => {
        onSave({
            id: currentNote.id,
            ...values,
        });
    };

    return (
        <SharedEditNoteModal
            isOpen={isOpen}
            onClose={onClose}
            onSubmit={handleSubmit}
            note={note}
            noteTypeOptions={NOTE_TYPE_OPTIONS}
            defaultNoteType="general"
            getInitialCommunicationDate={getInitialCommunicationDate}
            closeAfterSubmit={false}
            submittingState={{ isSubmitting, loadingLabel: 'Saving...' }}
        />
    );
};

export default EditNoteModal;
