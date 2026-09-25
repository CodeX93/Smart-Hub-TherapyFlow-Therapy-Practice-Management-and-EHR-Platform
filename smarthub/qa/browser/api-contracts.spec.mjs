import {test,expect,fixtures,loginApi,createTask,jsonResponse,items} from './fixtures.mjs';

test('[ROLE-02] API: anonymous and restricted staff cannot list tasks',async({request})=>{
  // Spring's configured entrypoint currently returns Forbidden for anonymous access.
  expect((await request.get('/api/v1/tasks')).status()).toBe(403);
  const headers=await loginApi(request,fixtures.alpha,'restricted');
  expect((await request.get('/api/v1/tasks',{headers})).status()).toBe(403);
});

test('[ROLE-01] API: wrong password is rejected',async({request})=>{
  const response=await request.post('/api/v1/auth/login',{data:{username:fixtures.alpha.admin.email,password:'wrong-password',orgSlug:fixtures.alpha.slug}});
  expect(response.status()).toBe(401);
});

test('[ROLE-02] API: separate tenant client lists remain isolated',async({request})=>{
  for(const tenant of [fixtures.alpha,fixtures.beta]) {
    const headers=await loginApi(request,tenant);
    const rows=items(await jsonResponse(await request.get('/api/v1/clients',{headers})));
    expect(rows.map(c=>c.fullName)).toContain(tenant.client.name);
    expect(rows.map(c=>c.fullName)).not.toContain(tenant===fixtures.alpha?fixtures.beta.client.name:fixtures.alpha.client.name);
  }
});

test('[TASK-01] API: task and comment create/edit/delete round trip',async({request})=>{
  const headers=await loginApi(request),task=await createTask(request,headers);
  const base=`/api/v1/tasks/${task.id}/comments`;
  const comment=await jsonResponse(await request.post(base,{headers,data:{content:'QA original',isInternal:false}}),201);
  await jsonResponse(await request.put(`${base}/${comment.id}`,{headers,data:{content:'QA edited',isInternal:false}}));
  expect(items(await jsonResponse(await request.get(base,{headers}))).find(c=>c.id===comment.id)?.content).toBe('QA edited');
  expect((await request.delete(`${base}/${comment.id}`,{headers})).status()).toBe(204);
  expect(items(await jsonResponse(await request.get(base,{headers}))).some(c=>c.id===comment.id)).toBe(false);
  expect((await request.delete(`/api/v1/tasks/${task.id}`,{headers})).status()).toBe(204);
  expect((await request.get(`/api/v1/tasks/${task.id}`,{headers})).status()).toBe(404);
});

test('[TASK-01] API: blank comment validation does not create a record',async({request})=>{
  const headers=await loginApi(request),task=await createTask(request,headers);
  const base=`/api/v1/tasks/${task.id}/comments`;
  expect((await request.post(base,{headers,data:{content:'   '}})).status()).toBe(400);
  expect(items(await jsonResponse(await request.get(base,{headers})))).toHaveLength(0);
});

for(const [field,value] of [['search','unique'],['status','pending'],['priority','high'],['assignedToId','admin'],['clientId','client']]) {
  test(`[FILTER-02] API: task ${field} filter selects expected fixture`,async({request})=>{
    const headers=await loginApi(request),title=`QA unique ${crypto.randomUUID()}`;
    const selected=await createTask(request,headers,{title,priority:'high'});
    const other=await createTask(request,headers,{title:`QA excluded ${crypto.randomUUID()}`,priority:'low',status:'completed',clientId:fixtures.alpha.unassignedClientId,assignedToId:fixtures.alpha.therapist.id});
    const actual=value==='unique'?title:value==='admin'?fixtures.alpha.admin.id:value==='client'?fixtures.alpha.client.id:value;
    const rows=items(await jsonResponse(await request.get('/api/v1/tasks',{headers,params:{[field]:actual,pageSize:100}})));
    expect(rows.map(r=>r.id)).toContain(selected.id);expect(rows.map(r=>r.id)).not.toContain(other.id);
  });
}

for(const [field,value,expected] of [['hasPortalAccess','true','assigned'],['hasPortalAccess','false','unassigned'],['unassigned','true','unassigned'],['therapistId','therapist','assigned']]) {
  test(`[FILTER-02] API: client ${field}=${value} filter`,async({request})=>{
    const headers=await loginApi(request);
    const rows=items(await jsonResponse(await request.get('/api/v1/clients',{headers,params:{[field]:value==='therapist'?fixtures.alpha.therapist.id:value}})));
    const want=expected==='assigned'?fixtures.alpha.client.id:fixtures.alpha.unassignedClientId;
    const omit=expected==='assigned'?fixtures.alpha.unassignedClientId:fixtures.alpha.client.id;
    expect(rows.map(c=>c.id)).toContain(want);expect(rows.map(c=>c.id)).not.toContain(omit);
  });
}

test('[FILTER-06] API: client pagination has no duplicates',async({request})=>{
  const headers=await loginApi(request);
  const first=items(await jsonResponse(await request.get('/api/v1/clients',{headers,params:{page:1,pageSize:1,sortBy:'fullName',sortOrder:'asc'}})));
  const second=items(await jsonResponse(await request.get('/api/v1/clients',{headers,params:{page:2,pageSize:1,sortBy:'fullName',sortOrder:'asc'}})));
  expect(first).toHaveLength(1);expect(second).toHaveLength(1);expect(first[0].id).not.toBe(second[0].id);
});

test('[ZOOM-01] API: fresh therapist has no configured Zoom credentials',async({request})=>{
  const headers=await loginApi(request,fixtures.alpha,'therapist');
  const body=await jsonResponse(await request.get('/api/v1/users/me/zoom-credentials/status',{headers}));
  expect(body.clientSecret).toBeFalsy();
  expect(body.accessToken).toBeFalsy();
  expect(body.refreshToken).toBeFalsy();
  expect(body.configured ?? body.isConfigured).toBe(false);
});
