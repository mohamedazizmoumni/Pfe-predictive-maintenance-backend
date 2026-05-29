# Download WebSocket Dependencies
Write-Host "Downloading WebSocket transitive dependencies..." -ForegroundColor Green

$m2Repo = "$env:USERPROFILE\.m2\repository"

# Dependencies to download
$deps = @(
    @{
        group = "org/springframework"
        artifact = "spring-messaging"
        version = "6.1.1"
    },
    @{
        group = "org/springframework"
        artifact = "spring-websocket"
        version = "6.1.1"
    }
)

foreach ($dep in $deps) {
    $groupPath = "$($dep.group)/$($dep.artifact)/$($dep.version)"
    $targetDir = Join-Path $m2Repo $groupPath
    $artifactName = "$($dep.artifact)-$($dep.version)"
    
    # Create directory
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    
    Write-Host "`nDownloading $($dep.artifact)..." -ForegroundColor Cyan
    
    # Download POM
    $pomUrl = "https://repo1.maven.org/maven2/$groupPath/$artifactName.pom"
    $pomFile = Join-Path $targetDir "$artifactName.pom"
    curl.exe -k -L -o $pomFile $pomUrl
    
    if (Test-Path $pomFile) {
        Write-Host "  POM: OK" -ForegroundColor Green
    }
    
    # Download JAR
    $jarUrl = "https://repo1.maven.org/maven2/$groupPath/$artifactName.jar"
    $jarFile = Join-Path $targetDir "$artifactName.jar"
    curl.exe -k -L -o $jarFile $jarUrl
    
    if (Test-Path $jarFile) {
        $size = (Get-Item $jarFile).Length
        Write-Host "  JAR: OK ($size bytes)" -ForegroundColor Green
    }
}

Write-Host "`nAll dependencies downloaded!" -ForegroundColor Yellow
Write-Host "Now try: mvn clean compile -DskipTests -o" -ForegroundColor Yellow
