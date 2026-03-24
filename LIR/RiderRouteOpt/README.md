# Rider Route Optimization Engine (LIR-01)

This repository contains the Rider's Route Optimization Engine for the UrbanBlack platform. The service provides pre-trip route previews and live route updates for riders.

## Features

- **Pre-trip Route Previews:** Estimates the best route and ETA before a trip begins.
- **Live Route Updates:** Recalculates routes in real-time adjusting to traffic events and driver locations.
- **Core Routing Analytics:** Utilizes OSRM or Valhalla as the foundational routing engine, integrating heavily with Google Directions API and Distance Matrix API.
- **Data Persistence:** Stores route segment data, ETA predictions, and fare factors into PostgreSQL.

## Technologies Used

- **PostgreSQL**: Persistence layer for route details and trip segment data.
- **Google Maps APIs**: Integration with Directions API and Distance Matrix.
- **Routing Engine**: OSRM / Valhalla
- **Python / ML**: AI/ML algorithms for ETA prediction and route optimization.

## Setup Instructions

1. **Install Dependencies:**
   Ensure you have your environment set up. If there's a `requirements.txt` file, run:
   ```bash
   pip install -r requirements.txt
   ```

2. **Environment Variables:**
   Copy the provided `.env` variables or create your own `.env` file based on the available properties in the system:
   - Database credentials (`DB_HOST`, `DB_USER`, etc.)
   - Google Maps API key (`GOOGLE_MAPS_API_KEY`)
   - OSRM / Valhalla URLs (`OSRM_BASE_URL`)

3. **Run the Application:**
   Start the FastAPI development server using Uvicorn:
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```
   The API will be available at `http://localhost:8000`. You can also view the interactive API documentation at `http://localhost:8000/docs`.

## Integrating Downstream

This service's output directly impacts:
- ETA Prediction Module
- Fare Calculation Engine
- Trip Integrity Validation
