[CmdletBinding()]
param([string]$ConfigPath = '', [string]$PrivateDirectory = '', [switch]$CompleteWorkshop)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $ConfigPath) { $ConfigPath = Join-Path $root '.env' }
if (-not $PrivateDirectory) { $PrivateDirectory = Join-Path $root '.docker-local' }
. (Join-Path $PSScriptRoot 'docker-common.ps1')
. (Join-Path $PSScriptRoot 'docker-oidc.ps1')
$settings = Read-DockerSettings $ConfigPath
$api = "http://localhost:$($settings.BACKEND_PORT)/api/v1"
$null = New-Item -ItemType Directory -Force -Path $PrivateDirectory
$accountPath = Join-Path $PrivateDirectory 'tv2-demo-accounts.json'
$manifestPath = Join-Path $PrivateDirectory 'tv2-demo-manifest.json'
$credentialPath = Join-Path $PrivateDirectory 'tv2-demo-credentials.txt'
$accounts = @(if (Test-Path $accountPath) { Get-Content $accountPath -Raw -Encoding UTF8 | ConvertFrom-Json })
$manifest = if (Test-Path $manifestPath) { Get-Content $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json -AsHashtable } else { @{} }

function Save-DemoManifest {
    $manifest | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
}
function Refresh-DemoToken {
    $token = Get-DockerAdminToken $settings
    $script:headers = @{ Authorization = 'Bearer ' + $token.access_token }
}
function Invoke-SampleApi([string]$Path, [string]$Method = 'Get', $Body = $null, [switch]$Retried) {
    $arguments = @{ Uri = $api + $Path; Method = $Method; Headers = $script:headers; TimeoutSec = 60 }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 20 -Compress))
    }
    try { $response = Invoke-RestMethod @arguments }
    catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($status -eq 401 -and -not $Retried) {
            Refresh-DemoToken
            return Invoke-SampleApi $Path $Method $Body -Retried
        }
        $code = 'unknown'
        $message = 'Request failed; existing records were preserved.'
        try { $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json; $code = $errorBody.code; $message = $errorBody.message }
        catch { $code = 'unreadable'; $message = 'The API returned a non-JSON error; existing records were preserved.' }
        throw "Sample API: $Method $Path, HTTP $status, code $code. $message"
    }
    if (-not $response.success) { throw "Sample API returned an unsuccessful envelope: $Path" }
    return $response.data
}
function Get-SampleRows([string]$Path) {
    $data = Invoke-SampleApi $Path
    if ($null -eq $data) { return }
    if ($data.PSObject.Properties['content']) { return $data.content }
    return $data
}
function New-DemoPassword {
    return [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
}

Refresh-DemoToken
$null = Invoke-SampleApi '/auth/me'
$users = @(Get-SampleRows '/admin/users?keyword=demo.tv2&size=100')
$definitions = @(
    @{ username='demo.tv2.admin'; role='ADMIN'; fullName='Trần Quốc Minh' },
    @{ username='demo.tv2.reception'; role='RECEPTIONIST'; fullName='Nguyễn Thị Lan' },
    @{ username='demo.tv2.warehouse'; role='WAREHOUSE_STAFF'; fullName='Trần Anh Tuấn' },
    @{ username='demo.tv2.technician'; role='TECHNICIAN'; fullName='Lê Hoàng Minh' },
    @{ username='demo.tv2.qc'; role='TECHNICIAN'; fullName='Trần Quốc Hùng' },
    @{ username='demo.tv2.customer.a'; role='CUSTOMER'; fullName='Nguyễn Minh An' },
    @{ username='demo.tv2.customer.b'; role='CUSTOMER'; fullName='Trần Ngọc Bình' }
)
foreach ($definition in $definitions) {
    $account = $accounts | Where-Object username -eq $definition.username | Select-Object -First 1
    $user = $users | Where-Object username -eq $definition.username | Select-Object -First 1
    if (-not $account) {
        if ($user) { throw "An account named $($definition.username) exists without this fixture's saved password. It was not reset." }
        $account = [pscustomobject]@{username=$definition.username;role=$definition.role;password=(New-DemoPassword);id=$null}
        $accounts += $account
        $accounts | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $accountPath -Encoding UTF8
    }
    if (-not $user) {
        $user = Invoke-SampleApi '/admin/users' 'Post' @{
            username=$definition.username;fullName=$definition.fullName;role=$definition.role
            email=($definition.username+'@example.test');password=$account.password
        }
        $users += $user
    }
    if ($user.fullName -ne $definition.fullName) {
        $user = Invoke-SampleApi "/admin/users/$($user.id)" 'Put' @{fullName=$definition.fullName;email=$user.email;role=$user.role;active=$user.active;version=$user.version}
    }
    $account.id = $user.id
    $accounts | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $accountPath -Encoding UTF8
}
$text = @("AutoGarage: http://localhost:$($settings.FRONTEND_PORT)", 'Local sample accounts. Passwords are private; existing passwords were not reset.', '')
foreach ($account in $accounts) { $text += "$($account.role) | $($account.username) | $($account.password)" }
$text | Set-Content -LiteralPath $credentialPath -Encoding UTF8
Write-Output 'Sample accounts ready: seven accounts covering all five roles.'

$customers = @(Get-SampleRows '/customers?search=KH-')
$customerNames = @('Nguyễn Minh An','Trần Ngọc Bình','Phạm Thu Chi','Công ty Vận tải An Bình','Lê Hoàng Dũng','Vũ Thanh Hà')
for ($i=1; $i -le 6; $i++) {
    $code = 'KH-' + $i.ToString('00')
    $customer = $customers | Where-Object customerCode -eq $code | Select-Object -First 1
    if (-not $customer) {
        $body = @{customerCode=$code;fullName=$customerNames[$i-1];phone=('09992000'+$i.ToString('00'))
            email="demo.tv2.kh$i@example.test";address=(@('12 Nguyễn Văn Cừ, Phường Chợ Quán, TP. Hồ Chí Minh','45 Lê Văn Sỹ, Phường Nhiêu Lộc, TP. Hồ Chí Minh','28 Nguyễn Đình Chiểu, Phường Xuân Hòa, TP. Hồ Chí Minh','86 Quốc lộ 1A, Phường Bình Hưng Hòa, TP. Hồ Chí Minh','17 Phan Văn Trị, Phường Hạnh Thông, TP. Hồ Chí Minh','63 Võ Văn Ngân, Phường Thủ Đức, TP. Hồ Chí Minh')[$i-1])
            customerType='INDIVIDUAL';creditLimit=5000000;paymentTermDays=15}
        if ($i -eq 1) { $body.userId = ($accounts | Where-Object username -eq 'demo.tv2.customer.a').id }
        if ($i -eq 2) { $body.userId = ($accounts | Where-Object username -eq 'demo.tv2.customer.b').id }
        if ($i -eq 4) { $body.customerType='BUSINESS';$body.companyName=$customerNames[$i-1];$body.creditLimit=20000000 }
        $customer = Invoke-SampleApi '/customers' 'Post' $body
        $customers += $customer
    }
    $manifest["customer$i"] = $customer
    Save-DemoManifest
}
$allVehicles = @(Get-SampleRows '/vehicles')
$owners = @(1,2,3,4,5,1,1)
$brands = @('Toyota','Honda','Mazda','Ford','Hyundai','Toyota','Honda')
$models = @('Vios','City','CX-5','Ranger','Accent','Innova','Civic')
for ($i=1; $i -le 7; $i++) {
    $plate = @('51K12345','51L23456','51H34567','51C45678','51G56789','51F67890','51H78901')[$i-1]
    $vehicle = $allVehicles | Where-Object licensePlate -eq $plate | Select-Object -First 1
    if (-not $vehicle) {
        $vehicle = Invoke-SampleApi '/vehicles' 'Post' @{
            customerId=$manifest["customer$($owners[$i-1])"].id;licensePlate=$plate
            brand=$brands[$i-1];model=$models[$i-1];manufactureYear=@(2021,2022,2020,2021,2023,2019,2020)[$i-1];color=@('Trắng','Bạc','Đỏ','Xám','Trắng','Bạc','Đen')[$i-1];fuelType=$(if ($i -eq 4) {'Dầu'} else {'Xăng'});odometer=10000*$i
        }
        $allVehicles += $vehicle
    }
    $manifest["vehicle$i"] = $vehicle
    Save-DemoManifest
}
$services = @(Get-SampleRows '/services?size=100')
$serviceDefinitions = @(
    @{serviceCode='DV-THAYDAU';serviceName='Thay dầu động cơ';serviceType='SINGLE';unit='lượt';unitPrice=350000;standardMinutes=45;warrantyDays=30;warrantyKm=1000},
    @{serviceCode='DV-KIEMTRA';serviceName='Kiểm tra tổng quát';serviceType='SINGLE';unit='lượt';unitPrice=200000;standardMinutes=60;warrantyDays=0;warrantyKm=0},
    @{serviceCode='BD-20000';serviceName='Gói bảo dưỡng 20.000 km';serviceType='PACKAGE';unit='gói';unitPrice=1200000;standardMinutes=90;intervalKm=20000;intervalMonths=12;warrantyDays=90;warrantyKm=3000},
    @{serviceCode='DV-PHANH';serviceName='Kiểm tra phanh';serviceType='SINGLE';unit='lượt';unitPrice=450000;standardMinutes=60;warrantyDays=60;warrantyKm=2000}
)
foreach ($definition in $serviceDefinitions) {
    $service = $services | Where-Object serviceCode -eq $definition.serviceCode | Select-Object -First 1
    if (-not $service) { $service = Invoke-SampleApi '/services' 'Post' $definition; $services += $service }
    $manifest[$definition.serviceCode] = $service
}
Save-DemoManifest
Write-Output 'Customer, vehicle and catalog fixtures ready: 6 customers, 7 vehicles, 4 services.'

$bays = @(Get-SampleRows '/service-bays?size=100')
foreach ($number in 1..3) {
    $code = "K0$number"
    $bay = $bays | Where-Object bayCode -eq $code | Select-Object -First 1
    if (-not $bay) {
        $bay = Invoke-SampleApi '/service-bays' 'Post' @{
            bayCode=$code;bayName="Khoang dịch vụ $number";bayType=@('STANDARD','LIFT','DIAGNOSTIC')[$number-1]
            description='Khu vực tiếp nhận và thực hiện dịch vụ'
        }
    }
    $manifest["bay$number"] = $bay
}
$employees = @(Get-SampleRows '/employees?size=100')
foreach ($kind in @('technician','qc')) {
    $account = $accounts | Where-Object username -eq "demo.tv2.$kind"
    $employee = $employees | Where-Object userId -eq $account.id | Select-Object -First 1
    if (-not $employee) {
        $employee = Invoke-SampleApi '/employees' 'Post' @{
            userId=$account.id;employeeCode=$(if ($kind -eq 'qc') {'NV-KD01'} else {'NV-KT01'});skillsJson='["GENERAL","MAINTENANCE","DIAGNOSIS"]'
            canQc=($kind -eq 'qc');hourlyCost=100000;baseSalary=8000000
        }
    }
    $manifest[$kind] = $employee
    $shifts = @(Get-SampleRows "/employee-shifts/employee/$($employee.id)")
    # Cancel only this fixture's old fixed daytime shifts; keep all non-fixture shifts intact.
    foreach ($old in @($shifts | Where-Object { $_.notes -match '^DEMO-TV2-(technician|qc)-[0-9]{8}$' -and $_.status -eq 'ACTIVE' })) {
        $null = Invoke-SampleApi "/employee-shifts/$($old.id)/cancel" 'Put'
    }
    $sessionStart = [DateTimeOffset]::UtcNow.AddMinutes(-5)
    $sessionEnd = [DateTimeOffset]::UtcNow.AddHours(4)
    $nextDay = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(7)).Date.AddDays(1)
    $nextStart = [DateTimeOffset]::new($nextDay.AddHours(8), [TimeSpan]::FromHours(7))
    if ($nextStart -lt $sessionEnd.AddHours(8)) { $nextStart = $sessionEnd.AddHours(8) }
    $sessionDefinitions = @(
        @{key=0;start=$sessionStart;end=$sessionEnd;suffix='SESSION'},
        @{key=1;start=$nextStart;end=$nextStart.AddHours(8);suffix='NEXT'}
    )
    foreach ($definition in $sessionDefinitions) {
        $note = "Ca làm việc $kind $($definition.suffix) " + $sessionStart.ToString('yyyyMMdd')
        $shift = $shifts | Where-Object { $_.notes -eq $note -and $_.status -eq 'ACTIVE' } | Select-Object -First 1
        if (-not $shift) {
            $shift = Invoke-SampleApi '/employee-shifts' 'Post' @{
                employeeId=$employee.id;startsAt=$definition.start.UtcDateTime.ToString('o');endsAt=$definition.end.UtcDateTime.ToString('o')
                shiftType=$(if ($definition.key -eq 0) { if ($sessionStart.ToOffset([TimeSpan]::FromHours(7)).Hour -ge 18) {'NIGHT'} else {'OVERTIME'} } else {'FULL_DAY'});notes=$note
            }
        }
        $manifest["$kind-shift-$($definition.key)"] = $shift
        if ($definition.key -eq 0 -and $shift.attendanceStatus -in @('SCHEDULED','ABSENT')) {
            $manifest["$kind-shift-0"] = Invoke-SampleApi "/employee-shifts/$($shift.id)/check-in" 'Post'
        }
    }
}
Save-DemoManifest
Write-Output 'Scheduling fixtures ready: 3 bays and 2 technicians with a current session and a next-day shift.'

