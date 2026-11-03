import { useState } from "react";
import type { LucideIcon } from "lucide-react";
import SeminarsPage from "./SeminarsPage";
import EventPlatformPage from "./EventPlatformPage";
import {
  ArrowLeft,
  ArrowRight,
  BriefcaseBusiness,
  Building2,
  CheckCircle2,
  CircleDollarSign,
  FileText,
  GraduationCap,
  Lightbulb,
  Plus,
  Rocket,
  ShieldCheck,
  Ticket,
  Trophy,
  Users,
} from "lucide-react";

type IdeaId =
  | "seminars"
  | "event-platform"
  | "education-promotions"
  | "job-advertisements"
  | "student-insights"
  | "hackathons";

type FieldType = "text" | "number" | "date" | "select" | "textarea";

interface FieldConfig {
  name: string;
  label: string;
  type: FieldType;
  placeholder?: string;
  required?: boolean;
  options?: string[];
}

interface ProfitIdea {
  id: IdeaId;
  title: string;
  shortDescription: string;
  description: string;
  earnings: string;
  example: string;
  icon: LucideIcon;
  tag: string;
}

interface SavedDraft {
  id: string;
  ideaId: IdeaId;
  title: string;
  estimate: number;
  createdAt: string;
}

const profitIdeas: ProfitIdea[] = [
  {
    id: "seminars",
    title: "Organize Paid Seminars",
    shortDescription:
      "Host workshops, technical sessions, and interview-preparation events.",
    description:
      "BarterLink can organize paid workshops covering Java, AI, blockchain, web development, interview preparation, and other useful skills.",
    earnings:
      "Ticket sales, sponsorships, and premium workshops.",
    example:
      "200 attendees × ₹149 per ticket = ₹29,800 gross ticket revenue before event costs, taxes, refunds, and applicable payment fees.",
    icon: Ticket,
    tag: "Events",
  },
  {
    id: "event-platform",
    title: "BarterLink as an Event Platform",
    shortDescription:
      "Let colleges, universities, training centres, and businesses manage their events.",
    description:
      "Allow external organizations to use BarterLink to create workshops, manage registrations, and coordinate event information without requiring BarterLink to organize everything itself.",
    earnings:
      "Event-organizing fees, subscriptions, or a fee for each paid registration.",
    example:
      "A college pays ₹5,000 to conduct a workshop using BarterLink. The illustrative revenue is ₹5,000 before applicable costs and taxes.",
    icon: Building2,
    tag: "Organizations",
  },
  {
    id: "education-promotions",
    title: "Education & Certification Promotions",
    shortDescription:
      "Showcase courses, learning platforms, certifications, and admissions opportunities.",
    description:
      "Partner with legitimate educational providers to promote relevant courses, workshops, certification programs, and admissions opportunities.",
    earnings:
      "Clearly disclosed sponsorships, affiliate commissions, and paid promotional placements.",
    example:
      "A training provider pays ₹3,000 for a sponsored workshop listing. Paid promotion should be clearly distinguishable from independent recommendations.",
    icon: GraduationCap,
    tag: "Education",
  },
  {
    id: "job-advertisements",
    title: "Promote Job Advertisements",
    shortDescription:
      "Provide paid vacancy listings for employers looking for skilled professionals.",
    description:
      "Employers can publish vacancies for developers, designers, tutors, interns, and other skilled professionals on BarterLink.",
    earnings:
      "Job-posting fees, featured listings, employer subscriptions, and recruitment services.",
    example:
      "20 employers × ₹1,000 per featured listing = ₹20,000 gross revenue before applicable costs and taxes. Listings should be screened for misleading or fraudulent jobs.",
    icon: BriefcaseBusiness,
    tag: "Recruitment",
  },
  {
    id: "student-insights",
    title: "Privacy-Safe Student Insights",
    shortDescription:
      "Explore consent-based introductions and genuinely anonymized aggregate reports.",
    description:
      "Selling identifiable student data is not recommended. Names, personal email addresses, phone numbers, educational records, and skill profiles can be personal data. Use voluntary introductions or genuinely anonymized aggregate reports where lawful.",
    earnings:
      "Clearly explained, consent-based introductions or privacy-safe aggregate reporting services.",
    example:
      "For a demo, estimate revenue from voluntary introductions or anonymized reports. Never make the sharing or sale of private student details the default price of using BarterLink.",
    icon: ShieldCheck,
    tag: "Privacy first",
  },
  {
    id: "hackathons",
    title: "Conduct Hackathons",
    shortDescription:
      "Host innovation challenges for students, developers, and technology communities.",
    description:
      "Run hackathons around AI, blockchain, web development, social impact, and other technology themes. Participants can work individually or in teams and present projects to judges.",
    earnings:
      "Sponsorships, optional entry fees, partner contributions, and sponsored challenges.",
    example:
      "A hackathon can combine entry-fee revenue with sponsorships. The amount shown in this demo is gross revenue and does not deduct prizes, venue costs, or other expenses.",
    icon: Trophy,
    tag: "Innovation",
  },
];

