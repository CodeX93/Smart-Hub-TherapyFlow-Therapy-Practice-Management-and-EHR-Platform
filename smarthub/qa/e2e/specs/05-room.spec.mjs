import { readText } from '../lib/forms.mjs';
import { built, expect, journey, note, test } from '../lib/journey.mjs';
import { room } from '../lib/fixtures.mjs';
import { need } from '../flows/prerequisites.mjs';
import { createRoom, fillRoomForm, openRoomForEdit, openRoomsSettings, submitRoomForm } from '../flows/rooms.mjs';

/** A therapy room: the clinic needs one before a therapist can be given a place to work. */
test.describe('therapy room', { tag: '@room' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('administrator');
  });

  test('the administrator adds a therapy room', async () => {
    test.skip(built('room'), 'resuming: the earlier run already created this room');
    await createRoom();
  });

  test('a duplicate room number is refused', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openRoomsSettings();
    await fillRoomForm({ ...room, name: `${room.name} duplicate` });
    await submitRoomForm();
    await expect(
      page.getByText(/already|exists|duplicate|unique|in use/i).first(),
      'the duplicate room number is refused',
    ).toBeVisible({ timeout: 30000 });
    note('duplicate room number refused');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('reopening the room shows every value that was saved', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openRoomForEdit();
    // These labels carry a hint ("Room Number * e.g. VR-02"), so read them the tolerant way.
    await expect.poll(async () => readText(page, 'Room Number'), { timeout: 20000 }).toBe(room.number);
    expect(await readText(page, 'Room Name'), 'the room name came back').toBe(room.name);
    expect(await readText(page, 'Capacity'), 'the capacity came back').toBe(room.capacity);
    expect(await readText(page, 'Equipment'), 'the equipment came back').toBe(room.equipment);
    note('therapy room number, name, capacity and equipment verified after reopening');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });
});
