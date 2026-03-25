from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from src.services.anomaly_service import detect_anomaly

router = APIRouter()


class DriverInput(BaseModel):
    lat: float
    lng: float
    speed: float
    bearing: Optional[float] = 0
    route: Optional[List[List[float]]] = []


@router.get("/")
def home():
    return {"message": "Geo Anomaly Detection API Running 🚀"}


@router.post("/detect-anomaly")
def detect(data: DriverInput):
    try:
        return detect_anomaly(data.dict(), data.route)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))