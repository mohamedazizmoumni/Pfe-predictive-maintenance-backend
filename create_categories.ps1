# PowerShell script to create all default categories via API
# Run this if migration V38 hasn't been applied yet

Write-Host "Creating default inventory categories..." -ForegroundColor Cyan

# Step 1: Login and get token
Write-Host "`n[1/2] Logging in as stock manager..." -ForegroundColor Yellow
try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body '{"email":"stockmanager@predictive-maintenance.local","password":"stockmanager"}'
    
    $token = $loginResponse.token
    Write-Host "✓ Login successful!" -ForegroundColor Green
}
catch {
    Write-Host "✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure the application is running on http://localhost:8080" -ForegroundColor Yellow
    exit 1
}

# Step 2: Create categories
Write-Host "`n[2/2] Creating categories..." -ForegroundColor Yellow

$categories = @(
    "Bearings",
    "Belts",
    "Motors",
    "Sensors",
    "Filters",
    "Lubricants",
    "Electrical",
    "Hydraulic",
    "Pneumatic",
    "Mechanical",
    "Safety Equipment",
    "Tools",
    "Fasteners",
    "Seals",
    "Gaskets"
)

$successCount = 0
$failCount = 0

foreach ($category in $categories) {
    try {
        $body = @{name = $category} | ConvertTo-Json
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/inventory/categories" `
            -Method POST `
            -Headers @{Authorization = "Bearer $token"} `
            -ContentType "application/json" `
            -Body $body
        Write-Host "  ✓ $category" -ForegroundColor Green
        $successCount++
    }
    catch {
        $errorMessage = $_.Exception.Message
        if ($errorMessage -like "*already exists*") {
            Write-Host "  ⊙ $category (already exists)" -ForegroundColor Gray
        }
        else {
            Write-Host "  ✗ $category - $errorMessage" -ForegroundColor Red
            $failCount++
        }
    }
}

# Summary
Write-Host "`n" + ("=" * 50) -ForegroundColor Cyan
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Created: $successCount" -ForegroundColor Green
Write-Host "  Failed: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Gray" })
Write-Host ("=" * 50) -ForegroundColor Cyan

if ($successCount -gt 0) {
    Write-Host "`n✓ Categories are ready! You can now create parts." -ForegroundColor Green
}
else {
    Write-Host "`n⚠ No new categories were created. They may already exist." -ForegroundColor Yellow
}

Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
