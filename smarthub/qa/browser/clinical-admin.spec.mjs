import {test,expect,fixtures,loginApi,loginPortal,jsonResponse,items} from './fixtures.mjs';

test('[SETTINGS-01] API: custom option create/update/read persists and stays tenant scoped',async({request})=>{
  const headers=await loginApi(request);
  const key='qa_'+crypto.randomUUID().replaceAll('-','');
  const category=await jsonResponse(await request.post('/api/v1/system-options/categories',{headers,data:{categoryKey:key,categoryName:'QA category',isSystem:false,isActive:true}}),201);
  const option=await jsonResponse(await request.post('/api/v1/system-options',{headers,data:{categoryId:category.id,optionKey:'qa_option',optionLabel:'QA original',isActive:true,isSystem:false,sortOrder:1}}),201);
  await jsonResponse(await request.put(`/api/v1/system-options/${option.id}`,{headers,data:{optionLabel:'QA renamed',isActive:true}}));
  const updated=await jsonResponse(await request.get(`/api/v1/system-options/${option.id}`,{headers}));expect(updated.optionLabel).toBe('QA renamed');
  const beta=await loginApi(request,fixtures.beta);
  const categories=items(await jsonResponse(await request.get('/api/v1/system-options/categories',{headers:beta})));
  expect(categories.map(c=>c.categoryKey)).not.toContain(key);
});

test('[SETTINGS-01] API: practice setting save/read and restricted-role denial',async({request})=>{
  const headers=await loginApi(request);
  await jsonResponse(await request.put('/api/v1/practice-configuration',{headers,data:{practiceName:'QA changed practice',practiceAddress:'Synthetic QA address'}}));
  const body=await jsonResponse(await request.get('/api/v1/practice-configuration',{headers}));expect(body.practiceName).toBe('QA changed practice');
  const restricted=await loginApi(request,fixtures.alpha,'restricted');
  expect((await request.put('/api/v1/practice-configuration',{headers:restricted,data:{practiceName:'Unauthorized change'}})).status()).toBe(403);
  expect((await jsonResponse(await request.get('/api/v1/practice-configuration',{headers}))).practiceName).toBe('QA changed practice');
});

test('[NOTIFY-01] API: in-app notification recipient, unread state, mark-read and deletion',async({request})=>{
  const headers=await loginApi(request);
  const notification=await jsonResponse(await request.post('/api/v1/notifications',{headers,data:{userId:fixtures.alpha.admin.id,type:'FORM_ASSIGNED',category:'FORM',title:'QA notification',message:'Synthetic QA message',priority:'MEDIUM'}}),201);
  const list=items(await jsonResponse(await request.get('/api/v1/notifications',{headers,params:{unreadOnly:true,pageSize:100}})));
  expect(list.map(n=>n.id)).toContain(notification.id);
  const other=await loginApi(request,fixtures.alpha,'therapist');
  expect(items(await jsonResponse(await request.get('/api/v1/notifications',{headers:other}))).map(n=>n.id)).not.toContain(notification.id);
  expect((await request.patch(`/api/v1/notifications/${notification.id}/read`,{headers})).status()).toBe(200);
  expect(items(await jsonResponse(await request.get('/api/v1/notifications',{headers,params:{unreadOnly:true,pageSize:100}}))).map(n=>n.id)).not.toContain(notification.id);
  expect((await request.delete(`/api/v1/notifications/${notification.id}`,{headers})).status()).toBe(200);
});

