import toast from "react-hot-toast";
import { useEffect, useMemo, useState } from "react";

import {
  RefreshCw,
  CalendarClock,
} from "lucide-react";

import {
  getRequesterSessions,
  getProviderSessions,
} from "@/services/SessionService";

import type Session from "@/models/sessions/Session";

import SessionCard from "./SessionCard";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

type SessionTab = "requester" | "provider";

function SessionsPage() {
  const [activeTab, setActiveTab] =
    useState<SessionTab>("requester");

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const [requesterSessions, setRequesterSessions] =
    useState<Session[]>([]);

  const [providerSessions, setProviderSessions] =
    useState<Session[]>([]);

  const loadSessions = async () => {
    try {
      setLoading(true);

      const [requester, provider] = await Promise.all([
        getRequesterSessions(),
        getProviderSessions(),
      ]);

      setRequesterSessions(requester);
      setProviderSessions(provider);
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Failed to load sessions"
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSessions();
  }, []);

  const handleRefresh = async () => {
    try {
      setRefreshing(true);

      await loadSessions();

      toast.success("Sessions refreshed");
    } finally {
      setRefreshing(false);
    }
  };

  const sessions = useMemo(() => {
    switch (activeTab) {
      case "provider":
        return providerSessions;

      default:
        return requesterSessions;
    }
  }, [
    activeTab,
    requesterSessions,
    providerSessions,
  ]);

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <CalendarClock
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">
              Sessions
            </h1>
          </div>

          <p className="mt-2 text-muted-foreground">
            Manage your ongoing service sessions.
          </p>
        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
        >
          {refreshing ? (
            <Spinner />
          ) : (
            <RefreshCw size={18} />
          )}

          Refresh
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-3">
        <Button
          variant={
            activeTab === "requester"
              ? "default"
              : "outline"
          }
          onClick={() => setActiveTab("requester")}
        >
          My Sessions

          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
            {requesterSessions.length}
          </span>
        </Button>

        <Button
          variant={
            activeTab === "provider"
              ? "default"
              : "outline"
          }
          onClick={() => setActiveTab("provider")}
        >
          Providing

          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
            {providerSessions.length}
          </span>
        </Button>
      </div>

      {/* Loading */}
      {loading ? (
        <Card>
          <CardContent className="flex h-72 items-center justify-center">
            <div className="flex flex-col items-center gap-4">
              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">
                Loading sessions...
              </p>
            </div>
          </CardContent>
        </Card>
      ) : sessions.length === 0 ? (
        <Card>
          <CardContent className="flex h-72 flex-col items-center justify-center text-center">
            <CalendarClock
              className="mb-4 text-muted-foreground"
              size={60}
            />

            <h2 className="text-2xl font-semibold">
              No Sessions Found
            </h2>

            <p className="mt-2 max-w-md text-muted-foreground">
              There are currently no sessions
              available in this section.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6">
          {sessions.map((session) => (
            <SessionCard
              key={session.sessionId}
              session={session}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default SessionsPage;