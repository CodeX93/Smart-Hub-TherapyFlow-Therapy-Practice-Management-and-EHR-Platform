import { lazy, Suspense, useRef, useMemo, useEffect } from "react";
import type { IJodit } from "jodit/esm/types/jodit";
import { ContentLoader } from "@/components/shared/ContentLoader";
// import { convert } from "html-to-text";

const JoditEditor = lazy(() => import("jodit-react"));

interface CustomJoditEditorProps {
  placeholder?: string;
  content: string;
  setContent: (content: string) => void;
  readonly?: boolean;
  className?: string;
  minHeight?: number;
  editorHeight?: number | string;
  scrollable?: boolean;
  contentVariant?: "default" | "report";
}

const REPORT_CONTENT_CSS = `
  body {
    font-family: inherit;
    font-size: 0.875rem;
    line-height: 1.65;
    color: #111827;
    margin: 0;
    padding: 0;
  }
  h2 {
    font-size: 1.5rem;
    font-weight: 700;
    color: #0f3d5c;
    margin: 0 0 1.25rem;
    padding-bottom: 0.75rem;
    border-bottom: 0.125rem solid #d8e6f0;
    line-height: 1.3;
    text-align: center;
  }
  h3 {
    font-size: 0.95rem;
    font-weight: 700;
    color: #0f3d5c;
    margin: 1.5rem 0 0.75rem;
    padding: 0.5rem 0.75rem;
    background: #eef6fb;
    border-left: 0.25rem solid #0f6eaa;
    border-radius: 0 0.5rem 0.5rem 0;
    line-height: 1.35;
    text-transform: none;
    letter-spacing: 0.01em;
  }
  h3:first-of-type {
    margin-top: 0.5rem;
  }
  p {
    margin: 0.35rem 0 0.75rem;
  }
  strong {
    color: #374151;
    font-weight: 600;
  }
  ul, ol {
    margin: 0.5rem 0 0.85rem;
    padding-left: 1.5rem;
  }
  li {
    margin: 0.3rem 0;
  }
`;

const DEFAULT_CONTENT_CSS = `
  body {
    font-family: inherit;
    font-size: 0.875rem;
    line-height: 1.6;
    color: #111827;
    margin: 0;
    padding: 0;
  }
  h2 {
    font-size: 1.25rem;
    font-weight: 700;
    margin: 1.25rem 0 0.75rem;
    line-height: 1.3;
  }
  h3 {
    font-size: 1.1rem;
    font-weight: 600;
    margin: 1rem 0 0.5rem;
    line-height: 1.35;
  }
  p {
    margin: 0.5rem 0;
  }
  ul, ol {
    margin: 0.5rem 0 0.75rem;
    padding-left: 1.5rem;
  }
  li {
    margin: 0.25rem 0;
  }
  .editor-tag {
    display: inline-block;
    padding: 0.75rem 1.125rem;
    margin: 0 0.25rem;
    background-color: #f0f0f0;
    border: 1px solid #ddd;
    border-radius: 0.9375rem;
    font-size: 0.75rem;
  }
`;

