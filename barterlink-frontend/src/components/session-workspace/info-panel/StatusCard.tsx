import InfoCard from "./InfoCard";

interface StatusCardProps {
    status?: string;
}

function StatusCard({
    status,
}: StatusCardProps) {
    return (
        <InfoCard>
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
                Session Status
            </p>

            <p className="mt-2 text-xl font-bold">
                {status?.replaceAll("_", " ")}
            </p>
        </InfoCard>
    );
}

export default StatusCard;