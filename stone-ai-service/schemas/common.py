"""通用响应模型"""

from typing import Generic, TypeVar

from pydantic import BaseModel

T = TypeVar("T")


class Result(BaseModel, Generic[T]):
    """统一响应格式 —— 与 Java 后端约定一致"""

    code: int = 200
    message: str = "success"
    data: T | None = None

    @classmethod
    def ok(cls, data: T | None = None, message: str = "success") -> "Result[T]":
        """成功返回"""
        return cls(code=200, message=message, data=data)

    @classmethod
    def fail(cls, code: int = 500, message: str="fail") -> "Result[None]":
        """失败返回"""
        return cls(code=code, message=message, data=None)
