import type { ReactNode } from "react";

import { Card, CardContent } from "@/components/ui/card";

interface FeatureCardProps {

  icon: ReactNode;

  title: string;

  description: string;

}

function FeatureCard({

  icon,

  title,

  description,

}: FeatureCardProps) {

  return (

    <Card
      className="
        group
        border-border/60
        bg-background/60
        backdrop-blur-md
        transition-all
        duration-300
        hover:-translate-y-2
        hover:border-primary/40
        hover:shadow-xl
      "
    >

      <CardContent className="space-y-5 p-7">

        <div
          className="
            flex
            h-14
            w-14
            items-center
            justify-center
            rounded-2xl
            bg-primary/10
            text-primary
            transition-all
            duration-300
            group-hover:scale-110
          "
        >

          {icon}

        </div>

        <div className="space-y-2">

          <h3 className="text-xl font-semibold">

            {title}

          </h3>

          <p className="leading-relaxed text-muted-foreground">

            {description}

          </p>

        </div>

      </CardContent>

    </Card>

  );

}

export default FeatureCard;