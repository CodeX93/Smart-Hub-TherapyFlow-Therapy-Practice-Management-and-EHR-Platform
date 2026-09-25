import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";
import { cn } from "@/lib/utils";

interface AuthCheckEmailProps {
  backToLoginPath: string;
  align?: "left" | "center";
}

const AuthCheckEmail = ({
  backToLoginPath,
  align = "center",
}: AuthCheckEmailProps) => {
  const navigate = useNavigate();

  return (
    <div
      className={cn(
        "flex flex-col gap-4",
        align === "center" ? "items-center" : "items-start"
      )}
    >
      <img src="/assets/check-email.png" alt="check" className="w-15 h-15" />
      <h2
        className={cn(
          "text-[1.5rem] font-semibold leading-8 text-(--text-primary-dark)",
          align === "center" && "text-center"
        )}
      >
        Check Your Email
      </h2>
      <p
        className={cn(
          "text-(--text-neutral-600) text-sm",
          align === "center" && "text-center"
        )}
      >
        Please check your email inbox and follow the instructions to reset your
        password. The link will expire after use.
      </p>
      <Button
        onClick={() => navigate(backToLoginPath)}
        className="my-0 w-full h-11.5 bg-(--bg-primary-dark) text-white text-[0.875rem] leading-5.5 mt-1 font-semibold rounded-full cursor-pointer"
      >
        Back to login
      </Button>
    </div>
  );
};

export default AuthCheckEmail;
