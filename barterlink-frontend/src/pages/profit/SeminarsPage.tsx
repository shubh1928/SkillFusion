import { useState, type FormEvent } from "react";
import {
  ArrowLeft,
  CalendarDays,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  GraduationCap,
  Plus,
  Ticket,
  Users,
  WalletCards,
} from "lucide-react";

interface SeminarForm {
  title: string;
  topic: string;
  date: string;
  format: string;
  duration: string;
  attendees: string;
  ticketPrice: string;
  sponsorship: string;
  expenses: string;
  description: string;
}

interface SeminarDraft {
  id: number;
  title: string;
  topic: string;
  date: string;
  format: string;
  attendees: number;
  ticketPrice: number;
  ticketRevenue: number;
  sponsorship: number;
  totalGrossRevenue: number;
  createdAt: string;
}

const initialForm: SeminarForm = {
  title: "Full Stack Java Career Workshop",
  topic: "Java and Interview Preparation",
  date: "",
  format: "Online",
  duration: "3 hours",
  attendees: "200",
  ticketPrice: "149",
  sponsorship: "0",
  expenses: "5000",
  description:
    "A practical workshop covering Java development and interview preparation.",
};

const formatINR = (amount: number): string =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number.isFinite(amount) ? amount : 0);

const toAmount = (value: string): number => {
  const amount = Number(value);

  return Number.isFinite(amount) && amount >= 0
    ? amount
    : 0;
};

const fieldClass =
  "w-full rounded-xl border border-border bg-background px-3.5 py-3 text-sm text-foreground outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20";

function FieldLabel({
  children,
  required = false,
}: {
  children: string;
  required?: boolean;
}) {
  return (
    <span className="mb-2 block text-sm font-medium text-foreground">
      {children}
      {required && <span className="ml-1 text-red-500">*</span>}
    </span>
  );
}

interface SeminarsPageProps {
  onBack: () => void;
}

