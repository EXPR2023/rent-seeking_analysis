param(
  [string]$BaseUrl = "http://localhost:8081/api",
  [string]$Username = "admin",
  [string]$Password = "admin123"
)

$ErrorActionPreference = "Stop"

function Assert-Success($Response, $Name) {
  if ($Response.code -ne "SUCCESS") {
    throw "$Name failed: $($Response | ConvertTo-Json -Depth 12)"
  }
  return $Response.data
}

function Invoke-Api($Method, $Path, $Headers = @{}, $Body = $null) {
  $uri = "$BaseUrl$Path"
  if ($null -eq $Body) {
    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $Headers
  }
  return Invoke-RestMethod `
    -Method $Method `
    -Uri $uri `
    -Headers $Headers `
    -ContentType "application/json; charset=utf-8" `
    -Body ($Body | ConvertTo-Json -Depth 10)
}

$login = Assert-Success (Invoke-Api Post "/auth/login" @{} @{ username = $Username; password = $Password }) "login"
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$stamp = Get-Date -Format "yyyyMMddHHmmss"

$roles = Assert-Success (Invoke-Api Get "/roles?enabled=true" $headers) "roles"
$analystRole = @($roles | Where-Object { $_.roleCode -eq "ANALYST" })[0]
if ($null -eq $analystRole) {
  throw "ANALYST role not found"
}

$user = Assert-Success (Invoke-Api Post "/users" $headers @{
  username = "demo_$stamp"
  password = "demo123456"
  realName = "Phase 8 Demo User $stamp"
  email = "demo_$stamp@example.local"
  phone = "13900000000"
  roleIds = @($analystRole.id)
}) "create user"

$user = Assert-Success (Invoke-Api Put "/users/$($user.id)/roles" $headers @{
  roleIds = @($analystRole.id)
}) "assign roles"

$regulationA = Assert-Success (Invoke-Api Post "/regulations" $headers @{
  title = "Phase 8 Demo Regulation A $stamp"
  code = "P8-A-$stamp"
  typeCode = "POLICY"
  publishDepartment = "Compliance Department"
  effectiveDate = "2026-01-01"
  expiryDate = $null
  applicableScope = "approval, vendor onboarding, internal compliance"
  status = "ACTIVE"
  content = "Article 1 Procurement projects should use open comparison. Article 2 In special cases, the competent department may directly appoint a vendor, with approval records retained. Article 3 Approval materials should be archived."
}) "create regulation A"

$regulationB = Assert-Success (Invoke-Api Post "/regulations" $headers @{
  title = "Phase 8 Demo Regulation B $stamp"
  code = "P8-B-$stamp"
  typeCode = "POLICY"
  publishDepartment = "Procurement Department"
  effectiveDate = "2026-01-01"
  expiryDate = $null
  applicableScope = "vendor onboarding, procurement approval"
  status = "ACTIVE"
  content = "Article 1 Procurement projects generally use open comparison. Article 2 The competent department may directly choose the vendor without public notice. Article 3 Approval materials are kept by the handler."
}) "create regulation B"

$task = Assert-Success (Invoke-Api Post "/conflict-tasks" $headers @{
  mainRegulationId = $regulationA.id
  compareRegulationIds = @($regulationB.id)
}) "create conflict task"

$firstConflictItem = @($task.items)[0]
if ($null -eq $firstConflictItem) {
  throw "conflict task created no items"
}

$reviewedConflict = Assert-Success (Invoke-Api Put "/conflict-items/$($firstConflictItem.id)/confirm" $headers @{
  reviewComment = "Phase 8 smoke confirmed"
}) "confirm conflict item"

$riskId = Assert-Success (Invoke-Api Post "/regulations/$($regulationA.id)/analyze" $headers @{
  force = $true
}) "risk analyze"

$risk = Assert-Success (Invoke-Api Get "/risk-analyses/$riskId" $headers) "risk detail"
if (@($risk.items).Count -lt 1) {
  throw "risk analysis created no risk items"
}

$risk = Assert-Success (Invoke-Api Put "/risk-analyses/$riskId/review" $headers @{
  reviewStatus = "CONFIRMED"
  reviewComment = "Phase 8 smoke confirmed"
}) "risk review"

$summary = Assert-Success (Invoke-Api Get "/dashboard/summary" $headers) "dashboard summary"
$riskLevels = Assert-Success (Invoke-Api Get "/dashboard/risk-levels" $headers) "risk levels"
$conflictStatus = Assert-Success (Invoke-Api Get "/dashboard/conflict-status" $headers) "conflict status"
$report = Assert-Success (Invoke-Api Get "/reports/regulation/$($regulationA.id)" $headers) "report"
$configs = Assert-Success (Invoke-Api Get "/system/configs" $headers) "configs"
$dicts = Assert-Success (Invoke-Api Get "/system/dicts" $headers) "dicts"
$logs = Assert-Success (Invoke-Api Get "/audit/logs?pageNo=1&pageSize=20" $headers) "logs"
$health = Assert-Success (Invoke-Api Get "/system/health") "health"

[ordered]@{
  user = $user.username
  regulations = @($regulationA.id, $regulationB.id)
  conflictTask = $task.id
  conflictItems = @($task.items).Count
  reviewedConflictStatus = $reviewedConflict.status
  riskAnalysis = $risk.id
  riskItems = @($risk.items).Count
  riskReviewStatus = $risk.reviewStatus
  dashboardRegulations = $summary.regulationTotal
  riskLevelChartItems = @($riskLevels).Count
  conflictStatusChartItems = @($conflictStatus).Count
  report = $report.reportName
  configs = @($configs).Count
  dicts = @($dicts).Count
  logs = $logs.total
  health = $health.backend
} | ConvertTo-Json -Depth 10