const CustomJoditEditor = ({
  placeholder,
  content,
  setContent,
  readonly = false,
  className,
  minHeight = 200,
  editorHeight,
  scrollable = false,
  contentVariant = "default",
}: CustomJoditEditorProps) => {
  const editor = useRef<IJodit>(null);

  const config = useMemo(
    () => ({
      readonly: readonly,
      placeholder: placeholder !== undefined ? placeholder : "Start typing...",
      toolbar: true,
      toolbarAdaptive: false,
      buttons: [
        "bold",
        "italic",
        "underline",
        "|",
        "ul",
        "ol",
        "|",
        // "source",
        // "strikethrough",
        // "|",
        // "superscript",
        // "subscript",
        // "|",
        // "font",
        // "fontsize",
        // "brush",
        // "undo",
        // "redo",
        // "paragraph",
        // "|",

        // "outdent",
        // "indent",
        // "|",
        // "left",
        // "center",
        // "right",
        // "justify",
        // "|",
        // "hr",
        // "table",
        // "link",
        // "|",
        // "image",
        // "video",
        // "file",
        // "|",
        // "copyformat",
        // "fullsize",
        // "print",
      ],
      disablePlugins: ["mobile", "stat", "xpath"],
      extraButtons: [],
      height: editorHeight ?? minHeight,
      minHeight,
      zIndex: 0,
      iframe: false,
      iframeStyle: "",
      style: {
        background: "transparent",
        border: "none",
        borderRadius: "0.625rem",
      },
      activeButtonsInReadOnly: ["source", "fullsize", "print", "about"],
      contentCss: contentVariant === "report" ? REPORT_CONTENT_CSS : DEFAULT_CONTENT_CSS,
    }),
    [placeholder, readonly, minHeight, editorHeight, scrollable, contentVariant],
  );

  useEffect(() => {
    const style = document.createElement("style");
    const reportWysiwygStyles =
      contentVariant === "report"
        ? `
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg {
        font-size: 0.875rem;
        line-height: 1.65;
        color: #111827;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg h2 {
        font-size: 1.5rem;
        font-weight: 700;
        color: #0f3d5c;
        margin: 0 0 1.25rem;
        padding-bottom: 0.75rem;
        border-bottom: 0.125rem solid #d8e6f0;
        line-height: 1.3;
        text-align: center;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg h3 {
        font-size: 0.95rem;
        font-weight: 700;
        color: #0f3d5c;
        margin: 1.5rem 0 0.75rem;
        padding: 0.5rem 0.75rem;
        background: #eef6fb;
        border-left: 0.25rem solid #0f6eaa;
        border-radius: 0 0.5rem 0.5rem 0;
        line-height: 1.35;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg h3:first-of-type {
        margin-top: 0.5rem;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg p {
        margin: 0.35rem 0 0.75rem;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg strong {
        color: #374151;
        font-weight: 600;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg ul,
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg ol {
        margin: 0.5rem 0 0.85rem;
        padding-left: 1.5rem;
      }
      .custom-jodit-editor-wrapper--report .jodit-wysiwyg li {
        margin: 0.3rem 0;
      }
    `
        : "";

    style.innerHTML = `
      .jodit-container {
        border: none !important;
        margin-bottom: 0;
        border-radius: 0.25rem !important;
        background-color: transparent !important;
      }
      .jodit-workplace {
        background-color: transparent !important;
        border-radius: 0 0 0.25rem 0.25rem !important;
      }
      .jodit-wysiwyg {
        background-color: #fff !important;
        padding: 1rem 1.25rem !important;
        min-height: ${minHeight}px;
        border: none !important;
        overflow-y: auto !important;
      }
      .custom-jodit-editor-wrapper--scrollable .jodit-container {
        display: flex !important;
        flex-direction: column !important;
      }
      .custom-jodit-editor-wrapper--scrollable .jodit-workplace {
        flex: 1 1 auto !important;
        min-height: 0 !important;
      }
      .jodit-toolbar__box {
        background-color: #fff !important;
        border-bottom: 1px solid #D8DBDF !important;
        border-radius: 0.25rem 0.25rem 0 0 !important;
      }
      .jodit-status-bar {
        display: none;
      }
      .jodit-toolbar-button__fontcolor .jodit-icon {
        background: linear-gradient(to right, red, orange, yellow, green, blue, indigo, violet) !important;
      }
      ${reportWysiwygStyles}
    `;
    document.head.appendChild(style);

    return () => {
      document.head.removeChild(style);
    };
  }, [minHeight, contentVariant]);

  return (
    <div
      className={`custom-jodit-editor-wrapper${contentVariant === "report" ? " custom-jodit-editor-wrapper--report" : ""}${scrollable ? " custom-jodit-editor-wrapper--scrollable h-full min-h-0" : ""}${className ? ` ${className}` : ""}`}
    >
      <Suspense
        fallback={
          <div style={{ minHeight }} className="flex">
            <ContentLoader className="min-h-0" />
          </div>
        }
      >
        <JoditEditor
          ref={editor}
          value={content}
          config={config}
          onBlur={(newContent) => setContent(newContent)}
          onChange={(newContent) => setContent(newContent)}
        />
      </Suspense>
    </div>
  );
};

export default CustomJoditEditor;
