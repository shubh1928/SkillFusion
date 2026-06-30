import { useEffect, useState } from "react";
import { Outlet } from "react-router";

import Sidebar from "./Sidebar";
import Topbar from "./Topbar";

function DashboardLayout() {

  const [sidebarOpen, setSidebarOpen] = useState(() => {
  const saved = localStorage.getItem("sidebar-open");

  return saved === null ? true : saved === "true";
});

useEffect(() => {
  localStorage.setItem(
    "sidebar-open",
    String(sidebarOpen)
  );
}, [sidebarOpen]);

  return (
    <div className="min-h-screen bg-background text-foreground">

        <div className="flex h-screen overflow-hidden">

          {sidebarOpen && (
            <div
              className="
                fixed
                inset-0
                z-40
                bg-black/50
                backdrop-blur-[2px]
                md:hidden
              "
              onClick={() => setSidebarOpen(false)}
            />
          )}

          <Sidebar
            sidebarOpen={sidebarOpen}
            setSidebarOpen={setSidebarOpen}
          />

          <div className="flex flex-1 flex-col overflow-hidden">

          <Topbar
            setSidebarOpen={setSidebarOpen}
          />

          <main className="flex-1 overflow-y-auto bg-muted/20 p-4 md:p-6">

            <div className="mx-auto w-full max-w-7xl">

              <Outlet />

            </div>

          </main>

        </div>

      </div>

    </div>
  );
}

export default DashboardLayout;