import ResourceCategory from "./ResourceCategory";
import ResourceVisibility from "./ResourceVisibility";

export default interface Resource {

  resourceId: string;

  ownerId: string;

  ownerName: string;

  title: string;

  description: string;

  category: ResourceCategory;

  visibility: ResourceVisibility;

  originalFileName: string;

  contentType: string;

  fileSize: number;

  downloadCount: number;

  createdAt: string;

}