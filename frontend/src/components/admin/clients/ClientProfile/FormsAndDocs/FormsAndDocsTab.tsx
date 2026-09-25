import { useState } from 'react';
import { FileText, Files } from 'lucide-react';
import { cn } from '../../../../../lib/utils';
import Documents from './Documents';
import ClinicalForms from './ClinicalForms';
import type { Client } from '@/types/client.type';

type Tab = 'clinical-forms' | 'documents';

interface FormsAndDocsTabProps {
    client: Client;
    isTabActive?: boolean;
    readOnly?: boolean;
}

const FormsAndDocsTab = ({ client, isTabActive = true, readOnly = false }: FormsAndDocsTabProps) => {
    const [activeTab, setActiveTab] = useState<Tab>('clinical-forms');
    const [mountedTabs, setMountedTabs] = useState<Set<Tab>>(new Set(['clinical-forms']));

    if (!mountedTabs.has(activeTab)) {
      setMountedTabs(new Set([...mountedTabs, activeTab]));
    }

    return (
        <div className="flex h-full min-h-0 flex-col overflow-hidden p-6">
            {/* Sub-navigation */}
            <div className="flex shrink-0 items-center gap-6 border-b border-gray-100 pb-1">
                <button
                    onClick={() => setActiveTab('clinical-forms')}
                    className={cn(
                        "flex items-center gap-2 pb-3 text-sm font-medium transition-colors relative",
                        activeTab === 'clinical-forms'
                            ? "text-gray-900 after:absolute after:bottom-[-0.0625rem] after:left-0 after:w-full after:h-0.5 after:bg-gray-900"
                            : "text-gray-500 hover:text-gray-700"
                    )}
                >
                    <FileText size={18} />
                    Clinical Forms
                </button>
                <button
                    onClick={() => setActiveTab('documents')}
                    className={cn(
                        "flex items-center gap-2 pb-3 text-sm font-medium transition-colors relative",
                        activeTab === 'documents'
                            ? "text-gray-900 after:absolute after:bottom-[-0.0625rem] after:left-0 after:w-full after:h-0.5 after:bg-gray-900"
                            : "text-gray-500 hover:text-gray-700"
                    )}
                >
                    <Files size={18} />
                    Documents
                </button>
            </div>

            {/* Content */}
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden pt-6">
                {mountedTabs.has('clinical-forms') ? (
                    <div
                        className={cn(
                            activeTab === 'clinical-forms'
                                ? 'block h-full min-h-0 min-w-0 overflow-x-hidden overflow-y-auto pr-2 custom-scrollbar [scrollbar-gutter:stable]'
                                : 'hidden',
                        )}
                    >
                        <ClinicalForms client={client} readOnly={readOnly} />
                    </div>
                ) : null}
                {mountedTabs.has('documents') ? (
                    <div
                        className={cn(
                            activeTab === 'documents' ? 'flex h-full min-h-0 flex-col' : 'hidden',
                        )}
                    >
                        <Documents
                            client={client}
                            isActive={isTabActive && activeTab === 'documents'}
                            readOnly={readOnly}
                        />
                    </div>
                ) : null}
            </div>
        </div>
    );
};

export default FormsAndDocsTab;
