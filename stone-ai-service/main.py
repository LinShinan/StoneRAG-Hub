import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from exceptions.handlers import register_exception_handlers
from routers.health import router as health_router
from routers.rag import router as rag_router

logging.basicConfig(
    level=settings.log_level,
    format="%(asctime)s | %(levelname)-7s | %(name)s | %(message)s",
    datefmt="%H:%M:%S",
)





app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
)

# 全局异常处理
register_exception_handlers(app)

# CORS —— 允许前端跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载路由
app.include_router(health_router)
app.include_router(rag_router)


# 根路径
@app.get("/")
async def root():
    return {"service": settings.app_name, "version": settings.app_version}
