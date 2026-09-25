import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useState } from 'react';
import { ChevronDown, ChevronUp } from "lucide-react";
import type { RefObject } from 'react';

interface EmailLog {
    id: string;
    title: string;
    status: string;
    date: string;
    details?: {
        recipient: string;
        trigger: string;
        relatedTo: string;
        id: string;
    };
}

const EmailHistoryItem = ({ log, isExpanded, onToggle }: { log: EmailLog; isExpanded: boolean; onToggle: () => void }) => {

    return (
        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden hover:shadow-sm transition-shadow">
            <div className="p-5">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-3">
                    <h4 className="text-base font-semibold text-gray-900">{log.title}</h4>
                    <div className="flex items-center gap-2 whitespace-nowrap text-xs leading-[1.125rem] font-normal text-(--text-neutral-600)">
                        <CalendarIcon size={16} className="size-4 shrink-0 text-(--text-neutral-600)" />
                        <span>{log.date}</span>
                    </div>
                </div>

                <div className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700 mb-4">
                    {log.status}
                </div>

                <div>
                    <button
                        onClick={onToggle}
                        className="flex items-center gap-1 text-sm font-medium text-[#517889] hover:text-[#517889]/80 transition-colors cursor-pointer"
                    >
                        {isExpanded ? (
                            <>
                                Hide Details
                                <ChevronUp size={16} />
                            </>
                        ) : (
                            <>
                                View Details
                                <ChevronDown size={16} />
                            </>
                        )}
                    </button>
                </div>
            </div>

            {isExpanded && log.details && (
                <div className="bg-gray-50 px-5 py-4 border-t border-gray-100/50 mx-2 mb-2 rounded-lg">
                    <div className="text-sm text-gray-600 space-y-1">
                        <p className="whitespace-pre-wrap">{log.details.trigger}</p>
                        <p className="text-gray-500 text-xs mt-2">
                            Related to: {log.details.relatedTo} &nbsp;|&nbsp; ID: {log.details.id}
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

interface ClientEmailHistoryProps {
  logs: EmailLog[];
  isLoading?: boolean;
  isLoadingMore?: boolean;
  observerTarget?: RefObject<HTMLDivElement | null>;
}

const ClientEmailHistory = ({
  logs,
  isLoading = false,
  isLoadingMore = false,
  observerTarget,
}: ClientEmailHistoryProps) => {
    const [expandedId, setExpandedId] = useState<string | null>(null);

    const handleToggle = (id: string) => {
        setExpandedId(current => current === id ? null : id);
    };

    return (
        <div className="space-y-4">
            <p className="text-sm text-gray-500 mb-4">{logs.length} communications sent</p>
            {isLoading ? (
              <ContentLoader size="sm" className="py-10 text-sm text-gray-500 mr-2" />
            ) : logs.length === 0 ? (
              <div className="py-10 text-center text-sm text-gray-500">No data found</div>
            ) : logs.map(log => (
                <EmailHistoryItem
                    key={log.id}
                    log={log}
                    isExpanded={expandedId === log.id}
                    onToggle={() => handleToggle(log.id)}
                />
            ))}
            {observerTarget ? <div ref={observerTarget} className="h-1 w-full" /> : null}
            {isLoadingMore ? (
              <ContentLoader size="sm" className="py-2 text-xs text-gray-500 mr-2" />
            ) : null}
        </div>
    );
};

export default ClientEmailHistory;
