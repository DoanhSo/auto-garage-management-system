[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$ConfigPath = '',
    [int]$FrontendPort = 0,
    [int]$BackendPort = 18080,
    [int]$KeycloakPort = 18081,
    [switch]$NoBrowser,
    [switch]$NoDemoData
)
$ErrorActionPreference = 'Stop'
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent $scriptDirectory }
. (Join-Path $scriptDirectory 'docker-common.ps1')
$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
if (-not $ConfigPath) { $ConfigPath = Join-Path $ProjectRoot '.docker-local/.env' }
$ConfigPath = [IO.Path]::GetFullPath($ConfigPath)
& docker info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Start Docker Desktop (Linux containers) and run this launcher again.' }
New-DockerConfiguration -Root $ProjectRoot -Config $ConfigPath -FrontendPort $FrontendPort -BackendPort $BackendPort -KeycloakPort $KeycloakPort
$manifest = $null
$manifestPath = Join-Path $ProjectRoot 'manifest.json'
if (Test-Path -LiteralPath $manifestPath) {
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    $backendImage = @($manifest.images | Where-Object { $_ -match '^autogarage-backend:[A-Za-z0-9._-]+$' })
    if ($backendImage.Count -ne 1) { throw 'Bundle manifest must identify one backend image.' }
    $bundleTag = $backendImage[0].Substring('autogarage-backend:'.Length)
    if (@($manifest.images | Where-Object { $_ -eq "autogarage-frontend:$bundleTag" }).Count -ne 1) { throw 'Bundle frontend/backend versions do not match.' }
    $current = Read-DockerSettings $ConfigPath
    if ($current.AUTOGARAGE_IMAGE_TAG -ne $bundleTag) {
        $lines = @([IO.File]::ReadAllLines($ConfigPath) | Where-Object { -not $_.StartsWith('AUTOGARAGE_IMAGE_TAG=') })
        $lines += "AUTOGARAGE_IMAGE_TAG=$bundleTag"
        [IO.File]::WriteAllText($ConfigPath, ($lines -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
        Write-Output "Updated bundle image version to $bundleTag; local credentials and data preserved."
    }
}
$settings = Read-DockerSettings $ConfigPath
$missingImages = $false
$images = @("autogarage-backend:$($settings.AUTOGARAGE_IMAGE_TAG)", "autogarage-frontend:$($settings.AUTOGARAGE_IMAGE_TAG)", 'mysql:8.4', 'postgres:16-alpine', 'quay.io/keycloak/keycloak:26.0')
foreach ($image in $images) {
    $available = & docker image ls --filter "reference=$image" --format '{{.ID}}'
    if ($LASTEXITCODE -ne 0 -or -not $available) { $missingImages = $true; continue }
    $available = & docker image inspect $image --format '{{.Id}}'
    $expected = if ($manifest -and $manifest.imageIds) { $manifest.imageIds.PSObject.Properties[$image].Value } else { $null }
    if ($LASTEXITCODE -ne 0 -or -not $available -or ($expected -and $available.Trim() -ne $expected)) { $missingImages = $true }
}
if ($missingImages) {
    $archive = Join-Path $ProjectRoot 'images.tar'
    if (-not (Test-Path -LiteralPath $archive)) { throw 'Images are missing. Extract the complete Docker bundle, or run scripts/build-docker.ps1 from source first.' }
    & docker load --input $archive
    if ($LASTEXITCODE -ne 0) { throw 'Could not load bundled Docker images.' }
}
foreach ($image in $images) {
    $expected = if ($manifest -and $manifest.imageIds) { $manifest.imageIds.PSObject.Properties[$image].Value } else { $null }
    $actual = & docker image inspect $image --format '{{.Id}}' 2>$null
    if ($LASTEXITCODE -ne 0 -or ($expected -and $actual.Trim() -ne $expected)) { throw ('Docker image does not match the bundle manifest: ' + $image) }
}
Invoke-DockerCompose -Root $ProjectRoot -Config $ConfigPath -Arguments @('config','--quiet')
Invoke-DockerCompose -Root $ProjectRoot -Config $ConfigPath -Arguments @('up','-d','--no-build','--pull','never','--wait','--wait-timeout','360')
if (-not $NoDemoData) { & (Join-Path $scriptDirectory 'seed-demo.ps1') -ConfigPath $ConfigPath }
$appUrl = "http://localhost:$($settings.FRONTEND_PORT)"
$credentials = Join-Path (Split-Path -Parent $ConfigPath) 'credentials.txt'
$text = @(
    "AutoGarage: $appUrl", "Username: $($settings.AUTH_BOOTSTRAP_USERNAME)", "Password: $($settings.GARAGE_ADMIN_PASSWORD)", '',
    "Keycloak admin: http://localhost:$($settings.KEYCLOAK_PORT)/admin/", "Username: $($settings.KEYCLOAK_ADMIN_USERNAME)", "Password: $($settings.KEYCLOAK_ADMIN_PASSWORD)", '',
    'Local demo credentials. Keep this file private; do not include it in exported bundles.'
)
[IO.File]::WriteAllText($credentials, ($text -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
Write-Output "Ready: $appUrl"
Write-Output "Login details: $credentials"
Write-Output "Swagger: http://localhost:$($settings.BACKEND_PORT)/swagger-ui/index.html"
if (-not $NoBrowser) { Start-Process $appUrl }
