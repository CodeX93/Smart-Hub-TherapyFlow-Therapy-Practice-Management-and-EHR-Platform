import '@/index.css';
import React, { StrictMode, Suspense, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { store } from '@/store';
import { SidebarContext } from '@/contexts/sidebar';
const AdminBillings = React.lazy(() => import('@/pages/admin/billings'));
const StaffBillings = React.lazy(() => import('@/pages/staff/billings'));
const TherapistBillings = React.lazy(() => import('@/pages/therapist/billings'));
function BillingsFixture({ role }) {
  const Page = role === 'admin' ? AdminBillings : role === 'staff' ? StaffBillings : TherapistBillings;
  const [isCollapsed, setIsCollapsed] = useState(false);
  return <SidebarContext.Provider value={{ isCollapsed, setIsCollapsed }}><Page /></SidebarContext.Provider>;
}
const AdminClients = React.lazy(() => import('@/pages/admin/clients'));
const StaffClients = React.lazy(() => import('@/pages/staff/clients'));
const TherapistClients = React.lazy(() => import('@/pages/therapist/clients'));
function ClientsFixture({ role }) {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const Page = role === 'admin' ? AdminClients : role === 'staff' ? StaffClients : TherapistClients;
  return <SidebarContext.Provider value={{ isCollapsed, setIsCollapsed }}><Page /></SidebarContext.Provider>;
}
import { usePagedItems } from '@/hooks/usePagedItems';
import NotificationTab from '@/components/notification/NotificationTab';
import EditTemplateModal from '@/components/admin-report-templates/EditTemplateModal';
import UploadTemplateModal from '@/components/admin-report-templates/UploadTemplateModal';
import SessionNoteAiTemplateModal from '@/components/sessions/SessionNoteAiTemplateModal';
import TextQuestion from '@/components/assessments/take-assessment/questions/TextQuestion';
import VoiceInputQuestion from '@/components/assessments/take-assessment/questions/VoiceInputQuestion';
import ReviewTranscriptionModal from '@/components/sessions/recording/ReviewTranscriptionModal';
import VoiceRecordingModal from '@/components/sessions/recording/VoiceRecordingModal';
import SystemSettings from '@/pages/super-admin/system-settings';
import IntegrationPanel from '@/pages/super-admin/integrations/components/IntegrationPanel';
import SessionNoteDetailModal from '@/components/sessions/SessionNoteDetailModal';
import { superAdminIntegrationsApi } from '@/store/api/super-admin/integrations.api';
import { sessionNotesApi } from '@/store/api/admin/sessionNotes.api';
import { useScopedPage } from '@/hooks/useScopedPage';
import { useAccumulatedBillingHistoryItems, useAccumulatedBillingInvoices, useAccumulatedBillingRecords } from '@/utils/billingHistory';
import { useClientTaskPages } from '@/hooks/useClientTaskPages';
import Toast from '@/components/shared/Toast';
import ActionDropdown from '@/components/shared/ActionDropdown';
import PrimaryAdministratorCard from '@/pages/super-admin/organisations/create/components/PrimaryAdministratorCard';
import OrganisationBasicsCard from '@/pages/super-admin/organisations/create/components/OrganisationBasicsCard';
import PlanBillingCard from '@/pages/super-admin/organisations/create/components/PlanBillingCard';
import SignatureForm from '@/components/clinical-forms/SignatureForm';
import { Form } from '@/components/ui/form';
import AdminTasksTab from '@/components/admin/clients/ClientProfile/Tasks/TasksTab';
import TherapistTasksTab from '@/components/clients/ClientProfile/Tasks/TasksTab';

import BillingTable from '@/components/billing-sections/BillingTable';
import { mapSessionBillingToInvoice, mapBillingHistoryToInvoices } from '@/utils/billingHistory';
function BillingLinksFixture({ role, records, history }) {
  const location = useLocation();
  const data = history ? mapBillingHistoryToInvoices(records) : records.map(mapSessionBillingToInvoice);
  return <><output data-testid="location">{JSON.stringify({ path: location.pathname, state: location.state })}</output>
    <BillingTable data={data} clientsBasePath={`/${role}/clients`} isAdmin={role === 'admin'} isTherapist={role === 'therapist'} onRecordPayment={(invoice) => record({payment: invoice.id})} onApplyDiscount={() => {}} onAction={(action) => record({action})} onSort={() => {}} /></>;
}
const pending = new Promise(() => {});
const events: unknown[] = [];
const record = (event: unknown) => events.push(event);

function BillingFixture({ response, page, scopeKey, suspend, kind }) {
  // Select one stable hook per keyed fixture; every implementation remains real.
  const useBilling = kind === 'raw' ? useAccumulatedBillingHistoryItems
    : kind === 'invoices' ? useAccumulatedBillingInvoices : useAccumulatedBillingRecords;
  const result = useBilling(response, page, scopeKey);
  if (suspend) throw pending;
  return <>
    <output data-testid="rows">{JSON.stringify(result.displayedItems ?? result.displayedInvoices)}</output>
    <output data-testid="ready">{String(result.isReadyToLoadMore)}</output>
    <button onClick={result.clearItems ?? result.clearInvoices}>Clear prior pages</button>
  </>;
}

function ScopedFixture({ scopeKey }) {
  const [page, setPage] = useScopedPage(scopeKey);
  return <><output>{scopeKey}:{page}</output><button onClick={() => setPage(page + 1)}>Next page</button></>;
}

function TaskQueryFixture({ clientId, assignedToId, skip }) {
  const result = useClientTaskPages(clientId, assignedToId, skip);
  return <>
    <output data-testid="rows">{JSON.stringify(result.tasks)}</output>
    <output data-testid="fetching">{String(result.isFetching)}</output>
    <button disabled={!result.hasMore || result.isFetching} onClick={result.handleLoadMore}>Next page</button>
    <button onClick={() => result.refetch()}>Refetch current page</button>
    <button onClick={result.refreshTasks}>Refresh after mutation</button>
  </>;
}

function OrganisationFixture() {
  const form = useForm({ mode: 'onBlur', defaultValues: {
    firstName: '', lastName: '', email: '', organisationName: '', tenantSlug: '', industry: '',
    plan: 'qa', billingCycle: 'Monthly', trialDays: '14', therapistsOverride: '', supervisorsOverride: '', clientsOverride: '',
  } });
  const plan = { planCode: 'qa', planName: 'QA Plan', billingCycle: 'monthly', trialDays: 14 };
  return <form onSubmit={form.handleSubmit(record)}>
    <PrimaryAdministratorCard form={form} />
    <OrganisationBasicsCard form={form} />
    <PlanBillingCard form={form} planOptions={[plan]} />
    <output style={{ display: 'block', height: 100 }} data-testid="form-state">{JSON.stringify({ dirty: form.formState.dirtyFields, touched: form.formState.touchedFields })}</output>
    <button type="button" onClick={() => form.setFocus('firstName')}>Focus first name</button>
    <button type="submit">Submit fixture</button>
  </form>;
}

function ToastFixture({ message, callback, duration = 1000 }) {
  return <Toast message={message} duration={duration} onClose={() => record(callback)} />;
}

function DropdownFixture({ custom }) {
  return <div data-testid="scroll-container" style={{ height: 180, overflow: 'auto' }}>
    <ActionDropdown trigger={custom ? <button ref={(node) => { if (node) record('custom-ref-attached'); }}>Custom actions</button> : undefined}
      actions={[
        { label: 'Disabled action', disabled: true, onClick: () => record('disabled') },
        { label: 'Choose action', onClick: () => record('chosen') },
        { label: 'More actions', subMenu: [{ label: 'Nested action', onClick: () => record('nested') }] },
      ]} />
    <div style={{ height: 1000 }} />
  </div>;
}

function SignatureFixture() {
  const form = useForm({ defaultValues: { clientName: 'QA Synthetic Client', date: '2026-09-11', signature: '' } });
  const [width, setWidth] = useState(600);
  return <Form {...form}><div style={{ width }}>
    <SignatureForm form={form} control={form.control} onSubmit={record} />
    <button onClick={() => setWidth(450)}>Resize signature</button>
    <output data-testid="signature">{form.watch('signature')}</output>
  </div></Form>;
}

function PagedFixture({ items, scopeKey, page }) {
  const result = usePagedItems(items, page, scopeKey);
  return <><output data-testid="rows">{JSON.stringify(result.items)}</output>
    <button onClick={() => result.updateItems(rows => rows.filter(row => row.id !== 2))}>Remove row two</button>
    <output data-testid="ready">{String(result.isReadyToLoadMore)}</output></>;
}
function NotificationFixture(props) { return <NotificationTab notifications={[]} onMarkAllRead={() => record('all-read')} {...props} />; }
function EditReportFixture(props) { return <EditTemplateModal onClose={() => record('closed')} onSubmit={async payload => record(payload)} {...props} />; }
function UploadReportFixture(props) { return <UploadTemplateModal onClose={() => record('closed')} onSubmit={async payload => record(payload)} {...props} />; }
function AiTemplateFixture(props) { return <SessionNoteAiTemplateModal onClose={() => record('closed')} onSave={record} previewContent="QA preview" {...props} />; }
function ReviewFixture(props) { return <ReviewTranscriptionModal onClose={() => record('closed')} onApply={record} {...props} />; }
function VoiceFixture(props) { return <VoiceRecordingModal onClose={() => record('closed')} onTranscriptionComplete={record} sessionData={{ dateTime: 'QA session', clientName: 'Synthetic client' }} {...props} />; }
function NoteDetailFixture(props) { return <SessionNoteDetailModal onClose={() => record('closed')} {...props} />; }

function AssessmentVoiceFixture({
  kind = 'text',
  value: initialValue = '',
}: {
  kind?: 'text' | 'textarea' | 'voice';
  value?: string;
}) {
  const [value, setValue] = useState(initialValue);
  const question = {
    id: 'qa-assessment-voice',
    type: kind === 'textarea' ? 'textarea' : kind === 'voice' ? 'voice' : 'text',
    label: kind === 'textarea' ? 'Long answer field' : 'Short answer field',
  } as const;
  return (
    <>
      {kind === 'voice' ? (
        <VoiceInputQuestion question={question} value={value} onChange={setValue} />
      ) : (
        <TextQuestion question={question} value={value} onChange={setValue} />
      )}
      <output data-testid="assessment-answer">{value}</output>
    </>
  );
}

const fixtures = {
  clients: ClientsFixture, paged: PagedFixture, notifications: NotificationFixture, editReport: EditReportFixture,
  uploadReport: UploadReportFixture, aiTemplate: AiTemplateFixture, review: ReviewFixture,
  voice: VoiceFixture, assessmentVoice: AssessmentVoiceFixture, systemSettings: SystemSettings,
  integration: IntegrationPanel, noteDetail: NoteDetailFixture,
  billings: BillingsFixture, billingLinks: BillingLinksFixture, billing: BillingFixture, scoped: ScopedFixture, tasks: TaskQueryFixture,
  organisation: OrganisationFixture, toast: ToastFixture, dropdown: DropdownFixture,
  signature: SignatureFixture, adminTasks: AdminTasksTab, therapistTasks: TherapistTasksTab,
};
const root = createRoot(document.getElementById('qa-root')!);
window.qa = {
  events,
  async refetchIntegration(key) {
    await store.dispatch(superAdminIntegrationsApi.endpoints.getSuperAdminIntegration.initiate(key, { subscribe: false, forceRefetch: true })).unwrap();
  },
  async refetchNote(id) {
    await store.dispatch(sessionNotesApi.endpoints.getSessionNoteById.initiate(id, { subscribe: false, forceRefetch: true })).unwrap();
  },
  render(name, props = {}, strict = true) {
    const Fixture = fixtures[name];
    const content = <Provider store={store}><MemoryRouter><Suspense fallback={<p>Pending render</p>}>
      <Fixture key={`${name}:${props.kind ?? ''}`} {...props} />
    </Suspense></MemoryRouter></Provider>;
    root.render(strict ? <StrictMode>{content}</StrictMode> : content);
  },
  unmount() { root.unmount(); },
};
