import { built, expect, journey, markBuilt, note, test } from '../lib/journey.mjs';
import { client, task, therapist } from '../lib/fixtures.mjs';
import { chooseSelect } from '../lib/forms.mjs';
import { need } from '../flows/prerequisites.mjs';
import {
  addComment,
  bell,
  closeComments,
  createTask,
  deleteTask,
  openComments,
  openTaskHistory,
  openTasks,
  openTherapistSession,
  setTaskStatus,
  taskCard,
} from '../flows/tasks.mjs';

/**
 * The admin task board: a task is created for a client out of the seeded option
 * catalogue, the client's own therapist is assigned without being asked, the edit
 * dialog is the one road a status change has, completing a task retires it from the
 * active board into the history, and deleting one removes it for good.
 */
test.describe('tasks', { tag: '@tasks' }, () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async () => {
    test.setTimeout(600000);
    await need('client');
  });

  test('an empty task cannot be created', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openTasks();
    await page.getByRole('button', { name: 'Add task' }).click();
    await expect(page.getByRole('heading', { name: /Create Task for/ }), 'the create dialog opens')
      .toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Create task', exact: true }),
      'a task without a title and client cannot even be submitted',
    ).toBeDisabled();
    note('an empty task cannot be submitted at all');
    await page.getByRole('button', { name: 'Cancel' }).last().click();
  });

  test('a task is created for the client and assigned to their therapist', async () => {
    test.skip(built('task'), 'resuming: the earlier run already created this task');
    await createTask(task);
    journey.state.taskAssignedAt = new Date().toISOString();
    markBuilt('task');
  });

  test('the card carries everything the task was created with', { tag: '@verify' }, async () => {
    await openTasks();
    const card = taskCard(task.title);
    await expect(card, 'the task is on the board').toBeVisible({ timeout: 30000 });
    await expect(card.getByText(client.name), 'the card names the client').toBeVisible();
    await expect(card.getByText(therapist.name), 'the card names the assigned therapist').toBeVisible();
    await expect(card.getByText('High', { exact: true }), 'the card wears its priority badge').toBeVisible();
    await expect(card.getByText('Pending', { exact: true }), 'the card wears its status badge').toBeVisible();
    note('the card shows the client, the therapist and the chosen priority and status');
  });

  test('a comment travels onto the task and survives an edit', async () => {
    const { page } = journey;
    await openTasks();
    await openComments(task.title);
    await addComment(task.comment);
    await expect(page.getByText(/Comments \(\d+\)/).first(), 'the thread counts it').toBeVisible();

    // The author can rewrite their own comment in place.
    await page.getByTitle('Edit comment').first().click();
    const editor = page.getByRole('textbox').last();
    await editor.fill(task.commentEdited);
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByText(task.commentEdited).first(), 'the rewrite replaces the comment')
      .toBeVisible({ timeout: 20000 });
    await expect(page.getByText(task.comment, { exact: true }), 'and the old wording is gone')
      .toBeHidden();
    await closeComments();
    note('a comment was added, rewritten in place, and kept');
  });

  test('the filters pick the task out of the pile', { tag: '@verify' }, async () => {
    const { page } = journey;
    await openTasks();
    await page.getByRole('button', { name: 'All tasks', exact: true }).click();
    await page.getByRole('button', { name: 'All time', exact: true }).click();

    // The search finds it by name, and a nonsense search finds nothing.
    // The search box only expands on hover or focus; filling focuses it without a
    // pointer, which the collapsed pill would otherwise intercept.
    const search = page.getByPlaceholder('Search tasks, clients...');
    await search.fill(task.title);
    await expect(taskCard(task.title), 'the search picks the task out').toBeVisible({ timeout: 30000 });
    await search.fill('nothing-carries-this-name');
    await expect(page.getByText('No tasks found').first(), 'a nonsense search comes back empty')
      .toBeVisible({ timeout: 30000 });
    await search.fill('');

    // The status filter honours what the task is — and is not.
    await page.getByText('Filters', { exact: true }).first().click();
    await chooseSelect(page, page, 'Status', 'Pending');
    await page.getByRole('button', { name: 'Apply filters' }).click();
    await expect(taskCard(task.title), 'the Pending filter keeps the pending task')
      .toBeVisible({ timeout: 30000 });

    // The filter list now comes from the seeded catalogue, so the exclusion check
    // can use Cancelled — a status the task never had.
    await page.getByText('Filters', { exact: true }).first().click();
    await chooseSelect(page, page, 'Status', 'Cancelled');
    await page.getByRole('button', { name: 'Apply filters' }).click();
    await expect(page.getByText('No tasks found').first(), 'the Cancelled filter leaves the pending task behind')
      .toBeVisible({ timeout: 30000 });

    // Two "Clear all" buttons exist — the chips row behind the overlay and the
    // panel's own; the panel's sits right before its Apply button, so it is
    // anchored off that sibling instead of matched by name across the page.
    await page.getByText('Filters', { exact: true }).first().click();
    // Clearing applies and closes the panel by itself — no Apply click after it.
    await page.getByRole('button', { name: 'Apply filters' })
      .locator('xpath=preceding-sibling::button[normalize-space()="Clear all"]').click();
    await expect(taskCard(task.title), 'clearing the filters brings the task back')
      .toBeVisible({ timeout: 30000 });
    note('search, time range and the status filter all pick the task out correctly');
  });

  test('the assignee sees the task, is told about it, and can answer', async () => {
    test.setTimeout(420000);
    const { context, page: staffPage } = await openTherapistSession();
    try {
      // The assignment reaches the assignee twice over: a "Task Assigned" bell
      // row in the app, and a "Task Assigned: <title> (<MRN>)" email.
      await bell(staffPage).click();
      await expect(
        staffPage.getByRole('heading', { name: 'Notifications' }).first(),
        'the bell opens',
      ).toBeVisible({ timeout: 30000 });
      await expect(
        staffPage.getByRole('heading', { name: 'Task Assigned' }).first(),
        'the assignment raised a Task Assigned bell row',
      ).toBeVisible({ timeout: 30000 });
      note('the assignee has a Task Assigned notification under the bell');
      await staffPage.keyboard.press('Escape');

      const assignedSince = new Date(journey.state.taskAssignedAt ?? Date.now() - 30 * 60 * 1000);
      const assignmentMail = await journey.mailbox.waitForMessage({
        to: therapist.email,
        subjectIncludes: `Task Assigned: ${task.title}`,
        since: assignedSince,
      });
      expect(
        assignmentMail.subject,
        'the email subject carries the task title and the client record',
      ).toMatch(new RegExp(`^Task Assigned: ${task.title} \\(.+\\)$`));
      note(`the assignee received "${assignmentMail.subject}"`);

      // Their own task board lists it, with the client on the card.
      await staffPage.goto('/therapist/tasks');
      const card = taskCard(task.title, staffPage);
      await expect(card, "the task is on the assignee's own board").toBeVisible({ timeout: 30000 });
      await expect(card.getByText(client.name), 'with the client it belongs to').toBeVisible();

      // The conversation crosses accounts: the admin's comment is there to answer.
      await openComments(task.title, staffPage);
      await expect(
        staffPage.getByText(task.commentEdited).first(),
        "the administrator's comment reached the assignee",
      ).toBeVisible({ timeout: 20000 });
      await addComment(task.therapistComment, staffPage);
      await closeComments(staffPage);
      note('the assignee saw the task on their own board and answered the comment');
    } finally {
      await context.close();
    }

    // The answer travels back to the administrator's side of the same thread.
    await openTasks();
    await openComments(task.title);
    await expect(
      journey.page.getByText(task.therapistComment).first(),
      "the assignee's answer reached the administrator",
    ).toBeVisible({ timeout: 20000 });
    await closeComments();
  });

  test('the edit dialog walks the task through its statuses', async () => {
    await openTasks();
    await setTaskStatus(task.title, 'In Progress');
    await expect(
      taskCard(task.title).getByText('In Progress', { exact: true }),
      'the card follows into the new status',
    ).toBeVisible({ timeout: 30000 });
    note('the status change followed onto the card');
  });

  test('a completed task retires from the active board into the history', async () => {
    const { page } = journey;
    await openTasks();
    await setTaskStatus(task.title, 'Completed');
    await expect(
      page.getByRole('heading', { name: task.title, exact: true }),
      'the active board lets go of the completed task without a reload',
    ).toBeHidden({ timeout: 30000 });
    note('the completed task left the active board the moment the save landed');

    await openTaskHistory();
    await page.getByRole('button', { name: 'Completed', exact: true }).click();
    await expect(taskCard(task.title), 'the history keeps the completed task')
      .toBeVisible({ timeout: 30000 });
    note('the completed task left the active board and turned up in the history');
  });

  test('a completed task can no longer be edited', { tag: '@negative' }, async () => {
    const { page } = journey;
    await openTaskHistory();
    await page.getByRole('button', { name: 'Completed', exact: true }).click();
    const card = taskCard(task.title);
    await expect(card).toBeVisible({ timeout: 30000 });
    await card.getByRole('button', { name: 'Open task actions' }).click();
    const menu = page.locator('[data-radix-popper-content-wrapper]').last();
    await expect(menu.getByRole('button', { name: 'Edit Task' }), 'editing a signed-off task is refused')
      .toBeDisabled();
    await page.keyboard.press('Escape');
    note('a completed task offers its details but no edit');
  });

  test('a deleted task is gone from the board', async () => {
    await createTask({ title: task.discardTitle });
    await deleteTask(task.discardTitle);
    note('the throwaway task was created and deleted cleanly');
  });
});
