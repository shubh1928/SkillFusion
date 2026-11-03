import {
  Mail,
  Calendar,
  ShieldCheck,
  BadgeCheck,
  User2,
  Github,
  Chrome,
} from "lucide-react";

import ResourceSection from "@/components/profile/resources/ResourceSection";

import { useNavigate } from "react-router";

import useAuth from "@/auth/store";

import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

function UserProfile() {

  const navigate = useNavigate();

  const user = useAuth((state) => state.user);

  const providerName =
    user?.provider === "google"
      ? "Google"
      : user?.provider === "github"
      ? "GitHub"
      : "BarterLink";

  const memberSince = user?.createdAt
    ? new Date(user.createdAt).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "long",
        year: "numeric",
      })
    : "N/A";

  const lastUpdated = user?.updatedAt
    ? new Date(user.updatedAt).toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "Never";

  return (

    <div className="relative overflow-hidden">

      {/* Background */}

      <div
        className="
          absolute
          inset-0
          -z-10
          bg-[radial-gradient(circle_at_top_left,#3b82f620,transparent_30%),radial-gradient(circle_at_bottom_right,#06b6d420,transparent_35%)]
        "
      />

      <div className="mx-auto max-w-7xl space-y-8">

        {/* Header */}

        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">

          <div>

            <h1 className="mt-5 text-4xl font-bold">
              My Profile
            </h1>

            <p className="mt-2 text-muted-foreground">
              Manage your SkillFusion identity and account information.
            </p>

          </div>

        </div>

        {/* Hero Card */}

        <Card className="overflow-hidden rounded-3xl">

          <CardContent className="p-8">

            <div className="flex flex-col gap-8 lg:flex-row lg:items-center">

              {/* Avatar */}

              <div>

                {user?.image ? (

                  <img
                    src={user.image}
                    alt={user.name}
                    className="
                      h-36
                      w-36
                      rounded-full
                      border-4
                      border-primary/20
                      object-cover
                    "
                  />

                ) : (

                  <div
                    className="
                      flex
                      h-36
                      w-36
                      items-center
                      justify-center
                      rounded-full
                      bg-primary
                      text-5xl
                      font-bold
                      text-primary-foreground
                    "
                  >
                    {user?.name?.charAt(0).toUpperCase()}
                  </div>

                )}

              </div>

              {/* User Details */}

              <div className="flex-1">

                <div className="flex flex-wrap items-center gap-3">

                  <h2 className="text-3xl font-bold">

                    {user?.name}

                  </h2>

                  <Badge
                    className="
                      rounded-full
                      bg-green-500/10
                      text-green-600
                    "
                  >
                    <BadgeCheck className="mr-1 h-4 w-4" />
                    Verified
                  </Badge>

                </div>

                <p className="mt-3 text-lg text-muted-foreground">

                  {user?.email}

                </p>

                <div className="mt-6 flex flex-wrap gap-3">

                  <Badge variant="secondary">

                    <ShieldCheck className="mr-2 h-4 w-4" />

                    {user?.role}

                  </Badge>

                  <Badge variant="secondary">

                    {user?.enabled
                      ? "Active Account"
                      : "Disabled"}

                  </Badge>

                  <Badge variant="secondary">

                    {providerName === "Google" && (
                      <Chrome className="mr-2 h-4 w-4" />
                    )}

                    {providerName === "GitHub" && (
                      <Github className="mr-2 h-4 w-4" />
                    )}

                    {providerName}

                  </Badge>

                </div>

              </div>

            </div>

          </CardContent>

        </Card>


                {/* Overview */}

        <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">

          <Card className="rounded-3xl">

            <CardContent className="p-6">

              <div className="mb-4 flex items-center justify-between">

                <Calendar className="h-8 w-8 text-primary" />

                <Badge variant="secondary">
                  Member
                </Badge>

              </div>

              <p className="text-sm text-muted-foreground">
                Member Since
              </p>

              <h3 className="mt-2 text-xl font-bold">
                {memberSince}
              </h3>

            </CardContent>

          </Card>

          <Card className="rounded-3xl">

            <CardContent className="p-6">

              <div className="mb-4 flex items-center justify-between">

                <Mail className="h-8 w-8 text-primary" />

                <Badge variant="secondary">
                  Email
                </Badge>

              </div>

              <p className="text-sm text-muted-foreground">
                Account Email
              </p>

              <h3 className="mt-2 break-all text-lg font-semibold">
                {user?.email}
              </h3>

            </CardContent>

          </Card>

          <Card className="rounded-3xl">

            <CardContent className="p-6">

              <div className="mb-4 flex items-center justify-between">

                <ShieldCheck className="h-8 w-8 text-primary" />

                <Badge variant="secondary">
                  Role
                </Badge>

              </div>

              <p className="text-sm text-muted-foreground">
                Account Role
              </p>

              <h3 className="mt-2 text-xl font-bold">
                {user?.role}
              </h3>

            </CardContent>

          </Card>

          <Card className="rounded-3xl">

            <CardContent className="p-6">

              <div className="mb-4 flex items-center justify-between">

                <User2 className="h-8 w-8 text-primary" />

                <Badge
                  className="
                    bg-green-500/10
                    text-green-600
                  "
                >
                  Online
                </Badge>

              </div>

              <p className="text-sm text-muted-foreground">
                Profile Status
              </p>

              <h3 className="mt-2 text-xl font-bold">

                {user?.enabled
                  ? "Verified"
                  : "Pending"}

              </h3>

            </CardContent>

          </Card>

        </div>



        {/* Quick Actions */}

        <Card className="rounded-3xl">

          <CardContent className="p-8">

            <div className="mb-8">

              <h2 className="text-2xl font-bold">
                Quick Actions
              </h2>

              <p className="text-muted-foreground">
                Navigate quickly to important modules.
              </p>

            </div>

            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-4">

              <Button
                className="h-16 rounded-2xl"
                onClick={() =>
                  navigate("/dashboard/skills")
                }
              >
                Manage Skills
              </Button>

              <Button
                variant="outline"
                className="h-16 rounded-2xl"
                onClick={() =>
                  navigate("/dashboard/wallet")
                }
              >
                Wallet
              </Button>

              <Button
                variant="outline"
                className="h-16 rounded-2xl"
                onClick={() =>
                  navigate("/dashboard/marketplace")
                }
              >
                Marketplace
              </Button>

              <Button
                variant="outline"
                className="h-16 rounded-2xl"
                onClick={() =>
                  navigate("/dashboard/sessions")
                }
              >
                Sessions
              </Button>

            </div>

          </CardContent>

        </Card>

        {/* Resource Library */}

            <ResourceSection />

                {/* Account Information */}

        <Card className="rounded-3xl">

          <CardContent className="p-8">

            <div className="mb-8">

              <h2 className="text-2xl font-bold">
                Account Information
              </h2>

              <p className="text-muted-foreground">
                Your SkillFusion account details.
              </p>

            </div>

            <div className="grid gap-6 lg:grid-cols-2">

              {/* Full Name */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <User2 className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Full Name
                  </span>

                </div>

                <p className="text-muted-foreground">
                  {user?.name}
                </p>

              </div>

              {/* Email */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <Mail className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Email Address
                  </span>

                </div>

                <p className="break-all text-muted-foreground">
                  {user?.email}
                </p>

              </div>

              {/* Login Provider */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <ShieldCheck className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Login Provider
                  </span>

                </div>

                <p className="text-muted-foreground">

                  {providerName}

                </p>

              </div>

              {/* Role */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <BadgeCheck className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Role
                  </span>

                </div>

                <p className="text-muted-foreground">
                  {user?.role}
                </p>

              </div>

              {/* Member Since */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <Calendar className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Member Since
                  </span>

                </div>

                <p className="text-muted-foreground">
                  {memberSince}
                </p>

              </div>

              {/* Last Updated */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <Calendar className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Last Updated
                  </span>

                </div>

                <p className="text-muted-foreground">
                  {lastUpdated}
                </p>

              </div>

              {/* Account Status */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <ShieldCheck className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Account Status
                  </span>

                </div>

                <Badge
                  className={
                    user?.enabled
                      ? "bg-green-500/10 text-green-600"
                      : "bg-red-500/10 text-red-600"
                  }
                >

                  {user?.enabled
                    ? "Verified"
                    : "Disabled"}

                </Badge>

              </div>

              {/* Member ID */}

              <div className="rounded-2xl border p-5">

                <div className="mb-3 flex items-center gap-3">

                  <BadgeCheck className="h-5 w-5 text-primary" />

                  <span className="font-semibold">
                    Member ID
                  </span>

                </div>

                <p className="break-all font-mono text-sm text-muted-foreground">

                  {user?.id}

                </p>

              </div>

            </div>

          </CardContent>

        </Card>

                {/* Tips */}

        <Card className="rounded-3xl">

          <CardContent className="p-8">

            <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">

              <div>

                <h2 className="text-2xl font-bold">

                  Keep Your Profile Updated

                </h2>

                <p className="mt-2 max-w-2xl text-muted-foreground">

                  A complete profile helps other members trust you,
                  improves your visibility in the marketplace, and
                  increases your chances of finding skill exchange
                  opportunities.

                </p>

              </div>

              <Button
                size="lg"
                disabled
                className="rounded-2xl"
              >

                Edit Profile

              </Button>

            </div>

          </CardContent>

        </Card>

      </div>

    </div>

  );

}

export default UserProfile;