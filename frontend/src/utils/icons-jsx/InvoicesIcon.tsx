export interface IconsProps {
    size?: number;
    color?: string;
    className?: string; 
    fill?: string;
    stroke?: string;
}

const InvoicesIcon = (props: IconsProps) => {
  return (
    <svg
      width={props.size || "20"}
      height={props.size || "20"}
      viewBox="0 0 20 20"
      fill={props.fill || "none"}
      className={props.className}
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M13.9625 1.66669H6.03751C5.07179 1.66669 4.58892 1.66669 4.19949 1.80219C3.46102 2.05915 2.88123 2.65601 2.63163 3.41624C2.5 3.81714 2.5 4.31423 2.5 5.3084V16.9785C2.5 17.6937 3.32083 18.0732 3.84008 17.598C4.14514 17.3189 4.60486 17.3189 4.90992 17.598L5.3125 17.9664C5.84716 18.4557 6.65284 18.4557 7.1875 17.9664C7.72216 17.4772 8.52784 17.4772 9.0625 17.9664C9.59716 18.4557 10.4028 18.4557 10.9375 17.9664C11.4722 17.4772 12.2778 17.4772 12.8125 17.9664C13.3472 18.4557 14.1528 18.4557 14.6875 17.9664L15.0901 17.598C15.3951 17.3189 15.8549 17.3189 16.1599 17.598C16.6792 18.0732 17.5 17.6937 17.5 16.9785V5.3084C17.5 4.31423 17.5 3.81714 17.3684 3.41624C17.1188 2.65601 16.539 2.05915 15.8005 1.80219C15.4111 1.66669 14.9282 1.66669 13.9625 1.66669Z"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
      />
      <path
        d="M8.75 9.16669L14.1667 9.16669"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
      <path
        d="M5.83398 9.16669H6.25065"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
      <path
        d="M5.83398 6.25H6.25065"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
      <path
        d="M5.83398 12.0833H6.25065"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
      <path
        d="M8.75 6.25H14.1667"
        stroke={props.stroke || "currentColor"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
      <path
        d="M8.75 12.0833H14.1667"
        stroke={props.stroke || "#EDEEF1"}
        strokeWidth="1.25"
        strokeLinecap="round"
      />
    </svg>
  );
};

export default InvoicesIcon;
