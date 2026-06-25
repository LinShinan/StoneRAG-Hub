from fastapi import APIRouter
from chromadb import HttpClient

from config import settings
from schemas.common import Result

router = APIRouter(tags=["health"])

@router.get("/health")
async def health_check():
    try:
        client = HttpClient(host=settings.chromadb_host, port=settings.chromadb_port)
        client.heartbeat()
        return Result.ok(data={"status":"ok","chromadb":"connected"})
    except Exception as e:
        return Result.ok(data={"status":"ok","chromadb":"disconnected"})

