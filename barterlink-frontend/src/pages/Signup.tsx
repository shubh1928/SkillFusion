import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  User,
  Mail,
  LockKeyhole,
  Eye,
  EyeOff,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  CheckCircle2Icon,
  Gift,
} from "lucide-react";
import { toast } from "react-hot-toast";
import { registerUser } from "@/services/AuthService";
import { NavLink, useNavigate, useSearchParams } from "react-router";
import { Spinner } from "@/components/ui/spinner";
import { Alert, AlertTitle } from "@/components/ui/alert";
import OAuth2Buttons from "@/components/OAuth2Buttons";

function Signup() {

const [searchParams] = useSearchParams();

const [data, setData] = useState(() => ({
  name: "",
  email: "",
  password: "",
  referrerCode:
    searchParams.get("referrerCode")?.trim() ?? "",
}));

  const [loading, setLoading] = useState<boolean>(false);

  //for eye icon in password field
  const [showPassword, setShowPassword] = useState(false);

  const [error, setError] = useState<any>(null);
  const navigate = useNavigate();

  // Handle input changes 
    const handleInputChange = (event:React.ChangeEvent<HTMLInputElement>) => {
    setData((prev) => ({
      ...prev,
      [event.target.name]: event.target.value,
    }));
  };

  // Handle form submission
  const handleFormSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
   // console.log(data);
    setLoading(true);
    setError(null);

    //Validation
    if(!data.name ||  data.name.trim() === ""){
      toast.error("Name is required");
      setLoading(false);
      return;
    }
    else if(!data.email || data.email.trim() === ""){
      toast.error("Email is required");
      setLoading(false);
      return;
    }
    else if(!data.password || data.password.trim() === ""){
      toast.error("Password is required");
      setLoading(false);
      return;
    }
    else if(data.password.length < 6){
      toast.error("Password must be atleast 6 characters long");
      setLoading(false);
      return;
    }
    else if(!/\S+@\S+\.\S+/.test(data.email)){
      toast.error("Please enter a valid email address");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
     const result = await registerUser({
  ...data,
  name: data.name.trim(),
  email: data.email.trim(),
  referrerCode: data.referrerCode.trim() || undefined,
});
      console.log("Signup successful:", result);
      toast.success("Signup successful! Please login to continue.");
      setData({
  name: "",
  email: "",
  password: "",
  referrerCode: "",
});

      //navigate to login page after successful signup
      navigate("/login");
       } catch (error: unknown) {
      const apiError = error as {
        response?: {
          status?: number;
          data?: {
            message?: string;
          };
        };
        message?: string;
      };

      const status = apiError.response?.status;

      const message =
        apiError.response?.data?.message ??
        apiError.message ??
        "Registration failed. Please try again.";

      console.error("Registration failed:", {
        status,
        message,
        error,
      });

      setError({
        response: {
          data: {
            message,
          },
        },
      });}
      finally {
        setLoading(false);
      }

    };

    const signInPath = data.referrerCode.trim()
  ? `/login?referrerCode=${encodeURIComponent(
      data.referrerCode.trim()
    )}`
  : "/login";

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
      <div className="absolute h-[550px] w-[550px] rounded-full bg-cyan-500/20 blur-3xl" />

      {/* Signup Card */}
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
            Join the SkillFusion Community
          </div>

          {/* Heading */}
          <h1 className="text-4xl md:text-5xl font-black leading-tight">
            Join SkillFusion
          </h1>

          {/* Subheading */}
          <p className="mt-4 text-gray-600 dark:text-gray-400 leading-relaxed">
            Start learning, teaching, and collaborating with skilled people from around the community.
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
            {/* Name */}
            <div className="space-y-2">
              <label className="text-sm text-gray-700 dark:text-gray-300">
                Full Name
              </label>

              <div className="relative">
                <User className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-cyan-400" />

                <Input
                  type="text"
                  placeholder="Enter your name"
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
                  name="name"
                  value={data.name}
                  onChange={handleInputChange}
                />
              </div>
            </div>

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
                  value={data.email}
                  onChange={handleInputChange}
                />
              </div>
            </div>

            {/* Optional Referral Code */}
<div className="space-y-2">
  <label className="text-sm text-gray-700 dark:text-gray-300">
    Referral Code (Optional)
  </label>

  <div className="relative">
    <Gift className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-cyan-400" />

    <Input
      type="text"
      placeholder="Enter your friend's referral code"
      className="h-14 pl-12 rounded-2xl border-black/10 dark:border-white/10 bg-black/5 dark:bg-white/5 backdrop-blur-xl focus-visible:ring-cyan-400 text-base uppercase"
      name="referrerCode"
      value={data.referrerCode}
      onChange={handleInputChange}
      autoComplete="off"
    />
  </div>

  <p className="text-xs text-gray-500 dark:text-gray-400">
    A referral code can only be applied when the account is created.
  </p>
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
                  placeholder="Create a password"
                  className="
                    h-14
                    pl-12 pr-12
                    rounded-2xl
                    border-black/10 dark:border-white/10
                    bg-black/5 dark:bg-white/5
                    backdrop-blur-xl
                    focus-visible:ring-cyan-400
                    text-base
                  "
                  name="password"
                  value={data.password}
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

            {/* Password Strength */}
            <div className="rounded-2xl border border-cyan-400/20 bg-cyan-400/10 p-4 backdrop-blur-xl">
              <div className="flex items-center gap-3">
                <ShieldCheck className="h-5 w-5 text-cyan-400" />

                <div>
                  <p className="text-sm font-medium text-cyan-400">
                    Strong Password Recommended
                  </p>

                  <p className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                    Use uppercase, lowercase, numbers & symbols.
                  </p>
                </div>
              </div>
            </div>

            {/* Signup Button */}
            <Button disabled={loading}
              className="
              w-full
              h-14
              rounded-2xl
              bg-cyan-400
              hover:bg-cyan-300
              text-black
              text-base
              font-bold
              shadow-[0_0_40px_rgba(34,211,238,0.4)]
            "
            >
            {loading ? <><Spinner/>Please Wait...</>  : "Create Account"}
              
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
            <OAuth2Buttons referrerCode={data.referrerCode} />

            {/* Footer Text */}
            <p className="text-center text-sm text-gray-600 dark:text-gray-400 pt-4">
              Already have an account?{" "}
              <NavLink to={signInPath} className={"block"}>
              <span   className="text-cyan-400 hover:text-cyan-300 cursor-pointer transition">
                Sign In
              </span>
              </NavLink>
            </p>
          </form>
        </div>
      </div>
    </div>
  );

}

export default Signup