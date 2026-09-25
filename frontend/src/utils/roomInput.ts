export const ROOM_FIELD_LIMITS = {
  roomNumber: 50,
  roomName: 255,
  equipment: 1000,
  capacityMin: 1,
  capacityMax: 1000,
} as const;

export const ROOM_NUMBER_PATTERN = /^[A-Za-z0-9 _-]+$/;

export function sanitizeRoomNumberInput(value: string): string {
  return value.replace(/[^A-Za-z0-9 _-]/g, "").slice(0, ROOM_FIELD_LIMITS.roomNumber);
}

export function sanitizeRoomCapacityInput(value: string): string {
  return value.replace(/\D/g, "").slice(0, 4);
}

export function isValidRoomCapacity(value?: string | null): boolean {
  const trimmed = value?.trim();
  if (!trimmed) return true;

  if (!/^\d+$/.test(trimmed)) return false;

  const parsed = Number.parseInt(trimmed, 10);
  return (
    Number.isFinite(parsed) &&
    parsed >= ROOM_FIELD_LIMITS.capacityMin &&
    parsed <= ROOM_FIELD_LIMITS.capacityMax
  );
}
