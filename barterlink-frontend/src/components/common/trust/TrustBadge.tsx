import type TrustAssessment
from "@/models/trust/TrustAssessment";

import { Badge } from "@/components/ui/badge";

import {
  ShieldCheck,
  ShieldAlert,
  ShieldX,
  ShieldQuestion,
} from "lucide-react";

import TrustAssessmentPopover
from "@/components/common/trust/TrustAssessmentPopover";

interface TrustBadgeProps {

  trustAssessment: TrustAssessment | null;

}

function TrustBadge({

  trustAssessment,

}: TrustBadgeProps) {


  if (!trustAssessment) {

    return (

      <Badge
        variant="outline"
        className="border-slate-300 text-slate-600"
      >
        ⚪ Not Yet Evaluated
      </Badge>

    );

  }

let badgeText = "";
let badgeClass = "";
let BadgeIcon = ShieldQuestion;

  switch (trustAssessment.riskLevel) {

    case "LOW":

      badgeText = "Trusted";

        badgeClass =
        "bg-emerald-100 text-emerald-700";

        BadgeIcon = ShieldCheck;


      break;

    case "MEDIUM":

      badgeText = "Caution";

        badgeClass =
        "bg-amber-100 text-amber-700";

        BadgeIcon = ShieldAlert;

      break;

    case "HIGH":

      badgeText = "High Risk";

        badgeClass =
        "bg-red-100 text-red-700";

        BadgeIcon = ShieldX;

      break;

    default:

      badgeText = "Unknown";

      badgeClass = "";

  }

  const badge = {

    text: badgeText,

    className: badgeClass,

    icon: BadgeIcon,

};

 return (

    <TrustAssessmentPopover
        trustAssessment={trustAssessment}
        badge={badge}
    >

        <Badge className={badgeClass}>

            <BadgeIcon
                className="mr-1 h-3.5 w-3.5"
            />

            {badgeText}

        </Badge>

    </TrustAssessmentPopover>

);

}

export default TrustBadge;