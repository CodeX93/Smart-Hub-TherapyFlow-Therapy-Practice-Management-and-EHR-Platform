import fs from 'node:fs';
import path from 'node:path';
import { test as base, expect } from '@playwright/test';

export const fixtures = JSON.parse(fs.readFileSync(path.join(process.env.QA_RUNTIME_DIR, 'ready.json'), 'utf8'));
export const test = base.extend({
  page: async ({page}, use) => {
    // Browser regression must not silently contact production APIs, analytics or providers.
    await page.route('**/*', route => {
      const u = new URL(route.request().url());
      if (['127.0.0.1','localhost'].includes(u.hostname) || ['data:','blob:'].includes(u.protocol)) return route.continue();
      return route.abort('blockedbyclient');
    });
    await use(page);
  },
});
export { expect };

export async function loginApi(request, tenant = fixtures.alpha, actor = 'admin') {
  const account = tenant[actor];
  const response = await request.post('/api/v1/auth/login', {data:{username:account.email,password:account.password,orgSlug:tenant.slug}});
  expect(response.status(), `real ${actor} password login status`).toBe(200);
  const body = await response.json();
  expect(body.accessToken, 'login issued an access token without a synthetic principal').toBeTruthy();
  return {Authorization:`Bearer ${body.accessToken}`};
}

export async function loginBrowser(page, tenant = fixtures.alpha, actor = 'admin') {
  await page.goto('/auth/staff/login');
  await page.getByRole('textbox', {name:/Email \/ Username/}).fill(tenant[actor].email);
  await page.getByLabel(/^Password/).fill(tenant[actor].password);
  await page.getByRole('button',{name:'Sign In',exact:true}).click();
  await expect(page).toHaveURL(new RegExp(`/${actor==='restricted'?'staff':actor}/`));
}

export async function jsonResponse(response, status = 200) {
  const detail=response.status()===status?'':(await response.text()).slice(0,1200);
  expect(response.status(), `HTTP ${response.url()}: ${detail}`).toBe(status);
  return response.json();
}

export async function createTask(request, headers, overrides={}) {
  return jsonResponse(await request.post('/api/v1/tasks',{headers,data:{
    title:`QA task ${crypto.randomUUID()}`,priority:'medium',status:'pending',clientId:fixtures.alpha.client.id,
    assignedToId:fixtures.alpha.admin.id,dueDate:new Date(Date.now()+86400000).toISOString(),...overrides,
  }}),201);
}

export function items(body) {
  const result = Array.isArray(body) ? body : body.items ?? body.content;
  expect(Array.isArray(result), 'response contains a record collection').toBeTruthy();
  return result;
}

export async function loginPortal(request, tenant = fixtures.alpha) {
  const response = await request.post('/api/v1/portal/login', {data:{email:tenant.client.email,password:tenant.client.password,orgSlug:tenant.slug}});
  const body = await jsonResponse(response);
  expect(body.accessToken).toBeTruthy();
  return {Authorization:`Bearer ${body.accessToken}`};
}
