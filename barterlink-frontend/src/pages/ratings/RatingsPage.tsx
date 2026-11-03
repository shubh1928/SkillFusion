import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import useAuth from "@/auth/store";

import {
  RefreshCw,
  Star,
} from "lucide-react";

import {
  getUserRatings,
  getUserReputation,
} from "@/services/RatingService";

import type Rating from "@/models/ratings/Rating";

import RatingCard from "./RatingCard";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

function RatingsPage() {

  const user =
    useAuth((state) => state.user);

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [ratings, setRatings] =
    useState<Rating[]>([]);

  const [reputation, setReputation] =
    useState(0);

      const loadRatings = async () => {

    if (!user?.id) {

      setLoading(false);

      return;

    }

    try {

      setLoading(true);

        const [

          userRatings,

          userReputation,

        ] = await Promise.all([

          getUserRatings(user.id),

          getUserReputation(user.id),

        ]);

      setRatings(userRatings);

      setReputation(userReputation);

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Failed to load ratings"

      );

    } finally {

      setLoading(false);

    }

  };

useEffect(() => {

  if (user?.id) {

    loadRatings();

  }

}, [user?.id]);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadRatings();

      toast.success(

        "Ratings refreshed"

      );

    } finally {

      setRefreshing(false);

    }

  };

    return (

    <div className="space-y-8">

      {/* Header */}

      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">

        <div>

          <div className="flex items-center gap-3">

            <Star
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">

              Ratings

            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            View your reputation and customer reviews.

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

          <CardContent className="flex h-72 items-center justify-center">

            <div className="flex flex-col items-center gap-4">

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">

                Loading ratings...

              </p>

            </div>

          </CardContent>

        </Card>

      ) : (

        <>

          {/* Reputation */}

          <Card>

            <CardContent className="flex flex-col items-center gap-4 p-8 text-center">

              <Star
                className="fill-yellow-400 text-yellow-400"
                size={56}
              />

              <h2 className="text-4xl font-bold">

                {reputation.toFixed(1)}

              </h2>

              <p className="text-muted-foreground">

                Average Rating

              </p>

              <p className="text-sm text-muted-foreground">

                Based on {ratings.length} Review
                {ratings.length !== 1 ? "s" : ""}

              </p>

            </CardContent>

          </Card>

          {/* Reviews */}

          <div className="space-y-6">

            <div className="flex items-center justify-between">

              <h2 className="text-2xl font-semibold">

                Customer Reviews

              </h2>

              <span className="text-sm text-muted-foreground">

                {ratings.length} Review
                {ratings.length !== 1 ? "s" : ""}

              </span>

            </div>


                        {ratings.length === 0 ? (

              <Card>

                <CardContent className="flex h-56 flex-col items-center justify-center text-center">

                  <Star
                    className="mb-4 text-muted-foreground"
                    size={60}
                  />

                  <h3 className="text-xl font-semibold">

                    No Ratings Yet

                  </h3>

                  <p className="mt-2 max-w-md text-muted-foreground">

                    You haven't received any ratings yet.
                    Completed services will appear here once
                    customers submit reviews.

                  </p>

                </CardContent>

              </Card>

            ) : (

              <div className="grid gap-6">

                {ratings.map((rating) => (

                  <RatingCard
                    key={rating.ratingId}
                    rating={rating}
                  />

                ))}

              </div>

            )}

          </div>

        </>

      )}

    </div>

  );

}

export default RatingsPage;