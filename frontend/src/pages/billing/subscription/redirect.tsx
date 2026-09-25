import { Navigate } from "react-router-dom";
import { getStoredUserRole } from "@/utils/authStorage";

const BillingSubscriptionRedirect = () => {
  const role = getStoredUserRole();

  if (role === "admin") {
    return <Navigate to="/admin/billing/subscription" replace />;
  }

  if (role === "staff") {
    return <Navigate to="/staff/billing/subscription" replace />;
  }

  return <Navigate to="/auth/login" replace />;
};

export default BillingSubscriptionRedirect;
