import joblib
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler
from src.data.data_loader import load_data
from src.features.feature_engineering import create_features

# Load data
df = load_data("data/raw/large_driver_data.csv")

# Create features
features = create_features(df)

# Scale features
scaler = StandardScaler()
scaled_features = scaler.fit_transform(features)

# Train model
model = IsolationForest(
    n_estimators=200,
    contamination=0.08,
    random_state=42
)

model.fit(scaled_features)

# Save both
joblib.dump(model, "model/anomaly_model.pkl")
joblib.dump(scaler, "model/scaler.pkl")

print("✅ Improved model trained successfully")
print("Model contamination:", model.contamination)
print("Number of samples:", len(features))