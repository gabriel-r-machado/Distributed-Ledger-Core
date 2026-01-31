# ========================================
# SMOKE TEST - PESSIMISTIC LOCKING
# Testa 5 threads tentando transferir simultaneamente
# ========================================

Write-Host "`n[INICIANDO SMOKE TEST DE CONCORRENCIA]`n" -ForegroundColor Cyan

# ========== PASSO 1: CRIAR USUARIOS ==========
Write-Host "[Passo 1] Criando usuarios..." -ForegroundColor Yellow

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$sender = @{
    firstName = "Gabriel"
    lastName = "Sender"
    document = "12345$timestamp"
    email = "sender$timestamp@smoketest.com"
    password = "senha123"
    userType = "COMMON"
    balance = 100
} | ConvertTo-Json

$receiver = @{
    firstName = "Maria"
    lastName = "Receiver"
    document = "98765$timestamp"
    email = "receiver$timestamp@smoketest.com"
    password = "senha123"
    userType = "COMMON"
    balance = 0
} | ConvertTo-Json

try {
    $senderResponse = Invoke-RestMethod -Uri "http://localhost:8081/users" -Method Post -Body $sender -ContentType "application/json"
    $senderId = $senderResponse.id
    Write-Host "[OK] Sender criado: ID = $senderId | Saldo = R$ 100,00" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Falha ao criar Sender: $_" -ForegroundColor Red
    exit 1
}

try {
    $receiverResponse = Invoke-RestMethod -Uri "http://localhost:8081/users" -Method Post -Body $receiver -ContentType "application/json"
    $receiverId = $receiverResponse.id
    Write-Host "[OK] Receiver criado: ID = $receiverId | Saldo = R$ 0,00" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Falha ao criar Receiver: $_" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

# ========== PASSO 2: ATAQUE DE CONCORRENCIA ==========
Write-Host "`n[Passo 2] INICIANDO ATAQUE: 5 threads transferindo R$ 50,00 simultaneamente..." -ForegroundColor Red
Write-Host "          Esperado: 1 sucesso + 4 falhas (saldo insuficiente)`n" -ForegroundColor Yellow

$transaction = @{
    value = 50
    senderId = $senderId
    receiverId = $receiverId
} | ConvertTo-Json

$jobs = @()

# Disparar 5 requisicoes SIMULTANEAS
1..5 | ForEach-Object {
    $threadId = $_
    $jobs += Start-Job -ScriptBlock {
        param($url, $body, $id)
        
        try {
            $response = Invoke-WebRequest -Uri $url -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
            Write-Output "Thread $id : [SUCESSO] HTTP $($response.StatusCode)"
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            $errorMsg = $_.ErrorDetails.Message
            Write-Output "Thread $id : [FALHOU] HTTP $statusCode - $errorMsg"
        }
    } -ArgumentList "http://localhost:8081/transactions", $transaction, $threadId
}

# Aguardar TODAS finalizarem
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

# Exibir resultados
$results | ForEach-Object { Write-Host $_ -ForegroundColor $(if ($_ -match "SUCESSO") { "Green" } else { "Yellow" }) }

Start-Sleep -Seconds 2

# ========== PASSO 3: VALIDACAO FINAL ==========
Write-Host "`n[Passo 3] VALIDANDO RESULTADOS...`n" -ForegroundColor Cyan

try {
    $senderFinal = Invoke-RestMethod -Uri "http://localhost:8081/users/$senderId" -Method Get
    $senderBalance = $senderFinal.wallet.balance
    
    Write-Host "[Saldo Final] Sender: R$ $senderBalance" -ForegroundColor Magenta
    
    if ($senderBalance -eq 50) {
        Write-Host "`n============================================" -ForegroundColor Green
        Write-Host "   TESTE PASSOU! Pessimistic Lock OK!" -ForegroundColor Green
        Write-Host "============================================" -ForegroundColor Green
        Write-Host "  - Apenas 1 transacao processada" -ForegroundColor Green
        Write-Host "  - Saldo nao ficou negativo" -ForegroundColor Green
        Write-Host "  - Lock protegeu contra race conditions" -ForegroundColor Green
    } elseif ($senderBalance -lt 0) {
        Write-Host "`n[FALHA CRITICA] Saldo NEGATIVO detectado! Lock NAO funcionou!" -ForegroundColor Red
    } else {
        Write-Host "`n[Atencao] Saldo inesperado: R$ $senderBalance (esperado: R$ 50,00)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[ERRO] Falha ao consultar saldo final: $_" -ForegroundColor Red
}

Write-Host "`n[FIM] Smoke Test finalizado!`n" -ForegroundColor Cyan
