[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ConfigPath)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$resolvedConfig = (Resolve-Path -LiteralPath $ConfigPath).Path
$compose = @('compose','--project-directory',$root,'--env-file',$resolvedConfig)
$privateDirectory = Join-Path $root '.docker-local'
$null = New-Item -ItemType Directory -Path $privateDirectory -Force
$backup = Join-Path $privateDirectory ('before-report-demo-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.sql')
$previousEncoding = $OutputEncoding
try {
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    & docker @compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces "$MYSQL_DATABASE"' |
        Set-Content -LiteralPath $backup -Encoding UTF8
    if ($LASTEXITCODE -ne 0) { throw 'Local database backup failed; no demo data was inserted.' }
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'seed-report-demo.sql') -Raw -Encoding UTF8 |
        & docker @compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" -D"$MYSQL_DATABASE" --default-character-set=utf8mb4 --batch'
    if ($LASTEXITCODE -ne 0) { throw 'Report demo seed failed; inspect the SQL error. The transaction was rolled back.' }
    Write-Output 'Report demo ready. DEMO-RPT records were preserved on replay; no login credentials were created.'
    Write-Output "Private pre-seed backup: $backup"
} finally {
    $OutputEncoding = $previousEncoding
}
