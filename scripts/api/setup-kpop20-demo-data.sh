#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:18080}"
API_BASE="${API_HOST%/}/api"
SEED_TAG="${SEED_TAG:-KPOP20_$(date +%Y%m%d)}"
SEED_KEYWORD="${SEED_KEYWORD:-KPOP20_}"
SEED_USER_COUNT="${SEED_USER_COUNT:-256}"
MAX_RESERVATIONS_PER_USER="${MAX_RESERVATIONS_PER_USER:-8}"
RANDOM_SEED="${RANDOM_SEED:-}"
JWT_SECRET="${JWT_SECRET:-ticketrush-local-dev-jwt-secret-key-change-this-value}"
PG_CONTAINER="${PG_CONTAINER:-ticket-core-service_postgres-db_1}"
PG_USER="${PG_USER:-postgres}"
PG_DB="${PG_DB:-mydb}"

if [[ -n "${RANDOM_SEED}" ]]; then
  RANDOM="${RANDOM_SEED}"
fi

for required in curl jq openssl shuf date mktemp; do
  if ! command -v "${required}" >/dev/null 2>&1; then
    echo "[ERROR] required command not found: ${required}" >&2
    exit 1
  fi
done

RUN_ID="$(date +%Y%m%d%H%M%S)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

FALLBACK_PNG="${TMP_DIR}/fallback.png"
printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIW2NkYGD4DwABBAEAQ0MyoQAAAABJRU5ErkJggg==' | base64 -d > "${FALLBACK_PNG}"

ACCESS_TOKEN=""
declare -a SEED_USER_IDS=()
USER_CURSOR=0
declare -a SUMMARY_LINES=()

log() {
  printf '[KPOP20] %s\n' "$*"
}

slugify() {
  local raw="${1:-}"
  printf '%s' "${raw}" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9]+/-/g; s/^-+//; s/-+$//'
}

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

build_access_token() {
  local user_id="${1:?user_id required}"
  local username="${2:?username required}"
  local role="${3:-ADMIN}"
  local now epoch_exp jti header payload header_b64 payload_b64 signature
  now="$(date +%s)"
  epoch_exp="$((now + 3600))"
  jti="$(cat /proc/sys/kernel/random/uuid)"
  header='{"alg":"HS256","typ":"JWT"}'
  payload="$(jq -nc \
    --arg sub "${user_id}" \
    --arg jti "${jti}" \
    --arg iss "ticketrush" \
    --arg typ "access" \
    --arg username "${username}" \
    --arg role "${role}" \
    --argjson iat "${now}" \
    --argjson exp "${epoch_exp}" \
    '{sub:$sub,jti:$jti,iss:$iss,iat:$iat,exp:$exp,typ:$typ,username:$username,role:$role}')"
  header_b64="$(printf '%s' "${header}" | b64url)"
  payload_b64="$(printf '%s' "${payload}" | b64url)"
  signature="$(printf '%s' "${header_b64}.${payload_b64}" | openssl dgst -binary -sha256 -hmac "${JWT_SECRET}" | b64url)"
  printf '%s' "${header_b64}.${payload_b64}.${signature}"
}

api_json() {
  local method="${1:?method required}"
  local endpoint="${2:?endpoint required}"
  local body="${3:-}"
  local auth_required="${4:-0}"
  local args=(-sS -f -X "${method}" "${API_BASE}${endpoint}" -H "Accept: application/json")
  if [[ "${auth_required}" == "1" ]]; then
    args+=(-H "Authorization: Bearer ${ACCESS_TOKEN}")
  fi
  if [[ -n "${body}" ]]; then
    args+=(-H "Content-Type: application/json" --data "${body}")
  fi
  curl "${args[@]}"
}

api_delete_no_content() {
  local endpoint="${1:?endpoint required}"
  curl -sS -f -X DELETE "${API_BASE}${endpoint}" >/dev/null
}

