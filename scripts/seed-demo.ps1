[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ConfigPath)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'docker-common.ps1')
. (Join-Path $PSScriptRoot 'docker-oidc.ps1')
$settings = Read-DockerSettings $ConfigPath
$api = "http://localhost:$($settings.BACKEND_PORT)/api/v1"
$token = Get-DockerAdminToken $settings
$headers = @{ Authorization = 'Bearer ' + $token.access_token }
function Invoke-DemoApi([string]$Path, [string]$Method = 'Get', $Body = $null) {
    $arguments = @{ Uri = $api + $Path; Method=$Method; Headers=$headers; ContentType='application/json' }
    if ($null -ne $Body) { $arguments.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 10 -Compress)) }
    try { $response = Invoke-RestMethod @arguments }
    catch { throw "Demo seed request failed: $Method $Path. Existing data was preserved." }
    if (-not $response.success) { throw "Demo API failed at $Path" }
    return $response.data
}
$null = Invoke-DemoApi '/auth/me'
$customers = @(Invoke-DemoApi '/customers?search=DEMO')
$vehicles = @(Invoke-DemoApi '/vehicles?search=DEMO')
$services = @(Invoke-DemoApi '/services?search=DEMO')
$bookings = @(Invoke-DemoApi '/bookings')
$catalog = @(
    @{ serviceCode='DEMO-OIL'; serviceName='DEMO - Thay dau va loc dau'; serviceType='SINGLE'; unit='luot'; unitPrice=350000; standardMinutes=45; warrantyDays=30; warrantyKm=1000 },
    @{ serviceCode='DEMO-CHECK'; serviceName='DEMO - Kiem tra tong quat'; serviceType='SINGLE'; unit='luot'; unitPrice=200000; standardMinutes=60; warrantyDays=0; warrantyKm=0 }
)
foreach ($entry in $catalog) {
    if (-not ($services | Where-Object serviceCode -eq $entry.serviceCode)) { $services += Invoke-DemoApi '/services' 'Post' $entry }
}
for ($index=1; $index -le 3; $index++) {
    $code = 'DEMO-KH00' + $index
    $customer = $customers | Where-Object customerCode -eq $code | Select-Object -First 1
    if (-not $customer) {
        $customer = Invoke-DemoApi '/customers' 'Post' @{
            customerCode=$code; fullName="Khach hang DEMO $index"; customerType='INDIVIDUAL';
            phone="090000000$index"; email="demo.customer$index@example.test"; address='Du lieu mau de xem giao dien'; creditLimit=0; paymentTermDays=0
        }
    }
    $plate = "DEMO00$index"
    $vehicle = $vehicles | Where-Object licensePlate -eq $plate | Select-Object -First 1
    if (-not $vehicle) {
        $vehicle = Invoke-DemoApi '/vehicles' 'Post' @{
            customerId=$customer.id; licensePlate=$plate; brand=@('Toyota','Honda','Mazda')[$index-1];
            model=@('Vios','City','CX-5')[$index-1]; manufactureYear=2023; color='Trang'; fuelType='Xang'; odometer=15000*$index
        }
    }
    if (-not ($bookings | Where-Object { $_.vehicleId -eq $vehicle.id -and $_.requestDescription -eq 'DEMO - Lich hen mau' })) {
        $service = $services | Where-Object serviceCode -eq 'DEMO-CHECK' | Select-Object -First 1
        $null = Invoke-DemoApi '/bookings' 'Post' @{
            customerId=$customer.id; vehicleId=$vehicle.id; serviceId=$service.id;
            appointmentAt=[DateTime]::UtcNow.Date.AddDays(1).AddHours(2+$index).ToString("yyyy-MM-ddTHH:mm:ss'Z'");
            requestDescription='DEMO - Lich hen mau'
        }
    }
}
Write-Output 'Demo data ready: 3 customers, 3 vehicles, 2 services and 3 bookings. Existing demo records were preserved.'
