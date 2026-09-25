interface SummaryCardProps {
    label: string;
    value: string;
}

const SummaryCard = ({ label, value }: SummaryCardProps) => {
    return (
        <div className="bg-white border border-(--neutral-200) rounded-lg p-4">
            <p className="text-xs text-(--text-neutral-600) mb-1">{label}</p>
            <p className="text-2xl font-bold text-(--text-primary-dark)">{value}</p>
        </div>
    );
};

export default SummaryCard;
