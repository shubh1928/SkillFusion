import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";

import InterestedProvidersDialog
from "./InterestedProvidersDialog";
import useAuth from "@/auth/store";

import {
  openNegotiation,
} from "@/services/NegotiationService";

import { useNavigate } from "react-router";

import {
  Plus,
  RefreshCw,
  BriefcaseBusiness,
} from "lucide-react";

import {
  getOpenRequests,
  getMyRequests,
  cancelRequest,
} from "@/services/MarketplaceService";

import {
  getRecommendations,
} from "@/services/RecommendationService";

import {
  expressInterest,
} from "@/services/InterestService";

import RequestDetailsDialog from "./RequestDetailsDialog";

import InterestActionDialog from "./InterestActionDialog";
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
  | "recommended";

function MarketplacePage() {

  const navigate = useNavigate();

  const user =
  useAuth((state) => state.user);

  const [activeTab, setActiveTab] =
    useState<MarketplaceTab>("open");

  const [loading, setLoading] =
    useState(true);

  const [dialogOpen, setDialogOpen] =
    useState(false);

  const [selectedRequest, setSelectedRequest] =
    useState<ServiceRequest | null>(null);

  const [detailsOpen, setDetailsOpen] =
    useState(false);
  

  const [refreshing, setRefreshing] =
    useState(false);

  const [openRequests, setOpenRequests] =
    useState<ServiceRequest[]>([]);

  const [myRequests, setMyRequests] =
    useState<ServiceRequest[]>([]);

    const [recommendedRequests, setRecommendedRequests] =
  useState<ServiceRequest[]>([]);

    const [interestDialogOpen, setInterestDialogOpen] =
  useState(false);

  const [selectedInterestRequest, setSelectedInterestRequest] =
    useState<ServiceRequest | null>(null);

    const [interestId, setInterestId] =
     useState<string | null>(null);

  const [providersDialogOpen, setProvidersDialogOpen] =
  useState(false);

const [selectedProvidersRequest, setSelectedProvidersRequest] =
  useState<ServiceRequest | null>(null);

  const loadMarketplace = async () => {

    try {

      setLoading(true);

const [
  open,
  mine,
  recommended,
] = await Promise.all([

  getOpenRequests(),
  getMyRequests(),
  getRecommendations(),

]);

setOpenRequests(open);

setMyRequests(mine);

setRecommendedRequests(recommended);

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

const handleInterested = async (
  request: ServiceRequest
) => {

  try {

    const interest =
      await expressInterest(
        request.requestId
      );

    setInterestId(
      interest.interestId
    );

    setSelectedInterestRequest(
      request
    );

    setInterestDialogOpen(true);

} catch (error: any) {

  const message = error?.response?.data?.message;

  const normalizedMessage =
    typeof message === "string"
      ? message.toLowerCase()
      : "";

  const fundingUnavailable =
    normalizedMessage.includes("insufficient available blt") ||
    normalizedMessage.includes("requester does not have a wallet");

  if (fundingUnavailable) {

    const markIneligible = (
      requests: ServiceRequest[]
    ): ServiceRequest[] =>
      requests.map((item) =>
        item.requestId === request.requestId
          ? {
              ...item,
              requesterHasSufficientTokens: false,
            }
          : item
      );

    setOpenRequests(markIneligible);
    setMyRequests(markIneligible);
    setRecommendedRequests(markIneligible);

    setSelectedRequest((current) =>
      current?.requestId === request.requestId
        ? {
            ...current,
            requesterHasSufficientTokens: false,
          }
        : current
    );

    setSelectedProvidersRequest((current) =>
      current?.requestId === request.requestId
        ? {
            ...current,
            requesterHasSufficientTokens: false,
          }
        : current
    );

  }

  toast.error(
    message ?? "Unable to express interest."
  );

}

};

const handleChat = async () => {

  if (!interestId) {

    toast.error(
      "Interest not found."
    );

    return;

  }

  try {

    const negotiation =
      await openNegotiation(
        interestId
      );

    setInterestDialogOpen(false);

    navigate(
      `/dashboard/negotiations/${negotiation.negotiationId}`
    );

  } catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Unable to start conversation."

    );

  }

};

const handleDirectInterest = () => {

  toast.success(
    "Interest sent successfully."
  );

  setInterestDialogOpen(false);

};

const handleViewDetails = (
  request: ServiceRequest
) => {

  setSelectedRequest(request);

  setDetailsOpen(true);

};


const handleViewInterests = (
  request: ServiceRequest
) => {

  setSelectedProvidersRequest(
    request
  );

  setProvidersDialogOpen(
    true
  );

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


  const handleStartNegotiation = async (
  interestId: string
) => {

  try {

        const negotiation =
          await openNegotiation(
            interestId
          );

        toast.success(
          "Conversation started."
        );

        setProvidersDialogOpen(
          false
        );

        // Later we'll use this ID
        console.log(
          negotiation.negotiationId
        );

        navigate(

          `/dashboard/negotiations/${negotiation.negotiationId}`

        );

  } catch (error: any) {

    toast.error(
      error?.response?.data?.message ??
      "Unable to start conversation."
    );

  }

};

const requests = useMemo(() => {

  switch (activeTab) {

    case "my":
      return myRequests;

    case "recommended":
      return recommendedRequests;

    default:
      return openRequests;

  }

}, [
  activeTab,
  openRequests,
  myRequests,
  recommendedRequests,
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
    activeTab === "recommended"
      ? "default"
      : "outline"
  }
  onClick={() =>
    setActiveTab("recommended")
  }
>
  ⭐ Recommended

  <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">
    {recommendedRequests.length}
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

        <div
  className="
    grid
    grid-cols-1
    md:grid-cols-2
    xl:grid-cols-3
    gap-6
    items-start
  "
>

{requests.map((request) => (

  <MarketplaceCard
    key={request.requestId}
    request={request}
      showInterestedButton={
        activeTab === "open" &&
        request.requesterId !== user?.id
      }
    showCancelButton={
      activeTab === "my"
    }
    onInterested={handleInterested}
    onCancel={handleCancel}
    onViewDetails={handleViewDetails}
    onViewInterests={handleViewInterests}
  />

))}

        </div>
        

      )}

<RequestDetailsDialog
  open={detailsOpen}
  onOpenChange={setDetailsOpen}
  request={selectedRequest}
  onInterested={handleInterested}
/>

<InterestActionDialog
  open={interestDialogOpen}
  onOpenChange={setInterestDialogOpen}
  request={selectedInterestRequest}
  onChat={handleChat}
  onDirectInterest={handleDirectInterest}
/>


<InterestedProvidersDialog
  open={providersDialogOpen}
  onOpenChange={setProvidersDialogOpen}
  request={selectedProvidersRequest}
  onStartNegotiation={handleStartNegotiation}
/>


    </div>

    

  );
}

export default MarketplacePage;