import { useEffect, useState } from "react";

import toast from "react-hot-toast";

import {
  MessageCircleMore,
  RefreshCw,
} from "lucide-react";

import NegotiationCard
from "./NegotiationCard";

import {
  getMyNegotiations,
} from "@/services/NegotiationService";

import type Negotiation
from "@/models/negotiation/Negotiation";

import { Button }
from "@/components/ui/button";

import { Spinner }
from "@/components/ui/spinner";

import { useNavigate } from "react-router";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

function NegotiationsPage() {

  const navigate = useNavigate();

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [negotiations, setNegotiations] =
    useState<Negotiation[]>([]);

  const loadNegotiations = async () => {

    try {

      setLoading(true);

      const response =
        await getMyNegotiations();

      setNegotiations(response);

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Failed to load negotiations"

      );

    } finally {

      setLoading(false);

    }

  };

  useEffect(() => {

    loadNegotiations();

  }, []);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadNegotiations();

      toast.success(
        "Negotiations refreshed"
      );

    } finally {

      setRefreshing(false);

    }

  };


const handleOpenConversation = (
  negotiation: Negotiation
) => {

  navigate(

    `/dashboard/negotiations/${negotiation.negotiationId}`

  );

};


    return (

    <div className="space-y-8">

      {/* Header */}

      <div
        className="
        flex
        flex-col
        gap-5
        md:flex-row
        md:items-center
        md:justify-between
        "
      >

        <div>

          <div className="flex items-center gap-3">

            <MessageCircleMore
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">

              Negotiations

            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            Manage your ongoing conversations,
            proposals and session requests.

          </p>

        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
        >

          {

            refreshing

              ? <Spinner />

              : <RefreshCw size={18} />

          }

          Refresh

        </Button>

      </div>

      {/* Loading */}

      {loading ? (

        <Card>

          <CardContent
            className="
            flex
            h-72
            items-center
            justify-center
            "
          >

            <div
              className="
              flex
              flex-col
              items-center
              gap-4
              "
            >

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">

                Loading negotiations...

              </p>

            </div>

          </CardContent>

        </Card>

      ) : negotiations.length === 0 ? (

        <Card>

          <CardContent
            className="
            flex
            h-72
            flex-col
            items-center
            justify-center
            text-center
            "
          >

            <MessageCircleMore
              className="
              mb-4
              text-muted-foreground
              "
              size={60}
            />

            <h2 className="text-2xl font-semibold">

              No Negotiations Yet

            </h2>

            <p
              className="
              mt-2
              max-w-md
              text-muted-foreground
              "
            >

              When a requester accepts your
              interest, your negotiations will
              appear here.

            </p>

          </CardContent>

        </Card>

      ) : (

            <div className="grid gap-6">

            {negotiations.map((negotiation) => (

                <NegotiationCard
                key={negotiation.negotiationId}
                negotiation={negotiation}
                onOpenConversation={
                    handleOpenConversation
                }
                />

            ))}

            </div>

      )}

    </div>

  );

}

export default NegotiationsPage;