$bookings = @(Get-SampleRows '/bookings')
$cases = @(
    @{number=1;key='PENDING';hour=9}, @{number=2;key='CONFIRMED';hour=10},
    @{number=4;key='RESCHEDULED';hour=11}, @{number=5;key='CANCELLED';hour=13},
    @{number=6;key='RECEIVED';hour=14}
)
foreach ($case in $cases) {
    $marker = @{PENDING='Bảo dưỡng định kỳ, kiểm tra dầu động cơ';CONFIRMED='Kiểm tra điều hòa làm mát yếu';RESCHEDULED='Kiểm tra hệ thống treo và lốp';CANCELLED='Bảo dưỡng trước chuyến đi xa';RECEIVED='Kiểm tra tiếng kêu khi phanh'}[$case.key]
    $booking = $bookings | Where-Object { $_.requestDescription -eq $marker -and $_.vehicleId -eq $manifest["vehicle$($case.number)"].id } | Select-Object -First 1
    $newBooking = -not $booking
    if ($newBooking) {
        $vehicle = $manifest["vehicle$($case.number)"]
        $day = [DateTimeOffset]::UtcNow.ToOffset([TimeSpan]::FromHours(7)).Date.AddDays(1)
        $appointment = [DateTimeOffset]::new($day.AddHours($case.hour),[TimeSpan]::FromHours(7))
        if ($case.key -eq 'RECEIVED') { $appointment = [DateTimeOffset]::UtcNow.AddMinutes(10) }
        $booking = Invoke-SampleApi '/bookings' 'Post' @{
            customerId=$vehicle.customerId;vehicleId=$vehicle.id;serviceId=$manifest['DV-KIEMTRA'].id
            appointmentAt=$appointment.UtcDateTime.ToString('o');requestDescription=$marker
        }
        if ($case.key -in @('CONFIRMED','RECEIVED')) { $booking = Invoke-SampleApi "/bookings/$($booking.id)/confirm" 'Post' }
        if ($case.key -eq 'RESCHEDULED') {
            $booking = Invoke-SampleApi "/bookings/$($booking.id)/reschedule" 'Post' @{newAppointmentAt=$appointment.AddDays(1).UtcDateTime.ToString('o')}
        }
        if ($case.key -eq 'CANCELLED') {
            $booking = Invoke-SampleApi "/bookings/$($booking.id)/cancel" 'Post' @{cancellationReason='Khách đổi kế hoạch, hẹn liên hệ đặt lịch sau'}
        }
    }
    $manifest[$case.key] = $booking
    Save-DemoManifest
}

