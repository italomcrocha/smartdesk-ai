$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$javaPath = $null
if ($env:JAVA_HOME) { $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'; if (Test-Path -LiteralPath $candidate) { $javaPath = $candidate } }
if (-not $javaPath) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) { $javaPath = Join-Path (Split-Path $compiler.Source) 'java.exe' }
}
if (-not $javaPath) { throw 'Configure JAVA_HOME para um JDK 17 ou superior.' }
if (-not (Test-Path -LiteralPath './backend/target/smartdesk-ai-1.0.0.jar')) { throw 'Compile primeiro o backend conforme o README.' }
if (-not $env:GEMINI_API_KEY -or $env:GEMINI_API_KEY -match 'SUA_NOVA_CHAVE|COLE_AQUI|SUA_CHAVE') {
    $secret = Read-Host 'Cole a chave do Gemini (nao sera exibida)' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
    try { $env:GEMINI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}
$env:PORT = '8081'
Write-Host 'Acesse http://localhost:8081 apos a inicializacao. Ctrl+C encerra o aplicativo.'
& $javaPath -jar './backend/target/smartdesk-ai-1.0.0.jar'