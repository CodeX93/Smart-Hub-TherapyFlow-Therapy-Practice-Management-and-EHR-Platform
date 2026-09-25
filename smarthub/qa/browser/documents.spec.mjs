import {test,expect,fixtures,loginApi,loginPortal,jsonResponse,items} from './fixtures.mjs';

// A synthetic one-pixel PNG; no patient records or external downloads.
const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=','base64');
const base=()=>`/api/v1/clients/${fixtures.alpha.client.id}/documents`;
async function upload(request,headers,overrides={}) {
  return jsonResponse(await request.post(base(),{headers,multipart:{
    file:{name:`qa-${crypto.randomUUID()}.png`,mimeType:'image/png',buffer:png},
    documentType:'OTHER',category:'UPLOADED',needsReview:'false',shareWithClient:'false',...overrides,
  }}),201);
}

test('[DOC-01] API: upload, client share/revoke, review and delete',async({request})=>{
  const headers=await loginApi(request),portal=await loginPortal(request);
  const document=await upload(request,headers,{needsReview:'true'});
  const portalList=async()=>items(await jsonResponse(await request.get('/api/v1/portal/documents',{headers:portal,params:{pageSize:100}})));
  expect((await portalList()).map(d=>d.id)).not.toContain(document.id);
  await jsonResponse(await request.patch(`${base()}/${document.id}/share`,{headers,data:{shareWithClient:true}}));
  expect((await portalList()).map(d=>d.id)).toContain(document.id);
  await jsonResponse(await request.patch(`${base()}/${document.id}/share`,{headers,data:{shareWithClient:false}}));
  expect((await portalList()).map(d=>d.id)).not.toContain(document.id);
  const reviewed=await jsonResponse(await request.patch(`${base()}/${document.id}/review`,{headers,data:{reviewStatus:'APPROVED',reviewNotes:'QA reviewed'}}));
  expect(reviewed.reviewStatus).toBe('APPROVED');
  await jsonResponse(await request.delete(`${base()}/${document.id}`,{headers}));
  expect((await request.get(`${base()}/${document.id}`,{headers})).status()).toBe(404);
});

for (const endpoint of ['download','file']) {
  test(`[DOC-01] API: authorized ${endpoint} returns exact bytes and enforces access`,async({request})=>{
    const headers=await loginApi(request);
    const document=await upload(request,headers);
    const url=`${base()}/${document.id}/${endpoint}`;
    const downloaded=await request.get(url,{headers,timeout:15000});
    expect(downloaded.status()).toBe(200);
    expect(downloaded.headers()['content-type']).toContain('image/png');
    expect(downloaded.headers()['content-disposition']).toContain('attachment;');
    expect(await downloaded.body()).toEqual(png);
    expect((await request.get(url)).status()).toBe(403);
    const restricted=await loginApi(request,fixtures.alpha,'restricted');
    expect((await request.get(url,{headers:restricted})).status()).toBe(403);
    const otherTenant=await loginApi(request,fixtures.beta);
    expect((await request.get(url,{headers:otherTenant})).status()).toBe(404);
  });
}

for (const field of ['documentType','category','reviewStatus','shareWithClient','search']) {
  test(`[FILTER-02] API: document ${field} filter selects exact matching record`,async({request})=>{
    const headers=await loginApi(request);
    const matching=await upload(request,headers,{documentType:'CONSENT',category:'FORMS',needsReview:'true',shareWithClient:'true'});
    const other=await upload(request,headers);
    const value={documentType:'CONSENT',category:'FORMS',reviewStatus:'PENDING',shareWithClient:'true',search:matching.originalName}[field];
    expect(value).toBeTruthy();
    const rows=items(await jsonResponse(await request.get(base(),{headers,params:{[field]:value,pageSize:100}})));
    expect(rows.map(d=>d.id)).toContain(matching.id);expect(rows.map(d=>d.id)).not.toContain(other.id);
  });
}
