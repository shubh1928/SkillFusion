import {
  Calendar,
  Download,
  Eye,
  FileText,
  Globe,
  Lock,
  Trash2,
} from "lucide-react";

import type Resource
from "@/models/resources/Resource";

import ResourceVisibility
from "@/models/resources/ResourceVisibility";

import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge }
from "@/components/ui/badge";

import { Button }
from "@/components/ui/button";

interface ResourceCardProps {

  resource: Resource;

  isOwner: boolean;

  onDownload: (
    resourceId: string
  ) => void;

  onDelete?: (
    resourceId: string
  ) => void;

  deleting: boolean;

}

function ResourceCard({

  resource,

  isOwner,

  onDownload,

  onDelete,

  deleting,

}: ResourceCardProps) {

  const formatFileSize = (
    bytes: number
  ) => {

    if (bytes < 1024)
      return `${bytes} B`;

    if (bytes < 1024 * 1024)
      return `${(bytes / 1024).toFixed(1)} KB`;

    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;

  };

  return (

    <Card
      className="
      relative
      overflow-hidden
      transition-all
      duration-300
      hover:-translate-y-1
      hover:shadow-lg
      "
    >

      {deleting && (

  <div
    className="
      absolute
      inset-0
      z-50
      flex
      flex-col
      items-center
      justify-center
      bg-background/80
      backdrop-blur-sm
    "
  >

    <Spinner className="size-8" />

    <p className="mt-3 text-sm font-medium">

      Deleting resource...

    </p>

  </div>

)}

      <CardContent className="space-y-5 p-6">

        {/* Header */}

        <div
          className="
          flex
          items-start
          justify-between
          gap-4
          "
        >

          <div className="flex-1">

            <h2
              className="
              line-clamp-2
              text-lg
              font-semibold
              "
            >

              {resource.title}

            </h2>

            <p
              className="
              mt-2
              line-clamp-3
              text-sm
              text-muted-foreground
              "
            >

              {resource.description}

            </p>

          </div>

          <FileText
            className="text-primary"
            size={28}
          />

        </div>

        {/* Category + Visibility */}

        <div className="flex flex-wrap gap-2">

          <Badge variant="outline">

            {resource.category.replaceAll(
              "_",
              " "
            )}

          </Badge>

          <Badge>

            {

              resource.visibility ===
              ResourceVisibility.PUBLIC

                ? (

                  <>

                    <Globe
                      className="mr-1 h-3 w-3"
                    />

                    Public

                  </>

                )

                : (

                  <>

                    <Lock
                      className="mr-1 h-3 w-3"
                    />

                    Private

                  </>

                )

            }

          </Badge>

        </div>

        {/* File Info */}

        <div
          className="
          rounded-lg
          border
          bg-muted/40
          p-4
          "
        >

          <div
            className="
            flex
            justify-between
            text-sm
            "
          >

            <span>

              File

            </span>

            <span className="font-medium">

              {resource.originalFileName}

            </span>

          </div>

          <div
            className="
            mt-2
            flex
            justify-between
            text-sm
            "
          >

            <span>

              Size

            </span>

            <span>

              {formatFileSize(
                resource.fileSize
              )}

            </span>

          </div>

        </div>

        {/* Footer */}

        <div
          className="
          flex
          items-center
          justify-between
          border-t
          pt-4
          "
        >

          <div
            className="
            space-y-2
            text-xs
            text-muted-foreground
            "
          >

            <div
              className="
              flex
              items-center
              gap-2
              "
            >

              <Calendar size={14} />

              {new Date(
                resource.createdAt
              ).toLocaleDateString()}

            </div>

            <div
              className="
              flex
              items-center
              gap-2
              "
            >

              <Eye size={14} />

              {resource.downloadCount} Downloads

            </div>

          </div>

          <div className="flex gap-2">

            <Button
                  variant="outline"
                  disabled={deleting}

              onClick={() =>

                onDownload(
                  resource.resourceId
                )

              }

            >

              <Download
                className="mr-2 h-4 w-4"
              />

              Download

            </Button>

            {

              isOwner && (

                <Button

                  variant="destructive"

                  disabled={deleting}

                  onClick={() =>

                    onDelete?.(
                      resource.resourceId
                    )

                  }

                >

                  <Trash2
                    className="mr-2 h-4 w-4"
                  />

                  Delete

                </Button>

              )

            }

          </div>

        </div>

      </CardContent>

    </Card>

  );

}

export default ResourceCard;