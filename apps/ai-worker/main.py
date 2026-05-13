from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI(title="HUB Feat Creators AI Worker")
model = SentenceTransformer("paraphrase-multilingual-MiniLM-L12-v2")


class EmbedRequest(BaseModel):
    text: str


class EmbedResponse(BaseModel):
    embedding: list[float]


@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest) -> EmbedResponse:
    vector = model.encode(req.text, normalize_embeddings=True)
    return EmbedResponse(embedding=vector.tolist())


@app.get("/health")
def health():
    return {"status": "ok"}
