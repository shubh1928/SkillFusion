import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

import { Button } from "@/components/ui/button";


import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

import {
  User,
  CalendarDays,
  MessageCircle,
} from "lucide-react";

import { useEffect, useState } from "react";

import toast from "react-hot-toast";

import type Interest
from "@/models/interest/Interest";

import {
  getRequestInterests,
} from "@/services/InterestService";

import { Spinner }
from "@/components/ui/spinner";

import type ServiceRequest from "@/models/marketplace/ServiceRequest";



interface InterestedProvidersDialogProps {

  open: boolean;

  onOpenChange: (
    open: boolean
  ) => void;

  request: ServiceRequest | null;

  onStartNegotiation: (
    interestId: string
  ) => void;

}

function InterestedProvidersDialog({

  open,

  onOpenChange,

  request,

  onStartNegotiation,

}: InterestedProvidersDialogProps) {


const [loading, setLoading] =
  useState(false);

const [interests, setInterests] =
  useState<Interest[]>([]);


  useEffect(() => {

  if (
    !open ||
    !request
  ) {

    setInterests([]);

    return;

  }

  const loadInterests =
    async () => {

      try {

        setLoading(true);

        const response =
          await getRequestInterests(
            request.requestId
          );

        setInterests(
          response
        );

      } catch (error: any) {

        toast.error(

          error?.response?.data?.message ??

          "Failed to load interested providers."

        );

      } finally {

        setLoading(false);

      }

    };

  loadInterests();

}, [
  open,
  request,
]);

  return (

    <Dialog
      open={open}
      onOpenChange={onOpenChange}
    >

      <DialogContent className="sm:max-w-3xl">

        <DialogHeader>

          <DialogTitle>

            Interested Providers

          </DialogTitle>

          <DialogDescription>

            Providers interested in

            <span className="font-medium">

              {" "}
              {request?.title}

            </span>

          </DialogDescription>

        </DialogHeader>

<div className="space-y-4">

  {loading ? (

    <div className="flex justify-center py-10">

      <Spinner />

    </div>

  ) : interests.length === 0 ? (

    <div className="py-8 text-center text-muted-foreground">

      No providers have shown interest yet.

    </div>

  ) : (

    interests.map((interest) => (

      <Card
        key={interest.interestId}
      >

        <CardContent className="flex items-center justify-between gap-6 p-5">

          <div className="flex items-center gap-4">

           {interest.providerImage ? (

  <img
    src={interest.providerImage}
    alt={interest.providerName}
    className="
      h-12
      w-12
      rounded-full
      object-cover
      border
    "
  />

) : (

  <div
    className="
      flex
      h-12
      w-12
      items-center
      justify-center
      rounded-full
      bg-primary
      text-primary-foreground
      font-semibold
    "
  >

    {interest.providerName.charAt(0).toUpperCase()}

  </div>

)}

            <div>

              <div className="flex items-center gap-2">

                <User
                  size={16}
                />

                <p className="font-semibold">

                  {interest.providerName}

                </p>

              </div>

              <div className="mt-2 flex items-center gap-2">

                <CalendarDays
                  size={15}
                />

                <span className="text-sm text-muted-foreground">

                  {new Date(
                    interest.createdAt
                  ).toLocaleDateString()}

                </span>

              </div>

            </div>

          </div>

          <div className="flex items-center gap-3">

            <Badge>

              {interest.status}

            </Badge>

            <Button
              onClick={() =>
                onStartNegotiation(
                  interest.interestId
                )
              }
            >

              <MessageCircle
                className="mr-2 h-4 w-4"
              />

              Start Conversation

            </Button>

          </div>

        </CardContent>

      </Card>

    ))

  )}

</div>

      </DialogContent>

    </Dialog>

  );

}

export default InterestedProvidersDialog;