import { Outlet, useLocation } from "react-router";

import Navbar from "../components/Navbar";
import Footer from "@/components/Footer";

import { Toaster } from "react-hot-toast";

function RootLayout() {

  const location = useLocation();

  const isDashboard =
    location.pathname.startsWith("/dashboard");

  return (
    <div className="min-h-screen flex flex-col">

      <Toaster />

      {/* Show Navbar only on public pages */}

      {!isDashboard && <Navbar />}

      <main className="flex-1">

        <Outlet />

      </main>

      {/* Show Footer only on public pages */}

      {!isDashboard && <Footer />}

    </div>
  );

}

export default RootLayout;