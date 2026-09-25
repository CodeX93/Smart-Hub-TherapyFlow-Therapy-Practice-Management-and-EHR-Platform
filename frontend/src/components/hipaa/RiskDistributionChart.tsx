import CustomSelect from "../form/CustomSelect";
import DonutChart from "../shared/charts/DonutChart";
import type { AdminAuditPeriod } from "@/store/api/admin/audit.api";
import type { HIPAARiskDistributionData } from "@/types/hipaa.types";

interface RiskDistributionChartProps {
  filter?: AdminAuditPeriod;
  onFilterChange?: (value: AdminAuditPeriod) => void;
  data: HIPAARiskDistributionData;
}

const RiskDistributionChart = ({
  filter = "monthly",
  onFilterChange,
  data,
}: RiskDistributionChartProps) => {
  const selectedFilter = filter.charAt(0).toUpperCase() + filter.slice(1);

  return (
    <div className="bg-white p-4 rounded-xl border border-(--neutral-100) shadow-xs flex flex-col h-full min-h-100">
      <div className="flex items-center justify-between mb-8">
        <h3 className="text-lg font-semibold text-(--text-primary-dark)">
          Risk Level Distribution
        </h3>
        <CustomSelect
          options={[
            { label: "Weekly", value: "weekly" },
            { label: "Monthly", value: "monthly" },
            { label: "Yearly", value: "yearly" },
          ]}
          value={filter}
          onChange={(value) => onFilterChange?.(value as AdminAuditPeriod)}
          className="rounded-full max-h-10 w-fit pb-0 pt-0"
          isSearch={false}
          placeholder={selectedFilter}
          closeOnScroll
        />
      </div>

      <div className="flex flex-1 items-center gap-8">
        <div className="flex flex-col gap-8 flex-1">
          {/* Total Events Box */}
          <div className="p-5 border border-(--neutral-200) rounded-2xl bg-white">
            <h4 className="text-2xl font-semibold text-(--text-primary-dark) mb-1">
              {data.total}
            </h4>
            <p className="text-sm text-(--text-neutral-600)">
              Total events by risk level
            </p>
          </div>

          {/* Legend */}
          <div className="flex flex-col gap-4">
            {data.breakdown.map((item, index) => (
              <div
                key={index}
                className="flex items-center justify-between w-full"
              >
                <div className="flex items-center gap-2">
                  <div
                    className="w-2.5 h-2.5 rounded"
                    style={{ backgroundColor: item.color }}
                  />
                  <span className="text-sm text-(--text-primary-dark)">
                    {item.label}
                  </span>
                </div>
                <span className="text-sm font-semibold text-(--text-primary-dark)">
                  {item.count}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Chart */}
        <div className="flex-[1.5] flex items-center justify-center relative">
          <DonutChart
            labels={data.breakdown.map((i) => i.label)}
            data={data.breakdown.map((i) => i.percentage)}
            colors={data.breakdown.map((i) => i.color)}
            width="100%"
          />
        </div>
      </div>
    </div>
  );
};

export default RiskDistributionChart;
