import React from "react";
import AuthRightBar from "../shared/AuthRightBar";
import { cn } from "@/lib/utils";

interface AuthLayoutWrapperProps {
  title: string;
  children: React.ReactNode;
  bottomContent?: React.ReactNode;
  showRightBar?: boolean;
  className?: string;
}

const AuthLayoutWrapper: React.FC<AuthLayoutWrapperProps> = ({
  title,
  children,
  bottomContent,
  showRightBar = false,
  className,
}) => {
  return (
    <div
      className={cn(
        "bg-white w-full md:min-h-153.75 shadow-md rounded-xl border-none overflow-hidden",
        showRightBar ? "max-w-269.25 grid md:grid-cols-2" : "max-w-134.75",
        className
      )}
    >
      <div
        className={cn(
          "md:px-12 md:py-12 px-4 py-5 flex flex-col justify-between gap-18 md:gap-36 relative",
          showRightBar ? "items-start" : "items-center"
        )}
      >
        <div className="w-full">
          <h2
            className={cn(
              "md:text-2xl text-xl font-semibold text-(--text-primary-dark)",
              showRightBar ? "text-left" : "text-center"
            )}
          >
            {title}
          </h2>

          {children}
        </div>

        {bottomContent && (
          <div
            className={cn(
              "flex items-center gap-1 w-full",
              showRightBar ? "justify-center" : "justify-center"
            )}
          >
            {bottomContent}
          </div>
        )}
      </div>

      {showRightBar && <AuthRightBar />}
    </div>
  );
};

export default AuthLayoutWrapper;
