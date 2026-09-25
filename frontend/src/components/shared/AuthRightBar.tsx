import { CircleCheck } from "lucide-react";
import { AUTH_STATIC_CONTENT } from "../../pages/auth/auth.static";

const AuthRightBar = () => {
  const { services, rightBoxTitle } = AUTH_STATIC_CONTENT;
  return (
    <div className="border-l border-var(--text-neutral-100) flex flex-col items-center">
      <div className="flex gap-3 bg-(--bg-primary-100) w-full items-center">
        <h1 className="text-(--text-primary-dark) md:text-lg font-medium max-w-[50%] md:max-w-[55%] md:px-12 md:py-6 px-4 py-8">
          {rightBoxTitle}
        </h1>
        <div className="px-5 md:px-12 pt-6 h-full w-full flex justify-end items-end bg-(--bg-primary-50) md:bg-(--bg-primary-100)">
          <img
            src="/assets/auth-right-sidee.png"
            alt="screenshot"
            className="border-(--text-primary-dark) border-t-3 border-l-3 border-r-3 rounded-t-xl p-0.5"
          />
        </div>
      </div>
      <div className="flex flex-col gap-3 md:px-12 md:py-12 px-4 py-8">
        {services?.map((service) => (
          <div key={service?.id} className="flex gap-4">
            <div className="mt-0.5">
              <CircleCheck
                className="text-(--text-primary-dark)"
                strokeWidth={1.5}
              />
            </div>
            <div className="flex flex-col gap-1">
              <h4 className="text-(--text-primary-dark) font-medium">
                {service?.title}
              </h4>
              <p className="text-sm text-(--text-neutral-600)">
                {service?.description}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AuthRightBar;
