from fastapi import APIRouter

from schemas.common import ApiResponse

router = APIRouter(tags=["health"])


@router.get("/health", response_model=ApiResponse)
async def health_check():
    """健康检查 —— Java 后端启动时调用此接口确认 Python 服务就绪"""
    return ApiResponse(
        data={
            "status": "ok",
            "chromadb": "not_connected",
        }
    )
