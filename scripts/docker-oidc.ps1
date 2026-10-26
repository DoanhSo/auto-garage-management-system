# Local bootstrap only: use the same authorization-code + S256 flow as the browser.
# The generated administrator has no interactive actions or MFA during initial seeding.
function Get-DockerAdminToken([hashtable]$Settings) {
    Add-Type -AssemblyName System.Net.Http
    $issuer = "http://localhost:$($Settings.KEYCLOAK_PORT)/realms/autogarage-realm"
    $callback = "http://localhost:$($Settings.FRONTEND_PORT)/login"
    function New-PkceValue {
        $bytes = New-Object byte[] 32
        $random = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($bytes) } finally { $random.Dispose() }
        [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
    }
    function New-FormContent([hashtable]$Values) {
        $fields = [Collections.Generic.Dictionary[string,string]]::new()
        foreach ($key in $Values.Keys) { $fields.Add($key, [string]$Values[$key]) }
        return [Net.Http.FormUrlEncodedContent]::new($fields)
    }
    $verifier = New-PkceValue
    $state = New-PkceValue
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $digest = $sha.ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier)) } finally { $sha.Dispose() }
    $challenge = [Convert]::ToBase64String($digest).TrimEnd('=').Replace('+','-').Replace('/','_')
    $query = New-FormContent @{
        client_id='autogarage-frontend'; response_type='code'; scope='openid profile email'
        redirect_uri=$callback; state=$state; code_challenge=$challenge; code_challenge_method='S256'
    }
    try { $queryString = $query.ReadAsStringAsync().GetAwaiter().GetResult() } finally { $query.Dispose() }
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.UseCookies = $false
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(30)
    $response = $null
    $stage = 'authorization page'
    try {
        $response = $client.GetAsync("$issuer/protocol/openid-connect/auth?$queryString").GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) { throw 'Bootstrap authorization page could not be opened.' }
        $html = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $form = [regex]::Match($html, '<form\b[^>]*\bid="kc-form-login"[^>]*>', 'IgnoreCase')
        $action = [regex]::Match($form.Value, '\baction="([^"]+)"', 'IgnoreCase')
        if (-not $form.Success -or -not $action.Success) { throw 'Bootstrap administrator requires an interactive login action; seed aborted.' }
        $actionUri = [Uri]::new([Uri]$issuer, [Net.WebUtility]::HtmlDecode($action.Groups[1].Value))
        if ($actionUri.GetLeftPart([UriPartial]::Authority) -ne ([Uri]$issuer).GetLeftPart([UriPartial]::Authority) -or
            -not $actionUri.AbsolutePath.StartsWith('/realms/autogarage-realm/login-actions/')) {
            throw 'Unexpected bootstrap login destination; credentials were not sent.'
        }
        # Browsers accept Secure cookies on localhost HTTP; .NET CookieContainer does not.
        # Forward this one bootstrap page's cookies only to its validated loopback form.
        $cookies = @($response.Headers.GetValues('Set-Cookie') | ForEach-Object { $_.Split(';')[0] }) -join '; '
        if (-not $cookies) { throw 'Bootstrap authorization cookies are missing.' }
        $response.Dispose()
        $stage = 'credential form submission'
        $formContent = New-FormContent @{username=$Settings.AUTH_BOOTSTRAP_USERNAME; password=$Settings.GARAGE_ADMIN_PASSWORD; credentialId=''}
        $loginRequest = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Post, $actionUri)
        $loginRequest.Content = $formContent
        $null = $loginRequest.Headers.TryAddWithoutValidation('Cookie', $cookies)
        try { $response = $client.SendAsync($loginRequest).GetAwaiter().GetResult() } finally { $loginRequest.Dispose() }
        $location = $response.Headers.Location
        $stage = 'authorization callback validation'
        if ([int]$response.StatusCode -ne 302 -or $null -eq $location -or $location.GetLeftPart([UriPartial]::Path) -ne $callback) {
            throw 'Bootstrap administrator login needs attention; no demo records were changed.'
        }
        $parameters = @{}
        foreach ($pair in $location.Query.TrimStart('?').Split('&')) {
            $parts = $pair.Split(@('='), 2, [StringSplitOptions]::None)
            if ($parts.Count -eq 2) { $parameters[[Uri]::UnescapeDataString($parts[0])] = [Uri]::UnescapeDataString($parts[1]) }
        }
        if ($parameters.state -ne $state -or -not $parameters.code -or $parameters.error) { throw 'Bootstrap authorization state was rejected.' }
        $response.Dispose()
        $stage = 'PKCE token exchange'
        $tokenContent = New-FormContent @{
            grant_type='authorization_code'; client_id='autogarage-frontend'; code=$parameters.code
            code_verifier=$verifier; redirect_uri=$callback
        }
        try { $response = $client.PostAsync("$issuer/protocol/openid-connect/token", $tokenContent).GetAwaiter().GetResult() } finally { $tokenContent.Dispose() }
        if (-not $response.IsSuccessStatusCode) { throw 'Bootstrap PKCE token exchange failed.' }
        $token = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        if (-not $token.access_token) { throw 'Bootstrap access token is missing.' }
        return $token
    } catch {
        # HttpClient exceptions can contain authorization codes in URLs. Do not forward them.
        $status = if ($null -ne $response) { [int]$response.StatusCode } else { 'none' }
        throw "Docker bootstrap PKCE failed at $stage (HTTP $status). Check Keycloak and the local generated credentials."
    } finally {
        if ($null -ne $response) { $response.Dispose() }
        $client.Dispose()
    }
}
