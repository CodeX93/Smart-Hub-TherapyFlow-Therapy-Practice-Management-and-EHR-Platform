import {test,expect} from './fixtures.mjs';

test('[ZOOM-06] frontend helper: join eligibility across modes, statuses and missing URL',async({page})=>{
  await page.goto('/auth/staff/login');
  const actual=await page.evaluate(async()=>{
    const {shouldShowZoomMeetingJoin}=await import('/src/utils/zoomMeeting.ts');
    const base={joinUrl:'https://example.test/qa-meeting',zoomEnabled:true,sessionMode:'online',status:'scheduled'};
    return [shouldShowZoomMeetingJoin(base),...['cancelled','canceled','no-show','no_show','noshow'].map(status=>shouldShowZoomMeetingJoin({...base,status})),
      shouldShowZoomMeetingJoin({...base,sessionMode:'in-person'}),shouldShowZoomMeetingJoin({...base,joinUrl:'  '}),
      shouldShowZoomMeetingJoin({...base,sessionMode:'virtual'})];
  });
  expect(actual).toEqual([true,false,false,false,false,false,false,false,true]);
});

for(const field of ['status','serviceId','sessionMode','startDate','endDate']) {
  test(`[FILTER-02] frontend helper: client appointment ${field} selects exact records`,async({page})=>{
    await page.goto('/auth/staff/login');
    const ids=await page.evaluate(async field=>{
      const {applyClientAppointmentFilters,DEFAULT_CLIENT_APPOINTMENT_FILTERS}=await import('/src/utils/clientAppointmentFilters.ts');
      const rows=[{id:1,status:'completed',serviceId:3,sessionMode:'online',sessionDate:'2026-06-15T12:00:00Z'},
        {id:2,status:'scheduled',serviceId:4,sessionMode:'in-person',sessionDate:field==='endDate'?'2026-06-16T12:00:00Z':'2026-06-14T12:00:00Z'}];
      const values={status:'completed',serviceId:'3',sessionMode:'online',startDate:new Date('2026-06-15T00:00:00Z'),endDate:new Date('2026-06-15T00:00:00Z')};
      return applyClientAppointmentFilters(rows,{...DEFAULT_CLIENT_APPOINTMENT_FILTERS,[field]:values[field]}).map(r=>r.id);
    },field);expect(ids).toEqual([1]);
  });
}

test('[FILTER-07] frontend helper: combined client appointment filters and empty reset',async({page})=>{
  await page.goto('/auth/staff/login');
  const actual=await page.evaluate(async()=>{
    const {applyClientAppointmentFilters,DEFAULT_CLIENT_APPOINTMENT_FILTERS}=await import('/src/utils/clientAppointmentFilters.ts');
    const rows=[{id:1,status:'completed',serviceId:3,sessionMode:'online',sessionDate:'2026-06-15T12:00:00Z'},
      {id:2,status:'completed',serviceId:4,sessionMode:'in-person',sessionDate:'2026-06-15T12:00:00Z'}];
    return [applyClientAppointmentFilters(rows,{...DEFAULT_CLIENT_APPOINTMENT_FILTERS,status:'completed',serviceId:'3',sessionMode:'online'}).map(r=>r.id),
      applyClientAppointmentFilters(rows,{...DEFAULT_CLIENT_APPOINTMENT_FILTERS,status:'scheduled',serviceId:'3'}).map(r=>r.id),
      applyClientAppointmentFilters(rows,DEFAULT_CLIENT_APPOINTMENT_FILTERS).map(r=>r.id)];
  });expect(actual).toEqual([[1],[],[1,2]]);
});

test('[FILTER-01] frontend helper: self-pay false differs from no invoice coverage filter',async({page})=>{
  await page.goto('/auth/staff/login');
  const actual=await page.evaluate(async()=>{
    const {buildPortalInvoicesQueryArgs,DEFAULT_CLIENT_INVOICE_FILTERS}=await import('/src/utils/clientInvoiceFilters.ts');
    return [null,'self_pay','covered'].map(insuranceCovered=>buildPortalInvoicesQueryArgs(1,{...DEFAULT_CLIENT_INVOICE_FILTERS,insuranceCovered},'  QA invoice  '));
  });
  expect(actual[0]).not.toHaveProperty('insuranceCovered');
  expect(actual[1].insuranceCovered).toBe(false);expect(actual[2].insuranceCovered).toBe(true);
  for(const args of actual)expect(args.search).toBe('QA invoice');
});

test('[FILTER-04] frontend helper: invoice date range and status retain query values',async({page})=>{
  await page.goto('/auth/staff/login');
  const args=await page.evaluate(async()=>{
    const {buildPortalInvoicesQueryArgs,DEFAULT_CLIENT_INVOICE_FILTERS}=await import('/src/utils/clientInvoiceFilters.ts');
    return buildPortalInvoicesQueryArgs(2,{...DEFAULT_CLIENT_INVOICE_FILTERS,startDate:new Date(2026,5,1),endDate:new Date(2026,5,30),paymentStatus:'partial'},'');
  });expect(args).toEqual({page:2,pageSize:20,paymentStatus:'partial',startDate:'2026-06-01',endDate:'2026-06-30'});
});
