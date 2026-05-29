# Fix Maven SSL Certificate Issue
# This script downloads and imports the Maven Central certificate

Write-Host "Fixing Maven SSL Certificate Issue..." -ForegroundColor Green

# Find Java home
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($javaHome) {
        $javaHome = Split-Path (Split-Path $javaHome)
    }
}

if (-not $javaHome) {
    Write-Host "ERROR: Could not find Java installation" -ForegroundColor Red
    Write-Host "Please set JAVA_HOME environment variable" -ForegroundColor Yellow
    exit 1
}

Write-Host "Java Home: $javaHome" -ForegroundColor Cyan

# Path to cacerts
$cacertsPath = Join-Path $javaHome "lib\security\cacerts"
if (-not (Test-Path $cacertsPath)) {
    $cacertsPath = Join-Path $javaHome "jre\lib\security\cacerts"
}

if (-not (Test-Path $cacertsPath)) {
    Write-Host "ERROR: Could not find cacerts file" -ForegroundColor Red
    exit 1
}

Write-Host "Cacerts Path: $cacertsPath" -ForegroundColor Cyan

# Download certificate
Write-Host "`nDownloading Maven Central certificate..." -ForegroundColor Yellow

$certUrl = "https://repo.maven.apache.org/maven2"
$certFile = "maven-central.cer"

try {
    # Use openssl to get certificate (if available)
    $opensslPath = Get-Command openssl -ErrorAction SilentlyContinue
    if ($opensslPath) {
        Write-Host "Using OpenSSL to extract certificate..." -ForegroundColor Cyan
        & openssl s_client -showcerts -connect repo.maven.apache.org:443 < /dev/null 2>&1 | openssl x509 -outform PEM > $certFile
        
        if (Test-Path $certFile) {
            Write-Host "Certificate downloaded successfully" -ForegroundColor Green
            
            # Import certificate
            Write-Host "`nImporting certificate into Java keystore..." -ForegroundColor Yellow
            Write-Host "Default password is: changeit" -ForegroundColor Cyan
            
            $keytoolPath = Join-Path $javaHome "bin\keytool.exe"
            & $keytoolPath -import -trustcacerts -alias maven-central -file $certFile -keystore $cacertsPath -storepass changeit -noprompt
            
            Write-Host "`nCertificate imported successfully!" -ForegroundColor Green
            Write-Host "You can now run: mvn spring-boot:run" -ForegroundColor Cyan
            
            # Cleanup
            Remove-Item $certFile -ErrorAction SilentlyContinue
        }
    } else {
        Write-Host "OpenSSL not found. Please install OpenSSL or use Option 1 (HTTP repository)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nAlternative: Use HTTP repository (see maven-settings.xml)" -ForegroundColor Yellow
}
