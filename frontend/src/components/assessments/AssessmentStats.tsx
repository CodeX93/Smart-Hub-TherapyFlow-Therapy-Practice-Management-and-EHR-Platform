
import type { AssessmentOverview } from "../../pages/therapist/therapist.static";

interface AssessmentStatsProps {
    data: AssessmentOverview;
}

const StatCard = ({ label, value }: { label: string; value: number }) => (
    <div className="bg-white p-4 rounded-xl border border-gray-100 shadow-xs flex flex-col items-start gap-1">
        <span className="text-xs font-medium text-gray-500">{label}</span>
        <span className="text-xl font-bold text-(--text-primary-dark)">{value}</span>
    </div>
);

const AssessmentStats = ({ data }: AssessmentStatsProps) => {
    return (
        <div className="grid grid-cols-4 gap-4 mb-8">
            <StatCard label="Total Assigned" value={data.totalAssigned} />
            <StatCard label="Completed" value={data.completed} />
            <StatCard label="In Progress" value={data.inProgress} />
            <StatCard label="Pending" value={data.pending} />
        </div>
    );
};

export default AssessmentStats;
