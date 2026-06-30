import { Button } from "@/components/ui/button";
import { Github } from "lucide-react";
import { NavLink } from 'react-router';

function OAuth2Buttons() {
  return (
            <div className="grid grid-cols-2 gap-4">
              {/* Google */}
            <NavLink to={`${import.meta.env.VITE_BASE_URL || "http://localhost:8084"}/oauth2/authorization/google`} className={"block"}> 
                <Button
                        type="button"
                        variant="outline"
                        className="
                            w-full
                            h-14
                            rounded-2xl
                            border border-black/10 dark:border-white/10
                            bg-black/5 dark:bg-white/5
                            hover:bg-black/10 dark:hover:bg-white/10
                            backdrop-blur-xl
                            text-sm
                            font-medium
                            cursor-pointer
                            flex items-center justify-center
                            transition-all duration-300
                            hover:scale-[1.02]
                        "
                    >
                        <img
                        src="https://www.svgrepo.com/show/475656/google-color.svg"
                        alt="Google"
                        className="h-5 w-5 mr-2"
                        />
                        Google
                </Button>
            </NavLink>


                {/* Github */}
                <NavLink to={`${import.meta.env.VITE_BASE_URL || "http://localhost:8084"}/oauth2/authorization/github`}>
                    <Button
                    type="button"
                    variant="outline"
                    className="
                      w-full
                      h-14
                      rounded-2xl
                      border border-black/10 dark:border-white/10
                      bg-black/5 dark:bg-white/5
                      hover:bg-black/10 dark:hover:bg-white/10
                      backdrop-blur-xl
                      text-sm
                      font-medium
                      cursor-pointer
                      flex items-center justify-center
                      transition-all duration-300
                      hover:scale-[1.02]
                    "
                >
                    <Github className="h-5 w-5 mr-2" />
                    Github
                </Button>
              </NavLink>

            </div>
  )
}

export default OAuth2Buttons