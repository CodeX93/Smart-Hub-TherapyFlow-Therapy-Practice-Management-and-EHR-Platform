import { cva } from "class-variance-authority";

const buttonVariants = cva(
  "relative inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-full font-semibold tracking-normal transition-colors outline-none disabled:pointer-events-none disabled:opacity-100 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-5 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40",
  {
    variants: {
      variant: {
        primary:
          "bg-[var(--btn-default-bg)] text-[var(--btn-default-text)] shadow-none hover:bg-[var(--btn-default-bg-hover)] active:bg-[var(--btn-default-bg-pressed)] active:shadow-[inset_0_2px_5px_0_var(--btn-pressed-shadow)] focus-visible:ring-[3px] focus-visible:ring-[var(--btn-focus-ring)] disabled:bg-[var(--btn-disabled-bg)] disabled:text-[var(--btn-disabled-text)]",
        default:
          "bg-[var(--btn-default-bg)] text-[var(--btn-default-text)] shadow-none hover:bg-[var(--btn-default-bg-hover)] active:bg-[var(--btn-default-bg-pressed)] active:shadow-[inset_0_2px_5px_0_var(--btn-pressed-shadow)] focus-visible:ring-[3px] focus-visible:ring-[var(--btn-focus-ring)] disabled:bg-[var(--btn-disabled-bg)] disabled:text-[var(--btn-disabled-text)]",
        destructive:
          "bg-destructive text-white shadow-none hover:bg-destructive/90 focus-visible:ring-[3px] focus-visible:ring-destructive/20 disabled:bg-[var(--btn-disabled-bg)] disabled:text-[var(--btn-disabled-text)] dark:bg-destructive/60",
        destructiveOutline:
          "border border-destructive bg-transparent text-destructive shadow-none hover:bg-destructive/5 active:bg-destructive/10 focus-visible:ring-[3px] focus-visible:ring-destructive/20 disabled:border-[var(--btn-outline-border)] disabled:text-[var(--btn-disabled-text)]",
        secondary:
          "border border-[var(--btn-outline-border)] bg-transparent text-[var(--btn-outline-text)] shadow-none hover:border-[var(--btn-outline-border-hover)] hover:bg-transparent hover:text-[var(--btn-outline-border-hover)] active:border-[var(--btn-outline-border-pressed)] active:text-[var(--btn-outline-border-pressed)] focus-visible:ring-[3px] focus-visible:ring-[var(--btn-outline-border)] disabled:border-[var(--btn-outline-border)] disabled:text-[var(--btn-disabled-text)]",
        outline:
          "border border-[var(--btn-outline-border)] bg-transparent text-[var(--btn-outline-text)] shadow-none hover:border-[var(--btn-outline-border-hover)] hover:bg-transparent hover:text-[var(--btn-outline-border-hover)] active:border-[var(--btn-outline-border-pressed)] active:text-[var(--btn-outline-border-pressed)] focus-visible:ring-[3px] focus-visible:ring-[var(--btn-outline-border)] disabled:border-[var(--btn-outline-border)] disabled:text-[var(--btn-disabled-text)]",
        tertiary:
          "rounded-lg bg-transparent text-[var(--btn-link-text)] shadow-none hover:bg-transparent hover:text-[var(--btn-link-text-hover)] hover:underline active:text-[var(--btn-link-text-pressed)] active:underline focus-visible:rounded-full focus-visible:ring-[3px] focus-visible:ring-[var(--btn-outline-border)] disabled:text-[var(--btn-disabled-text)] disabled:no-underline",
        ghost:
          "bg-transparent shadow-none hover:bg-accent hover:text-accent-foreground focus-visible:ring-[3px] focus-visible:ring-[var(--btn-outline-border)] dark:hover:bg-accent/50",
        link:
          "rounded-lg bg-transparent text-[var(--btn-link-text)] shadow-none underline-offset-4 hover:bg-transparent hover:text-[var(--btn-link-text-hover)] hover:underline active:text-[var(--btn-link-text-pressed)] active:underline focus-visible:rounded-full focus-visible:ring-[3px] focus-visible:ring-[var(--btn-outline-border)] disabled:text-[var(--btn-disabled-text)] disabled:no-underline",
      },
      size: {
        default: "h-10 px-6 text-sm leading-[1.375rem]",
        sm: "h-9 gap-1 px-6 text-sm leading-[1.375rem] [&_svg:not([class*='size-'])]:size-5",
        md: "h-10 px-6 text-sm leading-[1.375rem] [&_svg:not([class*='size-'])]:size-5",
        lg: "h-12 px-6 text-sm leading-[1.375rem] [&_svg:not([class*='size-'])]:size-6",
        xl: "h-14 px-6 text-base leading-6 [&_svg:not([class*='size-'])]:size-6",
        icon: "size-10 p-0",
        "icon-sm": "size-8",
        "icon-lg": "size-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
    compoundVariants: [
      {
        variant: "secondary",
        size: "xl",
        className: "border-[1.5px]",
      },
      {
        variant: "outline",
        size: "xl",
        className: "border-[1.5px]",
      },
    ],
  },
);

export { buttonVariants };
