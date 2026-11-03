import apiClient from "@/config/ApiClient";

import type Resource from "@/models/resources/Resource";
import type ResourceStatistics from "@/models/resources/ResourceStatistics";
import type UploadResourceRequest from "@/models/resources/UploadResourceRequest";
import type DownloadResource from "@/models/resources/DownloadResource";
import type ApiResponse from "@/models/ApiResponse";

export const uploadResource =
  async (
    data: UploadResourceRequest,
    file: File
  ): Promise<Resource> => {

    const formData = new FormData();

    formData.append(

      "data",

      new Blob(

        [JSON.stringify(data)],

        {
          type: "application/json",
        }

      )

    );

    formData.append(

      "file",

      file

    );

    const response =
      await apiClient.post(

        "/resources",

        formData,

        {

          headers: {

            "Content-Type":
              "multipart/form-data",

          },

        }

      );

    return response.data.data;

  };

export const getMyResources =
  async (): Promise<Resource[]> => {

    const response =
      await apiClient.get(

        "/resources/my"

      );

    return response.data.data;

  };

export const getResourcesByUser =
  async (
    userId: string
  ): Promise<Resource[]> => {

    const response =
      await apiClient.get(

        `/resources/user/${userId}`

      );

    return response.data.data;

  };

export const getPublicResources =
  async (): Promise<Resource[]> => {

    const response =
      await apiClient.get(

        "/resources/public"

      );

    return response.data.data;

  };

export const getMyStatistics =
  async (): Promise<ResourceStatistics> => {

    const response =
      await apiClient.get(

        "/resources/my/statistics"

      );

    return response.data.data;

  };

export const deleteResource =
  async (
    resourceId: string
  ): Promise<void> => {

    await apiClient.delete(

      `/resources/${resourceId}`

    );

  };

export const downloadResource =
  async (
    resourceId: string
  ): Promise<void> => {

    const response =
  await apiClient.get<ApiResponse<DownloadResource>>(
    `/resources/${resourceId}/download`
  );

    window.open(

      response.data.data.downloadUrl,

      "_blank"

    );

  };