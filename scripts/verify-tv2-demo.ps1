[CmdletBinding()]
param([string]$ConfigPath = '')
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $ConfigPath) { $ConfigPath = Join-Path $root '.env' }
. (Join-Path $PSScriptRoot 'docker-common.ps1')
. (Join-Path $PSScriptRoot 'docker-oidc.ps1')
$settings = Read-DockerSettings $ConfigPath
$api = "http://localhost:$($settings.BACKEND_PORT)/api/v1"
$accounts = @(Get-Content (Join-Path $root '.docker-local/tv2-demo-accounts.json') -Raw -Encoding UTF8 | ConvertFrom-Json)
$manifest = Get-Content (Join-Path $root '.docker-local/tv2-demo-manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable
$sessions = @{}
$checks = 0

function Read-As([string]$Username, [string]$Path) {
    try {
        $response = Invoke-RestMethod -Uri ($api + $Path) -Headers $sessions[$Username] -TimeoutSec 60
        return @{Status=200;Data=$response.data}
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($status -notin @(401,403,404)) { throw "Unexpected HTTP $status when reading $Path" }
        return @{Status=$status;Data=$null}
    }
}
function Assert-Demo([bool]$Condition, [string]$Label) {
    if (-not $Condition) { throw "FAIL: $Label" }
    $script:checks++
    Write-Output "PASS: $Label"
}

foreach ($account in $accounts) {
    $personal = $settings.Clone()
    $personal.AUTH_BOOTSTRAP_USERNAME = $account.username
    $personal.GARAGE_ADMIN_PASSWORD = $account.password
    $token = Get-DockerAdminToken $personal
    $sessions[$account.username] = @{Authorization='Bearer '+$token.access_token}
    $me = Read-As $account.username '/auth/me'
    Assert-Demo ($me.Status -eq 200 -and $me.Data.role -eq $account.role) "Login and role: $($account.username) / $($account.role)"
}
$admin = 'demo.tv2.admin'
$reception = 'demo.tv2.reception'
$warehouse = 'demo.tv2.warehouse'
$tech = 'demo.tv2.technician'
$a = 'demo.tv2.customer.a'
$b = 'demo.tv2.customer.b'
Assert-Demo ((Read-As $reception '/customers').Status -eq 200) 'Receptionist can view customers'
Assert-Demo ((Read-As $reception '/admin/users').Status -eq 403) 'Receptionist cannot administer accounts'
Assert-Demo ((Read-As $warehouse '/bookings').Status -eq 403) 'Warehouse staff cannot manage reception bookings'
Assert-Demo ((Read-As $tech "/repair-orders/$($manifest['received-order'].repairOrderId)").Status -eq 200) 'Assigned technician can read the sample repair order'
Assert-Demo ((Read-As $a "/vehicles/$($manifest['vehicle1'].id)").Status -eq 200) 'Customer A can read their vehicle'
Assert-Demo ((Read-As $a "/vehicles/$($manifest['vehicle2'].id)").Status -eq 403) 'Customer A cannot read Customer B vehicle'
$bookingList = (Read-As $a '/bookings').Data
if ($bookingList.PSObject.Properties['content']) { $bookingList = $bookingList.content }
$foreignBookings = @($bookingList | Where-Object customerId -ne $manifest['customer1'].id)
Assert-Demo ($foreignBookings.Count -eq 0) 'Customer booking list contains only their own records'
$history = Read-As $b "/vehicles/$($manifest['vehicle7'].id)/timeline"
$entries = @($history.Data.historyEntries)
$oldRecord = $entries | Where-Object repairOrderId -eq $manifest['history-order'].repairOrderId | Select-Object -First 1
Assert-Demo ($history.Status -eq 200 -and $null -ne $oldRecord -and $null -eq $oldRecord.totalAmount) 'New vehicle owner sees technical history with previous-owner finance hidden'
$invoiceId = $manifest['history-invoice'].id
Assert-Demo ((Read-As $b "/billing/invoices/$invoiceId").Status -eq 403) 'New owner cannot open previous-owner invoice'
Assert-Demo ((Read-As $a "/billing/invoices/$invoiceId").Status -eq 200) 'Original invoice owner retains invoice access after transfer'
$order = Read-As $admin "/repair-orders/$($manifest['history-order'].repairOrderId)"
$invoice = Read-As $admin "/billing/invoices/$invoiceId"
Assert-Demo ($order.Data.status -eq 'CLOSED' -and $invoice.Data.status -eq 'PAID') 'Sample workshop lifecycle finished with CLOSED order and PAID invoice'
Assert-Demo ((Read-As $admin "/vehicle-checks/$($manifest['received-check'].id)").Status -eq 200) 'Check-in checklist is accessible without fabricated photo evidence'
$attachments = Read-As $admin "/attachments/check/$($manifest['received-check'].id)"
Assert-Demo ($attachments.Status -eq 200 -and @($attachments.Data).Count -eq 0) 'Receipt has no fabricated attachment'
Assert-Demo ($manifest['DV-THAYDAU'].id -gt 0 -and $manifest['OIL-5W30-4L'].id -gt 0 -and $manifest['DV-THAYDAU'].serviceCode -eq 'DV-THAYDAU' -and $manifest['OIL-5W30-4L'].sku -eq 'OIL-5W30-4L') 'Service and stock fixtures use separate registry keys'
foreach ($sku in @('OIL-5W30-4L','PT-LOCDAU','PT-MAPHANH')) {
    $part = Read-As $warehouse "/inventory/parts/$($manifest[$sku].id)"
    Assert-Demo ($part.Status -eq 200 -and $part.Data.stockQuantity -eq 10 -and $part.Data.reservedQuantity -eq 0) "Posted opening balance: $sku"
}
Assert-Demo ($invoice.Data.totalAmount -eq 350000 -and $invoice.Data.paidAmount -eq 350000) 'Invoice total equals recorded payment'
Write-Output "Verified $checks sample login, role, ownership, inventory and lifecycle checks."
