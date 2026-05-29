# Setup Maven Settings to Fix SSL Issue
Write-Host "Setting up Maven settings to fix SSL certificate issue..." -ForegroundColor Green

$m2Dir = "$env:USERPROFILE\.m2"
$settingsFile = Join-Path $m2Dir "settings.xml"

# Create .m2 directory if it doesn't exist
if (-not (Test-Path $m2Dir)) {
    Write-Host "Creating .m2 directory: $m2Dir" -ForegroundColor Cyan
    New-Item -ItemType Directory -Path $m2Dir | Out-Null
}

# Backup existing settings if present
if (Test-Path $settingsFile) {
    $backupFile = "$settingsFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Write-Host "Backing up existing settings to: $backupFile" -ForegroundColor Yellow
    Copy-Item $settingsFile $backupFile
}

# Create new settings.xml
$settingsContent = @'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <mirrors>
        <mirror>
            <id>insecure-central</id>
            <mirrorOf>central</mirrorOf>
            <name>Insecure Central Repository (HTTP)</name>
            <url>http://insecure.repo1.maven.org/maven2</url>
        </mirror>
    </mirrors>
</settings>
'@

Write-Host "Writing settings.xml to: $settingsFile" -ForegroundColor Cyan
$settingsContent | Out-File -FilePath $settingsFile -Encoding UTF8

Write-Host "`n✓ Maven settings configured successfully!" -ForegroundColor Green
Write-Host "`nSettings file location: $settingsFile" -ForegroundColor Cyan
Write-Host "`nNow you can run:" -ForegroundColor Yellow
Write-Host "  mvn clean spring-boot:run" -ForegroundColor White
Write-Host "`nor from api-module directory:" -ForegroundColor Yellow
Write-Host "  cd api-module" -ForegroundColor White
Write-Host "  mvn spring-boot:run" -ForegroundColor White
