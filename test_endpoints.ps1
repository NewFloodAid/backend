$OutputEncoding = [Console]::OutputEncoding = [Text.Encoding]::UTF8
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlcmFkbWluIiwiYXBwVHlwZSI6IldFQiIsImFkbWluSWQiOjEsInJvbGUiOiJTVVBFUl9BRE1JTiIsImRpc3RyaWN0SWRzIjpbXSwiaWF0IjoxNzc5ODU2Mjk3LCJleHAiOjE3Nzk5NDI2OTd9.jbcEYXmzxc2KsLthrG2pXUt8HYMxSEMtEjXifggreNQ"
$headers = @{ "Authorization" = "Bearer $token" }

function Test-Endpoint {
    param($url, $desc)
    try {
        $response = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
        Write-Host "PASS: $desc"
    } catch {
        Write-Host "FAIL: $desc - Status: $($_.Exception.Response.StatusCode.value__) - $($_.Exception.Message)"
    }
}

Test-Endpoint "http://localhost:8080/api/provinces" "/api/provinces"
Test-Endpoint "http://localhost:8080/api/districts?province=เชียงใหม่" "/api/districts?province=เชียงใหม่"
Test-Endpoint "http://localhost:8080/api/assistanceTypes" "/api/assistanceTypes"
Test-Endpoint "http://localhost:8080/api/assistanceTypes/all" "/api/assistanceTypes/all"
Test-Endpoint "http://localhost:8080/api/assistanceTypes?districtId=532" "/api/assistanceTypes?districtId=532"
Test-Endpoint "http://localhost:8080/api/configs?key=government_phone_number" "/api/configs?key=government_phone_number"
Test-Endpoint "http://localhost:8080/api/reportStatuses?isUser=false" "/api/reportStatuses?isUser=false"
Test-Endpoint "http://localhost:8080/api/admin/admins" "/api/admin/admins"
