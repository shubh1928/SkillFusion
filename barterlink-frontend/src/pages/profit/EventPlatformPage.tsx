import { useState, type FormEvent } from "react";
import {
  ArrowLeft,
  Building2,
  CalendarDays,
  CheckCircle2,
  CircleDollarSign,
  ClipboardList,
  Clock3,
  Plus,
  Users,
  WalletCards,
} from "lucide-react";

type PricingModel =
  | "Fixed organizing fee"
  | "Monthly subscription"
  | "Per paid registration";

interface EventPlatformForm {
  organizationName: string;
  organizationType: string;
  eventTitle: string;
  category: string;
  deliveryMode: string;
  eventDate: string;
  capacity: string;
  expectedRegistrations: string;
  pricingModel: PricingModel;
  fixedFee: string;
  monthlyFee: string;
  subscriptionMonths: string;
  feePerRegistration: string;
  platformExpenses: string;
  description: string;
}

interface EventPlatformDraft {
  id: number;
  organizationName: string;
  organizationType: string;
  eventTitle: string;
  eventDate: string;
  pricingModel: PricingModel;
  capacity: number;
  expectedRegistrations: number;
  grossRevenue: number;
  estimatedBalance: number;
  createdAt: string;
}

interface EventPlatformPageProps {
  onBack: () => void;
}

const initialForm: EventPlatformForm = {
  organizationName: "",
  organizationType: "College / University",
  eventTitle: "AI for Students: Practical Workshop",
  category: "Artificial Intelligence",
  deliveryMode: "Online",
  eventDate: "",
  capacity: "100",
  expectedRegistrations: "60",
  pricingModel: "Fixed organizing fee",
  fixedFee: "5000",
  monthlyFee: "3000",
  subscriptionMonths: "1",
  feePerRegistration: "100",
  platformExpenses: "800",
  description:
    "A practical workshop for students, managed through the BarterLink event platform.",
};

const fieldClass =
  "w-full rounded-xl border border-border bg-background px-3.5 py-3 text-sm text-foreground outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-2 focus:ring-primary/20";

const toNumber = (value: string): number => {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
};

const toWholeNumber = (value: string): number =>
  Math.floor(toNumber(value));

const formatINR = (amount: number): string =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(amount);

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

