import csv
import re
import statistics
import time
from pathlib import Path

from recommendation.matcher import recommend_requests
from recommendation.schemas import (
    RecommendationRequest,
    MarketplaceRequest,
)

# ---------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------

TOP_K = 5
SEMANTIC_RUNS = 5
KEYWORD_RUNS = 100

ROOT = Path(__file__).resolve().parent
OUTPUT_DIR = ROOT / "benchmark_output"


# ---------------------------------------------------------
# 1. SKILL PROFILES
# These are the five profiles retrieved from your database.
# ---------------------------------------------------------

PROFILES = [
    {
        "profile_id": "P01",
        "source": "Actual",
        "skills": [
            "CSS", "Hibernate", "HTML", "Java", "JavaScript",
            "Machine Learning", "MySQL", "Python", "React",
            "Spring Boot"
        ],
    },
    {
        "profile_id": "P02",
        "source": "Actual",
        "skills": [
            "HTML", "Java", "MySQL", "React", "Spring Boot"
        ],
    },
    {
        "profile_id": "P03",
        "source": "Actual",
        "skills": [
            "Hibernate", "Java", "MySQL", "Spring Boot"
        ],
    },
    {
        "profile_id": "P04",
        "source": "Actual",
        "skills": [
            "Hibernate", "Java", "React", "Spring Boot"
        ],
    },
    {
        "profile_id": "P05",
        "source": "Actual",
        "skills": [
            "CSS", "HTML", "JavaScript"
        ],
    },
]


# ---------------------------------------------------------
# 2. MARKETPLACE REQUESTS
# Three actual requests and ten synthetic requests.
# Synthetic requests are benchmark-only; they are NOT
# inserted into your application database.
# ---------------------------------------------------------

REQUESTS = [
    {
        "request_id": "ACT-R01",
        "source": "Actual",
        "title": "Spring Boot REST API Development",
        "description": (
            "I need help developing a Spring Boot REST API. "
            "The work includes creating REST endpoints, handling "
            "request and response data, and connecting the API "
            "with a database."
        ),
    },
    {
        "request_id": "ACT-R02",
        "source": "Actual",
        "title": "React Dashboard Development",
        "description": (
            "I am building a web application and need help "
            "developing a responsive React dashboard. The work "
            "includes creating reusable components, connecting "
            "the frontend with REST APIs, handling application "
            "state, and improving the overall dashboard UI."
        ),
    },
    {
        "request_id": "ACT-R03",
        "source": "Actual",
        "title": "teach me java",
        "description": "teach me java lang",
    },
    {
        "request_id": "SYN-R01",
        "source": "Synthetic",
        "title": "Python Data Cleaning and Visualization",
        "description": (
            "I need help cleaning datasets using Python and "
            "Pandas, handling missing values, analysing data "
            "with NumPy, and creating charts to communicate insights."
        ),
    },
    {
        "request_id": "SYN-R02",
        "source": "Synthetic",
        "title": "Machine Learning Classification Model",
        "description": (
            "I need assistance building a classification model "
            "in Python using scikit-learn, preparing features, "
            "training the model, and evaluating its predictions."
        ),
    },
    {
        "request_id": "SYN-R03",
        "source": "Synthetic",
        "title": "MySQL Query Optimization",
        "description": (
            "I need help improving slow MySQL queries using joins, "
            "indexes, query execution plans, and database "
            "performance analysis."
        ),
    },
    {
        "request_id": "SYN-R04",
        "source": "Synthetic",
        "title": "Responsive HTML CSS JavaScript Website",
        "description": (
            "I need help creating a responsive website using "
            "HTML, CSS, Flexbox, CSS Grid, and JavaScript for "
            "mobile and desktop layouts."
        ),
    },
    {
        "request_id": "SYN-R05",
        "source": "Synthetic",
        "title": "Spring Security JWT Authentication",
        "description": (
            "I need help securing a Spring Boot REST API using "
            "Spring Security, JWT authentication, authorization "
            "rules, and protected endpoints."
        ),
    },
    {
        "request_id": "SYN-R06",
        "source": "Synthetic",
        "title": "Java OOP and Debugging Support",
        "description": (
            "I need help understanding Java classes, inheritance, "
            "exception handling, object-oriented programming, "
            "and debugging Java application errors."
        ),
    },
    {
        "request_id": "SYN-R07",
        "source": "Synthetic",
        "title": "React REST API Integration",
        "description": (
            "I need help connecting a React application to REST "
            "APIs using Axios, managing application state, and "
            "displaying data in reusable components."
        ),
    },
    {
        "request_id": "SYN-R08",
        "source": "Synthetic",
        "title": "Docker Deployment for Java Applications",
        "description": (
            "I need help containerizing a Java Spring Boot "
            "application with Docker and Docker Compose and "
            "preparing it for deployment."
        ),
    },
    {
        "request_id": "SYN-R09",
        "source": "Synthetic",
        "title": "UI UX Design and Figma Prototyping",
        "description": (
            "I need help designing user flows, creating wireframes, "
            "building interactive prototypes in Figma, and "
            "improving interface usability."
        ),
    },
    {
        "request_id": "SYN-R10",
        "source": "Synthetic",
        "title": "NLP Semantic Search Development",
        "description": (
            "I need help developing semantic search with Python, "
            "natural language processing, Sentence Transformers, "
            "text embeddings, and cosine similarity."
        ),
    },
]


