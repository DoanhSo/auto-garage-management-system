[CmdletBinding()]
param(
    [string]$OutputPath = (Join-Path (Split-Path -Parent $PSScriptRoot) '.env'),
    [switch]$WithBootstrapAdministrator,
    [ValidatePattern('^[A-Za-z0-9._-]{3,100}$')][string]$BootstrapUsername = 'garage.admin',
    [string]$BootstrapIdentityId = ''
)
$ErrorActionPreference = 'Stop'
$taskOutputPath = [System.IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $taskOutputPath) {
    Write-Output "Environment file already exists; preserved: $taskOutputPath"
    return
}
function New-LocalSecret([int]$ByteCount) {
    $taskBytes = New-Object byte[] $ByteCount
    $taskGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $taskGenerator.GetBytes($taskBytes) } finally { $taskGenerator.Dispose() }
    return [Convert]::ToBase64String($taskBytes)
}
$taskDbPassword = New-LocalSecret 24
$taskRootPassword = New-LocalSecret 24
$taskKeycloakDbPassword = New-LocalSecret 24
$taskKeycloakAdminPassword = New-LocalSecret 24
$taskSettings = @(
    '# Local secrets. Never commit or share this file.',
    "DB_PASSWORD=$taskDbPassword",
    "MYSQL_ROOT_PASSWORD=$taskRootPassword",
    'MYSQL_PORT=3307',
    'KEYCLOAK_PORT=8081',
    'KEYCLOAK_DB_USERNAME=keycloak',
    "KEYCLOAK_DB_PASSWORD=$taskKeycloakDbPassword",
    'KEYCLOAK_ADMIN_USERNAME=admin',
    "KEYCLOAK_ADMIN_PASSWORD=$taskKeycloakAdminPassword",
    'DB_URL=jdbc:mysql://localhost:3307/autogarage?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true',
    'DB_USERNAME=autogarage',
    'SERVER_PORT=8080',
    'KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/autogarage-realm',
    'KEYCLOAK_JWKS_URI=http://localhost:8081/realms/autogarage-realm/protocol/openid-connect/certs',
    'AUTH_JWT_AUDIENCE=autogarage-api',
    'AUTH_JWT_CLOCK_SKEW=0s',
    'AUTH_CORS_ORIGINS=http://localhost:5173',
    ('AUTH_BOOTSTRAP_ENABLED=' + $WithBootstrapAdministrator.IsPresent.ToString().ToLowerInvariant()),
    ('AUTH_BOOTSTRAP_USERNAME=' + $(if ($WithBootstrapAdministrator) { $BootstrapUsername } else { '' })),
    ('AUTH_BOOTSTRAP_FULL_NAME=' + $(if ($WithBootstrapAdministrator) { 'Garage Administrator' } else { '' })),
    "AUTH_BOOTSTRAP_IDENTITY_ID=$BootstrapIdentityId",
    'OPENAPI_ENABLED=true',
    'NOTIFICATION_WORKER_ENABLED=false',
    'NOTIFICATION_REMINDERS_ENABLED=false',
    'NOTIFICATION_EMAIL_ENABLED=false',
    'NOTIFICATION_SMS_ENABLED=false'
)
[System.IO.File]::WriteAllText($taskOutputPath, ($taskSettings -join [Environment]::NewLine) + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
Write-Output "Created local environment: $taskOutputPath"
Write-Output 'Secrets were generated locally and were not printed. Read the private file for bootstrap credentials if enabled.'
