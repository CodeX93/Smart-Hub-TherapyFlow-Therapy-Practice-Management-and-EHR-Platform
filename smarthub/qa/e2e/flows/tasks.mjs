import { expect } from '@playwright/test';
import { chooseSelect, fillText, nextWeekdayDate, pickCalendarDate } from '../lib/forms.mjs';
import { pickMenuItem, signInFully, toastText } from '../lib/ui.mjs';
import { journey, note } from '../lib/journey.mjs';
import { browserContextOptions } from '../lib/config.mjs';
import { client, therapist } from '../lib/fixtures.mjs';
import { codeFromMail } from './organisation.mjs';

/**
 * The admin task board: tasks are created for a client, carry the seeded title,
 * type, priority and status options, and the only way a task changes status is the
 * edit dialog — there is no drag and no shortcut, so the dialog is the whole story.
 */

export async function openTasks() {
  const { page } = journey;
  await page.goto('/admin/tasks', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('button', { name: 'Add task' }), 'the task board opens')
    .toBeVisible({ timeout: 30000 });
}

export async function openTaskHistory() {
  const { page } = journey;
  await page.goto('/admin/tasks/history', { waitUntil: 'domcontentloaded' });
  await expect(page.getByPlaceholder('Search task history...'), 'the task history opens')
    .toBeVisible({ timeout: 30000 });
}

/** The card a task occupies on the board, found by its title heading. */
export function taskCard(title, page = journey.page) {
  return page
    .getByRole('heading', { name: title, exact: true })
    .first()
    .locator('xpath=ancestor::div[3]');
}

/**
 * Creates a task for the journey's client. The title select is fed by the seeded
 * task_titles options; "Other (Custom Title)" keeps the run's name unique. The
 * assignee is not chosen at all: the client's own therapist fills in by himself.
 */
export async function createTask({ title, description }) {
  const { page } = journey;
  await openTasks();
  await page.getByRole('button', { name: 'Add task' }).click();
  await expect(page.getByRole('heading', { name: /Create Task for/ }), 'the create dialog opens')
    .toBeVisible();

  await chooseSelect(page, page, 'Task Title', 'Other (Custom Title)');
  await fillText(page, 'Custom title', title);
  if (description) await fillText(page, 'Description', description);
  await chooseSelect(page, page, 'Client', client.name);
  // The assignee field is disabled on purpose; choosing the client filled it in.
  await expect(page.getByText(therapist.name).first(), "the client's therapist was assigned by itself")
    .toBeVisible({ timeout: 20000 });
  const priority = await chooseSelect(page, page, 'Priority', 'High');
  const status = await chooseSelect(page, page, 'Status', 'Pending');
  await pickCalendarDate(page, page, 'Due Date', nextWeekdayDate('Monday', 2));

  await page.getByRole('button', { name: 'Create task', exact: true }).click();
  await toastText(page, /Task created successfully/i, 30000);
  await expect(taskCard(title), 'the new task is on the board').toBeVisible({ timeout: 30000 });
  note(`task "${title}" created for ${client.name} (${priority}, ${status})`);
  return { priority, status };
}

/** Opens the card's actions menu and picks one entry. */
export async function taskAction(title, action) {
  const { page } = journey;
  const card = taskCard(title);
  await expect(card, `the task "${title}" is on the board`).toBeVisible({ timeout: 30000 });
  await card.getByRole('button', { name: 'Open task actions' }).click();
  await pickMenuItem(page, action);
}

/** Moves a task to a new status through the edit dialog — the only path there is. */
export async function setTaskStatus(title, status) {
  const { page } = journey;
  await taskAction(title, 'Edit Task');
  await expect(page.getByRole('heading', { name: /Edit Task for/ }), 'the edit dialog opens')
    .toBeVisible();
  // The dialog re-renders when its option catalogue refetches, which can snap the
  // status menu shut mid-choice; clicking the trigger again recovers cleanly.
  for (let attempt = 1; ; attempt++) {
    try {
      await chooseSelect(page, page, 'Status', status);
      break;
    } catch (error) {
      if (attempt >= 3) throw error;
      note(`the status menu closed underneath the choice; retrying (${attempt})`);
    }
  }
  await page.getByRole('button', { name: 'Save changes' }).click();
  await toastText(page, /Task updated successfully/i, 30000);
  note(`task "${title}" moved to ${status}`);
}

export async function deleteTask(title) {
  const { page } = journey;
  await taskAction(title, 'Delete Task');
  await expect(page.getByRole('heading', { name: 'Delete task?' }), 'the delete dialog opens')
    .toBeVisible();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await toastText(page, /Task deleted successfully/i, 30000);
  await expect(journey.page.getByRole('heading', { name: title, exact: true }), 'the task is gone')
    .toBeHidden({ timeout: 30000 });
  note(`task "${title}" deleted`);
}

/** Opens a task's comment thread — the same dialog on the admin and therapist sides. */
export async function openComments(title, page = journey.page) {
  const card = taskCard(title, page);
  await expect(card, `the task "${title}" is on the board`).toBeVisible({ timeout: 30000 });
  await card.getByRole('button', { name: 'View Comments' }).click();
  await expect(page.getByRole('heading', { name: 'Task Comments' }), 'the comment thread opens')
    .toBeVisible({ timeout: 20000 });
}

/** Writes one comment into the open thread and waits for it to be listed. */
export async function addComment(text, page = journey.page) {
  await page.getByPlaceholder('Add comments here...').fill(text);
  await page.getByRole('button', { name: 'Add Comment' }).click();
  await expect(page.getByText(text).first(), 'the comment joins the thread')
    .toBeVisible({ timeout: 20000 });
  note(`comment added: "${text}"`);
}

export async function closeComments(page = journey.page) {
  await page.getByRole('button', { name: 'Close task comments' }).click();
  await expect(page.getByRole('heading', { name: 'Task Comments' })).toBeHidden();
}

/**
 * Signs the journey's therapist into their own browser context — a second pair of
 * eyes next to the administrator's — and hands back the context to close after use.
 * The first sign-in may be asked to enrol a second factor; the code is read from
 * the same mailbox the rest of the run uses.
 */
export async function openTherapistSession() {
  const context = await journey.browser.newContext(browserContextOptions);
  const page = await context.newPage();
  const result = await signInFully(page, {
    path: '/auth/staff/login',
    username: therapist.username,
    password: journey.state.therapistPassword ?? therapist.password,
    landedUrl: /\/therapist\//,
    activationPassword: `${therapist.password}x`,
    codeFor: (method, sentAt) =>
      method === 'SMS'
        ? Promise.reject(new Error('SMS codes have no automated reader in this suite'))
        : codeFromMail(therapist.email, sentAt),
    trustDevice: false,
    log: note,
  });
  if (!result.landed) {
    await context.close();
    throw new Error('the therapist could not sign in');
  }
  journey.state.therapistPassword = result.password ?? therapist.password;
  note(`therapist ${therapist.username} signed in on their own session`);
  return { context, page };
}

/** The visible notification bell on whichever layout the page shows. */
export function bell(page = journey.page) {
  return page.locator('button:has(svg.lucide-bell)').filter({ visible: true }).first();
}
