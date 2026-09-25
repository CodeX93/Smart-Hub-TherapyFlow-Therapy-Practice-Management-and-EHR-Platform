/* eslint-disable @typescript-eslint/no-explicit-any */
import React from "react";
import type { DocumentTableColumn, DocumentTableProps } from "../../types/document.type";

function parseColumnWidth(width?: string): number {
  if (!width) return 120;
  const remMatch = width.trim().match(/^(-?\d+(?:\.\d+)?)rem$/i);
  if (remMatch) {
    return Number.parseFloat(remMatch[1]) * 16;
  }
  const parsed = Number.parseFloat(width);
  return Number.isFinite(parsed) ? parsed : 120;
}

function getTableMinWidth(columns: DocumentTableColumn[]): number | undefined {
  if (columns.some((column) => column.width.includes("%"))) {
    return undefined;
  }

  return columns.reduce((sum, column) => sum + parseColumnWidth(column.width), 0);
}

const getCellAlignment = (align?: "left" | "center" | "right") => {
  if (align === "center") return "text-center";
  if (align === "right") return "text-right";
  return "text-left";
};

const DocumentTable = <T extends Record<string, any>>({
  columns,
  data,
  containerClassName = "",
  containerStyle,
  rowHeight = "4.5rem",
  headerHeight = "2.875rem",
  headerCellStyle,
  stickyHeader = false,
  rowKey = (_, index) => index,
  getRowClassName,
  getRowStyle,
}: DocumentTableProps<T>) => {
  const tableMinWidth = getTableMinWidth(columns);

  const defaultContainerStyle: React.CSSProperties = {
    borderRadius: "0.75rem",
    border: "1px solid #EDEEF1",
    boxShadow: "0px 2px 2px 0px #1E282E0A",
    background: "#FFFFFF",
    ...containerStyle,
  };

  const defaultHeaderStyle: React.CSSProperties = {
    height: headerHeight,
    background: "#F3F7F8",
    borderTopLeftRadius: "0.75rem",
    borderTopRightRadius: "0.75rem",
  };

  const defaultHeaderCellStyle: React.CSSProperties = {
    fontFamily: "Manrope",
    fontWeight: 600,
    fontSize: "0.75rem",
    lineHeight: "1.125rem",
    letterSpacing: "0%",
    verticalAlign: "middle",
    color: "#1B1C20",
  };

  return (
    <div
      className={`w-full max-w-full overflow-x-auto ${stickyHeader ? "overflow-y-visible" : "overflow-y-hidden"} ${containerClassName}`}
      style={defaultContainerStyle}
    >
      <table
        className="w-full table-fixed"
        style={tableMinWidth ? { minWidth: tableMinWidth } : undefined}
      >
        <thead className={stickyHeader ? "sticky top-0 z-10" : ""}>
          <tr style={defaultHeaderStyle}>
            {columns.map((column) => (
              <th
                key={column.key}
                className={`${getCellAlignment(column.align)} ${stickyHeader ? "sticky top-0 z-10" : ""} ${column.headerClassName || ""}`}
                style={{
                  width: column.width,
                  height: headerHeight,
                  padding: column.headerPadding || column.padding || "0.75rem 1rem",
                  background: "#F3F7F8",
                  ...defaultHeaderCellStyle,
                  ...headerCellStyle,
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
              className={getRowClassName?.(row, index)}
              style={{
                height: rowHeight,
                borderTop: "1px solid #EDEEF1",
                ...getRowStyle?.(row, index),
              }}
            >
              {columns.map((column) => {
                const value = row[column.key];
                const cellContent = column.render
                  ? column.render(value, row, index)
                  : value;

                return (
                  <td
                    key={column.key}
                    className={`min-w-0 overflow-hidden text-[0.875rem] leading-5.5 text-(--text-primary-dark) ${getCellAlignment(column.align)} ${column.className || ""}`}
                    style={{
                      width: column.width,
                      height: rowHeight,
                      padding: column.padding || "0.75rem 1rem",
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

export default DocumentTable;
