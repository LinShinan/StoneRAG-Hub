from fastapi import APIRouter

from schemas.common import Result
from schemas.rag import ParseRequest
from services.rag import DocumentParser

router = APIRouter(prefix="/rag",tags=["rag"])

@router.post("/parse")
async def parse(body: ParseRequest):
    parser = DocumentParser()
    result = parser.parse(
        doc_id= body.doc_id,
        file_path= body.file_path,
        file_type= body.file_type,
    )
    return Result.ok(data=result)