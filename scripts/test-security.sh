#!/usr/bin/env bash
set -euo pipefail

API="${API:-http://127.0.0.1:18081}"
: "${MOTORISTA_TOKEN:?Informe MOTORISTA_TOKEN}"
: "${PASSAGEIRO_TOKEN:?Informe PASSAGEIRO_TOKEN}"
: "${MOTORISTA_ID:?Informe MOTORISTA_ID}"
: "${PASSAGEIRO_ID:?Informe PASSAGEIRO_ID}"
: "${TURMA_ID:?Informe TURMA_ID}"

check() {
    local nome="$1" esperado="$2" token="$3" endereco="$4"
    local codigo
    if [[ "$token" == "-" ]]; then
        codigo=$(curl --connect-timeout 5 -s -o /dev/null -w "%{http_code}" "$API$endereco")
    else
        codigo=$(curl --connect-timeout 5 -s -o /dev/null -w "%{http_code}" \
          -H "Authorization: Bearer $token" "$API$endereco")
    fi
    printf '%-52s esperado=%s obtido=%s\n' "$nome" "$esperado" "$codigo"
    [[ "$codigo" == "$esperado" ]] || exit 1
}

check "Sem token: rotas protegidas" 401 - "/turmas/motorista/$MOTORISTA_ID"
check "Token falso" 401 "token-invalido" "/turmas/motorista/$MOTORISTA_ID"
check "Motorista acessa suas turmas" 200 "$MOTORISTA_TOKEN" "/turmas/motorista/$MOTORISTA_ID"
check "Passageiro nao acessa turmas do motorista" 403 "$PASSAGEIRO_TOKEN" "/turmas/motorista/$MOTORISTA_ID"
check "Passageiro acessa suas turmas" 200 "$PASSAGEIRO_TOKEN" "/turmas/usuario/$PASSAGEIRO_ID/todas"
check "Motorista nao finge ser passageiro" 403 "$MOTORISTA_TOKEN" "/turmas/usuario/$PASSAGEIRO_ID/todas"
check "Passageiro consulta sua confirmacao" 200 "$PASSAGEIRO_TOKEN" "/rota/minha-confirmacao?usuarioId=$PASSAGEIRO_ID&turmaId=$TURMA_ID"
check "Motorista nao acessa confirmacao alheia" 403 "$MOTORISTA_TOKEN" "/rota/minha-confirmacao?usuarioId=$PASSAGEIRO_ID&turmaId=$TURMA_ID"
check "Passageiro nao otimiza rotas" 403 "$PASSAGEIRO_TOKEN" "/rota/otimizar?sentido=ida&turmaId=$TURMA_ID"
check "Motorista otimiza suas rotas" 200 "$MOTORISTA_TOKEN" "/rota/otimizar?sentido=ida&turmaId=$TURMA_ID"

echo "Todas as verificacoes de autorizacao passaram."
