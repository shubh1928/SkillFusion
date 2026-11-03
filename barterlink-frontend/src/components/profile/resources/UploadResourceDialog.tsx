import { useEffect, useState } from "react";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Spinner } from "@/components/ui/spinner";

import {

  Select,

  SelectContent,

  SelectItem,

  SelectTrigger,

  SelectValue,

} from "@/components/ui/select";

import { Button } from "@/components/ui/button";

import { Input } from "@/components/ui/input";

import { Label } from "@/components/ui/label";

import ResourceCategory
from "@/models/resources/ResourceCategory";

import ResourceVisibility
from "@/models/resources/ResourceVisibility";

import toast from "react-hot-toast";

import {
  uploadResource,
} from "@/services/ResourceService";

import type UploadResourceRequest
from "@/models/resources/UploadResourceRequest";

interface UploadResourceDialogProps {

  open: boolean;

  onOpenChange: (
    open: boolean
  ) => void;

  onUploadSuccess: () => void;

}

function UploadResourceDialog({

  open,

  onOpenChange,

  onUploadSuccess,

}: UploadResourceDialogProps) {

  const [title, setTitle] =
    useState("");

  const [description, setDescription] =
    useState("");

  const [category, setCategory] =
    useState<ResourceCategory>(
      ResourceCategory.STUDY_MATERIAL
    );

  const [visibility, setVisibility] =
    useState<ResourceVisibility>(
      ResourceVisibility.PUBLIC
    );

  const [file, setFile] =
    useState<File | null>(null);

  const [uploading, setUploading] =
    useState(false);

  useEffect(() => {

    if (!open) {

      setTitle("");

      setDescription("");

      setCategory(
        ResourceCategory.STUDY_MATERIAL
      );

      setVisibility(
        ResourceVisibility.PUBLIC
      );

      setFile(null);

    }

  }, [open]);


  const handleUpload = async () => {

  if (!title.trim()) {

    toast.error(

      "Title is required."

    );

    return;

  }

  if (!file) {

    toast.error(

      "Please select a file."

    );

    return;

  }

  try {

    setUploading(true);

    const request:
      UploadResourceRequest = {

      title,

      description,

      category,

      visibility,

    };

    await uploadResource(

      request,

      file

    );

    toast.success(

      "Resource uploaded successfully."

    );

    onOpenChange(false);

    onUploadSuccess();

  }

  catch (error: any) {

    toast.error(

      error?.response?.data?.message ??

      "Failed to upload resource."

    );

  }

  finally {

    setUploading(false);

  }

};


  return (

    <Dialog

      open={open}

      onOpenChange={onOpenChange}

    >

      <DialogContent className="sm:max-w-2xl">

        {uploading && (

  <div
    className="
      absolute
      inset-0
      z-50
      flex
      flex-col
      items-center
      justify-center
      rounded-lg
      bg-background/80
      backdrop-blur-sm
    "
  >

    <Spinner className="size-10" />

    <p className="mt-4 text-sm font-medium">

      Uploading resource...

    </p>

  </div>

)}

        <DialogHeader>

          <DialogTitle>

            Upload Resource

          </DialogTitle>

        </DialogHeader>

        <div className="space-y-6">

          <div>

            <Label>

              Title

            </Label>

            <Input

              value={title}

              onChange={(event) =>

                setTitle(
                  event.target.value
                )

              }

            />

          </div>

          <div>

            <Label>

              Description

            </Label>

            <textarea

              value={description}

              onChange={(event) =>

                setDescription(
                  event.target.value
                )

              }

              className="
                min-h-28
                w-full
                rounded-md
                border
                p-3
              "

            />

          </div>

          <div className="grid gap-4 md:grid-cols-2">

            <div>

              <Label>

                Category

              </Label>

                    <Select

                      value={category}

                      onValueChange={(value) =>

                        setCategory(

                          value as ResourceCategory

                        )

                      }

                    >

                      <SelectTrigger className="w-full">

                        <SelectValue />

                      </SelectTrigger>

                      <SelectContent>

                        <SelectItem value={ResourceCategory.STUDY_MATERIAL}>
                          Study Material
                        </SelectItem>

                        <SelectItem value={ResourceCategory.DOCUMENTATION}>
                          Documentation
                        </SelectItem>

                        <SelectItem value={ResourceCategory.PROJECT}>
                          Project
                        </SelectItem>

                        <SelectItem value={ResourceCategory.SOURCE_CODE}>
                          Source Code
                        </SelectItem>

                        <SelectItem value={ResourceCategory.RESEARCH_PAPER}>
                          Research Paper
                        </SelectItem>

                        <SelectItem value={ResourceCategory.TEMPLATE}>
                          Template
                        </SelectItem>

                        <SelectItem value={ResourceCategory.TUTORIAL}>
                          Tutorial
                        </SelectItem>

                        <SelectItem value={ResourceCategory.PORTFOLIO}>
                          Portfolio
                        </SelectItem>

                        <SelectItem value={ResourceCategory.OTHER}>
                          Other
                        </SelectItem>

                      </SelectContent>

                    </Select>

            </div>

            <div>

              <Label>

                Visibility

              </Label>

                  <Select

                    value={visibility}

                    onValueChange={(value) =>

                      setVisibility(

                        value as ResourceVisibility

                      )

                    }

                  >

                    <SelectTrigger className="w-full">

                      <SelectValue />

                    </SelectTrigger>

                    <SelectContent>

                      <SelectItem value={ResourceVisibility.PUBLIC}>
                        Public
                      </SelectItem>

                      <SelectItem value={ResourceVisibility.PRIVATE}>
                        Private
                      </SelectItem>

                    </SelectContent>

                  </Select>

            </div>

          </div>

          <div>

            <Label>

              Resource File

            </Label>

            <Input

              type="file"

              onChange={(event) =>

                setFile(

                  event.target.files?.[0] ??

                  null

                )

              }

            />

          </div>

          <div className="flex justify-end gap-3">

           <Button

            variant="outline"

            onClick={() =>

              onOpenChange(false)

            }

            disabled={uploading}

          >

            Cancel

          </Button>

            <Button

                  onClick={handleUpload}

                  disabled={uploading}

                >

                  {uploading ? (

                    <>

                      <Spinner className="mr-2 size-4" />

                      Uploading...

                    </>

                  ) : (

                    "Upload Resource"

                  )}

                </Button>

          </div>

        </div>

      </DialogContent>

    </Dialog>

  );

}

export default UploadResourceDialog;