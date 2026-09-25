import ConfirmationModal from "../../../shared/ConfirmationModal";
import type { Client } from "../../../../types/client.type";

interface DeleteClientModalProps {
  client: Client | null;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isConfirmPending?: boolean;
}

const DeleteClientModal = ({
  client,
  isOpen,
  onClose,
  onConfirm,
  isConfirmPending = false,
}: DeleteClientModalProps) => {
  if (!client) return null;

  return (
    <ConfirmationModal
      type="delete"
      isOpen={isOpen}
      onClose={onClose}
      onConfirm={onConfirm}
      clientName={client.name}
      confirmButtonLoading={isConfirmPending}
    />
  );
};

export default DeleteClientModal;
