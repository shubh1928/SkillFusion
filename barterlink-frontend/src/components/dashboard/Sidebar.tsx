import { NavLink, useNavigate } from "react-router";
import type { Dispatch, SetStateAction } from "react";

import {
  LayoutDashboard,
  BriefcaseBusiness,
  FileText,
  CalendarClock,
  Wallet,
  Wrench,
  Star,
  User,
  LogOut,
  PanelLeft,
} from "lucide-react";

import useAuth from "@/auth/store";
import { Button } from "@/components/ui/button";



interface SidebarProps {
  sidebarOpen: boolean;
  setSidebarOpen: Dispatch<SetStateAction<boolean>>;
}

function Sidebar({
  sidebarOpen,
  setSidebarOpen,
}: SidebarProps) {
  const navigate = useNavigate();

  const logout = useAuth((state) => state.logout);
  const user = useAuth((state) => state.user);

  const menuItems = [
    {
      title: "Dashboard",
      icon: LayoutDashboard,
      path: "/dashboard",
      
    },
    {
      title: "Marketplace",
      icon: BriefcaseBusiness,
      path: "/dashboard/marketplace",
    },
    {
      title: "Agreements",
      icon: FileText,
      path: "/dashboard/agreements",
    },
    {
      title: "Sessions",
      icon: CalendarClock,
      path: "/dashboard/sessions",
    },
    {
      title: "Wallet",
      icon: Wallet,
      path: "/dashboard/wallet",
    },
    {
      title: "Skills",
      icon: Wrench,
      path: "/dashboard/skills",
    },
    {
      title: "Ratings",
      icon: Star,
      path: "/dashboard/ratings",
    },
    {
      title: "Profile",
      icon: User,
      path: "/dashboard/profile",
    },
  ];

  return (
      <aside
        className={`
          fixed
          top-0
          left-0
          z-50

          h-screen

          flex
          flex-col

         border-r
          bg-background
          shadow-xl
          md:shadow-none

          transition-[width,transform]
          duration-300
          ease-in-out

          ${
            sidebarOpen
              ? "w-72 translate-x-0"
              : "w-72 -translate-x-full md:w-20 md:translate-x-0"
          }

          md:relative

          overflow-hidden
        `}
      >
      {/* Logo */}
      

      <div
  className={`
    flex
    items-center
    border-b
    px-4
    py-5
    ${
sidebarOpen
  ? "justify-between"
  : "flex-col items-center gap-4"
    }
  `}
>

{sidebarOpen ? (

<>
  <div className="flex items-center gap-3">

    

    <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground font-bold text-lg">
      B
    </div>

    <div>

      <h1 className="text-lg font-bold">
        BarterLink
      </h1>

      <p className="text-xs text-muted-foreground">
        Skill Exchange Platform
      </p>

    </div>

  </div>

  <Button
    variant="ghost"
    size="icon"
    onClick={() => setSidebarOpen(false)}
  >
    <PanelLeft size={18} />
  </Button>
</>

) : (

<Button
  variant="ghost"
  onClick={() => setSidebarOpen(true)}
  className="
    relative
    h-11
    w-11
    rounded-xl
    bg-primary
    text-primary-foreground
    overflow-hidden
    group
    p-0
  "
>

  {/* B */}

  <span
    className="
      absolute
      inset-0
      flex
      items-center
      justify-center
      font-bold
      transition-all
      duration-300

      group-hover:opacity-0
      group-hover:scale-75
    "
  >
    B
  </span>
  

  {/* Drawer */}

  <PanelLeft
    size={18}
    className="
      absolute
      opacity-0
      scale-75
      transition-all
      duration-300

      group-hover:opacity-100
      group-hover:scale-100
    "
  />

</Button>

)}

      </div>

     {/* User */}

<div
  className={`
    border-b
    py-5
    ${
      sidebarOpen
        ? "px-6"
        : "flex items-center justify-center"
    }
  `}
>

  {sidebarOpen ? (

    <div className="space-y-1">

      <h3 className="font-semibold">
        {user?.name}
      </h3>

      <p className="text-sm text-muted-foreground">
        {user?.email}
      </p>

    </div>

  ) : (

    user?.image ? (

      <img
        src={user.image}
        alt={user.name}
        className="
          h-11
          w-11
          rounded-full
          object-cover
          border
          border-border
        "
      />

    ) : (

      <div
        className="
          flex
          h-11
          w-11
          items-center
          justify-center
          rounded-full
          bg-primary
          text-primary-foreground
          font-semibold
        "
      >
        {user?.name?.charAt(0)?.toUpperCase()}
      </div>

    )

  )}

</div>
      {/* Navigation */}

      <nav className="flex-1 space-y-2 overflow-y-auto px-3 py-4">

        {menuItems.map((item) => {

          const Icon = item.icon;

          return (

            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === "/dashboard"}
              className={({ isActive }) =>
              `
              flex
              items-center
              py-3
              rounded-xl
              transition-all
              duration-200

              ${
                sidebarOpen
                  ? "gap-3 px-4 justify-start"
                  : "justify-center px-0"
              }

              ${
                isActive
                  ? "bg-primary text-primary-foreground shadow-lg"
                  : "hover:bg-muted hover:scale-[1.02]"
              }
              `
              }
            >

<Icon size={20}/>

{

sidebarOpen && (

<span className="font-medium">

{item.title}

</span>

)

}

            </NavLink>

          );

        })}

      </nav>

      {/* Logout */}

      <div className="border-t p-4">

          <Button
          variant="outline"
          className={
            sidebarOpen
              ? "w-full justify-start gap-3"
              : "mx-auto h-12 w-12 rounded-xl p-0"
          }
          onClick={() => {
            logout();
            navigate("/");
          }}
        >

          <LogOut size={18} />

          {sidebarOpen && "Logout"}

        </Button>

      </div>

    </aside>
  );
}

export default Sidebar;