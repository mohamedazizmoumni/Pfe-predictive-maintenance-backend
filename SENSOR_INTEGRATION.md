# Sensor Data Ingestion & ML Prediction Integration

## Overview

This document describes how to integrate real industrial sensor data into the Sentinel predictive maintenance system. The system accepts real-time sensor telemetry, normalizes it according to the ML model's training data statistics, and returns RUL (Remaining Useful Life) predictions with risk classifications.

## Complete Data Flow

```
Factory/IoT Device
        ↓
    Telemetry API (POST /api/v1/machines/{id}/telemetry)
        ↓
    TelemetryNormalizationService
        (Feature scaling: [0, 1] range)
        ↓
    MlPredictionService
        (Call FastAPI /predict endpoint)
        ↓
    PredictionRecord Entity
        (Persist with audit trail)
        ↓
    Prediction Response + Record ID
        ↓
    Angular Dashboard Charts/Alerts
```

## API Endpoint: Telemetry Ingestion

### URL
```
POST /api/v1/machines/{machineId}/telemetry
```

### Authentication
- Required: JWT Bearer token with role: TECHNICIAN, MANAGER, DATA_SCIENTIST, ADMIN, or SUPER_ADMIN

### Request Headers
```
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

### Request Payload

The telemetry payload must contain:
- **machineId**: Long (required) - ID of machine sending data
- **timestamp**: LocalDateTime (required) - ISO 8601 format when data was recorded
- **operationalSettings**: Object (required) - 3 environmental parameters
- **sensorReadings**: Map<String, Double> (required) - 21 sensor readings

### Example Request

```json
{
  "machineId": 42,
  "timestamp": "2026-04-20T14:30:00",
  "operationalSettings": {
    "setting1": 560.5,
    "setting2": 0.42,
    "setting3": 0.75
  },
  "sensorReadings": {
    "sensor1": 370.5,
    "sensor2": 45.2,
    "sensor3": 38.9,
    "sensor4": 52.1,
    "sensor5": 42.3,
    "sensor6": 61.8,
    "sensor7": 40.3,
    "sensor8": 51.9,
    "sensor9": 60.8,
    "sensor10": 55.3,
    "sensor11": 48.2,
    "sensor12": 43.1,
    "sensor13": 52.9,
    "sensor14": 45.6,
    "sensor15": 39.8,
    "sensor16": 54.2,
    "sensor17": 47.5,
    "sensor18": 41.3,
    "sensor19": 56.1,
    "sensor20": 49.7,
    "sensor21": 44.2
  }
}
```

### Response Payload

```json
{
  "id": 1523,
  "machineId": 42,
  "predictedAt": "2026-04-20T14:30:00.125",
  "rulValue": 168.5,
  "confidenceLow": 150.0,
  "confidenceHigh": 187.0,
  "riskLevel": "MEDIUM",
  "modelVersion": "1.2.3",
  "triggeredBy": "technician@sentinel.com",
  "createdDate": "2026-04-20T14:30:00.125"
}
```

### Response Codes
- **200 OK**: Prediction successful - record stored
- **400 Bad Request**: Invalid payload (missing sensors, invalid machine ID, etc.)
- **401 Unauthorized**: Missing or invalid JWT token
- **403 Forbidden**: Insufficient role permission
- **404 Not Found**: Machine ID does not exist
- **500 Internal Server Error**: ML service unavailable or database error

## 21 Sensor Column Mapping

The following 21 sensors are from the C-MAPSS (NASA Commercial Modular Aero-Propulsion System Simulation) turbofan dataset:

| Sensor# | Physical Name | Range | Unit | Description |
|---------|----------------|-------|------|-------------|
| 1 | T2 | 370-644°F | Temperature | Low pressure compressor outlet temperature |
| 2 | T24 | 0-100% | Percentage | Low pressure compressor outlet pressure |
| 3 | T30 | 0-100% | Percentage | High pressure compressor outlet temperature |
| 4-21 | Mixed | 0-100% | Various | Additional pressure, temperature, and vibration sensors |

## Feature Normalization

All sensor values are normalized to `[0, 1]` range using min-max scaling:

```
normalized_value = (raw_value - min) / (max - min)

Where:
  min = minimum value from training data
  max = maximum value from training data
```

### Normalization Ranges (Loaded from `ml-feature-ranges.properties`)

```properties
# Operational Settings
operational-setting1.min=518.67
operational-setting1.max=598.67
operational-setting2.min=0.0
operational-setting2.max=0.84
operational-setting3.min=0.0
operational-setting3.max=1.0

