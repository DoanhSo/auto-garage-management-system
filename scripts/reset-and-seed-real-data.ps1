#requires -Version 5.1
<#
.SYNOPSIS
  Làm sạch dữ liệu demo cũ và nạp bộ dữ liệu thực tế cho AutoGarage (MySQL trong Docker).
.DESCRIPTION
  1. Kiểm tra container MySQL đang chạy và tài khoản garage.admin tồn tại.
  2. Sao lưu cơ sở dữ liệu hiện tại vào scripts/backups (bỏ qua bằng -SkipBackup).
  3. Chạy scripts/seed-production-real-data.sql (TRUNCATE các bảng nghiệp vụ, giữ flyway_schema_history).
  4. Đối chiếu số lượng bản ghi, đối soát tồn kho và quét chuỗi "DEMO".
  Thao tác này XÓA dữ liệu nghiệp vụ hiện có, nên cần -Force hoặc xác nhận gõ tay.
#>
[CmdletBinding()]
param(
    [string]$Container = 'autogarage-mysql-1',
    [string]$Database = 'autogarage',
    [switch]$SkipBackup,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$seedFile = Join-Path $scriptDir 'seed-production-real-data.sql'
$backupDir = Join-Path $scriptDir 'backups'

function Invoke-Mysql([string]$Sql) {
    # Truyền SQL qua stdin: PowerShell 5.1 làm mất dấu nháy kép khi gửi tham số cho docker.
    $shellCmd = 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot --default-character-set=utf8mb4 -N -B ' + $Database
    $out = $Sql | docker exec -i $Container sh -c $shellCmd
    if ($LASTEXITCODE -ne 0) { throw "Lệnh MySQL thất bại: $Sql" }
    return $out
}

if (-not (Test-Path $seedFile)) { throw "Không tìm thấy $seedFile" }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Chưa cài đặt hoặc chưa mở Docker.' }

$running = docker ps --filter "name=^/$Container$" --format '{{.Names}}'
if ($running -ne $Container) { throw "Container '$Container' không chạy. Khởi động Docker stack trước." }

$admin = Invoke-Mysql "SELECT COUNT(*) FROM users WHERE username = 'garage.admin'"
if ([int]$admin -lt 1) { throw "Thiếu tài khoản garage.admin trong '$Database'. Dừng để không mất quyền quản trị." }

if (-not $Force) {
    Write-Host "Sắp XÓA toàn bộ dữ liệu nghiệp vụ trong '$Database' (container $Container) và nạp dữ liệu mới." -ForegroundColor Yellow
    $answer = Read-Host "Gõ 'YES' để tiếp tục"
    if ($answer -ne 'YES') { Write-Host 'Đã hủy.'; exit 1 }
}

if (-not $SkipBackup) {
    New-Item -ItemType Directory -Force $backupDir | Out-Null
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backupFile = Join-Path $backupDir "$Database-$stamp.sql"
    docker exec $Container sh -c ('MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysqldump -uroot --single-transaction --routines --default-character-set=utf8mb4 ' + $Database + ' > /tmp/backup.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Sao lưu thất bại, dừng trước khi xóa dữ liệu.' }
    docker cp "${Container}:/tmp/backup.sql" $backupFile | Out-Null
    docker exec $Container rm -f /tmp/backup.sql | Out-Null
    Write-Host "Đã sao lưu: $backupFile" -ForegroundColor Green
}

docker cp $seedFile "${Container}:/tmp/seed-production-real-data.sql" | Out-Null
docker exec $Container sh -c ('MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot --default-character-set=utf8mb4 ' + $Database + ' < /tmp/seed-production-real-data.sql')
$seedExit = $LASTEXITCODE
docker exec $Container rm -f /tmp/seed-production-real-data.sql | Out-Null
if ($seedExit -ne 0) { throw "Nạp dữ liệu thất bại (mã $seedExit). Khôi phục từ bản sao lưu nếu cần." }
Write-Host 'Đã nạp dữ liệu thực tế.' -ForegroundColor Green

# Đối chiếu số lượng bản ghi tối thiểu cho từng bảng nghiệp vụ chính.
$expect = [ordered]@{
    users = 6; customers = 8; vehicles = 10; services = 14; service_bays = 6; suppliers = 5; parts = 15
    bookings = 6; repair_orders = 9; estimates = 9; repair_tasks = 9; vehicle_checks = 9
    inventory_documents = 6; inventory_transactions = 15; invoices = 8; payments = 8
}
$failed = $false
Write-Host ''
Write-Host ('{0,-26}{1,8}{2,10}' -f 'Bảng', 'Số dòng', 'Tối thiểu')
foreach ($table in $expect.Keys) {
    $count = [int](Invoke-Mysql "SELECT COUNT(*) FROM $table")
    $ok = $count -ge $expect[$table]
    if (-not $ok) { $failed = $true }
    Write-Host ('{0,-26}{1,8}{2,10}  {3}' -f $table, $count, $expect[$table], $(if ($ok) { 'OK' } else { 'THIẾU' })) -ForegroundColor $(if ($ok) { 'Gray' } else { 'Red' })
}

$mismatch = [int](Invoke-Mysql "SELECT COUNT(*) FROM parts p WHERE p.stock_quantity <> COALESCE((SELECT SUM(t.quantity_change) FROM inventory_transactions t WHERE t.part_id = p.id), 0)")
$demo = [int](Invoke-Mysql "SELECT (SELECT COUNT(*) FROM customers WHERE full_name LIKE '%DEMO%') + (SELECT COUNT(*) FROM vehicles WHERE license_plate LIKE '%DEMO%') + (SELECT COUNT(*) FROM parts WHERE sku LIKE '%DEMO%') + (SELECT COUNT(*) FROM repair_orders WHERE order_code LIKE '%DEMO%')")
Write-Host ''
Write-Host "Mã lệch tồn kho so với sổ kho : $mismatch" -ForegroundColor $(if ($mismatch -eq 0) { 'Green' } else { 'Red' })
Write-Host "Bản ghi còn chuỗi DEMO        : $demo" -ForegroundColor $(if ($demo -eq 0) { 'Green' } else { 'Red' })
if ($mismatch -ne 0 -or $demo -ne 0) { $failed = $true }

if ($failed) { Write-Host 'Hoàn tất nhưng có điểm chưa đạt, xem các dòng đỏ.' -ForegroundColor Red; exit 2 }
Write-Host 'Hoàn tất: dữ liệu thực tế đã sẵn sàng.' -ForegroundColor Green
