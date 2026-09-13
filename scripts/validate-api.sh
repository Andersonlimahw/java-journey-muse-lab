#!/usr/bin/env bash
# Executable mirror of src/test/postman/java-journey-muse-lab.postman_collection.json.
# Runs the same 23 requests in order against a local instance and asserts statuses.
# Usage: ./scripts/validate-api.sh [baseUrl]   (default http://localhost:8080)
set -euo pipefail

BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() { # check <label> <expected-status> <actual-status>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "PASS [$2] $1";
  else FAIL=$((FAIL+1)); echo "FAIL (want $2, got $3) $1"; fi
}

need() { command -v "$1" >/dev/null || { echo "missing dependency: $1"; exit 2; }; }
need curl; need jq

echo "== waiting for API at $BASE =="
for i in $(seq 1 30); do
  if curl -sf -o /dev/null "$BASE/trips"; then break; fi
  if [ "$i" = 30 ]; then echo "API not reachable at $BASE"; exit 1; fi
  sleep 2
done

echo "== trips slice =="
CREATE=$(curl -s -X POST "$BASE/trips" -H 'Content-Type: application/json' -d '{"destination":"Florianópolis, SC","starts_at":"2025-06-25T10:00:00","ends_at":"2025-07-02T10:00:00","emails_to_invite":["dev@muse.dev"],"owner_name":"Muse","owner_email":"muse@muse.dev"}')
TRIP=$(echo "$CREATE" | jq -r .uuid)
[ "$(echo "$CREATE" | jq '.participants | length')" = 1 ] && PASS=$((PASS+1)) && echo "PASS create returns only invited participants" || { FAIL=$((FAIL+1)); echo "FAIL create participants"; }
[ "${#TRIP}" = 36 ] && PASS=$((PASS+1)) && echo "PASS create returns trip uuid" || { FAIL=$((FAIL+1)); echo "FAIL create uuid"; }

check "list trips" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips")"
check "get trip" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP")"
check "get overview" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/overview")"

UPD=$(curl -s -X PUT "$BASE/trips/$TRIP" -H 'Content-Type: application/json' -d '{"destination":"Rio de Janeiro, RJ","starts_at":"2025-07-01T10:00:00","ends_at":"2025-07-10T10:00:00"}')
[ "$(echo "$UPD" | jq -r .startsAt)" = "2025-07-01T10:00:00" ] && PASS=$((PASS+1)) && echo "PASS update fixes startsAt (bugfix)" || { FAIL=$((FAIL+1)); echo "FAIL update startsAt: $(echo "$UPD" | jq -r .startsAt)"; }

check "invite" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/trips/$TRIP/invite" -H 'Content-Type: application/json' -d '{"email":"guest@muse.dev"}')"
[ "$(curl -s "$BASE/trips/$TRIP/participants" | jq 'length')" = 2 ] && PASS=$((PASS+1)) && echo "PASS trip has 2 participants" || { FAIL=$((FAIL+1)); echo "FAIL participants count"; }
check "confirm trip" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X PATCH "$BASE/trips/$TRIP/confirmation")"

echo "== participants slice =="
P=$(curl -s -X POST "$BASE/participants" -H 'Content-Type: application/json' -d "{\"name\":\"Ada\",\"email\":\"ada@muse.dev\",\"trip_id\":\"$TRIP\"}" | jq -r .id)
check "get participant" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/participants/$P")"
check "update participant" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/participants/$P" -H 'Content-Type: application/json' -d '{"name":"Ada Lovelace","email":"ada@muse.dev"}')"
check "confirm participant" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/participants/$P/confirm" -H 'Content-Type: application/json' -d '{"name":"Ada Lovelace","email":"ada@muse.dev"}')"
check "delete participant" 204 "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/participants/$P")"

echo "== activities slice =="
A=$(curl -s -X POST "$BASE/trips/$TRIP/activities" -H 'Content-Type: application/json' -d '{"title":"Visit Museum","occurs_at":"2025-07-05T14:00:00"}' | jq -r .activityId)
check "list activities" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/activities")"
check "get activity" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/activities/$A")"
check "update activity" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/trips/$TRIP/activities/$A" -H 'Content-Type: application/json' -d '{"title":"Visit Aquarium","occurs_at":"2025-07-06T10:00:00"}')"
check "delete activity" 204 "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/trips/$TRIP/activities/$A")"
check "deleted activity is gone" 404 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/activities/$A")"

echo "== links slice =="
L=$(curl -s -X POST "$BASE/trips/$TRIP/links" -H 'Content-Type: application/json' -d '{"title":"Airbnb","url":"https://airbnb.com/rooms/1"}' | jq -r .linkId)
check "list links" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/links")"
check "get link" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/links/$L")"
check "update link" 200 "$(curl -s -o /dev/null -w '%{http_code}' -X PUT "$BASE/trips/$TRIP/links/$L" -H 'Content-Type: application/json' -d '{"title":"Airbnb","url":"https://airbnb.com/rooms/2"}')"
check "delete link" 204 "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/trips/$TRIP/links/$L")"
check "deleted link is gone" 404 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/$TRIP/links/$L")"

echo "== negative cases =="
check "unknown trip 404" 404 "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/trips/00000000-0000-0000-0000-000000000000")"
check "blank activity title 400" 400 "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/trips/$TRIP/activities" -H 'Content-Type: application/json' -d '{"title":"","occurs_at":"2025-07-05T14:00:00"}')"

echo ""
echo "RESULT: $PASS passed, $FAIL failed"
[ "$FAIL" = 0 ]
