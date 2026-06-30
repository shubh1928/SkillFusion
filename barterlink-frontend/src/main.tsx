import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router";

import "./index.css";

import App from "./App";

import RootLayout from "./pages/RootLayout";

import Login from "./pages/Login";
import Signup from "./pages/Signup";
import About from "./pages/About";
import Services from "./pages/Services";
import OAuthFailure from "./pages/OAuthFailure";

import OAuthSuccess from "./pages/users/OAuthSuccess";
import Userlayout from "./pages/users/Userlayout";

import DashboardLayout from "./components/dashboard/DashboardLayout";

import Userhome from "./pages/users/Userhome";
import UserProfile from "./pages/users/UserProfile";

// Dashboard Pages (we'll build these next)
import MarketplacePage from "./pages/marketplace/MarketplacePage.tsx";
import AgreementsPage from "./pages/agreements/AgreementsPage";
import SessionsPage from "./pages/sessions/SessionsPage";
import WalletPage from "./pages/wallet/WalletPage";
import SkillsPage from "./pages/skills/SkillsPage";
import RatingsPage from "./pages/ratings/RatingsPage";


createRoot(document.getElementById("root")!).render(
  <BrowserRouter>
    <Routes>

      <Route path="/" element={<RootLayout />}>

        <Route index element={<App />} />

        <Route path="login" element={<Login />} />

        <Route path="signup" element={<Signup />} />

        <Route path="about" element={<About />} />

        <Route path="services" element={<Services />} />

        <Route path="oauth/success" element={<OAuthSuccess />} />

        <Route path="oauth/failure" element={<OAuthFailure />} />

        <Route path="/dashboard" element={<Userlayout />}>

          <Route element={<DashboardLayout />}>

            <Route index element={<Userhome />} />

            <Route path="marketplace" element={<MarketplacePage />} />

            <Route path="agreements" element={<AgreementsPage />} />

            <Route path="sessions" element={<SessionsPage />} />

            <Route path="wallet" element={<WalletPage />} />

            <Route path="skills" element={<SkillsPage />} />

            <Route path="ratings" element={<RatingsPage />} />

            <Route path="profile" element={<UserProfile />} />

          </Route>

        </Route>

      </Route>

    </Routes>
  </BrowserRouter>
);