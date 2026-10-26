$ErrorActionPreference = 'Stop'
function Read-DockerSettings([string]$Path) {
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines($Path)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) { continue }
        $pair = $line.Split(@('='), 2, [StringSplitOptions]::None)
        if ($pair.Count -ne 2 -or $pair[0] -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid Docker settings entry' }
        $values[$pair[0]] = $pair[1]
    }
    return $values
}
function Invoke-DockerCompose([string]$Root, [string]$Config, [string[]]$Arguments) {
    $settings = Read-DockerSettings $Config
    $previous = @{}
    try {
        foreach ($key in $settings.Keys) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, $settings[$key], 'Process')
        }
        & docker compose --project-directory $Root --file (Join-Path $Root 'compose.yaml') --env-file $Config @Arguments
        if ($LASTEXITCODE -ne 0) { throw 'Docker Compose failed. Check the service status/logs; local data was preserved.' }
    } finally {
        foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    }
}
function New-DockerConfiguration {
    param([string]$Root, [string]$Config, [int]$FrontendPort = 0, [int]$BackendPort = 18080, [int]$KeycloakPort = 18081)
    if (Test-Path -LiteralPath $Config) { return }
    $dockerPorts = (& docker ps --format '{{.Ports}}') -join ' '
    $reservedPorts = @([regex]::Matches($dockerPorts, ':(\d+)->') | ForEach-Object { [int]$_.Groups[1].Value })
    function Find-FreeDockerPort([int]$First) {
        for ($candidate = $First; $candidate -lt $First + 50; $candidate++) {
            if ($candidate -in $reservedPorts) { continue }
            $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $candidate)
            $listener.Server.ExclusiveAddressUse = $true
            try { $listener.Start(); return $candidate } catch [Net.Sockets.SocketException] { } finally { $listener.Stop() }
        }
        throw 'No free local port found; provide explicit port values.'
    }
    function New-DockerSecret {
        $bytes = New-Object byte[] 24
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        return [Convert]::ToBase64String($bytes).Replace('+','-').Replace('/','_').TrimEnd('=')
    }
    $frontend = Find-FreeDockerPort $(if ($FrontendPort -gt 0) { $FrontendPort } else { 3000 })
    $backend = Find-FreeDockerPort $BackendPort
    $keycloak = Find-FreeDockerPort ([Math]::Max($KeycloakPort, $backend + 1))
    $mysql = Find-FreeDockerPort 13307
    $settings = @(
        '# Generated locally. Do not share this file.',
        'COMPOSE_PROJECT_NAME=autogarage-demo',
        'AUTOGARAGE_IMAGE_TAG=2026.10.26.1',
        'PUBLIC_HOST=localhost',
        "FRONTEND_PORT=$frontend", "BACKEND_PORT=$backend", "KEYCLOAK_PORT=$keycloak", "MYSQL_PORT=$mysql",
        'DB_USERNAME=autogarage', ('DB_PASSWORD=' + (New-DockerSecret)),
        ('MYSQL_ROOT_PASSWORD=' + (New-DockerSecret)),
        'KEYCLOAK_DB_USERNAME=keycloak', ('KEYCLOAK_DB_PASSWORD=' + (New-DockerSecret)),
        'KEYCLOAK_ADMIN_USERNAME=admin', ('KEYCLOAK_ADMIN_PASSWORD=' + (New-DockerSecret)),
        'AUTH_BOOTSTRAP_ENABLED=true', 'AUTH_BOOTSTRAP_USERNAME=garage.admin',
        'AUTH_BOOTSTRAP_FULL_NAME=Garage Administrator',
        ('GARAGE_ADMIN_PASSWORD=' + (New-DockerSecret))
    )
    [IO.Directory]::CreateDirectory((Split-Path -Parent $Config)) | Out-Null
    [IO.File]::WriteAllText($Config, ($settings -join "`n") + "`n", [Text.UTF8Encoding]::new($false))
}
