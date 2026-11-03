import ResourceCategory from "./ResourceCategory";
import ResourceVisibility from "./ResourceVisibility";

export default interface UploadResourceRequest {

  title: string;

  description: string;

  category: ResourceCategory;

  visibility: ResourceVisibility;

}