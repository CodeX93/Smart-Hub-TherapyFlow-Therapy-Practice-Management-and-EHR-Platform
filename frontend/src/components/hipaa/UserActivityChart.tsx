import type { HIPAAUserActivityItem } from "@/types/hipaa.types";
import BarChart from "../shared/charts/BarChart";

interface UserActivityChartProps {
  data?: HIPAAUserActivityItem[];
}

const UserActivityChart = ({ data = [] }: UserActivityChartProps) => {
  return (
    <div className="bg-white p-4 rounded-xl border border-(--neutral-100) shadow-xs flex flex-col h-full overflow-visible">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-(--text-primary-dark)">
          User Activity Summary
        </h3>
        <p className="text-sm text-(--text-neutral-600)">
          Track who is making the most changes in the system
        </p>
      </div>
      <div className="flex-1 overflow-visible -ml-1">
        <BarChart
          categories={data.map((i) => i.name)}
          data={data.map((i) => i.count)}
          seriesName="Activities"
          tooltipLabel="Activities"
          colors={["#6C93A4"]}
          height={280}
        />
      </div>
    </div>
  );
};

export default UserActivityChart;
