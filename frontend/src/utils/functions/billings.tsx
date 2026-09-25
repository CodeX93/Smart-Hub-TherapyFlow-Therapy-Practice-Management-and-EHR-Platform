import { Dollar } from "@solar-icons/react-perf/category/money/Linear/Dollar";
import { CheckCircle } from "@solar-icons/react-perf/category/ui/Linear/CheckCircle";
import { UsersGroupRounded } from "@solar-icons/react-perf/category/users/Linear/UsersGroupRounded";
import { BillList } from "@solar-icons/react-perf/category/money/Linear/BillList";

const iconClassName =
  "h-12 w-12 rounded-xl bg-[#F3F7F8] p-3 text-[#3C4D58]";

export const getIcon = (iconName?: string) => {
  switch (iconName) {
    case "dollar":
      return <Dollar className={iconClassName} />;
    case "check":
      return <CheckCircle className={iconClassName} />;
    case "users":
      return <UsersGroupRounded className={iconClassName} />;
    case "list":
      return <BillList className={iconClassName} />;
    default:
      return null;
  }
};
