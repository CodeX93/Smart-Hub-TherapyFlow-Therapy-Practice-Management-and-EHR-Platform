import { useState } from "react";
import type { Client } from "@/types/client.type";
import { isClientInactive } from "@/utils/clientStatus";

export function useClosedFileNotice(panelOpen: boolean, client: Client | null) {
  const scope = JSON.stringify([panelOpen, client?.id, client?.clientStatus]);
  const [dismissal, setDismissal] = useState({ scope, dismissed: false });
  if (dismissal.scope !== scope) setDismissal({ scope, dismissed: false });
  return {
    isNoticeOpen: panelOpen && Boolean(client && isClientInactive(client)) && (dismissal.scope !== scope || !dismissal.dismissed),
    closeNotice: () => setDismissal({ scope, dismissed: true }),
  };
}
