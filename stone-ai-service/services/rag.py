import hashlib
import logging
import os

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from config import settings
from exceptions.base import BadRequestError, NotFoundError, ServiceUnavailableError
from schemas.rag import ChunkResult, ParseResponse
from langchain_community.document_loaders import UnstructuredMarkdownLoader, PyPDFLoader, Docx2txtLoader

logger = logging.getLogger(__name__)

SUPPORTED_TYPES = ["md", "txt", "pdf", "docx"]


def _load_text_file(file_path: str) -> list[Document]:
    """读取纯文本文件，utf-8 优先，失败回退 gbk"""
    for encoding in ["utf-8", "gbk"]:
        try:
            with open(file_path, encoding=encoding) as f:
                text = f.read()
            return [Document(page_content=text)]
        except UnicodeDecodeError:
            continue
    raise ServiceUnavailableError("无法识别文件编码，请使用 UTF-8 或 GBK 编码")


LOADER_MAP = {
    "md":   lambda p: UnstructuredMarkdownLoader(p, encoding="utf-8").load(),
    "txt":  _load_text_file,
    "pdf":  lambda p: PyPDFLoader(p).load(),
    "docx": lambda p: Docx2txtLoader(p).load(),
}

class DocumentParser:
    """文档解析器"""
    def __init__(self,chunk_size: int | None=None, chunk_overlap: int | None=None):
        self.chunk_size= chunk_size or settings.chunk_size
        self.chunk_overlap= chunk_overlap or settings.chunk_overlap

    def parse(self,doc_id: int, file_path: str, file_type: str) -> ParseResponse:
        # 1. 校验
        # 文档类型能否解析
        file_type = file_type.strip().lower()
        if file_type not in SUPPORTED_TYPES:
            raise BadRequestError(f"不支持的文档类型：'{file_type}'")
        # 文件是否存在
        if not os.path.exists(file_path):
            raise NotFoundError(f"文件不存在：'{file_path}'")
        if not os.path.isfile(file_path):
            raise BadRequestError(f"请提供文件路径，而不是目录路径：'{file_path}'")
        # 2. 根据文档类型用不同Loader解析文档
        try:
            docs = LOADER_MAP[file_type](file_path)
        except Exception as e:
            raise ServiceUnavailableError(f"解析文件失败：{e}")
        # 3. 使用RecursiveCharacterTextSplitter将文档切分为多个片段
        full_text = "\n\n".join(d.page_content for d in docs)
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
            separators=settings.chunk_separators,
        )
        chunks_text = splitter.split_text(full_text)

        # 4. 返回解析结果chunk列表
        chunks = []
        for idx, text in enumerate(chunks_text):
            chunks.append(ChunkResult(
                index=idx,
                text=text,
                char_count=len(text),
                hash=hashlib.md5(text.encode("utf-8")).hexdigest(),
            ))

        logger.info(
            "文档解析完成 | doc_id=%d | file_type=%s | chunks=%d",
            doc_id, file_type, len(chunks),
        )
        return ParseResponse(doc_id=doc_id, chunks=chunks)