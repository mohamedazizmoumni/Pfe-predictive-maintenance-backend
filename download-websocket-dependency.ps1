# Download WebSocket Dependency Manually
Write-Host "Downloading Spring Boot WebSocket dependency manually..." -ForegroundColor Green

$version = "3.2.0"
$m2Repo = "$env:USERPROFILE\.m2\repository"
$groupPath = "org/springframework/boot/spring-boot-starter-websocket/$version"
$targetDir = Join-Path $m2Repo $groupPath

# Create directory
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

# Base URL (using HTTP mirror)
$baseUrl = "https://repo1.maven.org/maven2/$groupPath"

# Files to download
$files = @(
    "spring-boot-starter-websocket-$version.pom",
    "spring-boot-starter-websocket-$version.jar"
)

foreach ($file in $files) {
    $url = "$baseUrl/$file"
    $output = Join-Path $targetDir $file
    
    Write-Host "Downloading $file..." -ForegroundColor Cyan
    
    try {
        # Disable SSL verification for PowerShell
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = {$true}
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12
        
        Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
        Write-Host "  Downloaded: $file" -ForegroundColor Green
    } catch {
        Write-Host "  Failed to download $file : $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Download complete! Now try: mvn clean compile -DskipTests" -ForegroundColor Yellow
