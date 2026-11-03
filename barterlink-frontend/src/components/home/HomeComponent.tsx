import {
  ArrowRight,
  Sparkles,
  BookOpen,
  Wallet,
  Star,
  Handshake,
} from "lucide-react";

import { Link } from "react-router";

import { Button } from "@/components/ui/button";

import FeatureCard from "@/pages/home/FeatureCard";
import StatCard from "@/pages/home/StatCard";

function HomeComponent() {

  const stats = [

    {
      value: "500+",
      label: "Community Members",
    },

    {
      value: "1200+",
      label: "Successful Sessions",
    },

    {
      value: "300+",
      label: "Skills Available",
    },

    {
      value: "4.9★",
      label: "Average Rating",
    },

  ];

  const features = [

    {
      icon: <BookOpen size={28} />,
      title: "Marketplace",
      description:
        "Browse skill requests and connect with people looking to learn or teach.",
    },

    {
      icon: <Handshake size={28} />,
      title: "Secure Agreements",
      description:
        "Every collaboration is backed by agreements and protected workflows.",
    },

    {
      icon: <Wallet size={28} />,
      title: "Wallet System",
      description:
        "Earn platform tokens by sharing your knowledge and completing sessions.",
    },

    {
      icon: <Star size={28} />,
      title: "Reputation",
      description:
        "Build trust through verified reviews and community ratings.",
    },

  ];

  return (

    <div className="relative overflow-hidden">

      {/* Background */}

      <div className="absolute inset-0 -z-20 bg-background" />

      <div className="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.18),transparent_35%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.18),transparent_35%)]" />

      <div className="absolute inset-0 -z-10 bg-[linear-gradient(to_right,rgba(148,163,184,0.08)_1px,transparent_1px),linear-gradient(to_bottom,rgba(148,163,184,0.08)_1px,transparent_1px)] bg-[size:60px_60px]" />

      {/* Hero */}

      <section className="mx-auto flex min-h-screen max-w-7xl flex-col items-center justify-center gap-16 px-6 py-20 lg:flex-row">

        {/* Left */}

        <div className="flex-1 space-y-8">

          <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/10 px-4 py-2 text-sm font-medium text-primary">

            <Sparkles size={16} />

            Community Driven Skill Exchange Platform

          </div>

          <div className="space-y-6">

            <h1 className="text-5xl font-extrabold leading-tight md:text-7xl">

              Learn.

              <br />

              Teach.

              <br />

              <span className="bg-gradient-to-r from-primary via-cyan-500 to-purple-500 bg-clip-text text-transparent">

                Grow Together.

              </span>

            </h1>

            <p className="max-w-2xl text-lg leading-8 text-muted-foreground">

              SkillFusion is a modern platform where people exchange
              skills instead of money. Connect with experts, create
              secure agreements, complete sessions, earn reputation,
              and grow together as a community.

            </p>

          </div>

          <div className="flex flex-wrap gap-4">

            <Button
              size="lg"
              asChild
            >

              <Link to="/signup">

                Get Started

                <ArrowRight className="ml-2" size={18} />

              </Link>

            </Button>

            <Button
              size="lg"
              variant="outline"
              asChild
            >

              <Link to="/signup">

                Explore Marketplace

              </Link>

            </Button>

          </div>

                  </div>

        {/* Right */}

        <div className="relative flex flex-1 items-center justify-center">

          {/* Main Glass Panel */}

          <div
            className="
              relative
              w-full
              max-w-2xl
              rounded-3xl
              border
              border-border/50
              bg-background/60
              p-8
              shadow-2xl
              backdrop-blur-xl
            "
          >

            <div className="mb-8 flex items-center justify-between">

              <div>

                <h2 className="text-2xl font-bold">

                  Skill Exchange

                </h2>

                <p className="text-sm text-muted-foreground">

                  Live Community

                </p>

              </div>

              <div
                className="
                  rounded-full
                  bg-green-500/20
                  px-3
                  py-1
                  text-xs
                  font-semibold
                  text-green-500
                "
              >

                ● Online

              </div>

            </div>

            {/* Skills */}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">

              {[
                "Java",
                "React",
                "Photography",
                "UI / UX",
                "Python",
                "Music",
              ].map((skill) => (

                <div
                  key={skill}
                  className="
                    rounded-xl
                    border
                    border-border/40
                    bg-background/60
                    px-4
                    py-3
                    text-center
                    font-medium
                    transition-all
                    duration-300
                    hover:scale-105
                    hover:border-primary
                  "
                >

                  {skill}

                </div>

              ))}

            </div>

          </div>

        </div>

      </section>

            {/* ===================== */}
      {/* Statistics */}
      {/* ===================== */}

      <section className="mx-auto max-w-7xl px-6 py-20">

        <div className="mb-12 text-center">

          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-primary">

            Community Growth

          </p>

          <h2 className="mt-4 text-4xl font-bold">

            Empowering Skill Exchange

          </h2>

          <p className="mx-auto mt-4 max-w-2xl text-muted-foreground">

            Thousands of learners and experts collaborate every day
            through secure agreements and successful service sessions.

          </p>

        </div>

        <div className="grid gap-6 sm:grid-cols-2 xl:grid-cols-4">

          {stats.map((stat) => (

            <StatCard
              key={stat.label}
              value={stat.value}
              label={stat.label}
            />

          ))}

        </div>

      </section>

      {/* ===================== */}
      {/* Features */}
      {/* ===================== */}

      <section className="mx-auto max-w-7xl px-6 py-20">

        <div className="mb-14 text-center">

          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-primary">

            Platform Features

          </p>

          <h2 className="mt-4 text-4xl font-bold">

            Everything You Need To Exchange Skills

          </h2>

          <p className="mx-auto mt-4 max-w-3xl text-muted-foreground">

            SkillFusion brings together learning, collaboration,
            secure agreements, wallet management and community
            reputation into one modern platform.

          </p>

        </div>

        <div className="grid gap-8 md:grid-cols-2 xl:grid-cols-4">

          {features.map((feature) => (

            <FeatureCard
              key={feature.title}
              icon={feature.icon}
              title={feature.title}
              description={feature.description}
            />

          ))}

        </div>

      </section>

            {/* ===================== */}
      {/* How It Works */}
      {/* ===================== */}

      <section className="mx-auto max-w-7xl px-6 py-24">

        <div className="mb-16 text-center">

          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-primary">

            How It Works

          </p>

          <h2 className="mt-4 text-4xl font-bold">

            Start Your Skill Exchange Journey

          </h2>

          <p className="mx-auto mt-4 max-w-3xl text-muted-foreground">

            Learn, teach and grow with a simple six-step workflow
            designed for secure and transparent collaboration.

          </p>

        </div>

        <div className="grid gap-8 md:grid-cols-3">

          {[
            {
              step: "01",
              title: "Create Your Profile",
              description:
                "Register and showcase your skills and interests.",
            },
            {
              step: "02",
              title: "Explore Marketplace",
              description:
                "Browse skill requests or publish your own learning needs.",
            },
            {
              step: "03",
              title: "Create Agreement",
              description:
                "Secure every exchange through digital agreements.",
            },
            {
              step: "04",
              title: "Complete Session",
              description:
                "Conduct learning sessions and track their progress.",
            },
            {
              step: "05",
              title: "Earn Tokens",
              description:
                "Receive platform rewards after successful completion.",
            },
            {
              step: "06",
              title: "Build Reputation",
              description:
                "Collect reviews and become a trusted community expert.",
            },
          ].map((item) => (

            <div
              key={item.step}
              className="
                rounded-3xl
                border
                border-border/50
                bg-background/60
                p-8
                backdrop-blur-md
                transition-all
                duration-300
                hover:-translate-y-2
                hover:border-primary/40
                hover:shadow-xl
              "
            >

              <div
                className="
                  mb-6
                  inline-flex
                  h-12
                  w-12
                  items-center
                  justify-center
                  rounded-full
                  bg-primary/10
                  text-lg
                  font-bold
                  text-primary
                "
              >

                {item.step}

              </div>

              <h3 className="mb-3 text-xl font-semibold">

                {item.title}

              </h3>

              <p className="leading-7 text-muted-foreground">

                {item.description}

              </p>

            </div>

          ))}

        </div>

      </section>

      {/* ===================== */}
      {/* Popular Skills */}
      {/* ===================== */}

      <section className="mx-auto max-w-7xl px-6 pb-24">

        <div className="mb-12 text-center">

          <p className="text-sm font-semibold uppercase tracking-[0.3em] text-primary">

            Popular Skills

          </p>

          <h2 className="mt-4 text-4xl font-bold">

            Learn From Experts

          </h2>

        </div>

        <div className="flex flex-wrap justify-center gap-4">

          {[
            "Java",
            "Spring Boot",
            "React",
            "UI / UX",
            "Photography",
            "Python",
            "Graphic Design",
            "Fitness",
            "Music",
            "Public Speaking",
            "Marketing",
            "Cooking",
            "AI",
            "Cyber Security",
            "Video Editing",
            "Data Analytics",
          ].map((skill) => (

            <div
              key={skill}
              className="
                rounded-full
                border
                border-border/50
                bg-background/60
                px-6
                py-3
                font-medium
                backdrop-blur-md
                transition-all
                duration-300
                hover:-translate-y-1
                hover:border-primary
                hover:bg-primary
                hover:text-primary-foreground
              "
            >

              {skill}

            </div>

          ))}

        </div>

      </section>

      {/* ===================== */}
      {/* Call To Action */}
      {/* ===================== */}

      <section className="mx-auto max-w-7xl px-6 pb-24">

        <div
          className="
            overflow-hidden
            rounded-[2rem]
            border
            border-primary/20
            bg-gradient-to-r
            from-primary/10
            via-cyan-500/10
            to-purple-500/10
            p-12
            text-center
            backdrop-blur-xl
          "
        >

          <h2 className="text-4xl font-bold">

            Ready to Exchange Your Skills?

          </h2>

          <p className="mx-auto mt-5 max-w-2xl text-lg text-muted-foreground">

            Join SkillFusion today and become part of a growing
            community where knowledge is the most valuable currency.

          </p>

          <div className="mt-10 flex flex-wrap justify-center gap-4">

            <Button
              size="lg"
              asChild
            >

              <Link to="/signup">

                Get Started

                <ArrowRight
                  className="ml-2"
                  size={18}
                />

              </Link>

            </Button>

            <Button
              size="lg"
              variant="outline"
              asChild
            >

              <Link to="/signup">

                Browse Marketplace

              </Link>

            </Button>

          </div>

        </div>

      </section>

    </div>

  );

}

export default HomeComponent;