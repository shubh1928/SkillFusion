import { FolderOpen } from "lucide-react";

import { Button } from "@/components/ui/button";

interface EmptyResourcesProps {

  isOwner: boolean;

  onUpload?: () => void;

}

function EmptyResources({

  isOwner,

  onUpload,

}: EmptyResourcesProps) {

  return (

    <div
      className="
      flex
      flex-col
      items-center
      justify-center
      rounded-xl
      border
      border-dashed
      py-20
      text-center
      "
    >

      <FolderOpen

        size={60}

        className="text-muted-foreground"

      />

      <h2
        className="
        mt-6
        text-2xl
        font-semibold
        "
      >

        No Resources Found

      </h2>

      <p
        className="
        mt-2
        max-w-md
        text-muted-foreground
        "
      >

        {

          isOwner

            ? "You haven't uploaded any resources yet. Share your study material, source code, documentation, templates, or projects with the community."

            : "This user hasn't shared any public resources yet."

        }

      </p>

      {

        isOwner && (

          <Button

            className="mt-8"

            onClick={onUpload}

          >

            Upload Your First Resource

          </Button>

        )

      }

    </div>

  );

}

export default EmptyResources;