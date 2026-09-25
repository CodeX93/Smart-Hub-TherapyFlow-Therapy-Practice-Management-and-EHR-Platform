import { lazy, Suspense } from "react";
import type { ApexOptions } from "apexcharts";

const Chart = lazy(() => import("react-apexcharts"));

/* =========================
   Config Builder
========================= */
type DonutChartConfigParams = {
  labels: string[];
  colors: string[];
  donutSize?: string;
  showDataLabels?: boolean;
  tooltipSuffix?: string;
};

const createDonutChartConfig = ({
  labels,
  colors,
  donutSize = "60%",
  showDataLabels = true,
  tooltipSuffix = "%",
}: DonutChartConfigParams): ApexOptions => ({
  chart: {
    type: "donut",
    fontFamily: "Manrope, sans-serif",
  },

  plotOptions: {
    pie: {
      donut: {
        size: donutSize,
        labels: {
          show: false,
        },
      },
    },
  },

  dataLabels: {
    enabled: showDataLabels,
    formatter: (val) => `${Math.round(val as number)}%`,
    style: {
      fontSize: "0.875rem",
      fontWeight: "bold",
      colors: ["#fff"],
    },
    dropShadow: {
      enabled: false,
    },
  },

  legend: {
    show: false,
  },

  colors,

  stroke: {
    show: true,
    width: 2,
    colors: ["#fff"],
  },

  tooltip: {
    enabled: true,
    theme: "dark",
    y: {
      formatter: (val) => `${val}${tooltipSuffix}`,
    },
  },

  labels,
});

/* =========================
   Reusable Component
========================= */
type DonutChartProps = {
  labels: string[];
  data: number[];
  colors: string[];
  donutSize?: string;
  showDataLabels?: boolean;
  tooltipSuffix?: string;
  width?: string | number;
};

const DonutChart = ({
  labels,
  data,
  colors,
  donutSize,
  showDataLabels,
  tooltipSuffix,
  width = "100%",
}: DonutChartProps) => {
  const options = createDonutChartConfig({
    labels,
    colors,
    donutSize,
    showDataLabels,
    tooltipSuffix,
  });

  return (
    <Suspense fallback={null}>
      <Chart options={options} series={data} type="donut" width={width} />
    </Suspense>
  );
};

export default DonutChart;
