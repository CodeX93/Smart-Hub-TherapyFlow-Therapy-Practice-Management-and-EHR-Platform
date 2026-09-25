const AuthTopbar = () => {
  return (
    <div>
      <div className="flex items-center gap-2">
        <div className="w-9.5 h-9.5 bg-(--bg-primary-dark) rounded-[0.6875rem] flex items-center justify-center uppercase text-(--text-primary-light) font-semibold">
          SH
        </div>
        <div>
          <h1 className="text-[1rem] font-semibold leading-6">SmartHub</h1>
          <p className="text-[0.75rem] text-(--text-neutral-600) font-medium leading-4.5">
            Intelligent Insights
          </p>
        </div>
      </div>
    </div>
  );
};

export default AuthTopbar;
