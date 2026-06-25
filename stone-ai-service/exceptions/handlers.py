"""FastAPI 异常处理器"""

import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from exceptions.base import AppException

logger = logging.getLogger(__name__)


async def _app_exception_handler(request: Request, exc: AppException):
    """处理业务异常"""
    logger.warning(
        "业务异常 | %s %s | code=%s | %s",
        request.method, request.url.path, exc.code, exc.message,
    )
    return JSONResponse(
        status_code=exc.status_code,
        content={"code": exc.code, "message": exc.message, "data": None},
    )


async def _global_exception_handler(request: Request, exc: Exception):
    """兜底 —— 未知异常"""
    logger.exception(
        "未处理异常 | %s %s",
        request.method, request.url.path,
    )
    return JSONResponse(
        status_code=500,
        content={"code": 50000, "message": "服务器内部错误", "data": None},
    )


def register_exception_handlers(app: FastAPI):
    app.add_exception_handler(AppException, _app_exception_handler)
    app.add_exception_handler(Exception, _global_exception_handler)
    logger.info("异常处理器注册完成")