const fieldsByIdea: Record<IdeaId, FieldConfig[]> = {
  seminars: [
    {
      name: "eventTitle",
      label: "Seminar or workshop title",
      type: "text",
      placeholder: "e.g. Java Full Stack Workshop",
      required: true,
    },
    {
      name: "topic",
      label: "Main topic",
      type: "select",
      options: [
        "Java",
        "Artificial Intelligence",
        "Blockchain",
        "Web Development",
        "Interview Preparation",
        "Other Skills",
      ],
    },
    {
      name: "eventDate",
      label: "Planned date",
      type: "date",
    },
    {
      name: "ticketPrice",
      label: "Ticket price per attendee (₹)",
      type: "number",
      placeholder: "149",
      required: true,
    },
    {
      name: "expectedAttendees",
      label: "Expected number of attendees",
      type: "number",
      placeholder: "200",
      required: true,
    },
    {
      name: "sponsorshipAmount",
      label: "Estimated sponsorship revenue (₹)",
      type: "number",
      placeholder: "0",
    },
  ],

  "event-platform": [
    {
      name: "organizationName",
      label: "College, institution, or business",
      type: "text",
      placeholder: "e.g. ABC College",
      required: true,
    },
    {
      name: "eventTitle",
      label: "Event title",
      type: "text",
      placeholder: "e.g. AI Awareness Workshop",
      required: true,
    },
    {
      name: "pricingModel",
      label: "Pricing model",
      type: "select",
      options: [
        "Fixed organizing fee",
        "Monthly subscription",
        "Per paid registration",
      ],
    },
    {
      name: "feeAmount",
      label: "Fee amount (₹)",
      type: "number",
      placeholder: "5000",
      required: true,
    },
    {
      name: "expectedRegistrations",
      label: "Expected paid registrations",
      type: "number",
      placeholder: "50",
    },
  ],

  "education-promotions": [
    {
      name: "providerName",
      label: "Education provider",
      type: "text",
      placeholder: "e.g. Training institute",
      required: true,
    },
    {
      name: "programName",
      label: "Course, certification, or program",
      type: "text",
      placeholder: "e.g. Cloud Computing Certification",
      required: true,
    },
    {
      name: "promotionType",
      label: "Promotion type",
      type: "select",
      options: [
        "Sponsored listing",
        "Affiliate promotion",
        "Sponsored workshop",
        "Admissions promotion",
      ],
    },
    {
      name: "sponsorshipFee",
      label: "Estimated fee or commission (₹)",
      type: "number",
      placeholder: "3000",
      required: true,
    },
    {
      name: "disclosure",
      label: "Promotion disclosure",
      type: "select",
      options: [
        "Paid promotion will be clearly labelled",
        "Affiliate relationship will be disclosed",
      ],
    },
  ],

  "job-advertisements": [
    {
      name: "employerName",
      label: "Employer or company",
      type: "text",
      placeholder: "e.g. Example Technologies",
      required: true,
    },
    {
      name: "jobTitle",
      label: "Job title",
      type: "text",
      placeholder: "e.g. Junior Java Developer",
      required: true,
    },
    {
      name: "listingType",
      label: "Listing type",
      type: "select",
      options: [
        "Standard job listing",
        "Featured job listing",
        "Employer subscription",
        "Recruitment service",
      ],
    },
    {
      name: "listingPrice",
      label: "Price per listing or package (₹)",
      type: "number",
      placeholder: "1000",
      required: true,
    },
    {
      name: "listingCount",
      label: "Number of paid listings or packages",
      type: "number",
      placeholder: "20",
      required: true,
    },
  ],

  "student-insights": [
    {
      name: "reportName",
      label: "Demo service or report name",
      type: "text",
      placeholder: "e.g. Technology Skills Overview",
      required: true,
    },
    {
      name: "serviceModel",
      label: "Privacy-safe service model",
      type: "select",
      options: [
        "Voluntary, consent-based introductions",
        "Anonymized aggregate reports",
      ],
    },
    {
      name: "unitCount",
      label: "Expected paid introductions or reports",
      type: "number",
      placeholder: "10",
      required: true,
    },
    {
      name: "unitPrice",
      label: "Illustrative fee per service (₹)",
      type: "number",
      placeholder: "500",
      required: true,
    },
  ],

  hackathons: [
    {
      name: "hackathonName",
      label: "Hackathon name",
      type: "text",
      placeholder: "e.g. SkillFusion Innovation Challenge",
      required: true,
    },
    {
      name: "theme",
      label: "Main theme",
      type: "select",
      options: [
        "Artificial Intelligence",
        "Blockchain",
        "Web Development",
        "Social Impact",
        "Open Innovation",
      ],
    },
    {
      name: "eventDate",
      label: "Planned date",
      type: "date",
    },
    {
      name: "expectedParticipants",
      label: "Expected participants",
      type: "number",
      placeholder: "100",
      required: true,
    },
    {
      name: "entryFee",
      label: "Entry fee per participant (₹)",
      type: "number",
      placeholder: "0",
      required: true,
    },
    {
      name: "sponsorshipAmount",
      label: "Estimated sponsorship revenue (₹)",
      type: "number",
      placeholder: "10000",
    },
  ],
};

