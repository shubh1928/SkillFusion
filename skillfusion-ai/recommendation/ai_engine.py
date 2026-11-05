from sentence_transformers import SentenceTransformer

print("Loading AI model...")

model = SentenceTransformer("all-MiniLM-L6-v2")

print("AI model loaded successfully.")


def create_embedding(text: str):
    return model.encode(
        text,
        convert_to_tensor=True
    )