export default function SeminarsPage({ onBack }: SeminarsPageProps) {

  const [form, setForm] = useState<SeminarForm>(initialForm);
  const [drafts, setDrafts] = useState<SeminarDraft[]>([]);
  const [feedback, setFeedback] = useState("");

  const attendees = Math.floor(toAmount(form.attendees));
  const ticketPrice = toAmount(form.ticketPrice);
  const sponsorship = toAmount(form.sponsorship);
  const expenses = toAmount(form.expenses);

  const ticketRevenue = attendees * ticketPrice;
  const totalGrossRevenue = ticketRevenue + sponsorship;

  // Demonstration assumption only; actual payment fees may differ.
  const estimatedPaymentFees = ticketRevenue * 0.02;

  const estimatedBalance =
    totalGrossRevenue - expenses - estimatedPaymentFees;

  const updateField = (
    field: keyof SeminarForm,
    value: string
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));

    setFeedback("");
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!form.title.trim()) {
      setFeedback("Please enter a workshop title.");
      return;
    }

    if (!form.date) {
      setFeedback("Please select a proposed workshop date.");
      return;
    }

    if (attendees < 1) {
      setFeedback("Expected attendance must be at least one person.");
      return;
    }

    const draft: SeminarDraft = {
      id: Date.now(),
      title: form.title.trim(),
      topic: form.topic,
      date: form.date,
      format: form.format,
      attendees,
      ticketPrice,
      ticketRevenue,
      sponsorship,
      totalGrossRevenue,
      createdAt: new Date().toLocaleString("en-IN"),
    };

    setDrafts((current) => [draft, ...current]);

    setFeedback(
      "Seminar demo draft created successfully. No event has been published and no payment has been collected."
    );
  };

  const resetForm = () => {
    setForm(initialForm);
    setFeedback("");
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      {/* Back navigation and page heading */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5 text-sm font-semibold text-foreground transition hover:bg-muted"
        >
          <ArrowLeft size={17} />
          SkillFusion Hub
        </button>

        <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs font-semibold text-foreground">
          <span className="h-2 w-2 rounded-full bg-emerald-500" />
    Live
        </span>
      </div>

      <header>
        <div className="mb-2 flex items-center gap-2 text-sm font-medium text-primary">
          <GraduationCap size={18} />
          SkillFusion Hub 01 · EVENTS AND LEARNING
        </div>

        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          Seminar Studio
        </h1>

        <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
          Plan workshops for the Skill Fusion community, configure ticket
          pricing, estimate event revenue, and create seminar drafts.
        </p>
      </header>

      {/* Intro card */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Ticket size={27} />
          </div>

          <div className="flex-1">
            <h2 className="text-xl font-bold text-foreground">
              Organize paid seminars
            </h2>

            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Run expert-led workshops on Java, AI, blockchain, web
              development, interview preparation, and other skills.
              Revenue can come from tickets, sponsors, and premium sessions.
            </p>
          </div>
        </div>
      </section>

      {/* Revenue overview */}
      <section>
        <div className="mb-4">
          <h2 className="text-lg font-bold text-foreground">
            Revenue overview
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Values update as you edit the workshop details.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Ticket size={17} />
              Ticket revenue
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(ticketRevenue)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Attendees × ticket price
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <CircleDollarSign size={17} />
              Sponsorship
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(sponsorship)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Estimated sponsor contribution
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <WalletCards size={17} />
              Total gross revenue
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(totalGrossRevenue)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Ticket revenue + sponsorship
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <CircleDollarSign size={17} />
              Estimated balance
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(estimatedBalance)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              After entered costs and assumed payment fees
            </p>
          </div>
        </div>
      </section>

      {/* Main creation workflow */}
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1.35fr)_minmax(280px,0.65fr)]">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-foreground">
              Create a workshop
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Enter the seminar details to prepare a demo draft.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <FieldLabel required>Workshop title</FieldLabel>

              <input
                required
                value={form.title}
                onChange={(event) =>
                  updateField("title", event.target.value)
                }
                placeholder="e.g. Full Stack Java Workshop"
                className={fieldClass}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <div>
                <FieldLabel required>Skill or topic</FieldLabel>

                <select
                  value={form.topic}
                  onChange={(event) =>
                    updateField("topic", event.target.value)
                  }
                  className={fieldClass}
                >
                  <option>Java and Interview Preparation</option>
                  <option>Artificial Intelligence</option>
                  <option>Blockchain Development</option>
                  <option>Web Development</option>
                  <option>Career and Interview Skills</option>
                  <option>Other Skills</option>
                </select>
              </div>

              <div>
                <FieldLabel required>Proposed date</FieldLabel>

                <input
                  type="date"
                  required
                  value={form.date}
                  onChange={(event) =>
                    updateField("date", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel>Event format</FieldLabel>

                <select
                  value={form.format}
                  onChange={(event) =>
                    updateField("format", event.target.value)
                  }
                  className={fieldClass}
                >
                  <option>Online</option>
                  <option>In-person</option>
                  <option>Hybrid</option>
                </select>
              </div>

              <div>
                <FieldLabel>Duration</FieldLabel>

                <select
                  value={form.duration}
                  onChange={(event) =>
                    updateField("duration", event.target.value)
                  }
                  className={fieldClass}
                >
                  <option>1 hour</option>
                  <option>2 hours</option>
                  <option>3 hours</option>
                  <option>Half day</option>
                  <option>Full day</option>
                </select>
              </div>

              <div>
                <FieldLabel required>Expected attendees</FieldLabel>

                <input
                  type="number"
                  min="1"
                  step="1"
                  required
                  value={form.attendees}
                  onChange={(event) =>
                    updateField("attendees", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel required>Ticket price (₹)</FieldLabel>

                <input
                  type="number"
                  min="0"
                  step="1"
                  required
                  value={form.ticketPrice}
                  onChange={(event) =>
                    updateField("ticketPrice", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel>Estimated sponsorship (₹)</FieldLabel>

                <input
                  type="number"
                  min="0"
                  step="1"
                  value={form.sponsorship}
                  onChange={(event) =>
                    updateField("sponsorship", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel>Estimated event expenses (₹)</FieldLabel>

                <input
                  type="number"
                  min="0"
                  step="1"
                  value={form.expenses}
                  onChange={(event) =>
                    updateField("expenses", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>
            </div>

            <div>
              <FieldLabel>Workshop description</FieldLabel>

              <textarea
                rows={3}
                value={form.description}
                onChange={(event) =>
                  updateField("description", event.target.value)
                }
                placeholder="Describe the topics, learning outcomes, and intended audience."
                className={`${fieldClass} resize-y`}
              />
            </div>

            {feedback && (
              <div
                role="status"
                className="flex items-start gap-2 rounded-xl border border-border bg-muted p-4 text-sm text-foreground"
              >
                <CheckCircle2
                  size={18}
                  className="mt-0.5 shrink-0 text-emerald-500"
                />
                <span>{feedback}</span>
              </div>
            )}

            <div className="flex flex-wrap gap-3 border-t border-border pt-5">
              <button
                type="submit"
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition hover:opacity-90"
              >
                <Plus size={17} />
                Create seminar draft
              </button>

              <button
                type="button"
                onClick={resetForm}
                className="rounded-xl border border-border bg-background px-5 py-3 text-sm font-semibold text-foreground transition hover:bg-muted"
              >
                Reset form
              </button>
            </div>
          </form>
        </section>

        {/* Financial breakdown */}
        <aside className="space-y-5">
          <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
            <h3 className="text-lg font-bold text-foreground">
              Revenue breakdown
            </h3>

            <p className="mt-1 text-sm text-muted-foreground">
              Illustrative estimates in Indian rupees.
            </p>

            <div className="mt-5 space-y-4">
              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Ticket sales
                </span>
                <span className="font-semibold tabular-nums">
                  {formatINR(ticketRevenue)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Sponsorship
                </span>
                <span className="font-semibold tabular-nums">
                  {formatINR(sponsorship)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 border-t border-border pt-4 text-sm">
                <span className="font-semibold">Gross revenue</span>
                <span className="font-bold tabular-nums">
                  {formatINR(totalGrossRevenue)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Event expenses
                </span>
                <span className="font-semibold tabular-nums">
                  −{formatINR(expenses)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Assumed payment fees (2%)
                </span>
                <span className="font-semibold tabular-nums">
                  −{formatINR(estimatedPaymentFees)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 border-t border-border pt-4">
                <span className="text-sm font-bold">
                  Estimated balance
                </span>
                <span className="text-lg font-bold tabular-nums text-primary">
                  {formatINR(estimatedBalance)}
                </span>
              </div>
            </div>

            <p className="mt-4 text-xs leading-5 text-muted-foreground">
              The 2% payment fee is a demonstration assumption, not a
              confirmed provider fee. This calculation excludes taxes
              and refunds; actual results may differ.
            </p>
          </section>

          <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
            <h3 className="font-bold text-foreground">
              Planning checklist
            </h3>

            <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
              <li className="flex gap-3">
                <CalendarDays
                  size={18}
                  className="shrink-0 text-primary"
                />
                Select the event date and duration.
              </li>

              <li className="flex gap-3">
                <Users
                  size={18}
                  className="shrink-0 text-primary"
                />
                Estimate attendance and ticket pricing.
              </li>

              <li className="flex gap-3">
                <CircleDollarSign
                  size={18}
                  className="shrink-0 text-primary"
                />
                Consider sponsors and operating expenses.
              </li>

              <li className="flex gap-3">
                <Clock3
                  size={18}
                  className="shrink-0 text-primary"
                />
                Review the plan before any real publication.
              </li>
            </ul>
          </section>
        </aside>
      </div>

      {/* Draft list */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-foreground">
              Seminar drafts
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Demo workshops created during this page session.
            </p>
          </div>

          <span className="rounded-full bg-muted px-3 py-1.5 text-xs font-semibold text-foreground">
            {drafts.length} drafts
          </span>
        </div>

        {drafts.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border px-4 py-10 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
              <CalendarDays size={23} />
            </div>

            <h3 className="mt-3 font-semibold text-foreground">
              No seminar drafts yet
            </h3>

            <p className="mt-1 text-sm text-muted-foreground">
              Complete the form above and select Create seminar draft.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {drafts.map((draft) => (
              <div
                key={draft.id}
                className="flex flex-col gap-4 rounded-xl border border-border p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="flex items-start gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                    <GraduationCap size={20} />
                  </div>

                  <div>
                    <h3 className="font-semibold text-foreground">
                      {draft.title}
                    </h3>

                    <p className="mt-1 text-sm text-muted-foreground">
                      {draft.topic} · {draft.format}
                    </p>

                    <p className="mt-1 text-xs text-muted-foreground">
                      {draft.date} · {draft.attendees} expected attendees
                    </p>

                    <p className="mt-1 text-xs text-muted-foreground">
                      Created {draft.createdAt}
                    </p>
                  </div>
                </div>

                <div className="sm:text-right">
                  <p className="text-xs text-muted-foreground">
                    Estimated gross revenue
                  </p>

                  <p className="mt-1 text-lg font-bold tabular-nums text-foreground">
                    {formatINR(draft.totalGrossRevenue)}
                  </p>

                  <span className="mt-2 inline-flex rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-foreground">
                    Demo draft
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

        <p className="mt-4 text-xs leading-5 text-muted-foreground">
          Drafts exist only in frontend memory and disappear when the page
          is refreshed. This interface does not publish events or collect
          payments.
        </p>
      </section>
    </div>
  );
}