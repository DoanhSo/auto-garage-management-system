[CmdletBinding()]
param([switch]$OfflineMaven)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'docker-common.ps1')
$root = Split-Path -Parent $PSScriptRoot
$config = Join-Path $root '.docker-local/.env'
New-DockerConfiguration -Root $root -Config $config
$settings = Read-DockerSettings $config
Push-Location $root
$previousTest = @{}
try {
    Invoke-DockerCompose -Root $root -Config $config -Arguments @('--profile','test','up','-d','--wait','--force-recreate','--renew-anon-volumes','mysql-test')
    foreach ($entry in @{
        TEST_DB_URL='jdbc:mysql://127.0.0.1:3308/autogarage_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true';
        TEST_DB_USERNAME='autogarage'; TEST_DB_PASSWORD='autogarage_test';
        VITE_API_BASE_URL='/api/v1'; VITE_KEYCLOAK_ISSUER='/realms/autogarage-realm';
        VITE_KEYCLOAK_CLIENT_ID='autogarage-frontend'; VITE_GOOGLE_LOGIN_ENABLED='false'
    }.GetEnumerator()) {
        $previousTest[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key,'Process')
        [Environment]::SetEnvironmentVariable($entry.Key,$entry.Value,'Process')
    }
    $mavenArgs = @('-B','-ntp')
    if ($OfflineMaven) { $mavenArgs += '-o' }
    $mavenArgs += 'package'
    & .\mvnw.cmd @mavenArgs *> (Join-Path $root '.docker-local/build-backend.log')
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests/package failed; inspect .docker-local/build-backend.log.' }
    if (-not (Test-Path -LiteralPath (Join-Path $root 'frontend/node_modules'))) {
        & npm --prefix frontend ci *> (Join-Path $root '.docker-local/install-frontend.log')
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed; inspect .docker-local/install-frontend.log.' }
    }
    & npm --prefix frontend run check *> (Join-Path $root '.docker-local/build-frontend.log')
    if ($LASTEXITCODE -ne 0) { throw 'Frontend check/build failed; inspect .docker-local/build-frontend.log.' }
    Invoke-DockerCompose -Root $root -Config $config -Arguments @('build','backend','frontend')
    Write-Output "Built tested images: autogarage-backend:$($settings.AUTOGARAGE_IMAGE_TAG), autogarage-frontend:$($settings.AUTOGARAGE_IMAGE_TAG)"
} finally {
    foreach ($key in $previousTest.Keys) { [Environment]::SetEnvironmentVariable($key,$previousTest[$key],'Process') }
    Pop-Location
}
