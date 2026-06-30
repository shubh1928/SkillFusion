import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import useAuth from "@/auth/store";

import {
  Plus,
  RefreshCw,
  BriefcaseBusiness,
} from "lucide-react";

import {
  getOpenRequests,
  getMyRequests,
  getAssignedRequests,
  acceptRequest,
  cancelRequest,
} from "@/services/MarketplaceService";

import type ServiceRequest from "@/models/marketplace/ServiceRequest";

import MarketplaceCard from "./MarketplaceCard";
import CreateRequestDialog from "./CreateRequestDialog";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import {
  Card,
  CardContent,
} from "@/components/ui/card";

type MarketplaceTab =
  | "open"
  | "my"
  | "assigned";

function MarketplacePage() {

    const user = useAuth((state) => state.user);

  const [activeTab, setActiveTab] =
    useState<MarketplaceTab>("open");

  const [loading, setLoading] =
    useState(true);

  const [dialogOpen, setDialogOpen] =
    useState(false);

  const [refreshing, setRefreshing] =
    useState(false);

  const [openRequests, setOpenRequests] =
    useState<ServiceRequest[]>([]);

  const [myRequests, setMyRequests] =
    useState<ServiceRequest[]>([]);

  const [assignedRequests, setAssignedRequests] =
    useState<ServiceRequest[]>([]);

  const loadMarketplace = async () => {

    try {

      setLoading(true);

      const [
        open,
        mine,
        assigned,
      ] = await Promise.all([

        getOpenRequests(),
        getMyRequests(),
        getAssignedRequests(),

      ]);

      setOpenRequests(open);
      setMyRequests(mine);
      setAssignedRequests(assigned);

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
          "Failed to load marketplace"
      );

    } finally {

      setLoading(false);

    }

  };

  useEffect(() => {

    loadMarketplace();

  }, []);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadMarketplace();

      toast.success(
        "Marketplace refreshed"
      );

    } finally {

      setRefreshing(false);

    }

  };

  const handleAccept = async (
    requestId: string
  ) => {

    try {

      await acceptRequest(requestId);

      toast.success(
        "Request accepted successfully"
      );

      await loadMarketplace();

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
          "Unable to accept request"
      );

    }

  };

  const handleCancel = async (
    requestId: string
  ) => {

    try {

      await cancelRequest(requestId);

      toast.success(
        "Request cancelled successfully"
      );

      await loadMarketplace();

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
          "Unable to cancel request"
      );

    }

  };

  const requests = useMemo(() => {

    switch (activeTab) {

      case "my":
        return myRequests;

      case "assigned":
        return assignedRequests;

      default:
        return openRequests;

    }

  }, [
    activeTab,
    openRequests,
    myRequests,
    assignedRequests,
  ]);

  return (

    <div className="space-y-8">

      {/* Header */}

      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">

        <div>

          <div className="flex items-center gap-3">

            <BriefcaseBusiness
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">
              Marketplace
            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            Browse community requests or
            create your own service request.

          </p>

        </div>

        <div className="flex gap-3">

          <Button
            variant="outline"
            onClick={handleRefresh}
            disabled={refreshing}
          >

            {
              refreshing
              ? <Spinner />
              : <RefreshCw size={18}/>
            }

            Refresh

          </Button>

          <Button
            onClick={() =>
              setDialogOpen(true)
            }
          >

            <Plus
              size={18}
              className="mr-2"
            />

            Create Request

          </Button>

        </div>

      </div>

      <CreateRequestDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onRequestCreated={loadMarketplace}
      />



      {/* Tabs */}

      <div className="flex flex-wrap gap-3">

        <Button
          variant={
            activeTab === "open"
              ? "default"
              : "outline"
          }
          onClick={() =>
            setActiveTab("open")
          }
        >
          Open Requests
          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
            {openRequests.length}
          </span>
        </Button>

        <Button
          variant={
            activeTab === "my"
              ? "default"
              : "outline"
          }
          onClick={() =>
            setActiveTab("my")
          }
        >
          My Requests
          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
            {myRequests.length}
          </span>
        </Button>

        <Button
          variant={
            activeTab === "assigned"
              ? "default"
              : "outline"
          }
          onClick={() =>
            setActiveTab("assigned")
          }
        >
          Assigned
          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
            {assignedRequests.length}
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
                Loading marketplace...
              </p>

            </div>

          </CardContent>

        </Card>

      ) : requests.length === 0 ? (

        <Card>

          <CardContent className="flex h-72 flex-col items-center justify-center text-center">

            <BriefcaseBusiness
              className="mb-4 text-muted-foreground"
              size={60}
            />

            <h2 className="text-2xl font-semibold">
              No Requests Found
            </h2>

            <p className="mt-2 max-w-md text-muted-foreground">

              There are currently no requests
              available in this section.

            </p>

            {activeTab === "open" && (

              <Button
                className="mt-6"
                onClick={() =>
                  setDialogOpen(true)
                }
              >
                <Plus
                  size={18}
                  className="mr-2"
                />

                Create First Request

              </Button>

            )}

          </CardContent>

        </Card>

      ) : (

        <div className="grid gap-6">

          {requests.map((request) => (

            <MarketplaceCard
              key={request.requestId}
              request={request}
                showAcceptButton={
                activeTab === "open" &&
                request.requesterId !== user?.id
                }
              showCancelButton={
                activeTab === "my"
              }
              onAccept={handleAccept}
              onCancel={handleCancel}
            />

          ))}

        </div>

      )}

    </div>

  );
}

export default MarketplacePage;