# ---------------------------------------------------------
# 3. PROVISIONAL HUMAN RELEVANCE JUDGEMENTS
#
# These must be reviewed before reporting results.
# Each set contains the profiles considered capable of
# addressing that request based on their listed skills.
#
# This is NOT ground truth automatically supplied by AI.
# ---------------------------------------------------------

RELEVANT_PROFILES = {
    "ACT-R01": {"P01", "P02", "P03", "P04"},
    "ACT-R02": {"P01", "P02", "P04"},
    "ACT-R03": {"P01", "P02", "P03", "P04"},

    "SYN-R01": {"P01"},
    "SYN-R02": {"P01"},
    "SYN-R03": {"P01", "P02", "P03"},
    "SYN-R04": {"P01", "P05"},
    "SYN-R05": {"P01", "P02", "P03", "P04"},
    "SYN-R06": {"P01", "P02", "P03", "P04"},
    "SYN-R07": {"P01", "P02", "P04"},
    "SYN-R08": set(),
    "SYN-R09": set(),
    "SYN-R10": {"P01"},
}


# ---------------------------------------------------------
# 4. KEYWORD-BASED BASELINE
# Ranks requests by exact skill phrase overlap.
# This is a simple experimental baseline, not a recreation
# of a specific published platform.
# ---------------------------------------------------------

def normalize(text):
    return re.sub(
        r"[^a-z0-9+#]+",
        " ",
        (text or "").lower()
    ).strip()


def keyword_rank(skills, requests):
    ranked = []

    normalized_skills = [
        normalize(skill) for skill in skills
    ]

    skill_tokens = set(
        " ".join(normalized_skills).split()
    )

    for item in requests:
        text = normalize(
            item["title"] + " " + item["description"]
        )

        padded_text = f" {text} "

        # Count exact skill-phrase matches.
        hits = sum(
            1
            for skill in normalized_skills
            if skill and f" {skill} " in padded_text
        )

        request_tokens = set(text.split())
        union = skill_tokens | request_tokens

        jaccard = (
            len(skill_tokens & request_tokens) / len(union)
            if union else 0.0
        )

        ranked.append({
            "request_id": item["request_id"],
            "title": item["title"],
            "source": item["source"],
            "score": float(hits),
            "tie_break": jaccard,
        })

    ranked.sort(
        key=lambda row: (
            -row["score"],
            -row["tie_break"],
            row["request_id"],
        )
    )

    return ranked[:TOP_K]


# ---------------------------------------------------------
# 5. CALL THE ACTUAL SKILL FUSION MATCHER
# Uses recommendation.matcher.recommend_requests().
# The model and similarity threshold remain unchanged.
# ---------------------------------------------------------

def semantic_rank(profile, requests):
    payload = RecommendationRequest(
        skills=profile["skills"],
        requests=[
            MarketplaceRequest(
                requestId=item["request_id"],
                title=item["title"],
                description=item["description"],
            )
            for item in requests
        ],
    )

    response = recommend_requests(payload)

    request_lookup = {
        item["request_id"]: item
        for item in requests
    }

    ranked = []

    for result in response.recommendations:
        request_id = str(result.requestId)
        item = request_lookup[request_id]

        ranked.append({
            "request_id": request_id,
            "title": item["title"],
            "source": item["source"],
            "score": float(result.similarityScore),
            "tie_break": 0.0,
        })

    return ranked[:TOP_K]


# ---------------------------------------------------------
# 6. METRICS AND CSV OUTPUT
# ---------------------------------------------------------

def precision_at_5(profile_id, ranked):
    relevant_count = sum(
        profile_id in RELEVANT_PROFILES[item["request_id"]]
        for item in ranked
    )

    # Fixed denominator of 5: missing recommendation positions
    # count as not relevant.
    return relevant_count / TOP_K, relevant_count


def write_csv(path, rows):
    if not rows:
        return

    with path.open(
        "w",
        newline="",
        encoding="utf-8-sig"
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=list(rows[0].keys())
        )
        writer.writeheader()
        writer.writerows(rows)


