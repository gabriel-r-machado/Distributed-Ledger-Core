#!/bin/bash

# ========================================
# SMOKE TEST - PESSIMISTIC LOCKING
# Testa 5 threads tentando transferir simultaneamente
# ========================================

echo -e "\n🔧 INICIANDO SMOKE TEST DE CONCORRÊNCIA\n"

# ========== PASSO 1: CRIAR USUÁRIOS ==========
echo "📝 Criando usuários..."

SENDER_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Gabriel",
    "lastName": "Sender",
    "document": "12345678901",
    "email": "sender@smoketest.com",
    "password": "senha123",
    "userType": "COMMON",
    "balance": 100
  }')

SENDER_BODY=$(echo "$SENDER_RESPONSE" | sed -e 's/HTTP_STATUS:.*//g')
SENDER_HTTP=$(echo "$SENDER_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTP_STATUS://')

if [ "$SENDER_HTTP" -eq 201 ] || [ "$SENDER_HTTP" -eq 200 ]; then
    SENDER_ID=$(echo $SENDER_BODY | grep -o '"id":"[^"]*' | sed 's/"id":"//')
    echo "✅ Sender criado: ID = $SENDER_ID | Saldo = R\$ 100,00"
else
    echo "❌ Erro ao criar Sender: HTTP $SENDER_HTTP"
    exit 1
fi

RECEIVER_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Maria",
    "lastName": "Receiver",
    "document": "98765432100",
    "email": "receiver@smoketest.com",
    "password": "senha123",
    "userType": "COMMON",
    "balance": 0
  }')

RECEIVER_BODY=$(echo "$RECEIVER_RESPONSE" | sed -e 's/HTTP_STATUS:.*//g')
RECEIVER_HTTP=$(echo "$RECEIVER_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTP_STATUS://')

if [ "$RECEIVER_HTTP" -eq 201 ] || [ "$RECEIVER_HTTP" -eq 200 ]; then
    RECEIVER_ID=$(echo $RECEIVER_BODY | grep -o '"id":"[^"]*' | sed 's/"id":"//')
    echo "✅ Receiver criado: ID = $RECEIVER_ID | Saldo = R\$ 0,00"
else
    echo "❌ Erro ao criar Receiver: HTTP $RECEIVER_HTTP"
    exit 1
fi

sleep 2

# ========== PASSO 2: ATAQUE DE CONCORRÊNCIA ==========
echo -e "\n🚨 INICIANDO ATAQUE: 5 threads transferindo R\$ 50,00 simultaneamente..."
echo "   Esperado: 1 sucesso + 4 falhas (saldo insuficiente)"
echo ""

# Disparar 5 requisições EM PARALELO (background &)
for i in {1..5}; do
  (
    RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST http://localhost:8081/api/transactions \
      -H "Content-Type: application/json" \
      -d "{
        \"value\": 50,
        \"senderId\": \"$SENDER_ID\",
        \"receiverId\": \"$RECEIVER_ID\"
      }")
    
    HTTP_CODE=$(echo "$RESPONSE" | tr -d '\n' | sed -e 's/.*HTTP_STATUS://')
    
    if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
      echo "Thread $i: ✅ SUCESSO (HTTP $HTTP_CODE)"
    else
      echo "Thread $i: ❌ FALHOU (HTTP $HTTP_CODE)"
    fi
  ) &
done

# Aguardar TODAS as threads finalizarem
wait

sleep 2

# ========== PASSO 3: VALIDAÇÃO FINAL ==========
echo -e "\n📊 VALIDANDO RESULTADOS...\n"

FINAL_RESPONSE=$(curl -s -X GET http://localhost:8081/api/users/$SENDER_ID)
FINAL_BALANCE=$(echo $FINAL_RESPONSE | grep -o '"balance":[0-9.]*' | sed 's/"balance"://')

echo "💰 Saldo final do Sender: R\$ $FINAL_BALANCE"

if [ "$FINAL_BALANCE" == "50.0" ] || [ "$FINAL_BALANCE" == "50" ]; then
    echo -e "\n🎉🎉🎉 TESTE PASSOU! Pessimistic Lock funcionou corretamente! 🎉🎉🎉"
    echo "   - Apenas 1 transação processada"
    echo "   - Saldo não ficou negativo"
    echo "   - Lock protegeu contra race conditions"
elif (( $(echo "$FINAL_BALANCE < 0" | bc -l) )); then
    echo -e "\n❌❌❌ FALHA CRÍTICA! Saldo NEGATIVO detectado! Lock NÃO funcionou!"
else
    echo -e "\n⚠️ Saldo inesperado: R\$ $FINAL_BALANCE (esperado: R\$ 50,00)"
fi

echo -e "\n✅ Smoke Test finalizado!\n"
