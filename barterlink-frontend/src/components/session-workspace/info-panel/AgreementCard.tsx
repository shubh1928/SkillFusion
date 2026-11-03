import type Agreement from "@/models/agreement/Agreement";

import InfoCard from "./InfoCard";

interface AgreementCardProps {
    agreement: Agreement | null;
}

function AgreementCard({
    agreement,
}: AgreementCardProps) {

    return (

        <InfoCard title="Agreement">

            <div className="space-y-4">

                <div>
                    <p className="text-xs text-muted-foreground">
                        Offer
                    </p>

                    <p className="font-medium">
                        {agreement?.offerTitle}
                    </p>
                </div>

                <div>
                    <p className="text-xs text-muted-foreground">
                        Request
                    </p>

                    <p className="font-medium">
                        {agreement?.requestTitle}
                    </p>
                </div>

                <div className="grid grid-cols-2 gap-4">

                    <div>

                        <p className="text-xs text-muted-foreground">
                            Exchange
                        </p>

                        <p className="font-medium">
                            {agreement?.exchangeMode?.replaceAll("_", " ")}
                        </p>

                    </div>

                    <div>

                        <p className="text-xs text-muted-foreground">
                            Hours
                        </p>

                        <p className="font-medium">
                            {agreement?.estimatedHours}
                        </p>

                    </div>

                </div>

                <div>

                    <p className="text-xs text-muted-foreground">
                        Tokens
                    </p>

                    <p className="font-medium">
                        {agreement?.agreedTokenAmount}
                    </p>

                </div>

            </div>

        </InfoCard>

    );

}

export default AgreementCard;