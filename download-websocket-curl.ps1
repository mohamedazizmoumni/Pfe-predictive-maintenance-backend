# Download WebSocket Dependency using curl
Write-Host "Downloading Spring Boot WebSocket dependency using curl..." -ForegroundColor Green

$version = "3.2.0"
$m2Repo = "$env:USERPROFILE\.m2\repository"
$groupPath = "org/springframework/boot/spring-boot-starter-websocket/$version"
$targetDir = Join-Path $m2Repo $groupPath

# Create directory
New-Item -ItemType Directory -Path $targetDir -Force | Out-Null

# Download POM file
$pomUrl = "https://repo1.maven.org/maven2/$groupPath/spring-boot-starter-websocket-$version.pom"
$pomFile = Join-Path $targetDir "spring-boot-starter-websocket-$version.pom"

Write-Host "Downloading POM file..." -ForegroundColor Cyan
curl.exe -k -L -o $pomFile $pomUrl

if (Test-Path $pomFile) {
    Write-Host "  POM downloaded successfully!" -ForegroundColor Green
} else {
    Write-Host "  POM download failed!" -ForegroundColor Red
}

# Download JAR file
$jarUrl = "https://repo1.maven.org/maven2/$groupPath/spring-boot-starter-websocket-$version.jar"
$jarFile = Join-Path $targetDir "spring-boot-starter-websocket-$version.jar"

Write-Host "Downloading JAR file..." -ForegroundColor Cyan
curl.exe -k -L -o $jarFile $jarUrl

if (Test-Path $jarFile) {
    Write-Host "  JAR downloaded successfully!" -ForegroundColor Green
    Write-Host "  Size: $((Get-Item $jarFile).Length) bytes" -ForegroundColor Cyan
} else {
    Write-Host "  JAR download failed!" -ForegroundColor Red
}

Write-Host ""
Write-Host "Download complete! Files saved to:" -ForegroundColor Yellow
Write-Host "  $targetDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now try: mvn clean compile -DskipTests -o" -ForegroundColor Yellow
