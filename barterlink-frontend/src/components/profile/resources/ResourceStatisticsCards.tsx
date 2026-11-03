import {
  BookOpen,
  Globe,
  Lock,
  Download,
} from "lucide-react";

import type ResourceStatistics
from "@/models/resources/ResourceStatistics";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

interface ResourceStatisticsCardsProps {

  statistics: ResourceStatistics;

}

function ResourceStatisticsCards({

  statistics,

}: ResourceStatisticsCardsProps) {

  const cards = [

    {

      title: "Total Resources",

      value: statistics.totalResources,

      icon: BookOpen,

      color: "text-blue-600",

    },

    {

      title: "Public",

      value: statistics.publicResources,

      icon: Globe,

      color: "text-green-600",

    },

    {

      title: "Private",

      value: statistics.privateResources,

      icon: Lock,

      color: "text-amber-600",

    },

    {

      title: "Downloads",

      value: statistics.totalDownloads,

      icon: Download,

      color: "text-violet-600",

    },

  ];

  return (

    <div
      className="
      grid
      gap-4
      md:grid-cols-2
      xl:grid-cols-4
      "
    >

      {cards.map((card) => {

        const Icon = card.icon;

        return (

          <Card
            key={card.title}
            className="
            transition-all
            hover:-translate-y-1
            hover:shadow-lg
            "
          >

            <CardContent className="p-6">

              <div
                className="
                flex
                items-center
                justify-between
                "
              >

                <div>

                  <p
                    className="
                    text-sm
                    text-muted-foreground
                    "
                  >

                    {card.title}

                  </p>

                  <h2
                    className="
                    mt-2
                    text-3xl
                    font-bold
                    "
                  >

                    {card.value}

                  </h2>

                </div>

                <div
                  className="
                  rounded-full
                  bg-muted
                  p-3
                  "
                >

                  <Icon
                    className={card.color}
                    size={24}
                  />

                </div>

              </div>

            </CardContent>

          </Card>

        );

      })}

    </div>

  );

}

export default ResourceStatisticsCards;