if (-not $manifest['received-order']) {
    $vehicle = $manifest['vehicle6']
    $order = Invoke-SampleApi '/reception/check-in' 'Post' @{
        bookingId=$manifest['RECEIVED'].id;vehicleId=$vehicle.id;odometerIn=$vehicle.odometer+50
        fuelLevelPercent=60;customerComplaint='Phanh phát tiếng kêu khi đi chậm; đề nghị kiểm tra má phanh và đĩa phanh'
    }
    $manifest['received-order'] = $order
    Save-DemoManifest
}
if (-not $manifest['received-check']) {
    $check = Invoke-SampleApi '/vehicle-checks' 'Post' @{
        repairOrderId=$manifest['received-order'].repairOrderId;checkType='CHECK_IN'
        checklistJson='{"exterior":"Trầy nhẹ cửa phải","interior":"Bình thường","warningLights":"Không có đèn lỗi"}'
        odometer=$manifest['vehicle6'].odometer+50;notes='Trầy nhẹ cửa phải; nội thất bình thường. Chưa bổ sung ảnh hiện trạng.'
    }
    $manifest['received-check'] = $check
    Save-DemoManifest
}
Write-Output 'Booking fixtures ready: PENDING, CONFIRMED, RESCHEDULED, CANCELLED, RECEIVED; one repair order and check-in checklist.'

