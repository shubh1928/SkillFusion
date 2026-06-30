import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";

import {
  FileText,
  RefreshCw,
} from "lucide-react";

import type Agreement from "@/models/agreement/Agreement";

import {
  getRequesterAgreements,
  getProviderAgreements,
} from "@/services/AgreementService";

import AgreementCard from "./AgreementCard";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import {
  Card,
  CardContent,
} from "@/components/ui/card";

type AgreementTab =
  | "requester"
  | "provider";

function AgreementsPage() {

  const [activeTab, setActiveTab] =
    useState<AgreementTab>("requester");

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [requesterAgreements, setRequesterAgreements] =
    useState<Agreement[]>([]);

  const [providerAgreements, setProviderAgreements] =
    useState<Agreement[]>([]);

  const loadAgreements = async () => {

    try {

      setLoading(true);

      const [
        requester,
        provider,
      ] = await Promise.all([

        getRequesterAgreements(),
        getProviderAgreements(),

      ]);

      setRequesterAgreements(requester);
      setProviderAgreements(provider);

    } catch (error: any) {

      toast.error(
        error?.response?.data?.message ??
        "Failed to load agreements"
      );

    } finally {

      setLoading(false);

    }

  };

  useEffect(() => {

    loadAgreements();

  }, []);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadAgreements();

      toast.success(
        "Agreements refreshed"
      );

    } finally {

      setRefreshing(false);

    }

  };

  const agreements = useMemo(() => {

    return activeTab === "requester"
      ? requesterAgreements
      : providerAgreements;

  }, [
    activeTab,
    requesterAgreements,
    providerAgreements,
  ]);

  return (

    <div className="space-y-8">

      {/* Header */}

      <div className="flex items-center justify-between">

        <div>

          <div className="flex items-center gap-3">

            <FileText
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">

              Agreements

            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            View agreements you've created
            or accepted.

          </p>

        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
        >

          {
            refreshing
            ? <Spinner />
            : <RefreshCw size={18}/>
          }

          Refresh

        </Button>

      </div>

      {/* Tabs */}

      <div className="flex gap-3">

        <Button
          variant={
            activeTab === "requester"
              ? "default"
              : "outline"
          }
          onClick={() =>
            setActiveTab("requester")
          }
        >

          Requester

          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">

            {requesterAgreements.length}

          </span>

        </Button>

        <Button
          variant={
            activeTab === "provider"
              ? "default"
              : "outline"
          }
          onClick={() =>
            setActiveTab("provider")
          }
        >

          Provider

          <span className="ml-2 rounded-full bg-background/20 px-2 py-0.5 text-xs">

            {providerAgreements.length}

          </span>

        </Button>

      </div>

      {/* Loading */}

      {loading ? (

        <Card>

          <CardContent className="flex h-72 items-center justify-center">

            <div className="flex flex-col items-center gap-4">

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">

                Loading agreements...

              </p>

            </div>

          </CardContent>

        </Card>

      ) : agreements.length === 0 ? (

        <Card>

          <CardContent className="flex h-72 flex-col items-center justify-center text-center">

            <FileText
              size={60}
              className="mb-4 text-muted-foreground"
            />

            <h2 className="text-2xl font-semibold">

              No Agreements Found

            </h2>

            <p className="mt-2 text-muted-foreground">

              No agreements are available
              in this section.

            </p>

          </CardContent>

        </Card>

      ) : (

        <div className="grid gap-6">

          {agreements.map((agreement) => (

            <AgreementCard
              key={agreement.agreementId}
              agreement={agreement}
            />

          ))}

        </div>

      )}

    </div>

  );

}

export default AgreementsPage;