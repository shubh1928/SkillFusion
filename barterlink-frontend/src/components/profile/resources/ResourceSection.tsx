import toast from "react-hot-toast";

import { Plus } from "lucide-react";

import { Button } from "@/components/ui/button";

import {
  getMyResources,
  getMyStatistics,
  deleteResource,
  downloadResource,
} from "@/services/ResourceService";

import ResourceStatisticsCards
from "./ResourceStatisticsCards";

import ResourceCard
from "./ResourceCard";

import EmptyResources
from "./EmptyResources";

import UploadResourceDialog
from "./UploadResourceDialog";
import { useEffect, useState } from "react";
import type Resource from "@/models/resources/Resource";
import type ResourceStatistics from "@/models/resources/ResourceStatistics";
import { Card, CardContent } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";

function ResourceSection() {

  const [resources, setResources] =
    useState<Resource[]>([]);
    
  const [deletingId, setDeletingId] =
  useState<string | null>(null);

  const [statistics, setStatistics] =
    useState<ResourceStatistics | null>(null);

  const [loading, setLoading] =
    useState(true);

    const [

  uploadDialogOpen,

  setUploadDialogOpen,

] = useState(false);    

  useEffect(() => {

    loadData();

  }, []);

  const loadData = async () => {

    try {

      const [

        resourceResponse,

        statisticsResponse,

      ] = await Promise.all([

        getMyResources(),

        getMyStatistics(),

      ]);

      setResources(
        resourceResponse
      );

      setStatistics(
        statisticsResponse
      );

    }

    catch (error) {

      console.error(
        "Failed to load resources",
        error
      );

    }

    finally {

      setLoading(false);

    }

  };


const handleDownload = async (
  resourceId: string
) => {

  try {

    await downloadResource(
      resourceId
    );

  }

  catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Failed to download resource."

    );

  }

};

const handleDelete = async (
  resourceId: string
) => {

  try {

    setDeletingId(resourceId);

    await deleteResource(resourceId);

    toast.success(
      "Resource deleted successfully."
    );

    await loadData();

  }

  catch (error: any) {

    toast.error(
      error?.response?.data?.message ??
      "Failed to delete resource."
    );

  }

  finally {

    setDeletingId(null);

  }

};


  if (loading) {

    return (

      <div
        className="
          flex
          justify-center
          py-12
        "
      >

        <Spinner className="size-8" />

      </div>

    );

  }

  return (

    <Card className="rounded-3xl">

      <CardContent className="p-8">

        <div className="space-y-2">

          <h2 className="text-2xl font-bold">

            Resource Library

          </h2>

          <p className="text-muted-foreground">

            Manage your uploaded learning materials,
            templates, documentation, portfolios,
            source code, and other resources.

          </p>

        </div>

        <div className="mt-8">

  {

    statistics && (

      <ResourceStatisticsCards

        statistics={statistics}

      />

    )

  }

</div>

            <div className="mt-8 flex justify-end">

            <Button

                onClick={() =>

                setUploadDialogOpen(true)

                }

            >

                <Plus className="mr-2 h-4 w-4" />

                Upload Resource

            </Button>

            </div>

            <div className="mt-8">

  {

    resources.length === 0 ? (

      <EmptyResources

        isOwner={true}

        onUpload={() =>

          setUploadDialogOpen(true)

        }

      />

    ) : (

      <div
        className="
          grid
          gap-6
          lg:grid-cols-2
        "
      >

        {

          resources.map(resource => (

            <ResourceCard
                  key={resource.resourceId}
                  resource={resource}
                  isOwner={true}
                  onDownload={handleDownload}
                  onDelete={handleDelete}
                  deleting={deletingId === resource.resourceId}
              />

          ))

        }

      </div>

    )

  }

</div>

<UploadResourceDialog

  open={uploadDialogOpen}

  onOpenChange={setUploadDialogOpen}

  onUploadSuccess={loadData}

/>

      </CardContent>

    </Card>

  );

}

export default ResourceSection;