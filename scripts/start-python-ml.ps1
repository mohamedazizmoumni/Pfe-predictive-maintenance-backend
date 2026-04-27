param(
    [string]$MlServiceDir = $env:PY_ML_SERVICE_DIR,
    [string]$PythonExe = "python",
    [int]$Port = 8000
)

if ([string]::IsNullOrWhiteSpace($MlServiceDir)) {
    Write-Error "Set PY_ML_SERVICE_DIR or pass -MlServiceDir to point to the Python ML service folder containing app.py"
    exit 1
}

if (-not (Test-Path "$MlServiceDir\app.py")) {
    Write-Error "app.py not found under $MlServiceDir"
    exit 1
}

Push-Location $MlServiceDir
try {
    if (Test-Path "requirements.txt") {
        & $PythonExe -m pip install -r requirements.txt
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to install Python dependencies"
        }
    }

    & $PythonExe -m uvicorn app:app --host 127.0.0.1 --port $Port
} finally {
    Pop-Location
}
