import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useState, useCallback, useEffect, useMemo } from "react";
import { Search, Edit2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import SettingsLayout from "./SettingsLayout";
import CustomInput from "../form/CustomInput";
import AddRoomModal from "./AddRoomModal";
import DeleteConfirmationModal from "./DeleteConfirmationModal";
import type { RoomFormData } from "@/schemas/settings.schema";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import ScrollToTopButton from "@/components/shared/ScrollToTopButton";
import Toast from "@/components/shared/Toast";
import {
  useCreateAdminRoomMutation,
  useDeleteAdminRoomMutation,
  useGetAdminRoomsQuery,
  useLazyGetAdminRoomByIdQuery,
  useUpdateAdminRoomMutation,
  type AdminRoom,
  type RoomRequestPayload,
} from "@/store/api/admin/rooms.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { isValidRoomCapacity } from "@/utils/roomInput";

function mapRoomToFormData(room: AdminRoom): RoomFormData {
  return {
    roomNumber: room.roomNumber,
    roomName: room.roomName,
    capacity: room.capacity ? String(room.capacity) : "",
    equipment: room.equipment ?? "",
    roomType: "PHYSICAL",
    isActive: room.isActive,
  };
}

function mapFormToRoomPayload(data: RoomFormData): RoomRequestPayload {
  const capacityRaw = data.capacity?.trim();
  let capacity: number | undefined;
  if (capacityRaw && isValidRoomCapacity(capacityRaw)) {
    capacity = Number.parseInt(capacityRaw, 10);
  }

  return {
    roomNumber: data.roomNumber.trim(),
    roomName: data.roomName.trim(),
    capacity,
    equipment: data.equipment?.trim() || undefined,
    roomType: "PHYSICAL",
    isActive: data.isActive,
  };
}

const TherapyRooms = () => {
  const [searchQuery, setSearchQuery] = useState("");
  const [displayedItems, setDisplayedItems] = useState(20);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const itemsPerPage = 20;
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [editingRoom, setEditingRoom] = useState<RoomFormData | null>(null);
  const [activeEditingId, setActiveEditingId] = useState<number | null>(null);
  const [activeStatusToggleId, setActiveStatusToggleId] = useState<number | null>(null);
  const [roomToDelete, setRoomToDelete] = useState<{
    id: number;
    number: string;
  } | null>(null);
  const { data: rooms = [], isLoading: isRoomsLoading, isFetching: isRoomsFetching } =
    useGetAdminRoomsQuery();
  const [triggerGetRoomById, { isFetching: isLoadingRoomDetails }] =
    useLazyGetAdminRoomByIdQuery();
  const [createRoom, { isLoading: isCreatingRoom }] = useCreateAdminRoomMutation();
  const [updateRoom, { isLoading: isUpdatingRoom }] = useUpdateAdminRoomMutation();
  const [deleteRoom, { isLoading: isDeletingRoom }] = useDeleteAdminRoomMutation();

  useEffect(() => {
    if (!toastMessage) return;
    const timer = window.setTimeout(() => setToastMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  const filteredRooms = useMemo(
    () =>
      rooms.filter(
        (r) =>
          r.roomName.toLowerCase().includes(searchQuery.toLowerCase()) ||
          r.roomNumber.toLowerCase().includes(searchQuery.toLowerCase()),
      ),
    [rooms, searchQuery],
  );

  const paginatedRooms = filteredRooms.slice(0, displayedItems);

  const toggleRoomStatus = async (room: AdminRoom) => {
    setActiveStatusToggleId(room.id);
    try {
      await updateRoom({
        roomId: room.id,
        body: {
          roomNumber: room.roomNumber,
          roomName: room.roomName,
          capacity: room.capacity,
          equipment: room.equipment,
          roomType: "PHYSICAL",
          isActive: !room.isActive,
        },
      }).unwrap();
      setToastType("success");
      setToastMessage(
        !room.isActive ? "Room activated successfully." : "Room deactivated successfully.",
      );
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      setActiveStatusToggleId(null);
    }
  };

  const handleLoadMore = useCallback(() => {
    setIsLoadingMore(true);
    setDisplayedItems((prev) => prev + itemsPerPage);
    setIsLoadingMore(false);
  }, []);

  const { observerTarget } = useInfiniteScroll({
    onLoadMore: handleLoadMore,
    hasMore: displayedItems < filteredRooms.length,
    isLoading: isLoadingMore,
  });

  const handleSaveRoom = async (data: RoomFormData) => {
    try {
      if (activeEditingId) {
        await updateRoom({
          roomId: activeEditingId,
          body: mapFormToRoomPayload(data),
        }).unwrap();
        setToastType("success");
        setToastMessage("Room updated successfully.");
      } else {
        await createRoom(mapFormToRoomPayload(data)).unwrap();
        setToastType("success");
        setToastMessage("Room created successfully.");
      }

      setIsAddModalOpen(false);
      setEditingRoom(null);
      setActiveEditingId(null);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleConfirmDelete = async () => {
    if (!roomToDelete) return;

    try {
      await deleteRoom(roomToDelete.id).unwrap();
      setToastType("success");
      setToastMessage("Room deleted successfully.");
      setRoomToDelete(null);
      setIsDeleteModalOpen(false);
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleEditRoom = async (roomId: number) => {
    setActiveEditingId(roomId);
    setEditingRoom(null);
    setIsAddModalOpen(true);

    try {
      const room = await triggerGetRoomById(roomId).unwrap();
      setEditingRoom(mapRoomToFormData(room));
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      setIsAddModalOpen(false);
      setActiveEditingId(null);
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <SettingsLayout
        title="Therapy Rooms"
        description="Configure rooms available for scheduling sessions"
        actionLabel="Add Room"
        onAction={() => {
          setEditingRoom(null);
          setActiveEditingId(null);
          setIsAddModalOpen(true);
        }}
        toolbar={
          <div className="w-full max-w-md">
            <CustomInput
              placeholder="Search by room number or name..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setDisplayedItems(20);
              }}
              icon={<Search className="size-4.5 text-(--text-neutral-600)" />}
              className="min-h-10 w-full rounded-full pb-0 pt-1.75 shadow-xs"
            />
          </div>
        }
      >
        <ScrollToTopButton />
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-4">
          <div className="min-h-0 flex-1 overflow-auto rounded-2xl border border-(--neutral-100) custom-scrollbar">
            <table className="w-full text-left border-collapse">
              <thead className="sticky top-0 bg-(--bg-primary-50) border-b border-(--neutral-100) z-10">
                <tr>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Room Number
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Room Name
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Capacity
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark)">
                    Status
                  </th>
                  <th className="p-4 text-sm font-semibold text-(--text-primary-dark) text-right">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--neutral-100)">
                {isRoomsLoading ? (
                  <tr>
                    <td
                      colSpan={5}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      <ContentLoader size="md" className="gap-2" />
                    </td>
                  </tr>
                ) : paginatedRooms.length > 0 ? (
                  paginatedRooms.map((room) => (
                    <tr
                      key={room.id}
                      className="hover:bg-(--bg-primary-light) transition-colors"
                    >
                      <td className="max-w-48 p-4 text-sm text-(--text-primary-dark)">
                        <div className="truncate" title={room.roomNumber}>
                          {room.roomNumber}
                        </div>
                      </td>
                      <td className="max-w-64 p-4 text-sm text-(--text-primary-dark)">
                        <div className="truncate" title={room.roomName}>
                          {room.roomName}
                        </div>
                      </td>
                      <td className="p-4 text-sm text-(--text-primary-dark)">
                        {room.capacity ?? "---"}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <Switch
                            checked={room.isActive}
                            disabled={activeStatusToggleId === room.id}
                            onCheckedChange={() => void toggleRoomStatus(room)}
                          />
                          <span className="text-sm text-(--text-primary-dark)">
                            {activeStatusToggleId === room.id
                              ? "Updating..."
                              : room.isActive
                                ? "Active"
                                : "Inactive"}
                          </span>
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => void handleEditRoom(room.id)}
                            className="p-2 hover:bg-slate-100 rounded-lg transition-colors text-(--text-neutral-500) hover:text-(--text-primary-dark) cursor-pointer"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => {
                              setRoomToDelete({ id: room.id, number: room.roomNumber });
                              setIsDeleteModalOpen(true);
                            }}
                            className="p-2 hover:bg-red-50 rounded-lg transition-colors text-(--text-neutral-500) hover:text-red-500 cursor-pointer"
                          >
                            <TrashIcon size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan={5}
                      className="p-10 text-center text-sm text-(--text-neutral-500)"
                    >
                      No rooms found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>

            <div
              ref={observerTarget}
              className="h-10 w-full flex items-center justify-center py-8"
            >
              {isLoadingMore ||
                (displayedItems < filteredRooms.length && (
                  <ContentLoader variant="inline" size="md" />
                ))}
              {!isRoomsLoading && isRoomsFetching && displayedItems >= filteredRooms.length ? (
                <ContentLoader variant="inline" size="md" />
              ) : null}
            </div>
          </div>
        </div>

        <AddRoomModal
          isOpen={isAddModalOpen}
          onClose={() => {
            setIsAddModalOpen(false);
            setEditingRoom(null);
            setActiveEditingId(null);
          }}
          onSave={(data) => void handleSaveRoom(data)}
          initialData={editingRoom || undefined}
          isSaving={isCreatingRoom || isUpdatingRoom}
          isLoadingInitialData={Boolean(activeEditingId && isLoadingRoomDetails)}
        />

        <DeleteConfirmationModal
          isOpen={isDeleteModalOpen}
          onClose={() => setIsDeleteModalOpen(false)}
          onConfirm={() => void handleConfirmDelete()}
          title={`Delete "Room ${roomToDelete?.number || ""}"`}
          description="Are you sure you want to delete this room?"
          isDeleting={isDeletingRoom}
        />
      </SettingsLayout>
    </div>
  );
};

export default TherapyRooms;
