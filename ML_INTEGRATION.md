# Java to Python ML Integration

This backend now integrates with an external Python ML inference service over HTTP.

## Python Contract

- Base URL: `http://127.0.0.1:8000` (configurable)
- `GET /health`
- `GET /model-info`
- `POST /predict`
- `POST /predict` request body:
  - `features`: 2D numeric array
  - every sample must have exactly 89 values

## New Java API

- `GET /api/v1/ml/health`
- `GET /api/v1/ml/model-info`
- `POST /api/v1/ml/predict`

Prediction endpoint validates payload shape and numeric values before calling Python.

## Configuration

Set in `api-module/src/main/resources/application.yml` under `ml`:

- `ml.service.base-url`
- `ml.service.timeout.connect`
- `ml.service.timeout.read`
- `ml.service.retry.max-attempts`
- `ml.service.retry.backoff`
- `ml.service.auth.header-name`
- `ml.service.auth.header-value`
- `ml.rate-limit.enabled`
- `ml.rate-limit.requests-per-minute`

Environment variables are supported:

- `ML_SERVICE_BASE_URL`
- `ML_SERVICE_CONNECT_TIMEOUT`
- `ML_SERVICE_READ_TIMEOUT`
- `ML_SERVICE_RETRY_MAX_ATTEMPTS`
- `ML_SERVICE_RETRY_BACKOFF`
- `ML_SERVICE_AUTH_HEADER_NAME`
- `ML_SERVICE_AUTH_HEADER_VALUE`
- `ML_RATE_LIMIT_ENABLED`
- `ML_RATE_LIMIT_RPM`

## Readiness Behavior

`/actuator/health/readiness` now includes `pythonMl` health.

Readiness is DOWN when:

- Python service is unreachable
- Python service returns errors
- model is not loaded (`model_loaded=false`)

## Error Mapping

- Python `422` -> Java `400 Bad Request`
- Python `5xx` / model failures -> Java `424 Failed Dependency`
- timeout or unreachable service -> Java `503 Service Unavailable`
- prediction API rate-limited -> Java `429 Too Many Requests`

## Correlation and Logs

- Incoming headers supported: `X-Correlation-ID`, `X-Request-ID`
- If missing, Java generates both IDs
- Java returns both IDs in response headers
- IDs are included in logs and propagated to Python via configured correlation header

## Local Run

1. Set Python service path: `setx PY_ML_SERVICE_DIR "C:\path\to\python-service"`
2. From backend repo, run:
   - `powershell -ExecutionPolicy Bypass -File .\scripts\start-backend-with-ml.ps1`

Or start Python only:

- `powershell -ExecutionPolicy Bypass -File .\scripts\start-python-ml.ps1`

Docker compose example is provided in `docker-compose.ml.yml`.
