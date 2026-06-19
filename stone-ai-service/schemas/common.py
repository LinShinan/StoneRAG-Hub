from typing import Any, Optional

from pydantic import BaseModel


class ApiResponse(BaseModel):
    """统一响应格式，与 Java 后端约定一致"""
    code: int = 200
    message: str = "success"
    data: Optional[Any] = None
