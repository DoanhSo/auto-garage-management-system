[CmdletBinding()]
param([string]$OutputDirectory = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'docker-common.ps1')
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root 'release/AutoGarage-Docker' }
$config = Join-Path $root '.docker-local/.env'
$settings = Read-DockerSettings $config
$output = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $output) { throw 'Output directory already exists; choose a new path to preserve the previous package.' }
[IO.Directory]::CreateDirectory($output) | Out-Null
foreach ($directory in @('scripts','docker/keycloak')) { [IO.Directory]::CreateDirectory((Join-Path $output $directory)) | Out-Null }
$compose = [IO.File]::ReadAllText((Join-Path $root 'compose.yaml')).Replace("`r`n","`n")
$compose = [regex]::Replace($compose, '(?m)^    build:\n(?:      [^\n]*\n)+', '')
[IO.File]::WriteAllText((Join-Path $output 'compose.yaml'), $compose, [Text.UTF8Encoding]::new($false))
Copy-Item -LiteralPath (Join-Path $root 'start-docker.cmd') -Destination $output
Copy-Item -LiteralPath (Join-Path $root 'docs/DOCKER.md') -Destination (Join-Path $output 'README.md')
foreach ($name in @('docker-common.ps1','start-docker.ps1','seed-demo.ps1')) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $output 'scripts')
}
foreach ($name in @('realm-export.json','healthcheck.sh','setup.sh','user-profile.json','auth-version-mapper.json')) {
    Copy-Item -LiteralPath (Join-Path $root "docker/keycloak/$name") -Destination (Join-Path $output 'docker/keycloak')
}
$images = @("autogarage-backend:$($settings.AUTOGARAGE_IMAGE_TAG)","autogarage-frontend:$($settings.AUTOGARAGE_IMAGE_TAG)",'mysql:8.4','postgres:16-alpine','quay.io/keycloak/keycloak:26.0')
$manifest = [ordered]@{ createdAt=[DateTime]::UtcNow.ToString('o'); platform='linux/amd64'; images=$images; includesSecrets=$false }
[IO.File]::WriteAllText((Join-Path $output 'manifest.json'), ($manifest | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipPath = $output + '.zip'
if (Test-Path -LiteralPath $zipPath) { throw 'Zip already exists; previous archive preserved.' }
$temporaryZip = $zipPath + '.incomplete'
if (Test-Path -LiteralPath $temporaryZip) { throw 'An incomplete export exists; choose a new output path.' }
$zipFile = [IO.File]::Create($temporaryZip)
$archive = [IO.Compression.ZipArchive]::new($zipFile, [IO.Compression.ZipArchiveMode]::Create, $false)
$dockerProcess = [Diagnostics.Process]::new()
try {
    foreach ($file in Get-ChildItem -LiteralPath $output -File -Recurse) {
        $relative = $file.FullName.Substring($output.Length + 1).Replace([IO.Path]::DirectorySeparatorChar, [char]'/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $relative, [IO.Compression.CompressionLevel]::Fastest) | Out-Null
    }
    # Stream the binary image archive straight into ZIP. Do not create a second multi-GB tar.
    $entry = $archive.CreateEntry('images.tar', [IO.Compression.CompressionLevel]::Fastest)
    $entryStream = $entry.Open()
    try {
        $dockerProcess.StartInfo = [Diagnostics.ProcessStartInfo]::new()
        $dockerProcess.StartInfo.FileName = 'docker'
        $dockerProcess.StartInfo.Arguments = 'save ' + ($images -join ' ')
        $dockerProcess.StartInfo.UseShellExecute = $false
        $dockerProcess.StartInfo.CreateNoWindow = $true
        $dockerProcess.StartInfo.RedirectStandardOutput = $true
        $dockerProcess.StartInfo.RedirectStandardError = $true
        $null = $dockerProcess.Start()
        $exportErrors = $dockerProcess.StandardError.ReadToEndAsync()
        $dockerProcess.StandardOutput.BaseStream.CopyTo($entryStream)
        $dockerProcess.WaitForExit()
        if ($dockerProcess.ExitCode -ne 0) { throw ('Could not export Docker images: ' + $exportErrors.Result) }
    } finally { $entryStream.Dispose() }
} finally {
    $dockerProcess.Dispose()
    $archive.Dispose()
    $zipFile.Dispose()
}
Move-Item -LiteralPath $temporaryZip -Destination $zipPath
$hash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText($zipPath + '.sha256', $hash + '  ' + [IO.Path]::GetFileName($zipPath) + "`n")
Write-Output "Docker bundle: $zipPath"
Write-Output "SHA256: $hash"
