param(
    [switch]$SkipTests
)

$repoRoot = Split-Path -Parent $PSScriptRoot

Push-Location $repoRoot
try {
    $mavenArgs = @('-f', "$repoRoot\pom.xml", '-pl', 'api-module', '-am')
    if ($SkipTests) {
        $mavenArgs += '-DskipTests'
    }
    $mavenArgs += 'spring-boot:run'

    mvn @mavenArgs
} finally {
    Pop-Location
}