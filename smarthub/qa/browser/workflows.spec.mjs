import {test,expect,fixtures,loginBrowser,loginApi,createTask,jsonResponse,items} from './fixtures.mjs';

test('[ROLE-01] browser: admin password login and persisted client list', async({page})=>{
  await loginBrowser(page);
  await page.goto('/admin/clients');
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true}).first()).toBeVisible();
  await expect(page.getByText(fixtures.beta.client.name,{exact:true})).toHaveCount(0);
  await page.reload();
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true}).first()).toBeVisible();
});

test('[ROLE-04] browser: therapist sees assigned client but not another organization', async({page})=>{
  await loginBrowser(page,fixtures.alpha,'therapist');
  await page.goto('/therapist/clients');
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true}).first()).toBeVisible();
  await expect(page.getByText('QA Alpha Unassigned',{exact:true})).toHaveCount(0);
  await expect(page.getByText(fixtures.beta.client.name,{exact:true})).toHaveCount(0);
});

test('[FILTER-05] browser: client search changes returned records and clear restores them', async({page})=>{
  await loginBrowser(page);
  await page.goto('/admin/clients');
  const search=page.getByPlaceholder(/search/i).first();
  await search.fill('QA Alpha Unassigned');
  await expect(page.getByText('QA Alpha Unassigned',{exact:true}).first()).toBeVisible();
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true})).toHaveCount(0);
  await search.fill('qa-nonexistent-record');
  await expect(page.getByText('QA Alpha Unassigned',{exact:true})).toHaveCount(0);
  await search.fill('');
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true}).first()).toBeVisible();
});

test('[SETTINGS-01] browser: settings tabs load real option data and rooms', async({page})=>{
  await loginBrowser(page);
  await page.goto('/admin/settings');
  await expect(page.getByRole('button',{name:'System Options',exact:true})).toBeVisible();
  const response=page.waitForResponse(r=>r.url().includes('/api/v1/rooms')&&r.request().method()==='GET');
  await page.getByRole('button',{name:'Therapy Rooms',exact:true}).click();
  expect((await response).status()).toBe(200);
  await expect(page.getByRole('button',{name:/add.*room/i})).toBeVisible();
});

test('[TASK-01] browser: task comment submitted through UI persists in API and after reload', async({page,request})=>{
  const headers=await loginApi(request);
  const task=await createTask(request,headers);
  await loginBrowser(page);
  await page.goto('/admin/tasks');
  await page.getByPlaceholder('Search tasks, clients...').fill(task.title);
  await expect(page.getByText(task.title,{exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'View Comments',exact:true})).toHaveCount(1);
  await page.getByRole('button',{name:'View Comments',exact:true}).click();
  const content=`QA comment ${crypto.randomUUID()}`;
  await page.getByPlaceholder(/comment/i).fill(content);
  const savedComment=page.waitForResponse(r=>r.url().endsWith(`/api/v1/tasks/${task.id}/comments`)&&r.request().method()==='POST');
  await page.getByRole('button',{name:/post|send|add comment/i}).click();
  expect((await savedComment).status()).toBe(201);
  await expect(page.getByPlaceholder(/comment/i)).toHaveValue('');
  await expect(page.getByText(content,{exact:true})).toBeVisible();
  const comments=items(await jsonResponse(await request.get(`/api/v1/tasks/${task.id}/comments`,{headers})));
  expect(comments.some(c=>c.content===content)).toBeTruthy();
  await page.reload();
  await page.getByPlaceholder('Search tasks, clients...').fill(task.title);
  await expect(page.getByRole('button',{name:'View Comments',exact:true})).toHaveCount(1);
  await page.getByRole('button',{name:'View Comments',exact:true}).click();
  await expect(page.getByText(content,{exact:true})).toBeVisible();
});

test('[ROLE-02] browser: anonymous protected route returns to authentication', async({page})=>{
  await page.goto('/admin/clients');
  await expect(page).toHaveURL(/\/auth\/(staff\/)?login/);
  await expect(page.getByText(fixtures.alpha.client.name,{exact:true})).toHaveCount(0);
});

test('[ROLE-06] browser: client portal login loads own appointments and survives reload', async({page})=>{
  await page.goto('/auth/login');
  const appointments=page.waitForResponse(r=>r.url().includes('/api/v1/portal/me/sessions-history')&&r.url().includes('scope=past')&&r.request().method()==='GET');
  await page.getByRole('textbox',{name:/Email/}).fill(fixtures.alpha.client.email);
  await page.getByLabel(/^Password/).fill(fixtures.alpha.client.password);
  await page.getByRole('button',{name:'Sign In',exact:true}).click();
  expect((await appointments).status()).toBe(200);
  await expect(page).toHaveURL(/\/user\/appointments/);
  await page.getByRole('button',{name:/^Previous/}).click();
  await expect(page.getByText('QA Alpha Therapist',{exact:true}).first()).toBeVisible();
  await page.reload();
  await expect(page).toHaveURL(/\/user\/appointments/);
  await page.getByRole('button',{name:/^Previous/}).click();
  await expect(page.getByText('QA Alpha Therapist',{exact:true}).first()).toBeVisible();
  await expect(page.getByText(fixtures.beta.client.name,{exact:true})).toHaveCount(0);
});
