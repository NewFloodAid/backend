$OutputEncoding = [Console]::OutputEncoding = [Text.Encoding]::UTF8

$token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluIiwiYXBwVHlwZSI6IldFQiIsImFkbWluSWQiOjEsInJvbGUiOiJTVVBFUl9BRE1JTiIsImRpc3RyaWN0SWRzIjpbXSwiaWF0IjoxNzc5ODU2Mjk3LCJleHAiOjE3Nzk5NDI2OTd9.jbcEYXmzxc2KsLthrG2pXUt8HYMxSEMtEjXifggreNQ"
$headers = @{ "Authorization" = "Bearer $token" }

try {
    $body = @{
        name = "ทดสอบ"
        isActive = $true
        district = @{ id = 532 }
    } | ConvertTo-Json -Depth 10

    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/assistanceTypes" -Headers $headers -Method Post -ContentType "application/json" -Body $body
    Write-Host "PASS: Create assistance type - Created ID: $($response.id)"

    if ($response.id) {
        $delResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/assistanceTypes/$($response.id)" -Headers $headers -Method Delete
        Write-Host "PASS: Delete assistance type - Status: $($delResponse.StatusCode)"
    }
} catch {
    Write-Host "FAIL: Error - $($_.Exception.Message)"
    if ($_.Exception.Response) {
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    }
}
