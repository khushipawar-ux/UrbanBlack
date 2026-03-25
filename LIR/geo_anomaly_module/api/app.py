from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

from api.routes import router
from src.exceptions.custom_exceptions import AppError, DatabaseError, ModelError
from src.utils.logger import logger

app = FastAPI(title="Geo Anomaly Detection API")


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error("Request validation error", extra={"path": request.url.path, "errors": exc.errors()})
    return JSONResponse(
        status_code=422,
        content={"message": "Invalid request payload", "details": exc.errors()},
    )


@app.exception_handler(AppError)
async def app_error_handler(request: Request, exc: AppError):
    logger.error("Application error", extra={"path": request.url.path, "error": str(exc)})
    status_code = 500
    if isinstance(exc, DatabaseError):
        status_code = 503
    return JSONResponse(
        status_code=status_code,
        content={"message": str(exc)},
    )


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled error", exc_info=exc)
    return JSONResponse(
        status_code=500,
        content={"message": "Internal server error"},
    )


app.include_router(router)