extract_youtube_video_id() {
  local url="${1:-}"
  if [[ "${url}" =~ [\?\&]v=([A-Za-z0-9_-]{6,}) ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
    return 0
  fi
  if [[ "${url}" =~ youtu\.be/([A-Za-z0-9_-]{6,}) ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
    return 0
  fi
  printf ''
}

cleanup_seeded_concerts() {
  local keyword="${1:?keyword required}"
  local pattern before_count after_count
  pattern="$(printf '%%%s%%' "${keyword}" | sed "s/'/''/g")"

  if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "${PG_CONTAINER}"; then
    before_count="$(
      docker exec -i "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" -tA \
        -c "SELECT COUNT(*) FROM concerts WHERE title ILIKE '${pattern}';"
    )"
    if [[ "${before_count}" == "0" ]]; then
      log "cleanup skipped (keyword=${keyword}, no matches)"
      return 0
    fi

    docker exec -i "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" <<SQL >/dev/null
WITH target_concerts AS (
  SELECT id FROM concerts WHERE title ILIKE '${pattern}'
)
DELETE FROM reservations r
USING seats s, concert_options o, target_concerts t
WHERE r.seat_id = s.id
  AND s.concert_option_id = o.id
  AND o.concert_id = t.id;

WITH target_concerts AS (
  SELECT id FROM concerts WHERE title ILIKE '${pattern}'
)
DELETE FROM seats s
USING concert_options o, target_concerts t
WHERE s.concert_option_id = o.id
  AND o.concert_id = t.id;

WITH target_concerts AS (
  SELECT id FROM concerts WHERE title ILIKE '${pattern}'
)
DELETE FROM sales_policies sp
USING target_concerts t
WHERE sp.concert_id = t.id;

WITH target_concerts AS (
  SELECT id FROM concerts WHERE title ILIKE '${pattern}'
)
DELETE FROM concert_options o
USING target_concerts t
WHERE o.concert_id = t.id;

WITH target_concerts AS (
  SELECT id FROM concerts WHERE title ILIKE '${pattern}'
)
DELETE FROM concerts c
USING target_concerts t
WHERE c.id = t.id;
SQL

    after_count="$(
      docker exec -i "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" -tA \
        -c "SELECT COUNT(*) FROM concerts WHERE title ILIKE '${pattern}';"
    )"
    log "cleanup completed (keyword=${keyword}, before=${before_count}, after=${after_count})"
    return 0
  fi

  local response ids
  response="$(curl -sS -f "${API_BASE}/concerts/search?keyword=${keyword}&page=0&size=300&sort=id,asc")"
  mapfile -t ids < <(printf '%s' "${response}" | jq -r '.items[]?.id')
  if (( ${#ids[@]} == 0 )); then
    log "cleanup skipped (keyword=${keyword}, no matches)"
    return 0
  fi
  for concert_id in "${ids[@]}"; do
    api_delete_no_content "/concerts/cleanup/${concert_id}" || true
  done
  log "cleanup completed (keyword=${keyword}, deleted=${#ids[@]})"
}

create_seed_users() {
  local count="${1:?count required}"
  local index username payload user_response user_id
  for ((index=1; index<=count; index++)); do
    username="seed_user_${RUN_ID}_${index}"
    payload="$(jq -nc --arg username "${username}" '{username:$username,tier:"VIP"}')"
    user_response="$(api_json POST "/users" "${payload}" 0)"
    user_id="$(printf '%s' "${user_response}" | jq -r '.id // empty')"
    if [[ -n "${user_id}" ]]; then
      SEED_USER_IDS+=("${user_id}")
    fi
  done
  log "seed users ready (${#SEED_USER_IDS[@]})"
}

compute_general_sale_start() {
  local bucket="${1:?bucket required}"
  case "${bucket}" in
    OPEN|SOLD_OUT)
      date -u -d "-$((15 + RANDOM % 240)) minutes" +"%Y-%m-%dT%H:%M:%S"
      ;;
    OPEN_SOON_5M)
      date -u -d "+$((60 + RANDOM % 200)) seconds" +"%Y-%m-%dT%H:%M:%S"
      ;;
    OPEN_SOON_1H)
      date -u -d "+$((600 + RANDOM % 2800)) seconds" +"%Y-%m-%dT%H:%M:%S"
      ;;
    PREOPEN)
      date -u -d "+$((7200 + RANDOM % 86400)) seconds" +"%Y-%m-%dT%H:%M:%S"
      ;;
    UNSCHEDULED)
      date -u -d "+30 days" +"%Y-%m-%dT%H:%M:%S"
      ;;
    *)
      date -u -d "-30 minutes" +"%Y-%m-%dT%H:%M:%S"
      ;;
  esac
}

