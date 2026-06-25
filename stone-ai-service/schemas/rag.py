from pydantic import BaseModel


# rag parse请求参数
class ParseRequest(BaseModel):
    doc_id: int
    file_path: str
    file_type: str


class ChunkResult(BaseModel):
    index: int
    text: str
    char_count: int
    hash: str

class ParseResponse(BaseModel):
    doc_id: int
    chunks: list[ChunkResult]