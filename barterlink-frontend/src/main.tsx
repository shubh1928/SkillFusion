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
import { TooltipProvider } from "@/components/ui/tooltip";

import OAuthSuccess from "./pages/users/OAuthSuccess";
import Userlayout from "./pages/users/Userlayout";
import SessionWorkspacePage from "./pages/sessions/SessionWorkspacePage";

import DashboardLayout from "./components/dashboard/DashboardLayout";

import Userhome from "./pages/users/Userhome";
import UserProfile from "./pages/users/UserProfile";

import ConversationPage from "./pages/negotiations/ConversationPage";

// Dashboard Pages (we'll build these next)
import MarketplacePage from "./pages/marketplace/MarketplacePage.tsx";
import AgreementsPage from "./pages/agreements/AgreementsPage";
import SessionsPage from "./pages/sessions/SessionsPage";
import WalletPage from "./pages/wallet/WalletPage";
import SkillsPage from "./pages/skills/SkillsPage";
import RatingsPage from "./pages/ratings/RatingsPage";
import NotificationsPage from "./pages/notifications/NotificationsPage";
import NegotiationsPage from "./pages/negotiations/NegotiationsPage";
import GovernancePage from "./pages/governance/GovernancePage";
import MyDisputesPage from "./pages/disputes/MyDisputesPage";
import ReferralPage from "./pages/referrals/ReferralPage";
import ProfitIdeasPage from "./pages/profit/ProfitIdeasPage";


createRoot(document.getElementById("root")!).render(
<TooltipProvider>
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

                    <Route
                        path="negotiations"
                        element={<NegotiationsPage />}
                    />

                    <Route
                        path="negotiations/:negotiationId"
                        element={<ConversationPage />}
                    />

                    <Route path="agreements" element={<AgreementsPage />} />

            <Route path="sessions" element={<SessionsPage />} />

            <Route path="sessions/:sessionId" element={<SessionWorkspacePage />}/>

            <Route path="wallet" element={<WalletPage />} />

            <Route path="governance" element={<GovernancePage />} />

            <Route path="governance" element={<GovernancePage />} />

            <Route path="my-disputes" element={<MyDisputesPage />} />
            <Route path="referrals" element={<ReferralPage />} />

            <Route   path="profit-ideas" element={<ProfitIdeasPage />}
/>

            <Route path="skills" element={<SkillsPage />} />

            <Route path="ratings" element={<RatingsPage />} />

            <Route path="notifications" element={<NotificationsPage />}/>

            <Route path="profile" element={<UserProfile />} />

          </Route>

        </Route>

      </Route>

    </Routes>
  </BrowserRouter>
</TooltipProvider>
);