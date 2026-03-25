class AppError(Exception):
    """Base application exception."""


class ModelError(AppError):
    """Issues inside model inference or training."""


class DatabaseError(AppError):
    """Issues with persistence layer."""
