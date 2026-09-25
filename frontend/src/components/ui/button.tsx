import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import type { VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";
import { buttonVariants } from "./button-variants";
import { LoadingDots, type LoadingDotsSize } from "./loading-dots";

type ButtonProps = React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    loading?: boolean;
    loadingLabel?: string;
  };

const getLoadingSize = (
  size: ButtonProps["size"],
): LoadingDotsSize => {
  if (size === "sm" || size === "lg" || size === "xl") return size;
  return "md";
};

function Button({
  className,
  variant = "default",
  size = "default",
  asChild = false,
  loading = false,
  loadingLabel = "Loading",
  disabled,
  children,
  ...props
}: ButtonProps) {
  const Comp = asChild ? Slot : "button";
  const isPrimary = variant === "default" || variant === "primary";

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      data-loading={loading || undefined}
      aria-busy={loading || undefined}
      disabled={disabled || loading}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    >
      {loading && !asChild ? (
        <>
          <span aria-hidden="true" className="invisible inline-flex items-center gap-[inherit]">
            {children}
          </span>
          <span className="absolute inset-0 flex items-center justify-center">
            <LoadingDots
              label={loadingLabel}
              size={getLoadingSize(size)}
              tone={isPrimary ? "inverse" : "neutral"}
            />
          </span>
        </>
      ) : (
        children
      )}
    </Comp>
  );
}

export { Button };
export type { ButtonProps };