# Sensors 1-21 (typical ranges)
sensor1.min=0.0
sensor1.max=100.0
# ... etc for sensor2 through sensor21
```

**Note**: Ranges are derived from training dataset statistics. Contact ML team if ranges need updating.

## Operational Settings

### Setting 1: Operating Condition 1 (T2)
- **Description**: Typically temperature-related parameter
- **Range**: 518.67 to 598.67 (scaled)
- **Impact**: Affects how model interprets other sensor readings

### Setting 2: Operating Condition 2 (T24)
- **Description**: Typically pressure/speed ratio
- **Range**: 0.0 to 0.84 (normalized)
- **Impact**: Critical for model accuracy

### Setting 3: Operating Condition 3 (T30)
- **Description**: Typically altitude/load parameter
- **Range**: 0.0 to 1.0 (normalized)
- **Impact**: Influences RUL estimation

## Risk Level Classifications

Based on predicted RUL (hours):

| Risk Level | RUL Range | Urgency | Action |
|-----------|-----------|---------|--------|
| **CRITICAL** | < 24 hours (1 day) | URGENT | Immediate maintenance/shutdown |
| **HIGH** | 24-72 hours (1-3 days) | HIGH | Schedule maintenance this week |
| **MEDIUM** | 72-168 hours (3-7 days) | MEDIUM | Schedule maintenance within 2 weeks |
| **LOW** | > 168 hours (> 7 days) | LOW | Continue normal monitoring |

## Getting Prediction History

### Endpoint: List Predictions
```
GET /api/v1/machines/{machineId}/predictions?page=0&size=20&sort=predictedAt,desc
```

### Endpoint: Latest Prediction
```
GET /api/v1/machines/{machineId}/predictions/latest
```

### Endpoint: RUL Trend (for Dashboard Charts)
```
GET /api/v1/machines/{machineId}/predictions/trend?days=30
```

Returns daily average RUL values suitable for time-series charting.

## Integration Examples

### Python Integration (Sending Telemetry)

```python
import requests
import json
from datetime import datetime

# Configuration
API_BASE = "http://localhost:8080/api/v1"
JWT_TOKEN = "eyJhbGciOiJIUzI1NiIs..."
MACHINE_ID = 42

# Prepare telemetry
telemetry = {
    "machineId": MACHINE_ID,
    "timestamp": datetime.now().isoformat(),
    "operationalSettings": {
        "setting1": 560.5,
        "setting2": 0.42,
        "setting3": 0.75
    },
    "sensorReadings": {
        f"sensor{i}": 50.0 + (i % 20)  # Example values
        for i in range(1, 22)
    }
}

# Send telemetry
headers = {
    "Authorization": f"Bearer {JWT_TOKEN}",
    "Content-Type": "application/json"
}

response = requests.post(
    f"{API_BASE}/machines/{MACHINE_ID}/telemetry",
    json=telemetry,
    headers=headers
)

if response.status_code == 200:
    prediction = response.json()
    print(f"RUL: {prediction['rulValue']} hours")
    print(f"Risk Level: {prediction['riskLevel']}")
    print(f"Record ID: {prediction['id']}")
else:
    print(f"Error: {response.status_code}")
    print(response.json())
```

### cURL Integration

```bash
curl -X POST http://localhost:8080/api/v1/machines/42/telemetry \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "machineId": 42,
    "timestamp": "2026-04-20T14:30:00",
    "operationalSettings": {
      "setting1": 560.5,
      "setting2": 0.42,
      "setting3": 0.75
    },
    "sensorReadings": {
      "sensor1": 370.5,
      "sensor2": 45.2,
      "sensor3": 38.9,
      "sensor4": 52.1,
      "sensor5": 42.3,
      "sensor6": 61.8,
      "sensor7": 40.3,
      "sensor8": 51.9,
      "sensor9": 60.8,
      "sensor10": 55.3,
      "sensor11": 48.2,
      "sensor12": 43.1,
      "sensor13": 52.9,
      "sensor14": 45.6,
      "sensor15": 39.8,
      "sensor16": 54.2,
      "sensor17": 47.5,
      "sensor18": 41.3,
      "sensor19": 56.1,
      "sensor20": 49.7,
      "sensor21": 44.2
    }
  }'
```

## Error Handling

### Missing Sensors

**Error**: `"missing sensor reading: sensor5"`

**Solution**: Ensure all 21 sensors are present in the request. No sensor can be omitted.

### Invalid Machine ID

**Error**: `"machine not found: 999"`

**Solution**: Verify the machine ID exists before sending telemetry. Check GET /api/v1/machines to list available machines.

### Out-of-Range Values

**Error**: `"value out of range for sensor1: 2000 (max is 100)"`

**Solution**: Verify sensor values are within expected physical ranges. Check with hardware team if values consistently exceed limits.

### Authentication Failure

**Error**: `"Unauthorized: Access Denied"`

**Solution**: Ensure JWT token is valid and includes one of: TECHNICIAN, MANAGER, DATA_SCIENTIST, ADMIN, SUPER_ADMIN role.

## Performance & Latency

- **Typical Prediction Latency**: 100-500 ms (includes ML service call)
- **Throughput**: ~100 predictions/second per backend instance
- **Persistence**: All predictions stored in PostgreSQL with indexes for fast queries
- **Cache**: Prediction history queries are optimized with database indexes on (machine_id, predicted_at)

## Security

- All telemetry endpoints require JWT authentication
- FastAPI ML services are called with internal X-Internal-Key header (not exposed to frontend)
- Sensor data is NOT persisted in the prediction record (only statistical summary)
- Full audit trail: triggeredBy field logs which user/system sent the prediction

## Next Steps

1. **Get JWT Token**: Authenticate via `/api/v1/auth/login` endpoint
2. **Register Machine**: Use `POST /api/v1/machines` to create a machine entity
3. **Add Sensors**: Use `POST /api/v1/machines/{id}/sensors` to define sensors
4. **Send Telemetry**: `POST /api/v1/machines/{id}/telemetry` with sensor data
5. **View Results**: Access `/api/v1/machines/{id}/predictions` for history
