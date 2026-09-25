import { useState } from 'react';
import { FileText, Files } from 'lucide-react';
import { cn } from '../../../../lib/utils';
import ClinicalForms from './ClinicalForms';
import Documents from './Documents';

type Tab = 'clinical-forms' | 'documents';

const FormsAndDocsTab = () => {
    const [activeTab, setActiveTab] = useState<Tab>('clinical-forms');

    return (
        <div className="p-6 space-y-6">
            {/* Sub-navigation */}
            <div className="flex items-center gap-6 border-b border-gray-100 pb-1">
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
            <div>
                {activeTab === 'clinical-forms' ? (
                    <ClinicalForms />
                ) : (
                    <Documents />
                )}
            </div>
        </div>
    );
};

export default FormsAndDocsTab;
