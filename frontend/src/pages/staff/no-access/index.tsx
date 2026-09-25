const StaffNoAccess = () => {
  return (
    <div className="flex flex-1 flex-col items-center justify-center py-16 text-center">
      <h1 className="text-2xl font-bold text-gray-900 mb-3">
        No access configured
      </h1>
      <p className="text-gray-500 max-w-md text-base leading-relaxed">
        No access has been configured for your role. Please contact your
        administrator.
      </p>
    </div>
  );
};

export default StaffNoAccess;
