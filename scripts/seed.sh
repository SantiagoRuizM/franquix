#!/usr/bin/env bash
# Loads sample franchises, branches and products through the API, then prints the top-stock
# product of each branch.
#
#   ./scripts/seed.sh                                                       # local stack
#   API_URL=https://franquix-api.onrender.com API_TOKEN=<token> ./scripts/seed.sh
#
# Requires curl and jq. Safe to re-run: a franchise whose name already exists (409) is skipped
# together with its branches and products.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
API_TOKEN="${API_TOKEN:-local-dev-token}"
BASE="${API_URL%/}/api/v1/franchises"

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "Missing dependency: $tool" >&2; exit 1; }
done

CONFLICT=3
FRANCHISE_ID=""
BRANCH_ID=""
CREATED=()

# post PATH JSON: prints the body on 201, returns $CONFLICT on 409 and exits on anything else.
post() {
  local response status
  response=$(curl -sS -m 120 -X POST "$BASE$1" \
    -H "Authorization: Bearer $API_TOKEN" -H 'Content-Type: application/json' \
    -d "$2" -w $'\n%{http_code}')
  status=${response##*$'\n'}
  response=${response%$'\n'*}
  case $status in
    201) printf '%s' "$response" ;;
    409) return "$CONFLICT" ;;
    *) echo "POST $BASE$1 failed with HTTP $status: $response" >&2; exit 1 ;;
  esac
}

franchise() {
  local body
  BRANCH_ID=""
  if body=$(post "" "$(jq -nc --arg name "$1" '{name: $name}')"); then
    FRANCHISE_ID=$(jq -r .id <<<"$body")
    CREATED+=("$FRANCHISE_ID|$1")
    echo "+ franchise $1"
  elif [ $? -eq "$CONFLICT" ]; then
    FRANCHISE_ID=""
    echo "= franchise $1 already exists, skipped"
  else
    exit 1
  fi
}

branch() {
  local body
  [ -n "$FRANCHISE_ID" ] || return 0
  body=$(post "/$FRANCHISE_ID/branches" "$(jq -nc --arg name "$1" '{name: $name}')") \
    || { echo "Could not create branch $1" >&2; exit 1; }
  BRANCH_ID=$(jq -r .id <<<"$body")
  echo "  + branch $1"
}

product() {
  [ -n "$FRANCHISE_ID" ] || return 0
  post "/$FRANCHISE_ID/branches/$BRANCH_ID/products" \
    "$(jq -nc --arg name "$1" --argjson stock "$2" '{name: $name, stock: $stock}')" >/dev/null \
    || { echo "Could not create product $1" >&2; exit 1; }
  echo "    + product $1 ($2)"
}

# The free Render plan sleeps when idle: wait for the instance before writing.
curl -sSf -m 120 -o /dev/null "${API_URL%/}/actuator/health" \
  || { echo "API not reachable at $API_URL" >&2; exit 1; }

franchise "Acme Coffee"
branch "Centro"
product "Coffee" 120
product "Latte" 120          # ties with Coffee: the top-stock pick goes to the name first in order
product "Bagel" 85
branch "Norte"
product "Espresso" 95
product "Muffin" 60
product "Bagel" 40
branch "Sur"                 # no products: left out of the top-stock result

franchise "Burger Hub"
branch "Chapinero"
product "Fries" 150
product "Classic Burger" 70
product "Milkshake" 45
branch "Cedritos"
product "Cheeseburger" 90
product "Fries" 90
product "Onion Rings" 30
branch "Poblado"
product "Veggie Burger" 30
product "Onion Rings" 55

franchise "Pizza Nova"
branch "Laureles"
product "Garlic Bread" 110
product "Pepperoni" 75
product "Margherita" 50
branch "Bocagrande"
product "Calzone" 35
product "Hawaiian" 20
branch "Envigado"
product "Lasagna" 25
product "Margherita" 0

for entry in "${CREATED[@]}"; do
  echo
  echo "Top stock per branch: ${entry#*|}"
  curl -sSf -m 120 "$BASE/${entry%%|*}/branches/top-stock-products" \
    -H "Authorization: Bearer $API_TOKEN" \
    | jq -r '.[] | "  \(.branchName): \(.productName) (\(.stock))"'
done
