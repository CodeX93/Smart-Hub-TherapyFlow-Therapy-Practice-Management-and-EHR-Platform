export const getDocumentIcon = (filename: string): string => {
  const extension = filename.split(".").pop()?.toLowerCase();

  if (extension === "pdf") {
    return "/documents/pdf.svg";
  } else if (["doc", "docx"].includes(extension || "")) {
    return "/documents/doc.svg";
  } else if (
    ["jpg", "jpeg", "png", "gif", "svg", "webp", "bmp"].includes(
      extension || ""
    )
  ) {
    return "/documents/img.svg";
  }

  return "/documents/doc.svg";
};