build_seat_layout_payload() {
  local seat_count="${1:?seat_count required}"
  jq -nc --argjson seatCount "${seat_count}" '
    def row_label($i): ("R" + (($i + 1) | tostring));
    def row_counts($seat_total; $rows):
      [range(0; $rows) as $i
        | (($seat_total / $rows | floor) + (if $i < ($seat_total % $rows) then 1 else 0 end))
      ];
    def make_rows($counts):
      [range(0; ($counts | length)) as $i
        | {label: row_label($i), from: 1, to: ($counts[$i])}
        | select(.to > 0)
      ];

    ($seatCount | tonumber) as $total
    | ($total * 18 / 100 | floor) as $a_base
    | ($total * 37 / 100 | floor) as $b_base
    | ($total - $a_base - $b_base) as $c_base
    | ($a_base | if . < 24 then 24 elif . > ($total - 60) then ($total - 60) else . end) as $a_count
    | ($b_base | if . < 48 then 48 else . end) as $b_base_adjusted
    | ($total - $a_count) as $remaining
    | ($b_base_adjusted | if . > ($remaining - 12) then ($remaining - 12) else . end) as $b_count
    | ($total - $a_count - $b_count) as $c_count
    | ($a_count | if . >= 54 then 3 else 2 end) as $a_rows
    | ($b_count | if . >= 125 then 5 else 4 end) as $b_rows
    | ($c_count | if . >= 135 then 6 elif . >= 95 then 5 else 4 end) as $c_rows
    | {
        sections: [
          {code: "A", rows: make_rows(row_counts($a_count; $a_rows))},
          {code: "B", rows: make_rows(row_counts($b_count; $b_rows))},
          {code: "C", rows: make_rows(row_counts($c_count; $c_rows))}
        ]
      }'
}

