import type { ReactNode } from "react";

import type TrustAssessment from "@/models/trust/TrustAssessment";

import { useState } from "react";
import { ChevronDown } from "lucide-react";

import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible";

import { Badge } from "@/components/ui/badge";

import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

import type { LucideIcon } from "lucide-react";

interface TrustBadgeConfig {

    text: string;

    className: string;

    icon: LucideIcon;

}

interface TrustAssessmentPopoverProps {

    trustAssessment: TrustAssessment;

    badge: TrustBadgeConfig;

    children: ReactNode;

}

function TrustAssessmentPopover({

    trustAssessment,

    badge,

    children,

}: TrustAssessmentPopoverProps) {

    const [isOpen, setIsOpen] = useState(false);

  return (

    <Popover>

      <PopoverTrigger asChild>

        {children}

      </PopoverTrigger>

      <PopoverContent
            side="top"
            align="center"
            collisionPadding={16}
            className="
                w-[380px]
                max-w-[calc(100vw-2rem)]
                rounded-xl
                p-5
                shadow-xl
            "
        >

        <div className="space-y-4">

<div className="space-y-1">

    <h3 className="flex items-center gap-2 text-base font-semibold">

        <badge.icon className="h-5 w-5" />

        Trust Assessment

    </h3>

    <p className="text-xs text-muted-foreground">

        AI-generated trust evaluation

    </p>

</div>

    <div className="border-t" />

    <div className="space-y-3 text-sm">

        <div className="flex items-center justify-between">

            <span className="text-muted-foreground">

                Risk Level

            </span>

            <div className="flex items-center gap-2">

                <Badge
                    className={badge.className}
                >

                    <badge.icon className="h-3 w-3" />

                    {badge.text}

                </Badge>

            </div>

        </div>

        <div className="flex items-center justify-between">

            <span className="text-muted-foreground">

                Trust Score

            </span>

            <span className="font-medium">

                {trustAssessment.trustScore.toFixed(0)} / 100

            </span>

        </div>

        <div className="flex items-center justify-between">

            <span className="text-muted-foreground">

                Risk Score

            </span>

            <span className="font-medium">

                {(trustAssessment.riskScore * 100).toFixed(0)}%

            </span>

        </div>

    </div>

    <Collapsible
    open={isOpen}
    onOpenChange={setIsOpen}
>

    <CollapsibleTrigger asChild>

        <button
            type="button"
            className="
                flex
                w-full
                items-center
                justify-between
                rounded-lg
                border
                px-3
                py-2
                transition-all
                duration-200
                hover:bg-muted/50
                hover:shadow-sm
                "
        >

            <div className="flex items-center gap-2">

                <span className="font-medium">

                    ✨ AI Explanation

                </span>

                {trustAssessment.reasons.length > 0 && (

                        <Badge variant="secondary">

                            {trustAssessment.reasons.length}

                        </Badge>

                    )}

            </div>

            <ChevronDown
                className={`h-4 w-4 transition-transform duration-200 ${
                    isOpen ? "rotate-180" : ""
                }`}
            />

        </button>

    </CollapsibleTrigger>

    <CollapsibleContent className="mt-3">

    <div
        className="
            max-h-60
            space-y-4
            overflow-y-auto
            scroll-smooth
            pr-2
        "
    >

        {trustAssessment.reasons.length > 0 ? (

    trustAssessment.reasons.map((reason, index) => (

        <div
            key={index}
            className="flex gap-3"
        >

            <div className="mt-2 h-2 w-2 shrink-0 rounded-full bg-primary" />

            <div className="space-y-1">

                <h4 className="text-sm font-medium">
                    {reason.feature}
                </h4>

                <p className="text-sm text-muted-foreground">
                    {reason.message}
                </p>

            </div>

        </div>

    ))

) : (

    <p className="text-sm text-muted-foreground">

        No AI explanations available.

    </p>

)}

    </div>

</CollapsibleContent>

</Collapsible>

<div className="border-t" />

<div className="space-y-1">

    <p className="text-xs text-muted-foreground">

        Last Evaluated

    </p>

    <p className="text-sm font-medium">

        {new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
}).format(new Date(trustAssessment.evaluatedAt))}

    </p>

</div>

</div>

      </PopoverContent>

    </Popover>

  );

}

export default TrustAssessmentPopover;