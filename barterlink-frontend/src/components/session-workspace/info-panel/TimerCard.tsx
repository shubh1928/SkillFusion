import InfoCard from "./InfoCard";

interface TimerCardProps {
    elapsedTime: string;
    completed: boolean;
}

function TimerCard({
    elapsedTime,
    completed,
}: TimerCardProps) {
    return (
        <InfoCard>

            <p className="text-sm text-muted-foreground">
                {completed
                    ? "Total Time"
                    : "Elapsed Time"}
            </p>

            <p className="mt-2 text-4xl font-bold text-primary">
                {elapsedTime}
            </p>

        </InfoCard>
    );
}

export default TimerCard;