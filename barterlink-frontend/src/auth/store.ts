import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";
import { loginUser, logoutUser } from "@/services/AuthService";
import { toast } from "react-hot-toast";
import {create} from "zustand";
import { persist } from "zustand/middleware";

const LOCAL_KEY = "auth_state";

//type AuthStatus = "idle" | "authenticating" | "authenticated" | "anonymous"

//Global auth store using zustand
type AuthState = {
    accessToken: string | null;
    user: User | null;
    authStatus: boolean;
    authLoading: boolean;

    login: (LoginData: LoginData) => Promise<LoginResponseData>;
    logout: (silent?: boolean) => void;

    checkLogin: () => boolean | undefined;

    changeLocalLoginData:(accessToken:string, user:User, authStatus:boolean) => void;
};


// main logic for global state management of auth
const useAuth = create<AuthState>()(
    
    persist(

    (set,get) => ({
    accessToken: null,
    user: null,
    authStatus: false,
    authLoading: false,

    changeLocalLoginData: (accessToken, user, authStatus) => {
        set({
            accessToken,
            user,
            authStatus,
        });
    },

    login: async (loginData) => {
        console.log("started login...")
        set({ authLoading: true })

        try {

        const loginResponseData = await loginUser(loginData);
        console.log("login response data: ", loginResponseData);

        set({
            accessToken: loginResponseData.accessToken,
            user: loginResponseData.user,
            authStatus: true,
            authLoading: false,
        });

        return loginResponseData;

        } 
        catch (error:any) 
        {
            console.error("Login failed: ", error);
            throw error;
        }
        finally {
            set({ authLoading: false });
        }

    },
    logout: async () => {

        try {
            // if(!silent) {
            //     //api call to server to logout user and invalidate token
            //     // await apiClient.post("/auth/logout");
            //     await logoutUser();
            // }

            set({ authLoading: true });
            await logoutUser();

        } catch (error) {
            toast.error("Logout failed: " + error);
        }
        finally {
            set({ authLoading: false });
        }
       
        // await logoutUser();

        set({
            accessToken: null,
            user: null,
            authLoading: false,
            authStatus: false,
        });

    },

    checkLogin: () => {
        if(get().accessToken && get().authStatus) {
            return true;
        }
        return false;

    },

})
    , 
        { name: LOCAL_KEY }
));

export default useAuth;