build_seat_numbers_from_layout() {
  local seat_layout="${1:?seat_layout required}"
  printf '%s' "${seat_layout}" | jq -r '
    [
      .sections[] as $section
      | if (($section.rows // []) | length) > 0 then
          (
            $section.rows[] as $row
            | range(($row.from // 1); (($row.to // 0) + 1))
            | "\($section.code)-\($row.label)-\(.)"
          )
        else
          (range(1; (($section.capacity // 0) + 1)) | "\($section.code)-\(.)")
        end
    ][]
  '
}

rewrite_option_seat_numbers_with_layout() {
  local option_id="${1:?option_id required}"
  local seat_layout="${2:?seat_layout required}"

  if ! command -v docker >/dev/null 2>&1; then
    return 1
  fi
  if ! docker ps --format '{{.Names}}' | grep -qx "${PG_CONTAINER}"; then
    return 1
  fi

  local -a seat_ids=()
  local -a seat_numbers=()
  local index escaped values_file

  mapfile -t seat_ids < <(
    docker exec "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" -tA \
      -c "SELECT id FROM seats WHERE concert_option_id = ${option_id} ORDER BY id;"
  )
  mapfile -t seat_numbers < <(build_seat_numbers_from_layout "${seat_layout}")

  if (( ${#seat_ids[@]} == 0 || ${#seat_numbers[@]} == 0 )); then
    return 1
  fi
  if (( ${#seat_ids[@]} != ${#seat_numbers[@]} )); then
    return 1
  fi

  values_file="${TMP_DIR}/seat-values-${option_id}.sql"
  : > "${values_file}"
  for ((index=0; index<${#seat_ids[@]}; index++)); do
    escaped="$(printf '%s' "${seat_numbers[index]}" | sed "s/'/''/g")"
    if (( index > 0 )); then
      printf ',\n' >> "${values_file}"
    fi
    printf '(%s, %s, %s)' "${seat_ids[index]}" "${option_id}" "'${escaped}'" >> "${values_file}"
  done

  docker exec -i "${PG_CONTAINER}" psql -U "${PG_USER}" -d "${PG_DB}" >/dev/null <<SQL
UPDATE seats AS s
SET seat_number = v.seat_number
FROM (
  VALUES
$(cat "${values_file}")
) AS v(id, concert_option_id, seat_number)
WHERE s.id = v.id
  AND s.concert_option_id = v.concert_option_id;
SQL
}

upload_thumbnail_from_youtube() {
  local concert_id="${1:?concert_id required}"
  local youtube_url="${2:-}"
  local video_id thumb_url image_file content_type
  video_id="$(extract_youtube_video_id "${youtube_url}")"
  image_file="${TMP_DIR}/thumb-${concert_id}.jpg"
  content_type="image/jpeg"
  if [[ -n "${video_id}" ]]; then
    thumb_url="https://img.youtube.com/vi/${video_id}/hqdefault.jpg"
    if ! curl -sS -fL "${thumb_url}" -o "${image_file}"; then
      cp "${FALLBACK_PNG}" "${image_file}"
      content_type="image/png"
    fi
  else
    cp "${FALLBACK_PNG}" "${image_file}"
    content_type="image/png"
  fi

  curl -sS -f -X POST "${API_BASE}/admin/concerts/${concert_id}/thumbnail" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -F "image=@${image_file};type=${content_type}" >/dev/null
}

create_hold_with_state() {
  local seat_id="${1:?seat_id required}"
  local target_state="${2:?target_state required}"
  local user_id hold_payload hold_response reservation_id state_payload
  local size="${#SEED_USER_IDS[@]}"

  if (( size == 0 )); then
    user_id=1
  else
    user_id="${SEED_USER_IDS[USER_CURSOR]}"
    USER_CURSOR=$(( (USER_CURSOR + 1) % size ))
  fi
  hold_payload="$(jq -nc --argjson userId "${user_id}" --argjson seatId "${seat_id}" '{userId:$userId,seatId:$seatId}')"
  if ! hold_response="$(api_json POST "/reservations/v6/holds" "${hold_payload}" 0 2>/dev/null)"; then
    return 1
  fi

  reservation_id="$(printf '%s' "${hold_response}" | jq -r '.id // empty')"
  if [[ -z "${reservation_id}" ]]; then
    return 1
  fi

  if [[ "${target_state}" == "HOLD" ]]; then
    return 0
  fi

  state_payload="$(jq -nc --argjson userId "${user_id}" '{userId:$userId}')"
  if ! api_json POST "/reservations/v6/${reservation_id}/paying" "${state_payload}" 0 >/dev/null 2>&1; then
    return 1
  fi

  if [[ "${target_state}" == "PAYING" ]]; then
    return 0
  fi

  state_payload="$(jq -nc --argjson userId "${user_id}" '{userId:$userId,paymentMethod:"CARD"}')"
  if ! api_json POST "/reservations/v6/${reservation_id}/confirm" "${state_payload}" 0 >/dev/null 2>&1; then
    return 1
  fi
  return 0
}

apply_open_state_distribution() {
  local option_id="${1:?option_id required}"
  local sale_bucket="${2:?sale_bucket required}"
  local seats_response seat_count hold_target paying_target confirmed_target max_occupied desired occupied_cursor seat_id
  local hold_success=0 paying_success=0 confirmed_success=0
  local -a seat_ids=()
  local -a shuffled=()

  seats_response="$(curl -sS -f "${API_BASE}/concerts/options/${option_id}/seat-map")"
  mapfile -t seat_ids < <(printf '%s' "${seats_response}" | jq -r '.[]?.id')
  seat_count="${#seat_ids[@]}"
  if (( seat_count == 0 )); then
    printf '%s|%s|%s|%s' 0 0 0 0
    return 0
  fi

  mapfile -t shuffled < <(printf '%s\n' "${seat_ids[@]}" | shuf)

  if [[ "${sale_bucket}" == "SOLD_OUT" ]]; then
    confirmed_target="${seat_count}"
    paying_target=0
    hold_target=0
  elif [[ "${sale_bucket}" == "OPEN" ]]; then
    hold_target=$(( seat_count * (4 + RANDOM % 5) / 100 ))
    paying_target=$(( seat_count * (4 + RANDOM % 5) / 100 ))
    confirmed_target=$(( seat_count * (28 + RANDOM % 22) / 100 ))
    if (( hold_target < 2 )); then hold_target=2; fi
    if (( paying_target < 2 )); then paying_target=2; fi
    if (( confirmed_target < 8 )); then confirmed_target=8; fi
    max_occupied=$(( seat_count * 75 / 100 ))
    if (( max_occupied < 1 )); then max_occupied=1; fi
    desired=$(( hold_target + paying_target + confirmed_target ))
    if (( desired > max_occupied )); then
      local overflow=$(( desired - max_occupied ))
      if (( confirmed_target > overflow )); then
        confirmed_target=$(( confirmed_target - overflow ))
      else
        overflow=$(( overflow - confirmed_target ))
        confirmed_target=1
        if (( paying_target > overflow )); then
          paying_target=$(( paying_target - overflow ))
        else
          overflow=$(( overflow - paying_target ))
          paying_target=1
          hold_target=$(( hold_target > overflow ? hold_target - overflow : 1 ))
        fi
      fi
    fi
  else
    printf '%s|%s|%s|%s' "${seat_count}" 0 0 0
    return 0
  fi

  occupied_cursor=0

  for ((i=0; i<confirmed_target && occupied_cursor<seat_count; i++)); do
    seat_id="${shuffled[occupied_cursor]}"
    occupied_cursor=$((occupied_cursor + 1))
    if create_hold_with_state "${seat_id}" "CONFIRMED"; then
      confirmed_success=$((confirmed_success + 1))
    fi
  done

  for ((i=0; i<paying_target && occupied_cursor<seat_count; i++)); do
    seat_id="${shuffled[occupied_cursor]}"
    occupied_cursor=$((occupied_cursor + 1))
    if create_hold_with_state "${seat_id}" "PAYING"; then
      paying_success=$((paying_success + 1))
    fi
  done

  for ((i=0; i<hold_target && occupied_cursor<seat_count; i++)); do
    seat_id="${shuffled[occupied_cursor]}"
    occupied_cursor=$((occupied_cursor + 1))
    if create_hold_with_state "${seat_id}" "HOLD"; then
      hold_success=$((hold_success + 1))
    fi
  done

  printf '%s|%s|%s|%s' "${seat_count}" "${hold_success}" "${paying_success}" "${confirmed_success}"
}

seed_single_concert() {
  local index="${1:?index required}"
  local artist="${2:?artist required}"
  local entertainment="${3:?ent required}"
  local youtube_url="${4:?youtube required}"
  local target_seat_count="${5:?seat_count required}"
  local sale_bucket="${6:?sale_bucket required}"

  local title artist_slug entertainment_slug debut_date concert_date ticket_price sale_start_at
  local create_payload create_response concert_id option_payload option_response option_id policy_payload occupancy
  local seat_layout actual_seat_count hold_count paying_count confirmed_count

  artist_slug="$(slugify "${artist}")"
  entertainment_slug="$(slugify "${entertainment}")"
  title="${artist} LIVE IN SEOUL ${SEED_TAG}"
  debut_date="$(date -u -d "-$((6 + RANDOM % 10)) years" +"%Y-%m-%d")"
  concert_date="$(date -u -d "+$((20 + index)) days" +"%Y-%m-%dT19:00:00")"
  ticket_price=$((99000 + (RANDOM % 12) * 10000))
  sale_start_at="$(compute_general_sale_start "${sale_bucket}")"

  create_payload="$(jq -nc \
    --arg title "${title}" \
    --arg artistName "${artist}" \
    --arg entertainmentName "${entertainment}" \
    --arg artistDisplayName "${artist}" \
    --arg artistGenre "K-POP" \
    --arg artistDebutDate "${debut_date}" \
    --arg entertainmentCountryCode "KR" \
    --arg entertainmentHomepageUrl "https://${entertainment_slug}.example.com" \
    --arg promoterName "KPOP LIVE PROMOTIONS" \
    --arg youtubeVideoUrl "${youtube_url}" \
    '{
      title:$title,
      artistName:$artistName,
      entertainmentName:$entertainmentName,
      artistDisplayName:$artistDisplayName,
      artistGenre:$artistGenre,
      artistDebutDate:$artistDebutDate,
      entertainmentCountryCode:$entertainmentCountryCode,
      entertainmentHomepageUrl:$entertainmentHomepageUrl,
      promoterName:$promoterName,
      youtubeVideoUrl:$youtubeVideoUrl
    }')"
  create_response="$(api_json POST "/admin/concerts" "${create_payload}" 1)"
  concert_id="$(printf '%s' "${create_response}" | jq -r '.id // empty')"
  if [[ -z "${concert_id}" ]]; then
    echo "[ERROR] failed to create concert: ${artist}" >&2
    exit 1
  fi

  seat_layout="$(build_seat_layout_payload "${target_seat_count}")"
  option_payload="$(jq -nc \
    --arg concertDate "${concert_date}" \
    --argjson seatCount "${target_seat_count}" \
    --argjson seatLayout "${seat_layout}" \
    --argjson ticketPriceAmount "${ticket_price}" \
    '{concertDate:$concertDate,seatCount:$seatCount,seatLayout:$seatLayout,ticketPriceAmount:$ticketPriceAmount,venueId:null}')"
  option_response="$(api_json POST "/admin/concerts/${concert_id}/options" "${option_payload}" 1)"
  option_id="$(printf '%s' "${option_response}" | jq -r '.id // empty')"
  if [[ -z "${option_id}" ]]; then
    echo "[ERROR] failed to create option: concertId=${concert_id}" >&2
    exit 1
  fi

  if [[ "${sale_bucket}" != "UNSCHEDULED" ]]; then
    policy_payload="$(jq -nc \
      --arg generalSaleStartAt "${sale_start_at}" \
      --argjson maxReservationsPerUser "${MAX_RESERVATIONS_PER_USER}" \
      '{
        presaleStartAt:null,
        presaleEndAt:null,
        presaleMinimumTier:null,
        generalSaleStartAt:$generalSaleStartAt,
        maxReservationsPerUser:$maxReservationsPerUser
      }')"
    api_json PUT "/admin/concerts/${concert_id}/sales-policy" "${policy_payload}" 1 >/dev/null
  fi

  upload_thumbnail_from_youtube "${concert_id}" "${youtube_url}"
  if ! rewrite_option_seat_numbers_with_layout "${option_id}" "${seat_layout}"; then
    log "seat number rewrite skipped (optionId=${option_id}, docker/postgres unavailable or shape mismatch)"
  fi
  occupancy="$(apply_open_state_distribution "${option_id}" "${sale_bucket}")"
  actual_seat_count="$(printf '%s' "${occupancy}" | cut -d'|' -f1)"
  hold_count="$(printf '%s' "${occupancy}" | cut -d'|' -f2)"
  paying_count="$(printf '%s' "${occupancy}" | cut -d'|' -f3)"
  confirmed_count="$(printf '%s' "${occupancy}" | cut -d'|' -f4)"
  if [[ "${actual_seat_count}" == "0" ]]; then
    echo "[ERROR] seat map empty after option creation: concertId=${concert_id} optionId=${option_id}" >&2
    echo "[ERROR] check backend build/version and seatLayout support; request sent seatCount=${target_seat_count}" >&2
    exit 1
  fi
  SUMMARY_LINES+=("${concert_id}|${artist}|${sale_bucket}|${actual_seat_count}|${hold_count}|${paying_count}|${confirmed_count}")
}

main() {
  local admin_username admin_user_payload admin_user_response admin_user_id
  local row index artist entertainment youtube seat_count sale_bucket dataset_total
  local dataset_rows

  log "seed start api=${API_BASE} tag=${SEED_TAG}"

  cleanup_seeded_concerts "${SEED_KEYWORD}"
  cleanup_seeded_concerts "NEONIX" || true

  admin_username="seed_admin_${RUN_ID}"
  admin_user_payload="$(jq -nc --arg username "${admin_username}" '{username:$username,tier:"VIP"}')"
  admin_user_response="$(api_json POST "/users" "${admin_user_payload}" 0)"
  admin_user_id="$(printf '%s' "${admin_user_response}" | jq -r '.id // empty')"
  if [[ -z "${admin_user_id}" ]]; then
    echo "[ERROR] failed to create seed admin user" >&2
    exit 1
  fi

  ACCESS_TOKEN="$(build_access_token "${admin_user_id}" "${admin_username}" "ADMIN")"
  create_seed_users "${SEED_USER_COUNT}"

  dataset_rows="$(cat <<'DATASET'
Stray Kids|JYP Entertainment|https://www.youtube.com/watch?v=0P0aQreFs8w|260|OPEN_SOON_5M
BTS|BIGHIT MUSIC|https://www.youtube.com/watch?v=gdZLi9oWNZg|320|OPEN
Saja Boys|Netflix|https://www.youtube.com/watch?v=2FS3JAPTKXs|180|OPEN
BLACKPINK|YG Entertainment|https://www.youtube.com/watch?v=IHNzOHi8sJs|300|OPEN
NewJeans|ADOR|https://youtu.be/DAEK5GrLb_Y?si=rhJGnEY4sB0jay5s|260|OPEN_SOON_1H
LE SSERAFIM|SOURCE MUSIC|https://www.youtube.com/watch?v=pyf8cbqyfPs|240|OPEN_SOON_1H
TOMORROW X TOGETHER|BIGHIT MUSIC|https://www.youtube.com/watch?v=P9tKTxbgdkk|230|OPEN_SOON_1H
ITZY|JYP Entertainment|https://www.youtube.com/watch?v=fE2h3lGlOsk|220|PREOPEN
TWICE|JYP Entertainment|https://www.youtube.com/watch?v=i0p1bmr0EmE|280|PREOPEN
SEVENTEEN|PLEDIS Entertainment|https://www.youtube.com/watch?v=-GQg25oP0S4|280|PREOPEN
HUNTRIX|Sony Animation|https://www.youtube.com/watch?v=yebNIHKAC4A|170|SOLD_OUT
BIGBANG|YG Entertainment|https://www.youtube.com/watch?v=2ips2mM7Zqw|220|SOLD_OUT
BABYMONSTER|YG Entertainment|https://www.youtube.com/watch?v=2wA_b6YHjqQ|210|SOLD_OUT
KARA|DSP Media|https://www.youtube.com/watch?v=XwcK-twSXB4|170|SOLD_OUT
MAMAMOO|RBW|https://www.youtube.com/watch?v=KhTeiaCezwM|200|SOLD_OUT
(G)I-DLE|CUBE Entertainment|https://www.youtube.com/watch?v=Jh4QFaPmdss|230|SOLD_OUT
NMIXX|JYP Entertainment|https://www.youtube.com/watch?v=Rd2wppggYxo|200|SOLD_OUT
aespa|SM Entertainment|https://www.youtube.com/watch?v=4TWR90KJl84|250|UNSCHEDULED
IVE|STARSHIP Entertainment|https://www.youtube.com/watch?v=6ZUIwj3FgUY|240|OPEN_SOON_1H
STAYC|High Up Entertainment|https://www.youtube.com/watch?v=SxHmoifp0oQ|190|OPEN_SOON_1H
KISS OF LIFE|S2 Entertainment|https://www.youtube.com/watch?v=oKVYm8mIUdo|180|UNSCHEDULED
Red Velvet|SM Entertainment|https://www.youtube.com/watch?v=R9At2ICm4LQ|220|OPEN_SOON_1H
OH MY GIRL|WM Entertainment|https://www.youtube.com/watch?v=HzOjwL7IP_o|190|OPEN_SOON_1H
Apink|IST Entertainment|https://www.youtube.com/watch?v=K5H-GvnNz2Y|180|UNSCHEDULED
DATASET
)"
  dataset_total="$(printf '%s\n' "${dataset_rows}" | awk -F'|' 'NF >= 5 {count += 1} END {print count + 0}')"
  if [[ "${dataset_total}" == "0" ]]; then
    echo "[ERROR] dataset rows are empty" >&2
    exit 1
  fi

  index=0
  while IFS='|' read -r artist entertainment youtube seat_count sale_bucket; do
    if [[ -z "${artist}" ]]; then
      continue
    fi
    index=$((index + 1))
    log "seeding ${index}/${dataset_total} artist=${artist} status=${sale_bucket}"
    seed_single_concert "${index}" "${artist}" "${entertainment}" "${youtube}" "${seat_count}" "${sale_bucket}"
  done <<< "${dataset_rows}"

  log "seed completed. summary:"
  printf '%-6s %-22s %-13s %7s %7s %7s %10s\n' "ID" "ARTIST" "STATUS" "SEATS" "HOLD" "PAYING" "CONFIRMED"
  for row in "${SUMMARY_LINES[@]}"; do
    IFS='|' read -r concert_id artist sale_bucket seat_count hold_count paying_count confirmed_count <<< "${row}"
    printf '%-6s %-22s %-13s %7s %7s %7s %10s\n' \
      "${concert_id}" "${artist}" "${sale_bucket}" "${seat_count}" "${hold_count}" "${paying_count}" "${confirmed_count}"
  done

  echo
  curl -sS -f "${API_BASE}/concerts/search?page=0&size=300&sort=id,asc" \
    | jq -r '
      .items
      | map(select(.title | contains("'"${SEED_TAG}"'")))
      | group_by(.saleStatus)
      | map({status: .[0].saleStatus, count: length})
      | sort_by(.status)
      | .[] | "\(.status)\t\(.count)"'
}

main "$@"
