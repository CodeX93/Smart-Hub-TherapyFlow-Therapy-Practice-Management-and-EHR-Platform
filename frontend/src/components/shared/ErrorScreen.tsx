import { Button } from "../ui/button";

// Import images
import ErrorImg from "../../assets/error.png";
import SessionImg from "../../assets/session.png";
import OfflineImg from "../../assets/offline.png";
import MaintenanceImg from "../../assets/maintenance.png";

export type ErrorType =
    | "404"
    | "401"
    | "403"
    | "500"
    | "503"
    | "session-expired"
    | "offline"
    | "maintenance";

interface ErrorScreenProps {
    type: ErrorType;
    onAction?: () => void;
    customTitle?: string;
    customDescription?: string;
    customButtonText?: string;
}

const ERROR_CONFIG: Record<ErrorType, {
    title: string;
    description: string;
    buttonText: string;
    image: string;
}> = {
    "404": {
        title: "Page not found",
        description: "The page you’re looking for doesn’t exist or was moved.",
        buttonText: "Go to Home",
        image: ErrorImg
    },
    "401": {
        title: "Login required",
        description: "Please sign in to continue",
        buttonText: "Sign in",
        image: ErrorImg
    },
    "403": {
        title: "Access denied",
        description: "You don’t have permission to view this page.",
        buttonText: "Go back",
        image: ErrorImg
    },
    "500": {
        title: "Something went wrong",
        description: "We ran into an issue on our end. Please try again.",
        buttonText: "Try again",
        image: ErrorImg
    },
    "503": {
        title: "Service unavailable",
        description: "The service is temporarily down. Try again shortly.",
        buttonText: "Try again",
        image: ErrorImg
    },
    "session-expired": {
        title: "Session expired",
        description: "For security reasons, please sign in again.",
        buttonText: "Sign in again",
        image: SessionImg
    },
    "offline": {
        title: "You’re offline",
        description: "Check your internet connection and try again.",
        buttonText: "Check status",
        image: OfflineImg
    },
    "maintenance": {
        title: "Under maintenance",
        description: "We’re making improvements. Please check back soon.",
        buttonText: "Retry",
        image: MaintenanceImg
    }
};

const ErrorScreen = ({
    type,
    onAction,
    customTitle,
    customDescription,
    customButtonText
}: ErrorScreenProps) => {
    const config = ERROR_CONFIG[type];

    // Fallback if type is invalid (though TS should prevent this)
    if (!config) return null;

    // Check if type is numeric for display
    const isNumericCode = /^\d+$/.test(type);

    return (
        <div className="flex flex-col items-center justify-center min-h-screen w-full p-6 text-center animate-in fade-in zoom-in-95 duration-300">
            <div className="mb-8 relative flex items-center justify-center">
                {/* Image Container */}
                <div className="relative">
                    <img
                        src={config.image}
                        alt={config.title}
                        className="w-full max-w-120 h-auto object-contain mx-auto grayscale opacity-140"
                    />

                    {/* Error Code Overlay */}
                    {isNumericCode && (
                        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                            <span className="text-5xl sm:text-6xl font-bold text-gray-500/30 tracking-[0.2em] transform -translate-y-2 select-none">
                                {type}
                            </span>
                        </div>
                    )}
                </div>
            </div>

            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-3 tracking-tight">
                {customTitle || config.title}
            </h1>

            <p className="text-gray-500 mb-8 max-w-md mx-auto text-base leading-relaxed">
                {customDescription || config.description}
            </p>

            <Button
                onClick={onAction}
                className="rounded-full px-10 h-12 bg-[#334155] hover:bg-[#334155]/90 text-white font-medium transition-all shadow-sm text-base cursor-pointer"
            >
                {customButtonText || config.buttonText}
            </Button>
        </div>
    );
};

export default ErrorScreen;
