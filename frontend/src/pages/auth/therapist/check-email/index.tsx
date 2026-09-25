import AuthLayoutWrapper from "@/components/auth/AuthLayoutWrapper";
import AuthCheckEmail from "@/components/auth/AuthCheckEmail";

const TherapistCheckEmail = () => {
  return (
    <AuthLayoutWrapper title="" showRightBar={false}>
      <AuthCheckEmail backToLoginPath="/auth/staff/login" />
    </AuthLayoutWrapper>
  );
};

export default TherapistCheckEmail;
