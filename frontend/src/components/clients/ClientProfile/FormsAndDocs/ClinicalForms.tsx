import { useState } from 'react';
import { Button } from "../../../ui/button";
import FormAssignmentDropdown from './FormAssignmentDropdown';
import AssignedFormsList from './AssignedFormsList';
import EmptyFormsState from './EmptyFormsState';

export interface FormTemplate {
    id: string;
    title: string;
}

const MOCK_TEMPLATES: FormTemplate[] = [
    { id: '1', title: 'Informed Consent' },
    { id: '2', title: 'Consectetur adipiscing elit' },
    { id: '3', title: 'Lorem ipsum dolor sit amet' },
    { id: '4', title: 'Duis aute irure dolor in reprehenderit in voluptate velit esse cillum' },
    { id: '5', title: 'Dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia' },
];

const ClinicalForms = () => {
    const [selectedForms, setSelectedForms] = useState<string[]>([]);
    const [assignedForms, setAssignedForms] = useState<string[]>([]); // simplified for now

    const handleAssign = () => {
        // Mock assignment
        if (selectedForms.length > 0) {
            setAssignedForms(prev => [...prev, ...selectedForms]);
            setSelectedForms([]);
        }
    };

    const handleDelete = (id: string) => {
        setAssignedForms(prev => prev.filter(formId => formId !== id));
    };

    return (
        <div className="space-y-8 animate-in fade-in duration-300">
            {/* Assignment Area */}
            <div className="flex gap-4 items-start">
                <div className="flex-1">
                    <FormAssignmentDropdown
                        options={MOCK_TEMPLATES}
                        selectedValues={selectedForms}
                        onChange={setSelectedForms}
                    />
                </div>
                <Button
                    onClick={handleAssign}
                    disabled={selectedForms.length === 0}
                    className="h-12 px-8 rounded-full bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white font-semibold disabled:opacity-50 disabled:cursor-not-allowed text-sm shadow-sm transition-all"
                >
                    Assign form(s) to client
                </Button>
            </div>

            {/* List Area */}
            <div>
                <h3 className="text-sm font-semibold text-gray-900 mb-4">Assigned Forms</h3>

                {assignedForms.length > 0 ? (
                    <AssignedFormsList forms={assignedForms} onDelete={handleDelete} />
                ) : (
                    <EmptyFormsState />
                )}
            </div>
        </div>
    );
};

export default ClinicalForms;
