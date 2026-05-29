# Fix Maven HTTP Blocker Issue
Write-Host "Fixing Maven HTTP blocker issue..." -ForegroundColor Green

$m2Dir = "$env:USERPROFILE\.m2"
$settingsFile = Join-Path $m2Dir "settings.xml"

# Create .m2 directory if it doesn't exist
if (-not (Test-Path $m2Dir)) {
    New-Item -ItemType Directory -Path $m2Dir | Out-Null
}

# Backup existing settings
if (Test-Path $settingsFile) {
    $backupFile = "$settingsFile.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Write-Host "Backing up existing settings to: $backupFile" -ForegroundColor Yellow
    Copy-Item $settingsFile $backupFile
}

# Create settings.xml that allows HTTP and uses alternative mirrors
$settingsContent = @'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <mirrors>
        <mirror>
            <id>aliyun-central</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Central</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </mirror>
        
        <mirror>
            <id>maven-default-http-blocker</id>
            <mirrorOf>dummy</mirrorOf>
            <name>Dummy mirror to override default blocking mirror</name>
            <url>http://0.0.0.0/</url>
            <blocked>false</blocked>
        </mirror>
    </mirrors>
</settings>
'@

Write-Host "Writing new settings.xml..." -ForegroundColor Cyan
$settingsContent | Out-File -FilePath $settingsFile -Encoding UTF8

Write-Host ""
Write-Host "Maven settings fixed!" -ForegroundColor Green
Write-Host ""
Write-Host "Settings file: $settingsFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now try: mvn clean install" -ForegroundColor Yellow
