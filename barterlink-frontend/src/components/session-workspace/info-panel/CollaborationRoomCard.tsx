import type CollaborationRoom
    from "@/models/collaboration/CollaborationRoom";

import InfoCard from "./InfoCard";

interface CollaborationRoomCardProps {
    collaborationRoom: CollaborationRoom | null;
}

function CollaborationRoomCard({
    collaborationRoom,
}: CollaborationRoomCardProps) {
    return (
        <InfoCard title="Collaboration Room">

            <p className="font-mono text-sm">
                {collaborationRoom?.roomId ?? "-"}
            </p>

            <p className="mt-2 text-sm">
                Status: {collaborationRoom?.status ?? "-"}
            </p>

        </InfoCard>
    );
}

export default CollaborationRoomCard;