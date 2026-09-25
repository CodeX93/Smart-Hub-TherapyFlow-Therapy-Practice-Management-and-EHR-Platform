interface AIGeneratedNoteProps {
    value?: string;
    onChange?: (value: string) => void;
    disabled?: boolean;
}

const AIGeneratedNote = ({ value, onChange, disabled = false }: AIGeneratedNoteProps) => {
    return (
        <div>
            <label className="text-sm font-medium text-(--text-primary-dark) mb-2 block">
                AI Generated Session Note (Rich Text Editor)
            </label>
            <div className="border border-(--neutral-200) rounded-xl p-4 bg-white">
                {/* Toolbar */}
                <div className="flex items-center gap-2 mb-3 pb-3 border-b border-(--neutral-200)">
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <span className="font-bold text-sm">B</span>
                    </button>
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <span className="italic text-sm">I</span>
                    </button>
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <span className="underline text-sm">U</span>
                    </button>
                    <div className="w-px h-6 bg-(--neutral-200)" />
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <circle cx="3" cy="4" r="1.5" fill="currentColor" />
                            <circle cx="3" cy="8" r="1.5" fill="currentColor" />
                            <circle cx="3" cy="12" r="1.5" fill="currentColor" />
                            <line x1="6" y1="4" x2="14" y2="4" stroke="currentColor" strokeWidth="1.5" />
                            <line x1="6" y1="8" x2="14" y2="8" stroke="currentColor" strokeWidth="1.5" />
                            <line x1="6" y1="12" x2="14" y2="12" stroke="currentColor" strokeWidth="1.5" />
                        </svg>
                    </button>
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <text x="2" y="5" fontSize="6" fill="currentColor">1.</text>
                            <text x="2" y="9" fontSize="6" fill="currentColor">2.</text>
                            <text x="2" y="13" fontSize="6" fill="currentColor">3.</text>
                            <line x1="6" y1="4" x2="14" y2="4" stroke="currentColor" strokeWidth="1.5" />
                            <line x1="6" y1="8" x2="14" y2="8" stroke="currentColor" strokeWidth="1.5" />
                            <line x1="6" y1="12" x2="14" y2="12" stroke="currentColor" strokeWidth="1.5" />
                        </svg>
                    </button>
                    <button type="button" className="p-2 hover:bg-(--neutral-50) rounded transition-colors">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                            <path d="M3 8L8 3L13 8" stroke="currentColor" strokeWidth="1.5" fill="none" />
                            <line x1="8" y1="3" x2="8" y2="13" stroke="currentColor" strokeWidth="1.5" />
                        </svg>
                    </button>
                </div>

                {/* Editor Content Area */}
                <textarea
                    value={value || ""}
                    onChange={(e) => onChange?.(e.target.value)}
                    disabled={disabled}
                    placeholder="AI-generated content will appear here after you click 'Generate Content'. You can edit it with rich text formatting..."
                    className="min-h-[7.5rem] w-full resize-y border-0 bg-transparent text-sm text-(--text-neutral-700) outline-none disabled:text-(--text-neutral-500)"
                />
            </div>
            <p className="mt-2 text-xs text-(--text-neutral-600)">
                Edit the AI-generated session note with formatting. This will be saved when you click "Create Note" or "Update Note".
            </p>
        </div>
    );
};

export default AIGeneratedNote;
