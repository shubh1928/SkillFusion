export default interface ApiResponse<T> {

  success: boolean;

  message: string;

  data: T;

}