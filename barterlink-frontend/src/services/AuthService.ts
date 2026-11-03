import type RegisertData from "@/models/RegisterData";
import apiClient from "@/config/ApiClient";
import type LoginData from "@/models/auth/LoginData";
import type LoginResponseData from "@/models/auth/LoginResponseData";
import type User from "@/models/User";

//register function 
export const registerUser = async (signupData: RegisertData) => {
    const response = await apiClient.post(
        "/auth/register",
        signupData,
        {
            timeout: 90000, // 90 seconds for blockchain wallet initialization
        }
    );

    return response.data;
};

//login function
export const loginUser = async (loginData: LoginData) =>  {
    const response = await apiClient.post<LoginResponseData>("/auth/login", loginData);
    return response.data;
}

//logout function
export const logoutUser = async () => {
    //api call to server to logout user and invalidate token
    const response = await apiClient.post("/auth/logout");
    return response.data;
}

//get current user function
export const getCurrentUser = async (emailId:string | undefined) => {
    //api call to server to get current user details
    const response = await apiClient.get<User>(`/users/email/${emailId}`);
    return response.data;
}

//refresh token function
export const refreshToken = async () => {
   const response = await apiClient.post<LoginResponseData>(
      "/auth/refresh",
      {},
      {
         withCredentials: true,
      }
   );

   return response.data;
};