export default function EventPlatformPage({
  onBack,
}: EventPlatformPageProps) {
  const [form, setForm] = useState<EventPlatformForm>(initialForm);
  const [drafts, setDrafts] = useState<EventPlatformDraft[]>([]);
  const [feedback, setFeedback] = useState("");

  const capacity = toWholeNumber(form.capacity);
  const expectedRegistrations = toWholeNumber(
    form.expectedRegistrations
  );
  const platformExpenses = toNumber(form.platformExpenses);

  const grossRevenue = (() => {
    switch (form.pricingModel) {
      case "Fixed organizing fee":
        return toNumber(form.fixedFee);

      case "Monthly subscription":
        return (
          toNumber(form.monthlyFee) *
          Math.max(1, toWholeNumber(form.subscriptionMonths))
        );

      case "Per paid registration":
        return (
          toNumber(form.feePerRegistration) *
          expectedRegistrations
        );

      default:
        return 0;
    }
  })();

  const estimatedBalance = grossRevenue - platformExpenses;

  const registrationPercentage =
    capacity > 0
      ? Math.min(100, (expectedRegistrations / capacity) * 100)
      : 0;

  const remainingSeats = Math.max(
    0,
    capacity - expectedRegistrations
  );

  const updateField = <K extends keyof EventPlatformForm>(
    field: K,
    value: EventPlatformForm[K]
  ) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
    setFeedback("");
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!form.organizationName.trim()) {
      setFeedback("Please enter the name of the organization.");
      return;
    }

    if (!form.eventTitle.trim()) {
      setFeedback("Please enter an event title.");
      return;
    }

    if (!form.eventDate) {
      setFeedback("Please select a proposed event date.");
      return;
    }

    if (capacity < 1) {
      setFeedback("Event capacity must be at least one person.");
      return;
    }

    if (expectedRegistrations > capacity) {
      setFeedback(
        "Expected registrations cannot exceed the event capacity."
      );
      return;
    }

    const draft: EventPlatformDraft = {
      id: Date.now(),
      organizationName: form.organizationName.trim(),
      organizationType: form.organizationType,
      eventTitle: form.eventTitle.trim(),
      eventDate: form.eventDate,
      pricingModel: form.pricingModel,
      capacity,
      expectedRegistrations,
      grossRevenue,
      estimatedBalance,
      createdAt: new Date().toLocaleString("en-IN"),
    };

    setDrafts((current) => [draft, ...current]);

    setFeedback(
      "Event platform demo draft created. The event has not been published and no payment has been collected."
    );
  };

  const resetForm = () => {
    setForm(initialForm);
    setFeedback("");
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      {/* Return to the existing Profit Ideas overview */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 rounded-xl border border-border bg-card px-4 py-2.5 text-sm font-semibold text-foreground transition hover:bg-muted"
        >
          <ArrowLeft size={17} />
          SkillFusion Hub
        </button>

        <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs font-semibold">
          <span className="h-2 w-2 rounded-full bg-emerald-500" />
          Live
        </span>
      </div>

      {/* Page heading */}
      <header>
        <div className="mb-2 flex items-center gap-2 text-sm font-medium text-primary">
          <Building2 size={18} />
          SkillFusion Hub 02 · EVENT MANAGEMENT
        </div>

        <h1 className="text-3xl font-bold tracking-tight text-foreground">
          Event Platform Studio
        </h1>

        <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
          Help institutions and businesses organize events through
          BarterLink. Configure event details, choose a platform pricing
          model, and estimate registrations and potential revenue.
        </p>
      </header>

      {/* Intro panel */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Building2 size={27} />
          </div>

          <div className="flex-1">
            <h2 className="text-xl font-bold text-foreground">
              Provide BarterLink as a service
            </h2>

            <p className="mt-2 text-sm leading-6 text-muted-foreground">
              Organizations can use the platform to plan workshops,
              present event information, estimate attendance, and manage
              registrations. BarterLink could earn through organizing
              fees, recurring subscriptions, or charges per paid
              registration.
            </p>
          </div>
        </div>
      </section>

      {/* Live estimate cards */}
      <section>
        <div className="mb-4">
          <h2 className="text-lg font-bold text-foreground">
            Platform business overview
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            These figures update when you change the form.
          </p>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <CircleDollarSign size={17} />
              Estimated platform revenue
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(grossRevenue)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Based on the selected pricing model
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Users size={17} />
              Expected registrations
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {expectedRegistrations}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Planned capacity: {capacity}
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <CalendarDays size={17} />
              Seats remaining
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {remainingSeats}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              Based on expected registrations
            </p>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <WalletCards size={17} />
              Estimated balance
            </div>

            <p className="mt-3 text-2xl font-bold tabular-nums text-foreground">
              {formatINR(estimatedBalance)}
            </p>

            <p className="mt-2 text-xs text-muted-foreground">
              After entered platform operating costs
            </p>
          </div>
        </div>
      </section>

      {/* Event form and business model */}
      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(280px,0.7fr)]">
        <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-foreground">
              Configure an organization and event
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Create a sample listing and estimate what the platform
              could charge.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <FieldLabel required>Organization name</FieldLabel>

              <input
                required
                value={form.organizationName}
                onChange={(event) =>
                  updateField("organizationName", event.target.value)
                }
                placeholder="e.g. ABC College"
                className={fieldClass}
              />
            </div>

            <div>
              <FieldLabel>Organization type</FieldLabel>

              <select
                value={form.organizationType}
                onChange={(event) =>
                  updateField("organizationType", event.target.value)
                }
                className={fieldClass}
              >
                <option>College / University</option>
                <option>Training Centre</option>
                <option>Private Business</option>
                <option>Community Organization</option>
                <option>Non-profit Organization</option>
              </select>
            </div>

            <div>
              <FieldLabel required>Event title</FieldLabel>

              <input
                required
                value={form.eventTitle}
                onChange={(event) =>
                  updateField("eventTitle", event.target.value)
                }
                placeholder="e.g. AI Awareness Workshop"
                className={fieldClass}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <div>
                <FieldLabel>Event category</FieldLabel>

                <select
                  value={form.category}
                  onChange={(event) =>
                    updateField("category", event.target.value)
                  }
                  className={fieldClass}
                >
                  <option>Artificial Intelligence</option>
                  <option>Java Development</option>
                  <option>Blockchain</option>
                  <option>Web Development</option>
                  <option>Career Development</option>
                  <option>Other</option>
                </select>
              </div>

              <div>
                <FieldLabel>Delivery mode</FieldLabel>

                <select
                  value={form.deliveryMode}
                  onChange={(event) =>
                    updateField("deliveryMode", event.target.value)
                  }
                  className={fieldClass}
                >
                  <option>Online</option>
                  <option>In-person</option>
                  <option>Hybrid</option>
                </select>
              </div>

              <div>
                <FieldLabel required>Proposed event date</FieldLabel>

                <input
                  type="date"
                  required
                  value={form.eventDate}
                  onChange={(event) =>
                    updateField("eventDate", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel required>Event capacity</FieldLabel>

                <input
                  type="number"
                  min="1"
                  step="1"
                  required
                  value={form.capacity}
                  onChange={(event) =>
                    updateField("capacity", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>

              <div>
                <FieldLabel required>Expected registrations</FieldLabel>

                <input
                  type="number"
                  min="0"
                  step="1"
                  required
                  value={form.expectedRegistrations}
                  onChange={(event) =>
                    updateField(
                      "expectedRegistrations",
                      event.target.value
                    )
                  }
                  className={fieldClass}
                />
              </div>
            </div>

            {/* Pricing model */}
            <div className="border-t border-border pt-5">
              <h3 className="font-bold text-foreground">
                Choose the platform pricing model
              </h3>

              <p className="mt-1 text-sm text-muted-foreground">
                The revenue calculation changes according to your
                selection.
              </p>

              <div className="mt-4">
                <FieldLabel>Pricing model</FieldLabel>

                <select
                  value={form.pricingModel}
                  onChange={(event) =>
                    updateField(
                      "pricingModel",
                      event.target.value as PricingModel
                    )
                  }
                  className={fieldClass}
                >
                  <option>Fixed organizing fee</option>
                  <option>Monthly subscription</option>
                  <option>Per paid registration</option>
                </select>
              </div>

              {form.pricingModel === "Fixed organizing fee" && (
                <div className="mt-4">
                  <FieldLabel required>
                    Fixed event-organizing fee (₹)
                  </FieldLabel>

                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={form.fixedFee}
                    onChange={(event) =>
                      updateField("fixedFee", event.target.value)
                    }
                    className={fieldClass}
                  />

                  <p className="mt-2 text-xs text-muted-foreground">
                    Illustrative example: ₹5,000 for one event.
                  </p>
                </div>
              )}

              {form.pricingModel === "Monthly subscription" && (
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                  <div>
                    <FieldLabel required>
                      Monthly subscription (₹)
                    </FieldLabel>

                    <input
                      type="number"
                      min="0"
                      step="1"
                      value={form.monthlyFee}
                      onChange={(event) =>
                        updateField("monthlyFee", event.target.value)
                      }
                      className={fieldClass}
                    />
                  </div>

                  <div>
                    <FieldLabel required>
                      Number of months
                    </FieldLabel>

                    <input
                      type="number"
                      min="1"
                      step="1"
                      value={form.subscriptionMonths}
                      onChange={(event) =>
                        updateField(
                          "subscriptionMonths",
                          event.target.value
                        )
                      }
                      className={fieldClass}
                    />
                  </div>
                </div>
              )}

              {form.pricingModel === "Per paid registration" && (
                <div className="mt-4">
                  <FieldLabel required>
                    Platform fee per paid registration (₹)
                  </FieldLabel>

                  <input
                    type="number"
                    min="0"
                    step="1"
                    value={form.feePerRegistration}
                    onChange={(event) =>
                      updateField(
                        "feePerRegistration",
                        event.target.value
                      )
                    }
                    className={fieldClass}
                  />

                  <p className="mt-2 text-xs text-muted-foreground">
                    The estimate uses the expected registrations entered
                    above.
                  </p>
                </div>
              )}

              <div className="mt-4">
                <FieldLabel>
                  Estimated platform operating costs (₹)
                </FieldLabel>

                <input
                  type="number"
                  min="0"
                  step="1"
                  value={form.platformExpenses}
                  onChange={(event) =>
                    updateField("platformExpenses", event.target.value)
                  }
                  className={fieldClass}
                />
              </div>
            </div>

            <div>
              <FieldLabel>Event description</FieldLabel>

              <textarea
                rows={3}
                value={form.description}
                onChange={(event) =>
                  updateField("description", event.target.value)
                }
                placeholder="Describe the event and its intended audience."
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
                  className="mt-0.5 shrink-0 text-primary"
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
                Create event demo draft
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

        {/* Dynamic business model summary */}
        <aside className="space-y-5">
          <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
            <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground">
              <CircleDollarSign size={18} />
              Revenue estimator
            </div>

            <p className="mt-3 text-3xl font-bold tabular-nums text-foreground">
              {formatINR(grossRevenue)}
            </p>

            <p className="mt-2 text-sm text-muted-foreground">
              Estimated gross platform revenue for the selected pricing
              period or event.
            </p>

            <div className="mt-5 space-y-4 border-t border-border pt-4">
              <div className="flex items-start justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Pricing model
                </span>

                <span className="max-w-[60%] text-right font-medium text-foreground">
                  {form.pricingModel}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 text-sm">
                <span className="text-muted-foreground">
                  Estimated costs
                </span>

                <span className="font-medium tabular-nums text-foreground">
                  −{formatINR(platformExpenses)}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 border-t border-border pt-4">
                <span className="text-sm font-semibold text-foreground">
                  Estimated balance
                </span>

                <span className="text-lg font-bold tabular-nums text-foreground">
                  {formatINR(estimatedBalance)}
                </span>
              </div>
            </div>

            <p className="mt-4 text-xs leading-5 text-muted-foreground">
              Estimates are illustrative. Actual earnings depend on
              contracts, registrations, costs, payment fees, taxes,
              refunds, and other applicable charges.
            </p>
          </section>

          <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
            <div className="flex items-center gap-2">
              <Users size={18} className="text-primary" />
              <h3 className="font-bold text-foreground">
                Registration forecast
              </h3>
            </div>

            <div className="mt-5 flex items-end justify-between gap-3">
              <div>
                <p className="text-2xl font-bold tabular-nums text-foreground">
                  {expectedRegistrations}
                  <span className="text-sm font-medium text-muted-foreground">
                    {" "}
                    / {capacity}
                  </span>
                </p>

                <p className="mt-1 text-xs text-muted-foreground">
                  Expected registrations / capacity
                </p>
              </div>

              <span className="text-sm font-semibold text-primary">
                {Math.round(registrationPercentage)}%
              </span>
            </div>

            <div
              className="mt-4 h-2 overflow-hidden rounded-full bg-muted"
              role="progressbar"
              aria-label="Expected registration capacity"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={Math.round(registrationPercentage)}
            >
              <div
                className="h-full rounded-full bg-primary transition-all"
                style={{ width: `${registrationPercentage}%` }}
              />
            </div>

            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-muted-foreground">
                Remaining capacity
              </span>

              <span className="font-semibold text-foreground">
                {remainingSeats} seats
              </span>
            </div>

            <p className="mt-4 text-xs leading-5 text-muted-foreground">
              This is a forecast preview, not a live registration count.
            </p>
          </section>

          <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
            <div className="flex items-center gap-2">
              <ClipboardList size={18} className="text-primary" />
              <h3 className="font-bold text-foreground">
                How the platform works
              </h3>
            </div>

            <ol className="mt-4 space-y-4 text-sm text-muted-foreground">
              <li className="flex gap-3">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                  1
                </span>

                <span>
                  An organization defines the event and its expected
                  audience.
                </span>
              </li>

              <li className="flex gap-3">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                  2
                </span>

                <span>
                  The organization chooses the platform pricing model.
                </span>
              </li>

              <li className="flex gap-3">
                <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground">
                  3
                </span>

                <span>
                  The estimated revenue and registration capacity are
                  reviewed before creating the demo draft.
                </span>
              </li>
            </ol>
          </section>
        </aside>
      </div>

      {/* Saved draft list */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-sm sm:p-6">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-bold text-foreground">
              Event platform drafts
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Demo listings prepared for different organizations.
            </p>
          </div>

          <span className="rounded-full bg-muted px-3 py-1.5 text-xs font-semibold text-foreground">
            {drafts.length} drafts
          </span>
        </div>

        {drafts.length === 0 ? (
          <div className="rounded-xl border border-dashed border-border px-4 py-10 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-muted-foreground">
              <Building2 size={23} />
            </div>

            <h3 className="mt-3 font-semibold text-foreground">
              No event drafts yet
            </h3>

            <p className="mt-1 text-sm text-muted-foreground">
              Fill in the event details and select Create event demo draft.
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
                    <Building2 size={20} />
                  </div>

                  <div>
                    <h3 className="font-semibold text-foreground">
                      {draft.eventTitle}
                    </h3>

                    <p className="mt-1 text-sm text-muted-foreground">
                      {draft.organizationName} · {draft.organizationType}
                    </p>

                    <p className="mt-1 text-xs text-muted-foreground">
                      {draft.eventDate} · {draft.pricingModel}
                    </p>

                    <p className="mt-1 text-xs text-muted-foreground">
                      {draft.expectedRegistrations} expected registrations
                      out of {draft.capacity}
                    </p>

                    <p className="mt-1 inline-flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock3 size={13} />
                      Created {draft.createdAt}
                    </p>
                  </div>
                </div>

                <div className="sm:text-right">
                  <p className="text-xs text-muted-foreground">
                    Estimated gross revenue
                  </p>

                  <p className="mt-1 text-lg font-bold tabular-nums text-foreground">
                    {formatINR(draft.grossRevenue)}
                  </p>

                  <p className="mt-1 text-xs text-muted-foreground">
                    Balance: {formatINR(draft.estimatedBalance)}
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
          Drafts remain in frontend memory only and are cleared when
          this page is refreshed. Nothing is published or sent to a server.
        </p>
      </section>
    </div>
  );
}