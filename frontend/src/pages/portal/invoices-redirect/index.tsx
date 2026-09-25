import { Navigate, useLocation } from "react-router-dom";

const PortalInvoicesRedirect = () => {
  const location = useLocation();

  return (
    <Navigate
      to={{ pathname: "/user/invoices", search: location.search }}
      replace
    />
  );
};

export default PortalInvoicesRedirect;
