import { ArrowLeft } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "../../components/ui/button";

const NotFound = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-(--bg-primary-light) relative overflow-hidden">
      {/* Background Decorations */}
      <div className="absolute -top-40 -left-40 w-100 h-100 bg-(--bg-primary-dark)/10 rounded-full blur-3xl" />
      <div className="absolute -bottom-40 -right-40 w-100 h-100 bg-(--bg-primary-dark)/20 rounded-full blur-3xl" />

      {/* Content Card */}
      <div className="relative z-10 bg-white/80 backdrop-blur-xl border border-(--text-neutral-100) rounded-3xl shadow-xl p-10 max-w-lg w-full text-center">
        {/* 404 Text */}
        <h1 className="text-[6rem] font-extrabold leading-none text-(--bg-primary-dark)">
          404
        </h1>

        <p className="mt-2 text-lg font-semibold text-(--text-primary-dark)">
          Page not found
        </p>

        <p className="mt-3 text-sm text-(--text-neutral-600)">
          Sorry, the page you are looking for doesn’t exist or has been moved.
        </p>

        {/* Divider */}
        <div className="my-6 h-px w-full bg-linear-to-r from-transparent via-(--text-neutral-200) to-transparent" />

        {/* Action */}
        <Button
          asChild
          className="rounded-lg bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white px-6 h-10"
        >
          <Link to="/" className="flex items-center gap-2">
            <ArrowLeft size={18} />
            Back to Dashboard
          </Link>
        </Button>
      </div>
    </div>
  );
};

export default NotFound;