test('[CHECK-01] API: checklist template, assignment, completion and reload',async({request})=>{
  const headers=await loginApi(request),base='/api/v1/checklists';
  const template=await jsonResponse(await request.post(`${base}/checklist-templates`,{headers,data:{name:`QA checklist ${crypto.randomUUID()}`,isActive:true,items:[{title:'QA required item',isRequired:true,category:'intake',itemOrder:1}]}}),201);
  const assignment=await jsonResponse(await request.post(`${base}/clients/${fixtures.alpha.client.id}/checklists`,{headers,data:{templateId:template.id}}),201);
  const list=items(await jsonResponse(await request.get(`${base}/client-checklist-items/${assignment.id}`,{headers})));expect(list).toHaveLength(1);
  await jsonResponse(await request.put(`${base}/client-checklist-items/${list[0].id}`,{headers,data:{isCompleted:true,notes:'QA completed'}}));
  const after=items(await jsonResponse(await request.get(`${base}/client-checklist-items/${assignment.id}`,{headers})));expect(after[0].isCompleted).toBe(true);
});

test('[FORMS-01] API: form template, required field, assignment and response persistence',async({request})=>{
  const headers=await loginApi(request),base='/api/v1/forms';
  const template=await jsonResponse(await request.post(`${base}/templates`,{headers,data:{name:`QA form ${crypto.randomUUID()}`,category:'intake',requiresSignature:false,isActive:true,fields:[{fieldType:'text',label:'QA response',isRequired:true,sortOrder:1}]}}),201);
  const assignment=await jsonResponse(await request.post(`${base}/assignments`,{headers,data:{templateId:template.id,clientId:fixtures.alpha.client.id}}),201);
  const fields=items(await jsonResponse(await request.get(`${base}/templates/${template.id}/fields`,{headers})));expect(fields).toHaveLength(1);
  const portalHeaders=await loginPortal(request);
  await jsonResponse(await request.post('/api/v1/portal/forms/responses',{headers:portalHeaders,data:{assignmentId:assignment.id,fieldId:fields[0].id,value:'QA supplied answer'}}));
  const saved=items(await jsonResponse(await request.get(`${base}/assignments/${assignment.id}/responses`,{headers})));
  expect(JSON.stringify(saved)).toContain('QA supplied answer');
});

test('[NOTE-01] API: session note draft persistence, finalization and amendment',async({request})=>{
  const headers=await loginApi(request),base='/api/v1/session-notes';
  const note=await jsonResponse(await request.post(base,{headers,data:{sessionId:fixtures.alpha.completedSessionId,clientId:fixtures.alpha.client.id,
    therapistId:fixtures.alpha.therapist.id,date:fixtures.alpha.sessionDate,sessionFocus:'QA focus',draftContent:'Synthetic QA note',isDraft:true,aiEnabled:false}}),201);
  expect((await jsonResponse(await request.get(`${base}/${note.id}`,{headers}))).sessionFocus).toBe('QA focus');
  await jsonResponse(await request.post(`${base}/${note.id}/finalize`,{headers}));
  await jsonResponse(await request.post(`${base}/${note.id}/amendments`,{headers,data:{amendmentText:'QA amendment',reason:'QA correction'}}),201);
  const history=items(await jsonResponse(await request.get(`${base}/${note.id}/amendments`,{headers})));expect(JSON.stringify(history)).toContain('QA amendment');
});

test('[MONEY-06] API: partial payment then percentage discount uses outstanding balance',async({request})=>{
  const headers=await loginApi(request),base='/api/v1/billing';
  const bill=await jsonResponse(await request.post(`${base}/sessions/${fixtures.alpha.completedSessionId}/billing`,{headers,data:{sessionId:fixtures.alpha.completedSessionId}}),201);
  expect(Number(bill.totalAmount)).toBe(100);
  const paid=await jsonResponse(await request.post(`${base}/billing/${bill.id}/record-payment`,{headers,data:{paymentMethod:'cash',paymentSide:'client',paymentAmount:40}}));
  expect(Number(paid.remainingDue)).toBe(60);
  const discounted=await jsonResponse(await request.patch(`${base}/billing/${bill.id}/discount`,{headers,data:{discountType:'percentage',discountValue:10}}));
  expect(Number(discounted.discountAmount)).toBe(6);expect(Number(discounted.remainingDue)).toBe(54);
  expect((await request.patch(`${base}/billing/${bill.id}/discount`,{headers,data:{discountType:'percentage',discountValue:20}})).status()).toBe(400);
});
