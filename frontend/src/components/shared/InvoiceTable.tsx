/* eslint-disable @typescript-eslint/no-explicit-any */
import React from "react";

export const TruncatedCellText = ({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) => {
  const text =
    children === null || children === undefined ? "" : String(children);

  if (!text) {
    return <span className={className}>—</span>;
  }

  return (
    <span className={`block min-w-0 truncate ${className}`} title={text}>
      {text}
    </span>
  );
};

export interface InvoiceTableColumn<T = any> {
  key: string;
  label: string;
  width: string;
  padding?: string;
  headerPadding?: string;
  headerContent?: React.ReactNode;
  render?: (value: any, row: T, index: number) => React.ReactNode;
  className?: string;
  headerClassName?: string;
  truncate?: boolean;
}

interface InvoiceTableProps<T = any> {
  columns: InvoiceTableColumn<T>[];
  data: T[];
  containerClassName?: string;
  containerStyle?: React.CSSProperties;
  rowHeight?: string;
  rowKey?: (row: T, index: number) => string | number;
  stickyHeader?: boolean;
}

const InvoiceTable = <T extends Record<string, any>>({
  columns,
  data,
  containerClassName = "",
  containerStyle,
  rowHeight = "4.5rem",
  rowKey = (_, index) => index,
  stickyHeader = false,
}: InvoiceTableProps<T>) => {
  const defaultContainerStyle: React.CSSProperties = {
    // height: "34.375rem",
    borderRadius: "0.75rem",
    border: "1px solid #EDEEF1",
    boxShadow: "0px 2px 2px 0px #1E282E0A",
    background: "#FFFFFF",
    ...containerStyle,
  };

  const defaultHeaderStyle: React.CSSProperties = {
    height: "2.875rem",
    background: "#F3F7F8",
    borderTopLeftRadius: "0.75rem",
    borderTopRightRadius: "0.75rem",
  };

  const defaultHeaderCellStyle: React.CSSProperties = {
    fontFamily: "Manrope",
    fontWeight: 600,
    fontSize: "0.875rem",
    lineHeight: "1.375rem",
    letterSpacing: "0%",
    verticalAlign: "middle",
    color: "#1B1C20",
  };

  return (
    <div
      className={`w-full ${stickyHeader ? "overflow-visible" : "overflow-auto"} ${containerClassName}`}
      style={defaultContainerStyle}
    >
      <table className="w-full table-fixed">
        <thead className={stickyHeader ? "sticky top-0 z-10" : ""}>
          <tr style={defaultHeaderStyle}>
            {columns.map((column) => (
              <th
                key={column.key}
                className={`text-left whitespace-nowrap ${stickyHeader ? "bg-[#F3F7F8]" : ""} ${column.headerClassName || ""}`}
                style={{
                  width: column.width,
                  height: "2.875rem",
                  padding: column.headerPadding || column.padding || "0.75rem 0.5rem",
                  ...defaultHeaderCellStyle,
                }}
              >
                {column.headerContent || column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, index) => (
            <tr
              key={rowKey(row, index)}
              style={{
                height: rowHeight,
                borderTop: "1px solid #EDEEF1",
              }}
            >
              {columns.map((column) => {
                const value = row[column.key];
                const shouldTruncate = column.truncate !== false;
                const cellContent = column.render
                  ? column.render(value, row, index)
                  : shouldTruncate
                    ? (
                        <TruncatedCellText className={column.className}>
                          {value}
                        </TruncatedCellText>
                      )
                    : value;

                return (
                  <td
                    key={column.key}
                    className={`min-w-0 max-w-0 overflow-hidden text-sm text-(--text-primary-dark) ${column.className || ""}`}
                    style={{
                      width: column.width,
                      height: rowHeight,
                      padding: column.padding || "1.25rem 0.5rem",
                    }}
                  >
                    {cellContent}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default InvoiceTable;

