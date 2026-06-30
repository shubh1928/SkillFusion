import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Mail,
  LockKeyhole,
  ArrowRight,
  Sparkles,
  Eye,
  EyeOff,
  CheckCircle2Icon,
} from "lucide-react";
import type LoginData from "@/models/LoginData";
import toast from "react-hot-toast";
import { useNavigate } from "react-router";
import { Alert, AlertTitle } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import useAuth from "@/auth/store";
import OAuth2Buttons from "@/components/OAuth2Buttons";

function Login() {

 const [loginData, setLoginData] = useState<LoginData>({
    email: "",
    password: "",
  });

const[loading, setLoading] = useState<boolean>(false);
const[error, setError] = useState<any>(null);

//for eye icon in password field
const [showPassword, setShowPassword] = useState(false);

const navigate = useNavigate();
const login = useAuth((state) => state.login);

const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;

    setLoginData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
};

  const handleFormSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    //validation
    if(loginData.email.trim() === "") {
      toast.error("Please enter email address");
      setLoading(false);
      return;
    }
    else if(!/\S+@\S+\.\S+/.test(loginData.email)){
      toast.error("Please enter a valid email address");
      setLoading(false);
      return;
    }

    if(loginData.password.trim() === "") {
      toast.error("Password is required");
      setLoading(false);
      return;
    }
    else if(loginData.password.length < 6){
      toast.error("Password must be atleast 6 characters long");
      setLoading(false);
      return;
    }

    // console.log(event.target);
    // console.log(loginData);

      try {
        setLoading(true);
        // const userInfo = await loginUser(loginData);

        //login function: useAuth
        await login(loginData);
        
        toast.success("Login successful");
        // console.log("User Info:", userInfo);

        //save user info to local storage
       // localStorage.setItem("userInfo", JSON.stringify(userInfo));
        //navigate to dashboard after successful login
        navigate("/dashboard");
      } catch (error:any) {
        toast.error("Login failed. Please try again later.");

        if(error?.status === 400){
          setError(error);
        }
        else if(error?.status === 401){
          setError(error);
        }
        else{
          setError({
            response: {
              data: {
                message: "An unexpected error occurred. Please try again later."
              }
            }
          });
        }

      }
      finally {
        setLoading(false);
      }

  }

 return (
    <div className="relative min-h-screen overflow-hidden bg-white text-black dark:bg-black dark:text-white transition-colors duration-500 flex items-center justify-center px-6 py-10">
      
      {/* Background Glow */}
      <div
        className="
        absolute inset-0
        bg-[radial-gradient(circle_at_top_left,#06b6d420,transparent_30%),radial-gradient(circle_at_bottom_right,#8b5cf615,transparent_35%)]
        dark:bg-[radial-gradient(circle_at_top_left,#00ffff15,transparent_30%),radial-gradient(circle_at_bottom_right,#8b5cf620,transparent_35%)]
      "
      />

      {/* Grid Background */}
      <div
        className="
        absolute inset-0
        bg-[linear-gradient(rgba(0,0,0,0.04)_1px,transparent_1px),linear-gradient(90deg,rgba(0,0,0,0.04)_1px,transparent_1px)]
        dark:bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)]
        bg-[size:70px_70px]
      "
      />

      {/* Glow Orb */}
      <div className="absolute h-[500px] w-[500px] rounded-full bg-cyan-500/20 blur-3xl" />

      {/* Login Card */}
      <div
        className="
        relative z-10
        w-full max-w-xl
        rounded-[32px]
        border border-black/10 dark:border-white/10
        bg-white/50 dark:bg-white/5
        backdrop-blur-3xl
        shadow-[0_0_80px_rgba(0,255,255,0.15)]
        overflow-hidden
      "
      >
        {/* Top Gradient */}
        <div className="h-1 w-full bg-gradient-to-r from-cyan-400 via-blue-500 to-violet-500" />

        <div className="p-8 md:p-10">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 rounded-full border border-cyan-400/20 bg-cyan-400/10 px-4 py-2 text-sm text-cyan-400 mb-6">
            <Sparkles className="h-4 w-4" />
            Community Powered Marketplace
          </div>

          {/* Heading */}
          <h1 className="text-4xl md:text-5xl font-black leading-tight">
            Welcome Back
          </h1>

          {/* Subheading */}
          <p className="mt-4 text-gray-600 dark:text-gray-400 leading-relaxed">
            Continue your journey of learning, teaching, and exchanging skills with the BarterLink community.
          </p>

        {/* Error */}
        {
          error && (
        <div className="mt-4">
          <Alert variant={"destructive"}>
            <CheckCircle2Icon/>
            <AlertTitle>{
            error?.response?error?.response?.data?.message: error?.message
            }</AlertTitle>
          </Alert>
        </div>
          )
        }

          {/* Form */}
          <form onSubmit={handleFormSubmit} className="mt-5 space-y-5">
            {/* Email */}
            <div className="space-y-2">
              <label className="text-sm text-gray-700 dark:text-gray-300">
                Email Address
              </label>

              <div className="relative">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-cyan-400" />

                <Input
                  type="email"
                  placeholder="Enter your email"
                  className="
                    h-14
                    pl-12
                    rounded-2xl
                    border-black/10 dark:border-white/10
                    bg-black/5 dark:bg-white/5
                    backdrop-blur-xl
                    focus-visible:ring-cyan-400
                    text-base
                  "
                  name="email"
                  value={loginData.email}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            {/* Password */}
            <div className="space-y-2">
              <label className="text-sm text-gray-700 dark:text-gray-300">
                Password
              </label>

              <div className="relative">
                <LockKeyhole className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-violet-400" />

                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  className="
                    h-14
                    pl-12
                    rounded-2xl
                    border-black/10 dark:border-white/10
                    bg-black/5 dark:bg-white/5
                    backdrop-blur-xl
                    focus-visible:ring-cyan-400
                    text-base
                  "
                  name = "password"
                  value={loginData.password}
                  onChange={handleInputChange}
                />

                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-cyan-400 transition"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>

              </div>
            </div>

            {/* Forgot Password - feature coming soon */}
            {/* <div className="flex justify-end">
              <button className="text-sm text-cyan-400 hover:text-cyan-300 transition">
                Forgot Password?
              </button>
            </div> */}

            {/* Login Button */}
            <Button disabled={loading}
              className="
              w-full
              h-14
              cursor-pointer
              rounded-2xl
              bg-cyan-400
              hover:bg-cyan-300
              text-black
              text-base
              font-bold
              shadow-[0_0_40px_rgba(34,211,238,0.4)]
            "
            >
              {loading ? <><Spinner/>Please Wait...</>  : "Sign In"}

              <ArrowRight className="ml-2 h-5 w-5" />
            </Button>

            {/* Divider */}
            <div className="relative py-2">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-black/10 dark:border-white/10" />
              </div>

              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white dark:bg-black px-3 text-gray-500">
                  Or continue with
                </span>
              </div>
            </div>

            {/* Social Buttons */}
            <OAuth2Buttons/>
          </form>
        </div>
      </div>
    </div>
  );

}

export default Login