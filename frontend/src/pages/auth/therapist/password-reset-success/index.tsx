import { Button } from "@/components/ui/button";
import { useLocation, useNavigate } from "react-router-dom";

const TherapistPasswordResetSuccess = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const path = location.pathname;
  const successMessage =
    (location.state as { message?: string } | null)?.message ??
    "Your password has been updated. You can now sign in using your new password.";

  const loginPath = path.includes("/staff/")
    ? "/auth/staff/login"
    : path.includes("/therapist/")
      ? "/auth/staff/login"
      : path.includes("/admin/")
        ? "/auth/admin/login"
        : "/auth/login";

  return (
    <div className="min-h-[calc(100vh-15.625rem)] md:min-h-auto flex items-center justify-center bg-blac">
      <div className="bg-white max-w-134.75 w-full shadow-md rounded-xl border-none">
        <div className="md:p-12 px-4 py-8 flex flex-col items-center gap-4">
          <img
            src="/assets/check-email.png"
            alt="check"
            className="w-15 h-15"
          />
          <h2 className="text-2xl font-semibold leading-8 text-(--text-primary-dark) text-center">
            Password Reset Successful
          </h2>
          <p className="text-(--text-neutral-600) text-sm text-center">
            {successMessage}
          </p>
          <Button
            onClick={() => navigate(loginPath)}
            className="my-0 w-full h-11.5 bg-(--bg-primary-dark) text-white text-sm leading-5.5 mt-1 font-semibold rounded-full cursor-pointer"
          >
            Back to login
          </Button>
        </div>
      </div>
    </div>
  );
};

export default TherapistPasswordResetSuccess;
