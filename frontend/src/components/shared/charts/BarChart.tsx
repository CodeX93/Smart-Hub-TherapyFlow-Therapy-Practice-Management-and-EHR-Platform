import { lazy, Suspense } from "react";
import type { ApexOptions } from "apexcharts";

const Chart = lazy(() => import("react-apexcharts"));

/* =========================
   Config Builder
========================= */
type BarChartConfigParams = {
  categories: string[];
  colors?: string[];
  tooltipLabel?: string;
  columnWidth?: string;
};

const createBarChartConfig = ({
  categories,
  colors = ["#6C93A4"],
  tooltipLabel = "Value",
  columnWidth = "40%",
}: BarChartConfigParams): ApexOptions => ({
  chart: {
    type: "bar",
    toolbar: { show: false },
    fontFamily: "Manrope, sans-serif",
    offsetX: -10,
    offsetY: -4,
  },

  plotOptions: {
    bar: {
      borderRadius: 4,
      columnWidth,
      distributed: false,
    },
  },

  dataLabels: { enabled: false },

  xaxis: {
    categories,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      rotate: -45,
      rotateAlways: true,
      trim: true,
      hideOverlappingLabels: false,
      maxHeight: 72,
      offsetY: 0,
      formatter: (value: string) =>
        value.length > 24 ? `${value.slice(0, 21)}...` : value,
      style: {
        colors: "#5B616E",
        fontSize: "0.6875rem",
      },
    },
  },

  yaxis: {
    labels: {
      offsetX: -6,
      style: {
        colors: "#5B616E",
        fontSize: "0.75rem",
      },
      formatter: (val) => val.toString(),
    },
    tickAmount: 4,
  },

  grid: {
    borderColor: "#EDEEF1",
    strokeDashArray: 0,
    xaxis: { lines: { show: false } },
    yaxis: { lines: { show: true } },
    padding: {
      left: 4,
      right: 12,
      top: 0,
      bottom: 0,
    },
  },

  colors,

  tooltip: {
    theme: "light",
    followCursor: true,
    intersect: false,
    custom: function ({ series, seriesIndex, dataPointIndex, w }) {
      const value = series[seriesIndex][dataPointIndex];
      const category = w.globals.labels[dataPointIndex];
      return `
        <div class="p-3 min-w-45 max-w-75">
          <div class="text-[#5B616E] text-sm mb-3 break-words [overflow-wrap:anywhere]">${category}</div>
          <div class="flex items-center justify-between gap-10">
            <span class="text-[#6C93A4] text-lg">${tooltipLabel}</span>
            <span class="text-[#6C93A4] text-lg font-medium">${value}</span>
          </div>
        </div>
      `;
    },
  },
});

/* =========================
   Reusable Component
========================= */
type BarChartProps = {
  categories: string[];
  data: number[];
  seriesName?: string;
  colors?: string[];
  tooltipLabel?: string;
  columnWidth?: string;
  height?: string | number;
};

const BarChart = ({
  categories,
  data,
  seriesName = "Series",
  colors,
  tooltipLabel,
  columnWidth,
  height = "100%",
}: BarChartProps) => {
  const options = createBarChartConfig({
    categories,
    colors,
    tooltipLabel,
    columnWidth,
  });

  const series = [
    {
      name: seriesName,
      data,
    },
  ];

  return (
    <Suspense fallback={null}>
      <Chart
        options={options}
        series={series}
        type="bar"
        height={height}
        width="100%"
      />
    </Suspense>
  );
};

export default BarChart;
