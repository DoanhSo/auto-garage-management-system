Add-Type -AssemblyName System.Drawing

function Optimize-Image {
    param(
        [string]$SourcePath,
        [string]$DestPath,
        [int]$MaxWidth,
        [int]$Quality = 90
    )
    if (-not (Test-Path $SourcePath)) {
        Write-Error "Source file not found: $SourcePath"
        return
    }

    $img = [System.Drawing.Image]::FromFile($SourcePath)
    $origW = $img.Width
    $origH = $img.Height
    $targetW = $origW
    $targetH = $origH

    if ($targetW -gt $MaxWidth) {
        $targetH = [int]($origH * ($MaxWidth / $origW))
        $targetW = $MaxWidth
    }

    $bmp = New-Object System.Drawing.Bitmap($targetW, $targetH)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $rect = New-Object System.Drawing.Rectangle(0, 0, $targetW, $targetH)
    $g.DrawImage($img, $rect)

    $img.Dispose()
    $g.Dispose()

    $codec = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq 'image/jpeg' }
    $encoderParams = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $encoderParams.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, [long]$Quality)

    $bmp.Save($DestPath, $codec, $encoderParams)
    $bmp.Dispose()

    $item = Get-Item $DestPath
    Write-Host "Processed: $(Split-Path $SourcePath -Leaf) -> $(Split-Path $DestPath -Leaf) ($targetW x $targetH, $([math]::Round($item.Length / 1KB, 1)) KB)"
}

$sourceDir = "C:\Users\mrdoanh\Downloads\anhlandingpage"
$targetDir = "c:\Users\mrdoanh\IdeaProjects\AutoGarage\AutoGarage\frontend\public\images\home"
$imagesDir = "c:\Users\mrdoanh\IdeaProjects\AutoGarage\AutoGarage\frontend\public\images"

$mapping = @(
    @{ Src = "anh1 (2).png"; Dest = "hero-banner.jpg"; MaxW = 1920; Q = 90 },
    @{ Src = "anh2.png";     Dest = "service-periodic.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh3 (2).png"; Dest = "service-alignment.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh4.png";     Dest = "service-engine.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh5.png";     Dest = "service-diagnostic.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh6.png";     Dest = "service-paint.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh7.png";     Dest = "service-brake.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh8.png";     Dest = "bay-01.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh9.png";     Dest = "bay-02.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh10.png";    Dest = "bay-03.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh11.png";    Dest = "bay-04.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh12.png";    Dest = "bay-05.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh13.png";    Dest = "bay-06.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh14.png";    Dest = "parts-ecosystem.jpg"; MaxW = 1200; Q = 88 },
    @{ Src = "anh15.png";    Dest = "garage-lounge.jpg"; MaxW = 1920; Q = 90 }
)

foreach ($m in $mapping) {
    $src = Join-Path $sourceDir $m.Src
    $dst = Join-Path $targetDir $m.Dest
    Optimize-Image -SourcePath $src -DestPath $dst -MaxWidth $m.MaxW -Quality $m.Q
}

# Also update garage-login.jpg with anh15 (Lounge)
$src15 = Join-Path $sourceDir "anh15.png"
$dstLogin = Join-Path $imagesDir "garage-login.jpg"
Optimize-Image -SourcePath $src15 -DestPath $dstLogin -MaxWidth 1920 -Quality 90

Write-Host "All 15 images processed successfully!"
