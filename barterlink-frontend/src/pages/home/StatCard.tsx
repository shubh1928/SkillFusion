import { Card, CardContent } from "@/components/ui/card";

interface StatCardProps {

  value: string;

  label: string;

}

function StatCard({

  value,

  label,

}: StatCardProps) {

  return (

    <Card
      className="
        border-border/60
        bg-background/60
        text-center
        backdrop-blur-md
        transition-all
        duration-300
        hover:-translate-y-1
        hover:shadow-lg
      "
    >

      <CardContent className="space-y-3 p-8">

        <h2
          className="
            bg-gradient-to-r
            from-primary
            via-cyan-500
            to-purple-500
            bg-clip-text
            text-4xl
            font-bold
            text-transparent
          "
        >

          {value}

        </h2>

        <p className="text-muted-foreground">

          {label}

        </p>

      </CardContent>

    </Card>

  );

}

export default StatCard;