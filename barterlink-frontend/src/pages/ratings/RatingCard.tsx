import {
  Star,
  Calendar,
  User,
} from "lucide-react";

import type Rating from "@/models/ratings/Rating";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

interface RatingCardProps {

  rating: Rating;

}

function RatingCard({

  rating,

}: RatingCardProps) {

  const stars = Array.from(

    { length: 5 },

    (_, index) => index + 1

  );

    return (

    <Card className="transition-all hover:-translate-y-1 hover:shadow-lg">

      <CardContent className="space-y-5 p-6">

        {/* Header */}

        <div className="flex items-start justify-between">

          <div className="space-y-2">

            <div className="flex items-center gap-2">

              <User
                className="text-primary"
                size={20}
              />

              <h3 className="text-lg font-semibold">

                {rating.reviewerName}

              </h3>

            </div>

            <div className="flex items-center gap-1">

              {stars.map((star) => (

                <Star
                  key={star}
                  size={18}
                  className={
                    star <= rating.stars
                      ? "fill-yellow-400 text-yellow-400"
                      : "text-muted-foreground"
                  }
                />

              ))}

            </div>

          </div>

        </div>

        {/* Review */}

        <div>

          <p className="text-sm leading-relaxed text-muted-foreground">

            {rating.review
              ? rating.review
              : "No written review provided."}

          </p>

        </div>

        {/* Footer */}

        <div className="flex items-center justify-between border-t pt-4">

          <div className="flex items-center gap-2 text-xs text-muted-foreground">

            <Calendar size={16} />

            <span>

              {new Date(
                rating.createdAt
              ).toLocaleDateString()}

            </span>

          </div>

          <span className="text-sm font-semibold">

            {rating.stars} / 5

          </span>

        </div>

      </CardContent>

    </Card>

  );

}

export default RatingCard;