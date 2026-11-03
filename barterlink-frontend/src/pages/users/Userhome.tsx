import { useEffect, useState } from "react";

import {
  Search,
  Plus,
  Wrench,
  Wallet,
  CalendarClock,
  Handshake,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import {
  loadDashboardData,
  type DashboardData,
} from "@/services/DashboardService";

function Userhome() {

  const [dashboard, setDashboard] =
    useState<DashboardData | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {

    loadDashboard();

  }, []);

  const navigate = useNavigate();

  const loadDashboard = async () => {

    try {

      setLoading(true);

      const data =
        await loadDashboardData();

      setDashboard(data);

    } catch (err) {

      console.error(err);

      setError("Failed to load dashboard.");

    } finally {

      setLoading(false);

    }

  };

  if (loading) {

    return (

      <div className="flex h-[70vh] items-center justify-center">

        Loading Dashboard...

      </div>

    );

  }

  if (error) {

    return (

      <div className="flex h-[70vh] items-center justify-center">

        {error}

      </div>

    );

  }

return (

  <div className="space-y-8">

    {/* Hero */}

    <Card
      className="
        overflow-hidden
        border-0
        bg-gradient-to-r
        from-primary/10
        via-primary/5
        to-background
      "
    >

      <CardContent className="p-8">

        <div
          className="
            flex
            flex-col
            gap-8
            lg:flex-row
            lg:items-center
            lg:justify-between
          "
        >

          {/* Left */}

          <div className="flex items-center gap-6">

            {
              dashboard?.user.image ? (

                <img
                  src={dashboard.user.image}
                  alt={dashboard.user.name}
                  className="
                    h-20
                    w-20
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
                    h-20
                    w-20
                    items-center
                    justify-center
                    rounded-full
                    bg-primary
                    text-3xl
                    font-bold
                    text-primary-foreground
                  "
                >
                  {dashboard?.user.name.charAt(0)}
                </div>

              )
            }

            <div>

              <p className="text-muted-foreground">

                Welcome back 👋

              </p>

              <h1 className="mt-1 text-4xl font-bold">

                {dashboard?.user.name}

              </h1>

              <p className="mt-3 max-w-xl text-muted-foreground">

                Ready to exchange your skills today?
                Discover talented people, earn tokens,
                and grow your reputation.

              </p>

            </div>

          </div>

          {/* Right */}

          <div className="flex gap-3">

          <Button
            size="lg"
            onClick={() => navigate("/dashboard/marketplace")}
          >

              <Search className="mr-2 h-5 w-5" />

              Find Skills

            </Button>

            <Button
              size="lg"
              variant="outline"
            >

              <Plus className="mr-2 h-5 w-5" />

              Offer Skill

            </Button>

          </div>

        </div>

      </CardContent>

    </Card>





    {/* Stats */}

    {/* Stats */}

<div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-4">

  <Card>

    <CardContent className="p-6">

      <div className="flex items-center justify-between">

        <div>

          <p className="text-sm text-muted-foreground">

            My Skills

          </p>

          <h2 className="mt-2 text-4xl font-bold">

            {dashboard?.skills.length ?? 0}

          </h2>

        </div>

        <div className="rounded-xl bg-primary/10 p-3">

          <Wrench className="h-7 w-7 text-primary" />

        </div>

      </div>

    </CardContent>

  </Card>



  <Card>

    <CardContent className="p-6">

      <div className="flex items-center justify-between">

        <div>

          <p className="text-sm text-muted-foreground">

            Wallet Tokens

          </p>

          <h2 className="mt-2 text-4xl font-bold">

            {dashboard?.wallet.availableTokens ?? 0}

          </h2>

        </div>

        <div className="rounded-xl bg-primary/10 p-3">

          <Wallet className="h-7 w-7 text-primary" />

        </div>

      </div>

    </CardContent>

  </Card>



  <Card>

    <CardContent className="p-6">

      <div className="flex items-center justify-between">

        <div>

          <p className="text-sm text-muted-foreground">

            Agreements

          </p>

          <h2 className="mt-2 text-4xl font-bold">

            {

              (dashboard?.requesterAgreements.length ?? 0) +

              (dashboard?.providerAgreements.length ?? 0)

            }

          </h2>

        </div>

        <div className="rounded-xl bg-primary/10 p-3">

          <Handshake className="h-7 w-7 text-primary" />

        </div>

      </div>

    </CardContent>

  </Card>



  <Card>

    <CardContent className="p-6">

      <div className="flex items-center justify-between">

        <div>

          <p className="text-sm text-muted-foreground">

            Active Sessions

          </p>

          <h2 className="mt-2 text-4xl font-bold">

            {

              (dashboard?.requesterSessions.length ?? 0) +

              (dashboard?.providerSessions.length ?? 0)

            }

          </h2>

        </div>

        <div className="rounded-xl bg-primary/10 p-3">

          <CalendarClock className="h-7 w-7 text-primary" />

        </div>

      </div>

    </CardContent>

  </Card>

</div>






    {/* Skills */}

    {/* Skills & Wallet */}

<div className="grid gap-6 lg:grid-cols-3">

  {/* My Skills */}

  <Card className="lg:col-span-2">

    <CardContent className="p-6">

      <div className="mb-6 flex items-center justify-between">

        <div>

          <h2 className="text-xl font-bold">

            My Skills

          </h2>

          <p className="text-sm text-muted-foreground">

            Skills you can offer to others

          </p>

        </div>

        <Wrench className="h-6 w-6 text-primary" />

      </div>

      {
        dashboard?.skills.length === 0 ? (

          <div className="py-10 text-center text-muted-foreground">

            No skills added yet.

          </div>

        ) : (

          <div className="grid gap-4 sm:grid-cols-2">

            {dashboard?.skills.map((skill) => (

              <div
                key={skill.id}
                className="
                  rounded-xl
                  border
                  p-4
                  transition-all
                  hover:border-primary
                  hover:shadow-md
                "
              >

                <h3 className="font-semibold">

                  {skill.skillName}

                </h3>

                <p className="mt-1 text-sm text-muted-foreground">

                  {skill.category}

                </p>

                <div className="mt-4">

                  <span
                    className="
                      rounded-full
                      bg-primary/10
                      px-3
                      py-1
                      text-xs
                      font-medium
                      text-primary
                    "
                  >
                    {skill.proficiencyLevel}
                  </span>

                </div>

              </div>

            ))}

          </div>

        )

      }

    </CardContent>

  </Card>



  {/* Wallet */}

  <Card>

    <CardContent className="p-6">

      <div className="mb-6 flex items-center justify-between">

        <div>

          <h2 className="text-xl font-bold">

            Wallet

          </h2>

          <p className="text-sm text-muted-foreground">

            Token Summary

          </p>

        </div>

        <Wallet className="h-6 w-6 text-primary" />

      </div>



      <div className="space-y-5">

        <div className="flex items-center justify-between">

          <span>

            Available

          </span>

          <span className="font-bold text-primary">

            {dashboard?.walletSummary.availableTokens}

          </span>

        </div>



        <div className="flex items-center justify-between">

          <span>

            Locked

          </span>

          <span className="font-bold">

            {dashboard?.walletSummary.lockedTokens}

          </span>

        </div>



        <div className="border-t pt-4">

          <div className="flex items-center justify-between">

            <span className="font-medium">

              Total Tokens

            </span>

            <span className="text-2xl font-bold">

              {dashboard?.walletSummary.totalTokens}

            </span>

          </div>

        </div>

      </div>

    </CardContent>

  </Card>

</div>






    {/* Marketplace */}

<Card>

  <CardContent className="p-6">

    <div className="mb-6 flex items-center justify-between">

      <div>

        <h2 className="text-xl font-bold">

          Marketplace

        </h2>

        <p className="text-sm text-muted-foreground">

          Your recent service requests

        </p>

      </div>

      <Button
        variant="outline"
        onClick={() => navigate("/dashboard/marketplace")}
      >
        View All
      </Button>

    </div>

    {
      dashboard?.marketplaceRequests.length === 0 ? (

        <div className="py-10 text-center text-muted-foreground">

          No marketplace requests yet.

        </div>

      ) : (

        <div className="space-y-4">

          {
            [...(dashboard?.marketplaceRequests ?? [])]
              .sort(
                (a, b) =>
                  new Date(b.createdAt).getTime() -
                  new Date(a.createdAt).getTime()
              )
              .slice(0, 5)
              .map((request) => (

                <div
                  key={request.requestId}
                  className="
                    flex
                    items-center
                    justify-between
                    rounded-xl
                    border
                    p-4
                    transition-all
                    hover:border-primary
                    hover:shadow-md
                  "
                >

                  <div>

                    <h3 className="font-semibold">

                      {request.title}

                    </h3>

                    <p className="mt-1 text-sm text-muted-foreground">
                      Mode: {request.exchangeMode.replace(/_/g, " ")}
                    </p>

                  <div className="mt-1 flex items-center gap-4 text-sm text-muted-foreground">

                    <span>
                      {request.estimatedHours} hrs
                    </span>

                    <span>
                      •
                    </span>

                    <span>
                      {request.exchangeMode === "DIRECT_BARTER"
  ? "Direct barter"
  : request.tokenAmount != null
    ? `${request.tokenAmount} Tokens`
    : "Token amount not set"} Tokens
                    </span>

                  </div>

                  </div>

                 <span
                    className={`
                      rounded-full
                      px-3
                      py-1
                      text-xs
                      font-semibold

                      ${
                        request.status === "OPEN"
                          ? "bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300"

                        : request.status === "ACCEPTED"
                          ? "bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300"

                        : request.status === "CANCELLED"
                          ? "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300"

                        : "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300"
                      }
                    `}
                  >

                    {request.status.replace("_", " ")}

                  </span>

                </div>

              ))
          }

        </div>

      )

    }

  </CardContent>

</Card>






    {/* Sessions & Agreements */}

<div className="grid gap-6 lg:grid-cols-2">

  {/* Sessions */}

  <Card>

    <CardContent className="p-6">

      <div className="mb-6 flex items-center justify-between">

        <div>

          <h2 className="text-xl font-bold">

            Upcoming Sessions

          </h2>

          <p className="text-sm text-muted-foreground">

            Your latest learning sessions

          </p>

        </div>

        <CalendarClock className="h-6 w-6 text-primary" />

      </div>

      {

        (
          (dashboard?.providerSessions.length ?? 0) +
          (dashboard?.requesterSessions.length ?? 0)
        ) === 0 ? (

          <div className="py-10 text-center text-muted-foreground">

            No sessions available.

          </div>

        ) : (



[
  ...(dashboard?.providerSessions ?? []),
  ...(dashboard?.requesterSessions ?? []),
]
.sort(
  (a, b) =>
    new Date(b.createdAt).getTime() -
    new Date(a.createdAt).getTime()
)
.slice(0, 4)
.map((session) => (

              <div
                key={session.sessionId}
                className="
                  mb-4
                  rounded-xl
                  border
                  p-4
                  transition-all
                  hover:border-primary
                  hover:shadow-md
                "
              >

                <div className="flex items-center justify-between">

                  <div>

                    <h3 className="font-semibold">

                      {session.providerName}

                    </h3>

                    <p className="text-sm text-muted-foreground">

                      Agreement #{session.agreementId.slice(0,8)}

                    </p>

                  </div>

                  <span
                    className="
                      rounded-full
                      bg-primary/10
                      px-3
                      py-1
                      text-xs
                      font-medium
                      text-primary
                    "
                  >

                    {session.status.replace("_"," ")}

                  </span>

                </div>

              </div>

            ))

        )

      }

    </CardContent>

  </Card>






      {/* Agreements */}

  <Card>

    <CardContent className="p-6">

      <div className="mb-6 flex items-center justify-between">

        <div>

          <h2 className="text-xl font-bold">

            Recent Agreements

          </h2>

          <p className="text-sm text-muted-foreground">

            Recently accepted agreements

          </p>

        </div>

        <Handshake className="h-6 w-6 text-primary" />

      </div>

      {

        (
          (dashboard?.providerAgreements.length ?? 0) +
          (dashboard?.requesterAgreements.length ?? 0)
        ) === 0 ? (

          <div className="py-10 text-center text-muted-foreground">

            No agreements yet.

          </div>

        ) : (

          [...(dashboard?.providerAgreements ?? []), ...(dashboard?.requesterAgreements ?? [])]

            .sort((a, b) => {
  const aTime = a.acceptedAt
    ? Date.parse(a.acceptedAt)
    : NaN;

  const bTime = b.acceptedAt
    ? Date.parse(b.acceptedAt)
    : NaN;

  const aValid = Number.isFinite(aTime);
  const bValid = Number.isFinite(bTime);

  // Keep missing or invalid dates at the end.
  if (!aValid && !bValid) return 0;
  if (!aValid) return 1;
  if (!bValid) return -1;

  // Newest accepted agreements first.
  return bTime - aTime;
})

            .slice(0,4)

            .map((agreement)=>(

              <div
                key={agreement.agreementId}
                className="
                  mb-4
                  rounded-xl
                  border
                  p-4
                  transition-all
                  hover:border-primary
                  hover:shadow-md
                "
              >

                <div className="flex items-center justify-between">

                  <div>

                    <h3 className="font-semibold">

                      {agreement.providerName}

                    </h3>

                    <p className="text-sm text-muted-foreground">

                      {agreement.escrowLockedTokens} Tokens Locked

                    </p>

                  </div>

                  <span
                    className="
                      rounded-full
                      bg-green-100
                      px-3
                      py-1
                      text-xs
                      font-medium
                      text-green-700
                      dark:bg-green-900/40
                      dark:text-green-300
                    "
                  >

                    {agreement.status}

                  </span>

                </div>

              </div>

            ))

        )

      }

    </CardContent>

  </Card>

</div>






  </div>

);

}

export default Userhome;