def main():
    if len(PROFILES) != 5 or len(REQUESTS) != 13:
        raise ValueError("Unexpected benchmark input count.")

    request_ids = {item["request_id"] for item in REQUESTS}

    if request_ids != set(RELEVANT_PROFILES):
        raise ValueError(
            "Every request must have a relevance judgement."
        )

    for request_id, relevant_profiles in RELEVANT_PROFILES.items():
        if not relevant_profiles.issubset(
            {p["profile_id"] for p in PROFILES}
        ):
            raise ValueError(
                f"Unknown profile in relevance labels for {request_id}"
            )

    OUTPUT_DIR.mkdir(exist_ok=True)

    profile_results = []
    timing_results = []
    ranking_results = []

    print(f"Skill profiles: {len(PROFILES)}")
    print(f"Requests: {len(REQUESTS)}")
    print(f"Profile/request pairs: {len(PROFILES) * len(REQUESTS)}")
    print("Loading and warming up the semantic model...")
    print("Model-loading time will not be included.")

    for profile in PROFILES:
        profile_id = profile["profile_id"]
        print(f"\nTesting profile {profile_id}...")

        # Warm up the semantic model once before measuring it.
        semantic_rank(profile, REQUESTS)

        semantic_times = []
        semantic_result = []

        for run_number in range(1, SEMANTIC_RUNS + 1):
            start = time.perf_counter()
            current_rank = semantic_rank(profile, REQUESTS)
            elapsed_ms = (
                time.perf_counter() - start
            ) * 1000

            semantic_times.append(elapsed_ms)
            semantic_result = current_rank

            timing_results.append({
                "profile_id": profile_id,
                "method": "Skill Fusion semantic",
                "run_number": run_number,
                "elapsed_ms": round(elapsed_ms, 4),
            })

        keyword_times = []
        keyword_result = []

        for run_number in range(1, KEYWORD_RUNS + 1):
            start = time.perf_counter()
            current_rank = keyword_rank(
                profile["skills"],
                REQUESTS
            )
            elapsed_ms = (
                time.perf_counter() - start
            ) * 1000

            keyword_times.append(elapsed_ms)
            keyword_result = current_rank

            timing_results.append({
                "profile_id": profile_id,
                "method": "Keyword baseline",
                "run_number": run_number,
                "elapsed_ms": round(elapsed_ms, 6),
            })

        for method, ranked, times in [
            (
                "Keyword baseline",
                keyword_result,
                keyword_times,
            ),
            (
                "Skill Fusion semantic",
                semantic_result,
                semantic_times,
            ),
        ]:
            p5, relevant_count = precision_at_5(
                profile_id,
                ranked
            )

            profile_results.append({
                "profile_id": profile_id,
                "profile_source": profile["source"],
                "method": method,
                "mean_latency_ms": round(
                    statistics.mean(times), 4
                ),
                "precision_at_5_percent": round(
                    p5 * 100, 2
                ),
                "relevant_in_top5": relevant_count,
                "returned_recommendations": len(ranked),
            })

            for rank, item in enumerate(ranked, start=1):
                ranking_results.append({
                    "profile_id": profile_id,
                    "method": method,
                    "rank": rank,
                    "request_id": item["request_id"],
                    "title": item["title"],
                    "data_source": item["source"],
                    "score": round(item["score"], 4),
                    "relevant_0_or_1": int(
                        profile_id
                        in RELEVANT_PROFILES[item["request_id"]]
                    ),
                })

    overall_results = []

    for method in [
        "Keyword baseline",
        "Skill Fusion semantic",
    ]:
        rows = [
            row for row in profile_results
            if row["method"] == method
        ]

        overall_results.append({
            "method": method,
            "profiles_tested": len(rows),
            "requests_in_candidate_set": len(REQUESTS),
            "average_precision_at_5_percent": round(
                statistics.mean(
                    row["precision_at_5_percent"]
                    for row in rows
                ), 2
            ),
            "mean_matching_time_ms": round(
                statistics.mean(
                    row["mean_latency_ms"]
                    for row in rows
                ), 4
            ),
            "note": (
                "Includes actual and synthetic requests; "
                "relevance labels require human review. "
                "Timing excludes model loading."
            ),
        })

    write_csv(
        OUTPUT_DIR / "overall_summary.csv",
        overall_results
    )
    write_csv(
        OUTPUT_DIR / "summary_by_profile.csv",
        profile_results
    )
    write_csv(
        OUTPUT_DIR / "run_timings.csv",
        timing_results
    )
    write_csv(
        OUTPUT_DIR / "top5_rankings.csv",
        ranking_results
    )

    print("\nOVERALL RESULTS")
    for row in overall_results:
        print(
            f"{row['method']}: "
            f"Precision@5 = "
            f"{row['average_precision_at_5_percent']}%; "
            f"mean matching time = "
            f"{row['mean_matching_time_ms']} ms"
        )

    print("\nResults saved in:")
    print(OUTPUT_DIR)


if __name__ == "__main__":
    main()