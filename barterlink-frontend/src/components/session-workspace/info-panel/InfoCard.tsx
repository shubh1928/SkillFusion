import type { ReactNode } from "react";

interface InfoCardProps {
    title?: string;
    children: ReactNode;
}

function InfoCard({
    title,
    children,
}: InfoCardProps) {
    return (
        <div className="rounded-xl border p-4">
            {title && (
                <h3 className="mb-4 font-semibold">
                    {title}
                </h3>
            )}

            {children}
        </div>
    );
}

export default InfoCard;