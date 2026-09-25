import { expect } from '@playwright/test';
import { fillText } from '../lib/forms.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { room } from '../lib/fixtures.mjs';

export async function openRoomsSettings() {
  await journey.page.goto('/admin/settings?tab=therapy-rooms');
}

/** Fills the Add Room form with `values` and leaves it unsubmitted. */
export async function fillRoomForm(values) {
  const { page } = journey;
  await page.getByRole('button', { name: 'Add Room', exact: true }).first().click();
  await expect(page.getByRole('heading', { name: 'Add New Room' })).toBeVisible();
  await fillText(page, 'Room Number', values.number);
  await fillText(page, 'Room Name', values.name);
  if (values.capacity !== undefined) await fillText(page, 'Capacity', values.capacity);
  if (values.equipment !== undefined) await fillText(page, 'Equipment', values.equipment);
}

export async function submitRoomForm() {
  await journey.page.getByRole('button', { name: 'Add Room', exact: true }).last().click();
}

export async function createRoom() {
  const { page } = journey;
  await openRoomsSettings();
  await fillRoomForm(room);
  await submitRoomForm();
  await expect(page.getByText(room.name).first(), 'the room is listed after saving')
    .toBeVisible({ timeout: 30000 });
  markBuilt('room');
  note(`therapy room ${room.number} — ${room.name} created`);
}

/** Opens the saved room in its editor; the row's controls are icon-only. */
export async function openRoomForEdit() {
  const { page } = journey;
  await openRoomsSettings();
  const roomRow = page.getByRole('row').filter({ hasText: room.number }).first();
  await expect(roomRow, 'the room is listed').toBeVisible({ timeout: 30000 });
  await roomRow.getByRole('button').first().click();
  await expect(page.getByRole('heading', { name: 'Edit Room' }), 'the room opens for editing')
    .toBeVisible({ timeout: 30000 });
}
