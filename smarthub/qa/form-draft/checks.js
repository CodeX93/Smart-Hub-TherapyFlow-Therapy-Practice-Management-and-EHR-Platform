async page => {
 const results=[]; const errors=[]; page.on('pageerror',e=>errors.push(e.message));
 const fields=[{id:11,fieldId:101,label:'First required answer',isRequired:true},{id:12,fieldId:102,label:'Second required answer',isRequired:true},{id:13,fieldId:103,label:'Optional note',isRequired:false}].map(f=>({...f,fieldType:'TEXT',placeholder:null,helpText:null,options:null,sortOrder:f.id,sectionTitle:null}));
 const answers={1:{},2:{}}; let status='ASSIGNED'; let fail=false; let failRead=false; let submitted=0; let signed=0; let signature=null;
 const assignment={id:1,templateId:1,clientId:1,templateName:'Draft regression form',templateCategory:'CUSTOM',assignedAt:'2026-09-11T12:00:00Z',createdAt:'2026-09-11T12:00:00Z',dueDate:'2026-09-18T12:00:00Z',completedAt:null,submittedAt:null,instructions:'Save answers before signing.',requiresSignature:true};
 const base=page.url().match(/^https?:\/\/[^/]+/)[0];
 await page.route('**/*',async route=>{
  const url=route.request().url(); if(!url.startsWith(base+'/'))return route.abort();
  const path=url.slice(base.length).split('?')[0]; const req=route.request();
  if(!path.startsWith('/api/'))return route.continue();
  const json=(body,status=200)=>route.fulfill({status,json:body});
  if(path==='/api/v1/portal/me')return json({id:1,fullName:'QA Draft Client',email:'draft@example.test'});
  if(path==='/api/v1/forms/assignments')return json([{...assignment,status}]);
  if(/^\/api\/v1\/portal\/forms\/assignments\/\d+$/.test(path)){const id=Number(path.split('/').pop());return json({...assignment,id,status:id===1?status:'ASSIGNED',fields,context:{clientData:null,therapistData:null,practiceData:null}});}
  if(/^\/api\/v1\/portal\/forms\/responses\/\d+$/.test(path)){const id=Number(path.split('/').pop());if(failRead)return json({message:'Responses temporarily unavailable'},503);return json(Object.entries(answers[id]).map(([field,value])=>({id:Number(field),assignmentId:id,assignmentFieldId:Number(field),value})));}
  if(path==='/api/v1/portal/forms/responses'&&req.method()==='POST'){
   const b=req.postDataJSON(); if(fail&&b.assignmentFieldId===13){failRead=true;return json({message:'Draft storage unavailable'},503);}
   answers[b.assignmentId][b.assignmentFieldId]=b.value;status='IN_PROGRESS';return json({id:b.assignmentFieldId,...b});
  }
  if(path==='/api/v1/portal/forms/signature'&&req.method()==='POST'){signed++;signature={assignmentId:1,signatureData:req.postDataJSON().signatureData,signerName:'QA Draft Client',signedAt:'2026-09-11T12:00:00Z'};return json(signature);}
  if(path.startsWith('/api/v1/portal/forms/signature/'))return signature?json(signature):json({message:'No signature'},404);
  if(path==='/api/v1/portal/forms/submit/1'){submitted++;status='COMPLETED';return json({...assignment,status});}
  return json({message:'Unexpected API request '+path},500);
 });
 const check=(value,message)=>{if(!value)throw new Error(message);};
 const textbox=name=>page.getByRole('textbox',{name,exact:true});
 const save=()=>page.getByRole('button',{name:'Save Draft',exact:true});
 async function test(name,fn){try{await fn();results.push({name,status:'passed'});}catch(e){results.push({name,status:'failed',error:e.message});}}
 await test('staff assigned date uses creation date through real API normalization',async()=>{
  await page.goto(base+'/staff');await page.getByText('Draft regression form',{exact:true}).waitFor();
  check(await page.getByText('Assigned: Sep 11, 2026',{exact:true}).count()===1,'Assigned date must be September 11, not due date September 18');
 });
 await test('partial draft saves, reloads, clears values, survives failures, isolates assignments and submits',async()=>{
  await page.goto(base+'/user/clinical-forms/1');await textbox('First required answer *').waitFor();
  check(await save().count()===1,'Save Draft action missing');
  await textbox('First required answer *').fill('Partial answer');await textbox('Optional note').fill('Remove this later');await save().click();
  await page.getByRole('status').filter({hasText:'Draft saved'}).waitFor();
  check(submitted===0&&signed===0,'Saving draft must not submit or accept signing terms');
  await page.screenshot({path:'output/playwright/form-draft-saved.png',fullPage:true});
  await page.reload();await textbox('First required answer *').waitFor();
  check(await textbox('First required answer *').inputValue()==='Partial answer','Draft lost on reload');
  check(await textbox('Second required answer *').inputValue()==='','Draft incorrectly required every answer');
  await textbox('Optional note').fill('');await save().click();await page.getByRole('status').filter({hasText:'Draft saved'}).waitFor();
  await page.reload();await textbox('Optional note').waitFor();check(await textbox('Optional note').inputValue()==='','Cleared answer resurrected');
  fail=true;await textbox('First required answer *').fill('Edited answer');await textbox('Optional note').fill('Retain this after failure');await save().click();
  await page.getByRole('alert').filter({hasText:'Draft'}).waitFor();
  check(await textbox('Optional note').inputValue()==='Retain this after failure','Refetch overwrote unsaved answer after partial failure');
  check(submitted===0,'Draft failure submitted form');
  fail=false;failRead=false;await save().click();await page.getByRole('status').filter({hasText:'Draft saved'}).waitFor();
  await page.getByRole('link',{name:'Open second assignment'}).click();await textbox('First required answer *').waitFor();
  check(await textbox('First required answer *').inputValue()==='','Answers leaked to another assignment');
  await page.goto(base+'/user/clinical-forms/1');await textbox('First required answer *').waitFor();
  check(await textbox('Optional note').inputValue()==='Retain this after failure','Retry did not persist draft');
  await page.getByRole('button',{name:'Submit Form',exact:true}).click();check(submitted===0,'Incomplete draft submitted');
  await textbox('Second required answer *').fill('Complete answer');
  await page.getByRole('button',{name:'Type Name',exact:true}).click();
  await textbox('Type your full name Your typed name will be used as your electronic signature').fill('QA Draft Client');
  await page.getByRole('button',{name:'Submit Form',exact:true}).click();
  await page.getByText('Completed',{exact:true}).waitFor();
  check(submitted===1&&signed===1,'Final submission missing signature or duplicate submit');
  check(await save().count()===0,'Completed form still offers draft editing');
  await page.reload();await page.getByText('Completed',{exact:true}).waitFor();
  check(await textbox('First required answer *').isDisabled(),'Completed answer still editable');
  await page.screenshot({path:'output/playwright/form-draft-completed.png',fullPage:true});
 });
 check(errors.length===0,'Browser errors: '+errors.join(';'));
 return {results,passed:results.filter(r=>r.status==='passed').length,failed:results.filter(r=>r.status==='failed').length};
}