if ($CompleteWorkshop) {
    if (-not $manifest['received-diagnosis-assignment']) {
        $receivedId = $manifest['received-order'].repairOrderId
        $diagnosis = @(Get-SampleRows "/repair-orders/$receivedId/tasks") |
            Where-Object taskType -eq 'DIAGNOSIS' | Select-Object -First 1
        $manifest['received-diagnosis-assignment'] = Invoke-SampleApi "/repair-tasks/$($diagnosis.id)/assignments" 'Post' @{
            employeeId=$manifest['technician'].id;assignmentRole='MAIN'
            startsAt=[DateTimeOffset]::UtcNow.UtcDateTime.ToString('o')
            endsAt=[DateTimeOffset]::UtcNow.AddMinutes(45).UtcDateTime.ToString('o')
        }
        Save-DemoManifest
    }

    $suppliers = @(Get-SampleRows '/inventory/suppliers?size=100')
    $supplier = $suppliers | Where-Object supplierCode -eq 'NCC-ANPHAT' | Select-Object -First 1
    if (-not $supplier) {
        $supplier = Invoke-SampleApi '/inventory/suppliers' 'Post' @{
            supplierCode='NCC-ANPHAT';supplierName='Phụ tùng An Phát';phone='0999200099';email='demo.tv2.supplier@example.test'
        }
    }
    $manifest['supplier'] = $supplier
    $parts = @(Get-SampleRows '/inventory/parts?size=100')
    $partDefinitions = @(
        @{sku='OIL-5W30-4L';partName='Dầu động cơ 5W30';unit='can';purchasePrice=250000;sellingPrice=350000;reorderLevel=3;warrantyDays=30;warrantyKm=1000},
        @{sku='PT-LOCDAU';partName='Lọc dầu';unit='cái';purchasePrice=70000;sellingPrice=120000;reorderLevel=5;warrantyDays=90;warrantyKm=3000},
        @{sku='PT-MAPHANH';partName='Má phanh trước';unit='bộ';purchasePrice=450000;sellingPrice=650000;reorderLevel=2;warrantyDays=180;warrantyKm=10000}
    )
    foreach ($definition in $partDefinitions) {
        $part = $parts | Where-Object sku -eq $definition.sku | Select-Object -First 1
        if (-not $part) {
            $definition.preferredSupplierId = $supplier.id
            $part = Invoke-SampleApi '/inventory/parts' 'Post' $definition
        }
        $manifest[$definition.sku] = $part
    }
    if (-not $manifest['opening-stock']) {
        $existingDocuments = @(Get-SampleRows '/inventory/documents?size=100')
        $existingOpening = $existingDocuments | Where-Object { $_.reason -eq 'Tồn đầu kỳ kho phụ tùng' -and $_.status -ne 'CANCELLED' } | Select-Object -First 1
        if ($existingOpening) { $manifest['opening-stock'] = $existingOpening; Save-DemoManifest }
    }
    if (-not $manifest['opening-stock']) {
        $items = @($partDefinitions | ForEach-Object { @{partId=$manifest[$_.sku].id;quantity=10;unitCost=$_.purchasePrice} })
        $manifest['opening-stock'] = Invoke-SampleApi '/inventory/documents' 'Post' @{
            documentType='OPENING';reason='Tồn đầu kỳ kho phụ tùng';items=$items
        }
        Save-DemoManifest
    }
    if ($manifest['opening-stock'].status -eq 'DRAFT') {
        $id = $manifest['opening-stock'].id
        $null = Invoke-SampleApi "/inventory/documents/$id/post" 'Post' @{requestKey="sample-opening-$id"}
        $manifest['opening-stock'] = Invoke-SampleApi "/inventory/documents/$id"
        Save-DemoManifest
    }
    Write-Output 'Inventory fixtures ready: one supplier, three parts and a posted opening-stock document.'

    if (-not $manifest['history-order']) {
        $vehicle = $manifest['vehicle7']
        $manifest['history-order'] = Invoke-SampleApi '/reception/check-in' 'Post' @{
            vehicleId=$vehicle.id;odometerIn=$vehicle.odometer+50;fuelLevelPercent=50
            customerComplaint='Bảo dưỡng định kỳ, thay dầu động cơ theo số km'
        }
        Save-DemoManifest
    }
    $historyId = $manifest['history-order'].repairOrderId
    if (-not $manifest['history-estimate']) {
        $null = Invoke-SampleApi "/repair-orders/$historyId/diagnosis" 'Patch' @{diagnosis='Dầu động cơ đến hạn thay; không phát hiện rò rỉ hoặc đèn cảnh báo.'}
        $tasks = @(Get-SampleRows "/repair-orders/$historyId/tasks")
        foreach ($task in @($tasks | Where-Object { $_.taskType -eq 'DIAGNOSIS' -and $_.status -ne 'COMPLETED' })) {
            $null = Invoke-SampleApi "/repair-tasks/$($task.id)/complete" 'Post' @{completionNote='Đã kiểm tra mức dầu, rò rỉ và đèn cảnh báo'}
        }
        $manifest['history-estimate'] = Invoke-SampleApi '/estimates' 'Post' @{
            repairOrderId=$historyId;notes='Thay dầu động cơ theo lịch bảo dưỡng'
            items=@(@{itemType='SERVICE';serviceId=$manifest['DV-THAYDAU'].id;quantity=1})
        }
        Save-DemoManifest
    }
    $estimate = Invoke-SampleApi "/estimates/$($manifest['history-estimate'].id)"
    if ($estimate.status -eq 'DRAFT') { $estimate = Invoke-SampleApi "/estimates/$($estimate.id)/send" 'Post' }
    if ($estimate.status -in @('SENT','PARTIALLY_APPROVED')) {
        $lines = @($estimate.items | Where-Object { $_.approvalStatus -ne 'APPROVED' } | ForEach-Object {
            @{estimateItemId=$_.id;decision='APPROVED';note='Khách xác nhận đồng ý báo giá'}
        })
        if ($lines.Count -gt 0) { $estimate = Invoke-SampleApi "/estimates/$($estimate.id)/decide" 'Post' @{lines=$lines;requestKey="sample-estimate-$($estimate.id)-approve"} }
    }
    $manifest['history-estimate'] = $estimate
    Save-DemoManifest
    $order = Invoke-SampleApi "/repair-orders/$historyId"
    if ($order.status -in @('APPROVED','IN_PROGRESS','WAITING_PARTS','WAITING_QC')) {
        $tasks = @(Get-SampleRows "/repair-orders/$historyId/tasks")
        $repairTask = $tasks | Where-Object taskType -eq 'REPAIR' | Select-Object -First 1
        if (-not $repairTask) { throw 'The approved sample estimate did not generate a repair task.' }
        if (-not $manifest['history-qc-assignment']) {
            $manifest['history-qc-assignment'] = Invoke-SampleApi "/repair-tasks/$($repairTask.id)/assignments" 'Post' @{
                employeeId=$manifest['qc'].id;assignmentRole='QC'
                startsAt=[DateTimeOffset]::UtcNow.AddMinutes(-1).UtcDateTime.ToString('o')
                endsAt=[DateTimeOffset]::UtcNow.AddMinutes(45).UtcDateTime.ToString('o')
            }
            Save-DemoManifest
        }
        foreach ($task in @($tasks | Where-Object status -ne 'COMPLETED')) {
            $null = Invoke-SampleApi "/repair-tasks/$($task.id)/complete" 'Post' @{completionNote='Đã thực hiện dịch vụ và kiểm tra lại'}
        }
        $order = Invoke-SampleApi "/repair-orders/$historyId"
        if ($order.status -eq 'WAITING_QC') {
            $qcAccount = $accounts | Where-Object username -eq 'demo.tv2.qc'
            $qcSettings = $settings.Clone()
            $qcSettings.AUTH_BOOTSTRAP_USERNAME = $qcAccount.username
            $qcSettings.GARAGE_ADMIN_PASSWORD = $qcAccount.password
            $qcToken = Get-DockerAdminToken $qcSettings
            $adminHeaders = $script:headers
            try {
                $script:headers = @{Authorization='Bearer '+$qcToken.access_token}
                $manifest['history-qc'] = Invoke-SampleApi '/quality-checks' 'Post' @{
                    repairOrderId=$historyId;result='PASSED';checklistJson='{"engine":"Đạt","leak":"Không rò dầu"}'
                    odometer=70100;notes='Không rò dầu, động cơ vận hành bình thường'
                }
            } finally { $script:headers = $adminHeaders }
            Save-DemoManifest
        }
    }
    if (-not $manifest['history-invoice']) {
        $manifest['history-invoice'] = Invoke-SampleApi '/billing/invoices/issue' 'Post' @{
            repairOrderId=$historyId;requestKey="sample-order-$historyId-invoice"
        }
        Save-DemoManifest
    }
    if (-not $manifest['history-payment']) {
        $invoice = $manifest['history-invoice']
        $manifest['history-payment'] = Invoke-SampleApi '/billing/payments/receive' 'Post' @{
            invoiceId=$invoice.id;amount=$invoice.totalAmount;paymentMethod='CASH'
            requestKey="sample-invoice-$($invoice.id)-payment";notes='Khách thanh toán đủ bằng tiền mặt'
        }
        Save-DemoManifest
    }
    $order = Invoke-SampleApi "/repair-orders/$historyId"
    if ($order.status -ne 'CLOSED') {
        $order = Invoke-SampleApi "/repair-orders/$historyId/handover" 'Post' @{
            odometerOut=70100;requestKey="sample-order-$historyId-handover";notes='Đã bàn giao xe và hướng dẫn lịch bảo dưỡng tiếp theo'
        }
    }
    $manifest['closed-history-order'] = $order
    $vehicle = Invoke-SampleApi "/vehicles/$($manifest['vehicle7'].id)"
    if ($vehicle.customerId -eq $manifest['customer1'].id) {
        $vehicle = Invoke-SampleApi "/vehicles/$($vehicle.id)/transfer" 'Post' @{
            newCustomerId=$manifest['customer2'].id;transferNote='Chuyển quyền sở hữu theo yêu cầu của chủ xe'
        }
    }
    $manifest['transferred-history-vehicle'] = $vehicle
    Save-DemoManifest
    Write-Output 'End-to-end fixture ready: approved estimate, completed repair, QC passed, paid invoice, handover, warranty and ownership transfer.'
}
Write-Output "Private login details: $credentialPath"
Write-Output "Fixture IDs and scenarios: $manifestPath"
