from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # --- 服务 ---
    app_name: str = "Stone AI Service"
    app_version: str = "0.1.0"
    debug: bool = True

    # --- ChromaDB ---
    chromadb_host: str = "localhost"
    chromadb_port: int = 8000
    chromadb_collection: str = "stone_rag"

    # --- 阿里云百炼 API（兼容 OpenAI SDK） ---
    dashscope_api_key: str = ""

    # --- Embedding ---
    embedding_model: str = "text-embedding-v3"
    embedding_api_base: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    # --- LLM ---
    llm_model: str = "qwen-plus"
    llm_api_base: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    llm_temperature: float = 0.7

    # --- 文档分块 ---
    chunk_size: int = 500
    chunk_overlap: int = 50

    # --- RAG ---
    top_k: int = 5
    rerank_enabled: bool = True

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
