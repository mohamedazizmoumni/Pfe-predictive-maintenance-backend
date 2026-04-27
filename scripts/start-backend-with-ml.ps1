param(
    [string]$MlServiceDir = $env:PY_ML_SERVICE_DIR,
    [string]$PythonExe = "python",
    [int]$MlPort = 8000
)

if ([string]::IsNullOrWhiteSpace($MlServiceDir)) {
    Write-Error "Set PY_ML_SERVICE_DIR or pass -MlServiceDir"
    exit 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$mlScript = Join-Path $PSScriptRoot "start-python-ml.ps1"

$mlProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", "`"$mlScript`"", "-MlServiceDir", "`"$MlServiceDir`"", "-PythonExe", "`"$PythonExe`"", "-Port", "$MlPort" -PassThru

Write-Host "Started Python ML service process with PID $($mlProcess.Id)"
Write-Host "Starting Spring Boot backend..."

Push-Location $repoRoot
try {
    $env:ML_SERVICE_BASE_URL = "http://127.0.0.1:$MlPort"
    mvn -pl api-module spring-boot:run
} finally {
    Pop-Location
}
