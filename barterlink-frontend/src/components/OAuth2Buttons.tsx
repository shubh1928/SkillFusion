import { Button } from "@/components/ui/button";
import { Github } from "lucide-react";

interface OAuth2ButtonsProps {
  referrerCode?: string | null;
}

function OAuth2Buttons({ referrerCode }: OAuth2ButtonsProps) {
  const buildOAuthUrl = (
    provider: "google" | "github"
  ): string => {
    const backendBaseUrl = (
      import.meta.env.VITE_BASE_URL || "http://localhost:8084"
    ).replace(/\/+$/, "");

    const url = new URL(
      `${backendBaseUrl}/oauth2/authorization/${provider}`
    );

    const code = referrerCode?.trim();

    if (code) {
      url.searchParams.set("referrerCode", code);
    }

    return url.toString();
  };

  const startOAuth = (
    provider: "google" | "github"
  ) => {
    window.location.assign(buildOAuthUrl(provider));
  };

  return (
    <div className="grid grid-cols-2 gap-4">
      <Button
        type="button"
        variant="outline"
        onClick={() => startOAuth("google")}
        className="w-full h-14 rounded-2xl border border-black/10 dark:border-white/10 bg-black/5 dark:bg-white/5 hover:bg-black/10 dark:hover:bg-white/10 backdrop-blur-xl text-sm font-medium cursor-pointer flex items-center justify-center transition-all duration-300 hover:scale-[1.02]"
      >
        <img
          src="https://www.svgrepo.com/show/475656/google-color.svg"
          alt=""
          aria-hidden="true"
          className="h-5 w-5 mr-2"
        />
        Google
      </Button>

      <Button
        type="button"
        variant="outline"
        onClick={() => startOAuth("github")}
        className="w-full h-14 rounded-2xl border border-black/10 dark:border-white/10 bg-black/5 dark:bg-white/5 hover:bg-black/10 dark:hover:bg-white/10 backdrop-blur-xl text-sm font-medium cursor-pointer flex items-center justify-center transition-all duration-300 hover:scale-[1.02]"
      >
        <Github className="h-5 w-5 mr-2" />
        Github
      </Button>
    </div>
  );
}

export default OAuth2Buttons;