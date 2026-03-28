from fastapi import FastAPI
from api.recommend import router

app = FastAPI()

app.include_router(router)