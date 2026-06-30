import { Github, Linkedin, Heart } from "lucide-react";

function Footer() {
  return (
    <footer className="border-t border-border/50 bg-background/60 backdrop-blur-xl">

      <div className="container mx-auto flex flex-col items-center justify-between gap-4 px-6 py-6 md:flex-row">

        {/* Brand */}

        <div className="flex items-center gap-3">

          <div
            className="
              flex
              h-9
              w-9
              items-center
              justify-center
              rounded-lg
              bg-primary
              font-bold
              text-primary-foreground
            "
          >
            B
          </div>

          <div>

            <h2 className="font-semibold">

              BarterLink

            </h2>

            <p className="text-xs text-muted-foreground">

              Skill Exchange Platform

            </p>

          </div>

        </div>

        {/* Links */}

        <div className="flex items-center gap-6 text-sm text-muted-foreground">

          <a
            href="#"
            className="transition-colors hover:text-primary"
          >
            Privacy
          </a>

          <a
            href="#"
            className="transition-colors hover:text-primary"
          >
            Terms
          </a>

          <a
            href="#"
            className="transition-colors hover:text-primary"
          >
            Contact
          </a>

        </div>

        {/* Social */}

        <div className="flex items-center gap-3">

          <button className="transition-colors hover:text-primary">

            <Github size={18} />

          </button>

        </div>

      </div>

      <div className="border-t border-border/50">

        <div className="container mx-auto flex flex-col items-center justify-between gap-2 px-6 py-4 text-xs text-muted-foreground md:flex-row">

          <span>

            © 2026 BarterLink. All rights reserved.

          </span>

          <span className="flex items-center gap-2">

            Built with

            <Heart
              size={14}
              className="fill-red-500 text-red-500"
            />

            for lifelong learning.

          </span>

        </div>

      </div>

    </footer>
  );
}

export default Footer;