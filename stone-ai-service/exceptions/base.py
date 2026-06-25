"""业务异常类"""


class AppException(Exception):
    """业务异常基类"""

    def __init__(self, message: str, code: int = 50000, status_code: int = 500):
        self.message = message
        self.code = code
        self.status_code = status_code


class NotFoundError(AppException):
    """资源不存在 → code=40001"""
    def __init__(self, message: str = "资源不存在"):
        super().__init__(message, code=40001, status_code=404)


class BadRequestError(AppException):
    """参数错误 → code=40000"""
    def __init__(self, message: str = "参数错误"):
        super().__init__(message, code=40000, status_code=400)


class ServiceUnavailableError(AppException):
    """依赖服务不可用 → code=50000"""
    def __init__(self, message: str = "服务暂不可用"):
        super().__init__(message, code=50000, status_code=503)
