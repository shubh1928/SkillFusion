import {
  Moon,
  Sun,
  PanelLeft,
} from "lucide-react";

import {
  useEffect,
  useMemo,
  useState,
  type Dispatch,
  type SetStateAction,
} from "react";

import { useLocation } from "react-router";

import useAuth from "@/auth/store";

import { Button } from "@/components/ui/button";

import NotificationBell from "@/components/notifications/NotificationBell";

interface TopbarProps {
  setSidebarOpen: Dispatch<SetStateAction<boolean>>;
}

function Topbar({
  setSidebarOpen,
}: TopbarProps) {
  const location = useLocation();

  const user = useAuth((state) => state.user);

  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem("theme") !== "light";
  });

  useEffect(() => {
    document.documentElement.classList.toggle(
      "dark",
      darkMode,
    );
  }, [darkMode]);

  const toggleTheme = () => {
    const newTheme = !darkMode;

    setDarkMode(newTheme);

    localStorage.setItem(
      "theme",
      newTheme ? "dark" : "light",
    );
  };

  const pageTitle = useMemo(() => {
    const pathname = location.pathname;

    if (pathname === "/dashboard") return "Dashboard";
    if (pathname.includes("marketplace")) return "Marketplace";
    if (pathname.includes("agreements")) return "Agreements";
    if (pathname.includes("sessions")) return "Sessions";
    if (pathname.includes("wallet")) return "Wallet";
    if (pathname.includes("referrals")) return "Refer & Earn";
    if (pathname.includes("skills")) return "Skills";
    if (pathname.includes("ratings")) return "Ratings";
    if (pathname.includes("notifications")) return "Notifications";
    if (pathname.includes("profile")) return "Profile";

    return "SkillFusion";
  }, [location.pathname]);

  return (
    <header className="sticky top-0 z-40 border-b border-border/50 bg-background/70 backdrop-blur-xl">
      <div className="flex h-16 items-center justify-between px-4 md:px-6">
        {/* Left side */}
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <PanelLeft size={20} />
          </Button>

          <div>
            <h1 className="text-xl font-bold md:text-2xl">
              {pageTitle}
            </h1>

            <p className="hidden text-sm text-muted-foreground sm:block">
              Welcome back, {user?.name?.split(" ")[0]}
            </p>
          </div>
        </div>

        {/* Right side */}
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="icon"
            onClick={toggleTheme}
            aria-label="Toggle theme"
          >
            {darkMode ? (
              <Sun size={18} />
            ) : (
              <Moon size={18} />
            )}
          </Button>

          {/* Notification dropdown and unread badge */}
          <NotificationBell />

          {/* User avatar */}
          {user?.image ? (
            <img
              src={user.image}
              alt={user.name}
              className="
                h-10
                w-10
                rounded-full
                border
                border-border
                object-cover
              "
            />
          ) : (
            <div
              className="
                flex
                h-10
                w-10
                items-center
                justify-center
                rounded-full
                bg-primary
                font-semibold
                text-primary-foreground
              "
            >
              {user?.name?.charAt(0)?.toUpperCase() ?? "U"}
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

export default Topbar;