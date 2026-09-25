import { Navigate, useLocation } from "react-router-dom";

const ClientActivateAccount = () => {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const token = params.get("token")?.trim();

  if (!token) {
    return <Navigate to="/auth/login" replace />;
  }

  const query = params.toString();
  const suffix = query ? `?${query}` : "";

  return <Navigate to={`/portal/activate/${encodeURIComponent(token)}${suffix}`} replace />;
};

export default ClientActivateAccount;
