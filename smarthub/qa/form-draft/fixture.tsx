import '@/index.css';
import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { store } from '@/store';
import ClinicalFormDetail from '@/pages/user/clinical-form-detail';
import AssignedFormsList from '@/components/admin/clients/ClientProfile/FormsAndDocs/AssignedFormsList';
import { useGetFormAssignmentsQuery } from '@/store/api/admin/clients.api';
function Staff() {
 const { data = [] } = useGetFormAssignmentsQuery({clientId: 1});
 return <AssignedFormsList forms={data} onView={() => {}} onDelete={() => {}} />;
}
createRoot(document.getElementById('root')!).render(<StrictMode><Provider store={store}><BrowserRouter>
 <Link to="/user/clinical-forms/2">Open second assignment</Link>
 <Routes><Route path="/staff" element={<Staff />} /><Route path="/user/clinical-forms/:formId" element={<ClinicalFormDetail />} /></Routes>
</BrowserRouter></Provider></StrictMode>);
