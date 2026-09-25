import { SuspenseOutlet } from "@/components/shared/SuspenseOutlet";
import AuthTopbar from "../components/topbar/AuthTopbar"

const AuthLayout = () => {
  return (
    <div className="min-h-screen bg-(--bg-primary-light) md:px-10 md:py-10 px-4 py-6">
      <div>
        <AuthTopbar />
        <main className="md:px-5 md:pt-4 flex items-center justify-center md:mt-15 mt-8">
          <SuspenseOutlet />
        </main>
      </div>
    </div>
  )
}

export default AuthLayout
