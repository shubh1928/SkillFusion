import { Button } from './ui/button'
import { NavLink, useNavigate } from 'react-router'
import useAuth from '@/auth/store';
import { Moon, Sun } from "lucide-react";
import { useEffect, useState } from "react";

function Navbar() {

const checkLogin = useAuth((state) => state.checkLogin);
const user = useAuth((state) => state.user);
const logout = useAuth((state) => state.logout);
const navigate = useNavigate();

const [darkMode, setDarkMode] = useState(
  localStorage.getItem("theme") !== "light"
);

useEffect(() => {

  const storedTheme = localStorage.getItem("theme");

  if (storedTheme === "light") {
    document.documentElement.classList.remove("dark");
    setDarkMode(false);
  } else {
    document.documentElement.classList.add("dark");
    setDarkMode(true);
  }

}, []);


const toggleTheme = () => {

  if (darkMode) {
    document.documentElement.classList.remove("dark");
    localStorage.setItem("theme", "light");
  } else {
    document.documentElement.classList.add("dark");
    localStorage.setItem("theme", "dark");
  }

  setDarkMode(!darkMode);
};

 return (
  <header className="sticky top-0 z-50 px-4 py-4">

    <nav
      className="
        mx-auto
        flex
        max-w-7xl
        items-center
        justify-between
        rounded-2xl
        border
        border-border/50
        bg-background/70
        px-6
        py-3
        shadow-xl
        backdrop-blur-xl
      "
    >

      {/* Logo */}

      <NavLink
        to="/"
        className="flex items-center gap-3"
      >

        <div
          className="
            flex
            h-10
            w-10
            items-center
            justify-center
            rounded-xl
            bg-gradient-to-br
            from-primary
            to-violet-500
            text-lg
            font-bold
            text-white
            shadow-lg
          "
        >
          SF
        </div>

        <div>

          <h1 className="text-lg font-bold">

            SkillFusion

          </h1>

          <p className="text-xs text-muted-foreground">

            Skill Exchange Platform

          </p>

        </div>

      </NavLink>

      {/* Right */}

      <div className="flex items-center gap-3">

        <Button
          variant="outline"
          size="icon"
          onClick={toggleTheme}
          className="rounded-xl"
        >

          {darkMode ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}

        </Button>

        {

          checkLogin()

          ?

          <>

            <NavLink
              to="/dashboard/profile"
              className="
                rounded-xl
                px-4
                py-2
                text-sm
                font-medium
                transition-colors
                hover:bg-accent
              "
            >

              {user?.name}

            </NavLink>

            <Button
              variant="outline"
              size="sm"
              onClick={async () => {
                    await logout();
                    navigate("/");
                }}
            >

              Logout

            </Button>

          </>

          :

          <>

            <NavLink
              to="/"
              className={({ isActive }) =>
                `rounded-xl px-4 py-2 text-sm transition-colors ${
                  isActive
                    ? "bg-primary text-primary-foreground"
                    : "hover:bg-accent"
                }`
              }
            >

              Home

            </NavLink>

            <NavLink to="/login">

              <Button
                variant="ghost"
                size="sm"
              >

                Login

              </Button>

            </NavLink>

            <NavLink to="/signup">

              <Button
                size="sm"
                className="
                  rounded-xl
                  bg-gradient-to-r
                  from-primary
                  to-violet-500
                  hover:opacity-90
                "
              >

                Signup

              </Button>

            </NavLink>

          </>

        }

      </div>

    </nav>

  </header>
);
}

export default Navbar