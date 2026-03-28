# 🚀 Start-End Consistency Engine

## 📌 Overview

The **Start-End Consistency Engine** is a machine learning–powered system designed to **rank and recommend optimal rides for drivers** based on efficiency, feasibility, and time constraints.

This project integrates:

* 📊 Data processing from PostgreSQL
* 🤖 ML model (XGBoost) for scoring rides
* ⚡ FastAPI backend for real-time recommendations
* 🌍 Google Maps API for route estimation

---

## 🎯 Problem Statement

Drivers often receive multiple ride options but lack a systematic way to choose the most efficient ride considering:

* Distance
* Time remaining in shift
* Ride duration
* Fare value

This system solves that by **predicting a consistency score** and ranking rides accordingly.

---

## 🧠 Machine Learning Approach

### Model Used

* **XGBoost Regressor**

### Feature Engineering

The following features are used to train the model:

| Feature          | Description                 |
| ---------------- | --------------------------- |
| `ride_km`        | Distance of the ride        |
| `duration_min`   | Estimated ride duration     |
| `fare`           | Ride earnings               |
| `hour`           | Hour of ride request        |
| `remaining_time` | Time left in driver shift   |
| `distance_proxy` | Proxy for travel efficiency |

---

### 📉 Model Performance

* **Metric:** RMSE
* **Score:** ~0.09

Indicates strong prediction accuracy and consistency.

---

## ⚙️ System Architecture

```
PostgreSQL DB  →  Feature Engineering  →  XGBoost Model
        ↓
     FastAPI Backend  →  Google Maps API → Final Recommendations
```

---

## 🔌 API Endpoints

### 🔹 Health Check

```
GET /
```

Response:

```json
{
  "message": "API Running 🚀"
}
```

---

### 🔹 Get Ride Recommendations

```
GET /recommend/{driver_id}
```

#### Example:

```
http://127.0.0.1:8000/recommend/<driver_id>
```

#### Response:

```json
[
  {
    "ride_id": "abc123",
    "driver_id": "xyz456",
    "score": 0.84,
    "fare": 250,
    "ride_km": 6.5,
    "estimated_distance": "15.6 km",
    "estimated_time": "28 mins",
    "reason": "Recommended due to good distance and available shift time"
  }
]
```

---

## 🗂️ Project Structure

```
start_end_consistency_engine/
│
├── api/
│   └── recommend.py        # FastAPI routes
│
├── config/
│   └── settings.py         # Environment configs
│
├── utils/
│   └── db.py               # Database connection
│
├── models/
│   └── xgb_model.pkl       # Trained ML model
│
├── notebooks/
│   └── 01_data_exploration.ipynb  # Training & analysis
│
├── .env                    # Environment variables
├── requirements.txt
└── README.md
```

---

## 🛠️ Setup Instructions

### 1️⃣ Clone Repository

```bash
git clone <repo_url>
cd start_end_consistency_engine
```

---

### 2️⃣ Create Virtual Environment

```bash
python -m venv venv
venv\Scripts\activate
```

---

### 3️⃣ Install Dependencies

```bash
pip install -r requirements.txt
```

---

### 4️⃣ Configure Environment Variables

Create `.env` file:

```env
DB_HOST=localhost
DB_NAME=urban_black
DB_USER=postgres
DB_PASSWORD=your_password
DB_PORT=5432

GOOGLE_MAPS_API_KEY=your_api_key
MODEL_PATH=models/xgb_model.pkl
AVG_SPEED=30
```

---

### 5️⃣ Run API Server

```bash
uvicorn api.recommend:app --reload
```

---

### 6️⃣ Open Swagger UI

👉 http://127.0.0.1:8000/docs

---

## 🌍 Google Maps Integration

The system uses Google Maps Directions API to:

* Estimate route distance
* Calculate travel time
* Improve recommendation quality

---

## ⚠️ Notes

* Ensure valid Google Maps API key
* PostgreSQL must be running
* Model file must exist in `/models`

---

## 🚀 Future Improvements

* Real-time traffic integration
* Driver behavior modeling
* Dynamic pricing features
* UI dashboard for visualization
* Deployment (Docker + Cloud)

---

## 👨‍💻 Author

Ayush Singh
B.Tech Computer Engineering

---

## ⭐ Key Highlights

* End-to-end ML pipeline
* Real-time API integration
* Production-ready structure
* Scalable design

---

## 🏁 Conclusion

This project demonstrates a complete pipeline from **data → model → API → real-world integration**, making it suitable for real-world ride optimization systems.

---
