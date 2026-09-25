interface SectionHeadingProps {
  title: string;
  subtitle: string;
}

function SectionHeading(props: SectionHeadingProps) {
  return (
    <div>
      <div className="text-[#1f2d38] text-[1.0625rem] font-semibold leading-7">
        {props.title}
      </div>
      <div className="mt-1 text-[#8a96a3] text-[0.6875rem] font-normal leading-4">
        {props.subtitle}
      </div>
    </div>
  );
}

export default SectionHeading;
