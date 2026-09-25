/**
 * Opens invoice HTML in a new tab and triggers the browser print dialog
 * (matches the previous CRM download/print behavior).
 *
 * Pass a pre-opened window from the click handler when the HTML is fetched
 * asynchronously — browsers block window.open after await.
 */
export function openInvoiceHtmlForPrint(
  html: string,
  preOpenedWindow?: Window | null,
): void {
  const blob = new Blob([html], { type: "text/html;charset=utf-8" });
  const url = window.URL.createObjectURL(blob);
  const printWindow = preOpenedWindow ?? window.open(url, "_blank");

  if (!printWindow) {
    window.URL.revokeObjectURL(url);
    throw new Error("Please allow pop-ups to open and print the invoice.");
  }

  if (preOpenedWindow) {
    try {
      printWindow.location.href = url;
    } catch {
      printWindow.document.open();
      printWindow.document.write(html);
      printWindow.document.close();
      window.URL.revokeObjectURL(url);
    }
  }

  let printed = false;
  const triggerPrint = () => {
    if (printed || printWindow.closed) {
      return;
    }
    printed = true;
    try {
      printWindow.focus();
      printWindow.print();
    } finally {
      window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
    }
  };

  printWindow.addEventListener("load", () => {
    window.setTimeout(triggerPrint, 250);
  });

  // Fallback if the load event already fired for the blob URL.
  window.setTimeout(triggerPrint, 800);
}
