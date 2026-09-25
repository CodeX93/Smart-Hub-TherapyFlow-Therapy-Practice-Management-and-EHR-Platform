import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthCheckEmail from "@/components/auth/AuthCheckEmail";

const CheckEmail = () => {
  return (
    <AuthLayoutWrapper title="" showRightBar={true}>
      <AuthCheckEmail backToLoginPath="/auth/login" align="left" />
    </AuthLayoutWrapper>
  );
};

export default CheckEmail;
