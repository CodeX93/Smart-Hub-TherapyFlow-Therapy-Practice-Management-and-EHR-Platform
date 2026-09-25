import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useNavigate } from "react-router-dom";
import StatusBadge from "../../components/StatusBadge";
import type { OrganisationRow } from "../../organisations.data";
import { getStatusVariant } from "../details.utils";

interface OrganisationHeaderCardProps {
  org: OrganisationRow;
  organisationId?: number | null;
  onManageBilling?: () => void;
}

function getHeaderBadgeClassName(org: OrganisationRow): string {
  const variant = getStatusVariant(org.status);

  if (variant === "green") {
    return "border-transparent bg-[#dcfce7] text-[#16a34a]";
  }

  if (variant === "yellow") {
    return "border-transparent bg-[#fef3c7] text-[#d97706]";
  }

  return "border-transparent bg-[#eef2f6] text-[#667483]";
}

function getSecondaryActionClassName(): string {
  return cn(
    "h-10 rounded-full border border-[#d4dde6] bg-white px-5",
    "text-[#40505d] text-[0.8125rem] font-medium leading-5 shadow-none hover:bg-[#f7fafc]"
  );
}

function getPrimaryActionClassName(): string {
  return cn(
    "h-10 rounded-full bg-[#435564] px-5 text-[0.8125rem] font-semibold leading-5",
    "text-white shadow-none hover:bg-[#394957]"
  );
}

function OrganisationHeaderCard(props: OrganisationHeaderCardProps) {
  const navigate = useNavigate();

  function handleTenantSettings() {
    navigate("/super-admin/organisations/" + props.org.slug + "/settings", {
      state:
        props.organisationId !== undefined && props.organisationId !== null
          ? { organisationId: props.organisationId }
          : undefined,
    });
  }

  function handleManageBilling() {
    if (props.onManageBilling) {
      props.onManageBilling();
      return;
    }
    navigate("/super-admin/billings-and-plans");
  }

  return (
    <div className="w-full rounded-[1rem] border border-[#e3ebf3] bg-white px-4 py-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)] md:px-5">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex min-w-0 items-center gap-4">
          <div className="grid h-[3.625rem] w-[3.625rem] shrink-0 place-items-center rounded-[1rem] bg-[#f2f5f7] text-[#22313d] text-[1.3125rem] font-medium leading-none">
            {props.org.name.slice(0, 1)}
          </div>

          <div className="min-w-0">
            <div className="flex min-w-0 flex-wrap items-center gap-2.5">
              <div className="truncate text-[#1f2d38] text-[1.25rem] font-bold leading-7">
                {props.org.name}
              </div>
              <StatusBadge
                variant={getStatusVariant(props.org.status)}
                className={getHeaderBadgeClassName(props.org)}
              >
                {props.org.status}
              </StatusBadge>
            </div>
            <div className="mt-1 flex flex-wrap items-center gap-1.5 text-[#8a96a3] text-[0.8125rem] font-normal leading-5">
              <span>Slug: {props.org.slug}</span>
              <span aria-hidden="true">&bull;</span>
              <span>{props.org.rrCode}</span>
            </div>
          </div>
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-3">
          <Button
            variant="outline"
            className={getSecondaryActionClassName()}
            onClick={handleTenantSettings}
          >
            Tenant Settings
          </Button>
          <Button className={getPrimaryActionClassName()} onClick={handleManageBilling}>
            Manage Billing
          </Button>
        </div>
      </div>
    </div>
  );
}

export default OrganisationHeaderCard;
