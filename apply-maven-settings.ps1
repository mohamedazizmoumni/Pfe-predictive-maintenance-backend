# Apply Maven Settings with SSL Disabled
Write-Host "Applying Maven settings with SSL verification disabled..." -ForegroundColor Green

$sourceFile = "maven-settings-disable-ssl.xml"
$m2Dir = "$env:USERPROFILE\.m2"
$targetFile = Join-Path $m2Dir "settings.xml"

# Create .m2 directory if it doesn't exist
if (-not (Test-Path $m2Dir)) {
    New-Item -ItemType Directory -Path $m2Dir | Out-Null
    Write-Host "Created .m2 directory" -ForegroundColor Cyan
}

# Backup existing settings
if (Test-Path $targetFile) {
    $backupFile = "$targetFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Write-Host "Backing up existing settings to: $backupFile" -ForegroundColor Yellow
    Copy-Item $targetFile $backupFile
}

# Copy new settings
Copy-Item $sourceFile $targetFile -Force
Write-Host ""
Write-Host "Maven settings applied successfully!" -ForegroundColor Green
Write-Host "Settings file: $targetFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now try: mvn clean install -DskipTests" -ForegroundColor Yellow