const toNumber = (value?: string): number => {
  const parsed = Number(value ?? "0");
  return Number.isFinite(parsed) ? Math.max(0, parsed) : 0;
};

const formatINR = (amount: number): string =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);

function calculateEstimate(
  ideaId: IdeaId,
  values: Record<string, string>
): number {
  switch (ideaId) {
    case "seminars":
      return (
        toNumber(values.ticketPrice) *
          toNumber(values.expectedAttendees) +
        toNumber(values.sponsorshipAmount)
      );

    case "event-platform":
      return values.pricingModel === "Per paid registration"
        ? toNumber(values.feeAmount) *
            toNumber(values.expectedRegistrations)
        : toNumber(values.feeAmount);

    case "education-promotions":
      return toNumber(values.sponsorshipFee);

    case "job-advertisements":
      return (
        toNumber(values.listingPrice) *
        toNumber(values.listingCount)
      );

    case "student-insights":
      return (
        toNumber(values.unitPrice) *
        toNumber(values.unitCount)
      );

    case "hackathons":
      return (
        toNumber(values.entryFee) *
          toNumber(values.expectedParticipants) +
        toNumber(values.sponsorshipAmount)
      );

    default:
      return 0;
  }
}

export default function ProfitIdeasPage() {
  const [activeIdea, setActiveIdea] = useState<IdeaId | null>(null);
  const [formData, setFormData] = useState<Record<string, string>>({});
  const [savedDrafts, setSavedDrafts] = useState<SavedDraft[]>([]);
  const [formError, setFormError] = useState("");
  const [notice, setNotice] = useState("");

  const selectedIdea = profitIdeas.find(
    (idea) => idea.id === activeIdea
  );

  const activeFields = activeIdea
    ? fieldsByIdea[activeIdea]
    : [];

  const revenueEstimate =
    activeIdea !== null
      ? calculateEstimate(activeIdea, formData)
      : 0;

  const openIdea = (ideaId: IdeaId) => {
    setActiveIdea(ideaId);
    setFormData({});
    setFormError("");
    setNotice("");
  };

  const updateField = (name: string, value: string) => {
    setFormData((current) => ({
      ...current,
      [name]: value,
    }));
    setFormError("");
    setNotice("");
  };

  const saveDemoDraft = () => {
    if (!activeIdea || !selectedIdea) {
      return;
    }

    const missingField = activeFields.find(
      (field) =>
        field.required &&
        !formData[field.name]?.trim()
    );

    if (missingField) {
      setFormError(`${missingField.label} is required.`);
      return;
    }

    const titleField = activeFields.find((field) =>
      [
        "eventTitle",
        "organizationName",
        "providerName",
        "employerName",
        "reportName",
        "hackathonName",
      ].includes(field.name)
    );

    const draftTitle =
      (titleField ? formData[titleField.name]?.trim() : "") ||
      selectedIdea.title;

    const draft: SavedDraft = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      ideaId: activeIdea,
      title: draftTitle,
      estimate: revenueEstimate,
      createdAt: new Date().toLocaleString("en-IN"),
    };

    setSavedDrafts((current) => [draft, ...current]);
    setNotice(
      "Demo draft created. It is stored only in the current page session."
    );
    setFormError("");
  };

    // Render the dedicated seminar interface inside Profit Ideas.
  if (activeIdea === "seminars") {
    return (
      <SeminarsPage
        onBack={() => {
          setActiveIdea(null);
          setFormData({});
          setFormError("");
          setNotice("");
        }}
      />
    );
  }

  if (activeIdea === "event-platform") {
  return (
    <EventPlatformPage
      onBack={() => {
        setActiveIdea(null);
        setFormData({});
        setFormError("");
        setNotice("");
      }}
    />
  );
}

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-medium text-primary">
            <Lightbulb size={17} />
            Platform Business Models
          </div>

          <h1 className="text-3xl font-bold tracking-tight">
            SkillFusion Hub
          </h1>

          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            Explore possible revenue streams for BarterLink and create
            interactive demonstration drafts.
          </p>
        </div>

        <div className="inline-flex w-fit items-center gap-2 rounded-full border px-3 py-2 text-xs font-semibold">
          <span className="h-2 w-2 rounded-full bg-emerald-500" />
          Live
        </div>
      </header>

      {activeIdea === null ? (
        <>
         <section className="rounded-2xl border bg-card p-6 shadow-sm sm:p-8">
  <div className="grid gap-6 md:grid-cols-[1fr_auto] md:items-center">
    <div className="max-w-2xl">
      <span className="inline-flex items-center gap-2 rounded-full border bg-muted px-3 py-1 text-xs font-semibold text-foreground">
        <CircleDollarSign size={14} />
        Revenue exploration
      </span>

      <h2 className="mt-4 text-2xl font-bold sm:text-3xl">
        Turn platform activity into opportunities.
      </h2>

      <p className="mt-3 text-sm leading-6 text-muted-foreground">
        Explore six business ideas, enter your own assumptions,
        estimate potential gross revenue, and save demo drafts
        for discussion.
      </p>
    </div>

    <div className="hidden h-20 w-20 items-center justify-center rounded-2xl border bg-primary/10 text-primary md:flex">
      <Rocket size={36} />
    </div>
  </div>
</section>

          <section>
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold">
                  Choose a business idea
                </h2>
                <p className="mt-1 text-sm text-muted-foreground">
                  Select a card to open its dedicated demo interface.
                </p>
              </div>

              <span className="shrink-0 rounded-full bg-muted px-3 py-1 text-xs font-medium">
                {profitIdeas.length} ideas
              </span>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {profitIdeas.map((idea, index) => {
                const Icon = idea.icon;

                return (
                  <button
                    key={idea.id}
                    type="button"
                    onClick={() => openIdea(idea.id)}
                    className="group flex h-full flex-col rounded-2xl border bg-card p-5 text-left shadow-sm transition hover:-translate-y-1 hover:border-primary/50 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary/40"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
                        <Icon size={24} />
                      </div>

                      <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
                        {idea.tag}
                      </span>
                    </div>

                    <div className="mt-5 flex-1">
                      <div className="mb-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                        Idea {String(index + 1).padStart(2, "0")}
                      </div>

                      <h3 className="text-lg font-bold leading-snug">
                        {idea.title}
                      </h3>

                      <p className="mt-2 text-sm leading-6 text-muted-foreground">
                        {idea.shortDescription}
                      </p>
                    </div>

                    <div className="mt-5 flex items-center justify-between border-t pt-4 text-sm font-semibold text-primary">
                      Open
                      <ArrowRight
                        size={17}
                        className="transition group-hover:translate-x-1"
                      />
                    </div>
                  </button>
                );
              })}
            </div>
          </section>

          <section className="rounded-2xl border bg-card p-5 sm:p-6">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-amber-500/10 text-amber-600">
                <ShieldCheck size={21} />
              </div>

              <div>
                <h2 className="font-bold">
                  Build revenue without compromising trust
                </h2>
                <p className="mt-1 text-sm leading-6 text-muted-foreground">
                  Label paid promotions, screen job listings, make
                  sponsorships transparent, and protect student privacy.
                  Revenue figures are illustrative estimates—not guaranteed
                  income or profit.
                </p>
              </div>
            </div>
          </section>

          {savedDrafts.length > 0 && (
            <section className="rounded-2xl border bg-card p-5 sm:p-6">
              <div className="mb-4 flex items-center gap-2">
                <FileText size={19} className="text-primary" />
                <h2 className="text-lg font-bold">
                  Recent demo drafts
                </h2>
              </div>

              <div className="space-y-3">
                {savedDrafts.slice(0, 5).map((draft) => {
                  const draftIdea = profitIdeas.find(
                    (idea) => idea.id === draft.ideaId
                  );

                  return (
                    <div
                      key={draft.id}
                      className="flex flex-col gap-3 rounded-xl border p-4 sm:flex-row sm:items-center sm:justify-between"
                    >
                      <div>
                        <p className="font-semibold">{draft.title}</p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {draftIdea?.title} · {draft.createdAt}
                        </p>
                      </div>

                      <div className="font-semibold tabular-nums">
                        {formatINR(draft.estimate)}
                      </div>
                    </div>
                  );
                })}
              </div>

              <p className="mt-3 text-xs text-muted-foreground">
                Drafts are temporary and will be cleared when the page
                is refreshed.
              </p>
            </section>
          )}
        </>
      ) : selectedIdea ? (
        <>
          <button
            type="button"
            onClick={() => {
              setActiveIdea(null);
              setFormError("");
              setNotice("");
            }}
            className="inline-flex items-center gap-2 rounded-lg px-2 py-2 text-sm font-semibold text-muted-foreground transition hover:bg-muted hover:text-foreground"
          >
            <ArrowLeft size={17} />
            Back to all ideas
          </button>

          <section className="rounded-2xl border bg-card p-5 sm:p-7">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                {(() => {
                  const Icon = selectedIdea.icon;
                  return <Icon size={28} />;
                })()}
              </div>

              <div className="flex-1">
                <span className="text-xs font-semibold uppercase tracking-wider text-primary">
                  {selectedIdea.tag} · Live
                </span>

                <h2 className="mt-2 text-2xl font-bold">
                  {selectedIdea.title}
                </h2>

                <p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">
                  {selectedIdea.description}
                </p>
              </div>
            </div>
          </section>

          <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1.25fr)_minmax(300px,0.75fr)]">
            <section className="rounded-2xl border bg-card p-5 sm:p-6">
              <div className="mb-5">
                <h3 className="text-lg font-bold">
                  Create a demo draft
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">
                  Enter sample details below. Required fields are marked
                  with an asterisk.
                </p>
              </div>

              <div className="space-y-5">
                {activeFields.map((field) => (
                  <div key={field.name} className="space-y-2">
                    <label
                      htmlFor={field.name}
                      className="block text-sm font-semibold"
                    >
                      {field.label}
                      {field.required && (
                        <span className="ml-1 text-red-500">*</span>
                      )}
                    </label>

                    {field.type === "select" ? (
                      <select
                        id={field.name}
                        value={
                          formData[field.name] ??
                          field.options?.[0] ??
                          ""
                        }
                        onChange={(event) =>
                          updateField(field.name, event.target.value)
                        }
                        className="w-full rounded-xl border bg-background px-3 py-3 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
                      >
                        {field.options?.map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    ) : field.type === "textarea" ? (
                      <textarea
                        id={field.name}
                        value={formData[field.name] ?? ""}
                        onChange={(event) =>
                          updateField(field.name, event.target.value)
                        }
                        placeholder={field.placeholder}
                        rows={3}
                        className="w-full resize-y rounded-xl border bg-background px-3 py-3 text-sm outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
                      />
                    ) : (
                      <input
                        id={field.name}
                        type={field.type}
                        min={field.type === "number" ? "0" : undefined}
                        step={field.type === "number" ? "any" : undefined}
                        value={formData[field.name] ?? ""}
                        onChange={(event) =>
                          updateField(field.name, event.target.value)
                        }
                        placeholder={field.placeholder}
                        required={field.required}
                        className="w-full rounded-xl border bg-background px-3 py-3 text-sm outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20"
                      />
                    )}
                  </div>
                ))}
              </div>

              {formError && (
                <div
                  role="alert"
                  className="mt-5 rounded-xl border border-red-500/30 bg-red-500/5 px-4 py-3 text-sm text-red-600"
                >
                  {formError}
                </div>
              )}

              {notice && (
                <div
                  role="status"
                  className="mt-5 flex items-start gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-700"
                >
                  <CheckCircle2 size={18} className="mt-0.5 shrink-0" />
                  <span>{notice}</span>
                </div>
              )}

              <button
                type="button"
                onClick={saveDemoDraft}
                className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:opacity-90 sm:w-auto"
              >
                <Plus size={17} />
                Create demo draft
              </button>

              <p className="mt-3 text-xs leading-5 text-muted-foreground">
                This action only creates a temporary demo entry in the
                current page. It does not publish or submit information
                to a server.
              </p>
            </section>

            <aside className="space-y-5">
              <section className="rounded-2xl border bg-card p-5 sm:p-6">
                <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
                  <CircleDollarSign size={18} />
                  Illustrative gross revenue
                </div>

                <div className="mt-3 text-3xl font-bold tabular-nums">
                  {formatINR(revenueEstimate)}
                </div>

                <p className="mt-2 text-xs leading-5 text-muted-foreground">
                  This is an estimate based on the form values. It is
                  gross revenue—not profit—and does not automatically
                  deduct event expenses, prizes, refunds, taxes, or
                  payment fees.
                </p>

                {activeIdea === "student-insights" && (
                  <div className="mt-4 rounded-xl border border-amber-500/30 bg-amber-500/5 p-3 text-sm leading-5">
                    The estimate applies only to consent-based services
                    or anonymized aggregate reports. Do not use this
                    feature to sell identifiable student information.
                  </div>
                )}

                {activeIdea === "education-promotions" && (
                  <div className="mt-4 rounded-xl border p-3 text-sm leading-5 text-muted-foreground">
                    Sponsored content and affiliate relationships must
                    be disclosed clearly to users.
                  </div>
                )}

                {activeIdea === "job-advertisements" && (
                  <div className="mt-4 rounded-xl border p-3 text-sm leading-5 text-muted-foreground">
                    Verify employers and screen listings before treating
                    a job advertisement as legitimate.
                  </div>
                )}
              </section>

              <section className="rounded-2xl border bg-card p-5 sm:p-6">
                <h3 className="font-bold">How this idea earns</h3>

                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  {selectedIdea.earnings}
                </p>

                <div className="mt-4 border-t pt-4">
                  <h4 className="text-sm font-semibold">
                    Example
                  </h4>

                  <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    {selectedIdea.example}
                  </p>
                </div>
              </section>

              <section className="rounded-2xl border bg-card p-5 sm:p-6">
                <div className="flex items-center gap-2">
                  <Users size={18} className="text-primary" />
                  <h3 className="font-bold">Demo workflow</h3>
                </div>

                <ol className="mt-4 space-y-3 text-sm text-muted-foreground">
                  <li className="flex gap-3">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                      1
                    </span>
                    Enter example details in the form.
                  </li>

                  <li className="flex gap-3">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                      2
                    </span>
                    Review the illustrative revenue estimate.
                  </li>

                  <li className="flex gap-3">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                      3
                    </span>
                    Create a temporary demo draft.
                  </li>
                </ol>
              </section>
            </aside>
          </div>

          {savedDrafts.filter((draft) => draft.ideaId === activeIdea).length >
            0 && (
            <section className="rounded-2xl border bg-card p-5 sm:p-6">
              <div className="mb-4 flex items-center gap-2">
                <FileText size={19} className="text-primary" />
                <h3 className="text-lg font-bold">
                  Drafts for this idea
                </h3>
              </div>

              <div className="space-y-3">
                {savedDrafts
                  .filter((draft) => draft.ideaId === activeIdea)
                  .map((draft) => (
                    <div
                      key={draft.id}
                      className="flex flex-col gap-3 rounded-xl border p-4 sm:flex-row sm:items-center sm:justify-between"
                    >
                      <div>
                        <p className="font-semibold">{draft.title}</p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          Demo draft · {draft.createdAt}
                        </p>
                      </div>

                      <p className="font-semibold tabular-nums">
                        {formatINR(draft.estimate)}
                      </p>
                    </div>
                  ))}
              </div>
            </section>
          )}
        </>
      ) : null}
    </div>
  );
}