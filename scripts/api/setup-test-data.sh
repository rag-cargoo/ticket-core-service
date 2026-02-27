#!/usr/bin/env bash
set -euo pipefail

API_HOST="${API_HOST:-http://127.0.0.1:8080}"
SETUP_API="${SETUP_API:-${API_HOST}/api/concerts/setup}"
SEARCH_API="${SEARCH_API:-${API_HOST}/api/concerts/search}"
CLEANUP_API_PREFIX="${CLEANUP_API_PREFIX:-${API_HOST}/api/concerts/cleanup}"
CONTENT_TYPE="Content-Type: application/json"
SEED_MODE="${SEED_MODE:-top20}"                # top20 | single | random
SINGLE_INDEX="${SINGLE_INDEX:-0}"
RUN_TAG="${RUN_TAG:-KPOP20_20260227}"
START_OFFSET_DAYS="${START_OFFSET_DAYS:-10}"
START_TIME="${START_TIME:-19:00:00}"           # HH:MM:SS
DEFAULT_SEAT_COUNT="${DEFAULT_SEAT_COUNT:-120}"
DEFAULT_OPTION_COUNT="${DEFAULT_OPTION_COUNT:-2}"
REQUEST_DELAY_SEC="${REQUEST_DELAY_SEC:-0}"
CLEAN_BEFORE_SEED="${CLEAN_BEFORE_SEED:-1}"    # 1이면 기존 LIVE IN SEOUL 더미를 삭제 후 생성

BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

cleanup_existing_seed_concerts_by_prefix() {
  local title_prefix="$1"
  local encoded_keyword
  local search_response
  local ids

  encoded_keyword="$(urlencode "${title_prefix}")"
  search_response="$(curl -sS "${SEARCH_API}?keyword=${encoded_keyword}&page=0&size=500&sort=id,asc")"
  ids="$(echo "${search_response}" | jq -r --arg prefix "${title_prefix} " '.items[] | select((.title // "") | startswith($prefix)) | .id')"

  if [[ -z "${ids}" ]]; then
    return 0
  fi

  while IFS= read -r concert_id; do
    [[ -z "${concert_id}" ]] && continue
    local status_code
    status_code="$(curl -sS -o /tmp/setup_cleanup_${concert_id}.txt -w '%{http_code}' -X DELETE "${CLEANUP_API_PREFIX}/${concert_id}")"
    if [[ "${status_code}" != "200" ]]; then
      echo -e "${RED}Failed to cleanup existing seeded concert id=${concert_id} (status=${status_code})${NC}"
      cat "/tmp/setup_cleanup_${concert_id}.txt"
      exit 1
    fi
    echo -e "${YELLOW}  - cleaned existing seeded concert id=${concert_id}${NC}"
  done <<< "${ids}"
}

cleanup_all_live_in_seoul_seed_concerts() {
  local encoded_keyword
  local search_response
  local ids

  encoded_keyword="$(urlencode "LIVE IN SEOUL")"
  search_response="$(curl -sS "${SEARCH_API}?keyword=${encoded_keyword}&page=0&size=500&sort=id,asc")"
  ids="$(echo "${search_response}" | jq -r '.items[] | select((.title // "") | test("LIVE IN SEOUL")) | .id')"

  if [[ -z "${ids}" ]]; then
    return 0
  fi

  echo -e "${YELLOW}Cleaning existing LIVE IN SEOUL seed concerts...${NC}"
  while IFS= read -r concert_id; do
    [[ -z "${concert_id}" ]] && continue
    local status_code
    status_code="$(curl -sS -o /tmp/setup_cleanup_all_${concert_id}.txt -w '%{http_code}' -X DELETE "${CLEANUP_API_PREFIX}/${concert_id}")"
    if [[ "${status_code}" != "200" ]]; then
      echo -e "${RED}Failed to cleanup existing seeded concert id=${concert_id} (status=${status_code})${NC}"
      cat "/tmp/setup_cleanup_all_${concert_id}.txt"
      exit 1
    fi
    echo -e "${YELLOW}  - cleaned id=${concert_id}${NC}"
  done <<< "${ids}"
}

# artistName|artistDisplayName|artistGenre|artistDebutDate|entertainmentName|entCountryCode|entHomepage|promoterName|promoterCountryCode|promoterHomepage|venueName|venueCity|venueCountryCode|venueAddress|youtubeVideoUrl|seatCount|optionCount
mapfile -t SEED_ROWS <<'EOF'
Stray Kids|Stray Kids|K-POP|2018-03-25|JYP Entertainment|KR|https://www.jype.com|JYP Live|KR|https://www.jype.com|Jangchung Arena|Seoul|KR|241 Dongho-ro, Jung-gu, Seoul|https://www.youtube.com/watch?v=0P0aQreFs8w|140|2
BTS|BTS|K-POP|2013-06-13|BIGHIT MUSIC|KR|https://ibighit.com|HYBE Live|KR|https://hybecorp.com|Jamsil Main Stadium|Seoul|KR|25 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=gdZLi9oWNZg|180|2
BLACKPINK|BLACKPINK|K-POP|2016-08-08|YG Entertainment|KR|https://www.ygfamily.com|YG PLUS Live|KR|https://www.ygplus.com|KSPO Dome|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=IHNzOHi8sJs|160|2
SEVENTEEN|SEVENTEEN|K-POP|2015-05-26|PLEDIS Entertainment|KR|https://www.pledis.co.kr|PLEDIS Live|KR|https://www.pledis.co.kr|Inspire Arena|Incheon|KR|127 Gonghangmunhwa-ro, Jung-gu, Incheon|https://www.youtube.com/watch?v=-GQg25oP0S4|150|2
TWICE|TWICE|K-POP|2015-10-20|JYP Entertainment|KR|https://www.jype.com|JYP Live|KR|https://www.jype.com|KSPO Dome|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=i0p1bmr0EmE|150|2
NewJeans|NewJeans|K-POP|2022-07-22|ADOR|KR|https://ador.world|HYBE Live|KR|https://hybecorp.com|BEXCO Auditorium|Busan|KR|55 APEC-ro, Haeundae-gu, Busan|https://www.youtube.com/watch?v=ArmDp-zijuc|130|2
ILLIT|ILLIT|K-POP|2024-03-25|BELIFT LAB|KR|https://beliftlab.com|BELIFT Live|KR|https://beliftlab.com|Blue Square Mastercard Hall|Seoul|KR|294 Itaewon-ro, Yongsan-gu, Seoul|https://www.youtube.com/watch?v=Vk5-c_v4gMU|120|2
IVE|IVE|K-POP|2021-12-01|STARSHIP Entertainment|KR|https://www.starship-ent.com|Starship Square|KR|https://www.starship-ent.com|KSPO Dome|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=6ZUIwj3FgUY|140|2
BIGBANG|BIGBANG|K-POP|2006-08-19|YG Entertainment|KR|https://www.ygfamily.com|YG PLUS Live|KR|https://www.ygplus.com|Jamsil Indoor Stadium|Seoul|KR|25 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=2ips2mM7Zqw|140|2
LE SSERAFIM|LE SSERAFIM|K-POP|2022-05-02|SOURCE MUSIC|KR|https://www.sourcemusic.com|HYBE Live|KR|https://hybecorp.com|Inspire Arena|Incheon|KR|127 Gonghangmunhwa-ro, Jung-gu, Incheon|https://www.youtube.com/watch?v=pyf8cbqyfPs|135|2
Saja Boys|Saja Boys|K-POP|2025-06-20|KPop Demon Hunters Universe|US|https://www.netflix.com|Netflix K-Content Live|US|https://www.netflix.com|Yes24 Live Hall|Seoul|KR|6 Gucheonmyeon-ro, Gwangjin-gu, Seoul|https://www.youtube.com/watch?v=2FS3JAPTKXs|110|2
aespa|aespa|K-POP|2020-11-17|SM Entertainment|KR|https://www.smtown.com|Dream Maker Ent|KR|https://www.dreammakerlive.com|Inspire Arena|Incheon|KR|127 Gonghangmunhwa-ro, Jung-gu, Incheon|https://www.youtube.com/watch?v=4TWR90KJl84|145|2
HUNTRIX|HUNTRIX|K-POP|2025-06-20|KPop Demon Hunters Universe|US|https://www.netflix.com|Sony Animation Live|US|https://www.sonypicturesanimation.com|Olympic Hall|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=yebNIHKAC4A|110|2
(G)I-DLE|(G)I-DLE|K-POP|2018-05-02|CUBE Entertainment|KR|https://www.cubeent.co.kr|Cube Live|KR|https://www.cubeent.co.kr|Olympic Hall|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=Jh4QFaPmdss|120|2
TOMORROW X TOGETHER|TOMORROW X TOGETHER|K-POP|2019-03-04|BIGHIT MUSIC|KR|https://ibighit.com|HYBE Live|KR|https://hybecorp.com|Jamsil Indoor Stadium|Seoul|KR|25 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=P9tKTxbgdkk|130|2
BABYMONSTER|BABYMONSTER|K-POP|2023-11-27|YG Entertainment|KR|https://www.ygfamily.com|YG PLUS Live|KR|https://www.ygplus.com|KSPO Dome|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=2wA_b6YHjqQ|125|2
MAMAMOO|MAMAMOO|K-POP|2014-06-19|RBW|KR|https://www.rbbridge.com|RBW Live|KR|https://www.rbbridge.com|Jamsil Indoor Stadium|Seoul|KR|25 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=KhTeiaCezwM|125|2
STAYC|STAYC|K-POP|2020-11-12|High Up Entertainment|KR|https://highup-ent.com|High Up Live|KR|https://highup-ent.com|Olympic Hall|Seoul|KR|424 Olympic-ro, Songpa-gu, Seoul|https://www.youtube.com/watch?v=SxHmoifp0oQ|110|2
KARA|KARA|K-POP|2007-03-29|DSP Media|KR|https://www.dspmedia.co.kr|DSP Live|KR|https://www.dspmedia.co.kr|KBS Arena|Seoul|KR|376 Gonghang-daero, Gangseo-gu, Seoul|https://www.youtube.com/watch?v=XwcK-twSXB4|110|2
KISS OF LIFE|KISS OF LIFE|K-POP|2023-07-05|S2 Entertainment|KR|https://www.s2-ent.co.kr|S2 Live|KR|https://www.s2-ent.co.kr|Yes24 Live Hall|Seoul|KR|6 Gucheonmyeon-ro, Gwangjin-gu, Seoul|https://www.youtube.com/watch?v=oKVYm8mIUdo|95|2
EOF

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[Admin] Setting up concert dummy data...${NC}"
echo -e "${BLUE}Mode=${SEED_MODE}, API=${SETUP_API}${NC}"
echo -e "${BLUE}====================================================${NC}"

declare -a target_rows
case "${SEED_MODE}" in
  top20|all)
    target_rows=("${SEED_ROWS[@]}")
    ;;
  single)
    if ! [[ "${SINGLE_INDEX}" =~ ^[0-9]+$ ]]; then
      echo -e "${RED}SINGLE_INDEX must be a non-negative integer.${NC}"
      exit 1
    fi
    if (( SINGLE_INDEX < 0 || SINGLE_INDEX >= ${#SEED_ROWS[@]} )); then
      echo -e "${RED}SINGLE_INDEX out of range. max=$(( ${#SEED_ROWS[@]} - 1 ))${NC}"
      exit 1
    fi
    target_rows=("${SEED_ROWS[${SINGLE_INDEX}]}")
    ;;
  random)
    random_index=$((RANDOM % ${#SEED_ROWS[@]}))
    target_rows=("${SEED_ROWS[${random_index}]}")
    ;;
  *)
    echo -e "${RED}Unknown SEED_MODE: ${SEED_MODE}. Use top20|single|random.${NC}"
    exit 1
    ;;
esac

declare -a created_concert_ids=()
declare -a created_option_ids=()

if [[ "${CLEAN_BEFORE_SEED}" = "1" && ( "${SEED_MODE}" = "top20" || "${SEED_MODE}" = "all" ) ]]; then
  cleanup_all_live_in_seoul_seed_concerts
fi

for index in "${!target_rows[@]}"; do
  row="${target_rows[${index}]}"
  IFS='|' read -r \
    artist_name \
    artist_display_name \
    artist_genre \
    artist_debut_date \
    entertainment_name \
    entertainment_country_code \
    entertainment_homepage_url \
    promoter_name \
    promoter_country_code \
    promoter_homepage_url \
    venue_name \
    venue_city \
    venue_country_code \
    venue_address \
    youtube_video_url \
    row_seat_count \
    row_option_count <<< "${row}"

  seat_count="${row_seat_count:-${DEFAULT_SEAT_COUNT}}"
  option_count="${OPTION_COUNT:-${row_option_count:-${DEFAULT_OPTION_COUNT}}}"
  day_offset=$((START_OFFSET_DAYS + index))
  concert_date="$(date -d "+${day_offset} days" +"%Y-%m-%d")T${START_TIME}"
  concert_title_prefix="${artist_display_name} LIVE IN SEOUL"
  if [[ "${CLEAN_BEFORE_SEED}" = "1" && "${SEED_MODE}" != "top20" && "${SEED_MODE}" != "all" ]]; then
    cleanup_existing_seed_concerts_by_prefix "${concert_title_prefix}"
  fi
  concert_title="${concert_title_prefix} ${RUN_TAG}"

  payload="$(jq -nc \
    --arg title "${concert_title}" \
    --arg artistName "${artist_name}" \
    --arg artistDisplayName "${artist_display_name}" \
    --arg artistGenre "${artist_genre}" \
    --arg artistDebutDate "${artist_debut_date}" \
    --arg entertainmentName "${entertainment_name}" \
    --arg entertainmentCountryCode "${entertainment_country_code}" \
    --arg entertainmentHomepageUrl "${entertainment_homepage_url}" \
    --arg promoterName "${promoter_name}" \
    --arg promoterCountryCode "${promoter_country_code}" \
    --arg promoterHomepageUrl "${promoter_homepage_url}" \
    --arg venueName "${venue_name}" \
    --arg venueCity "${venue_city}" \
    --arg venueCountryCode "${venue_country_code}" \
    --arg venueAddress "${venue_address}" \
    --arg concertDate "${concert_date}" \
    --argjson seatCount "${seat_count}" \
    --argjson optionCount "${option_count}" \
    --arg youtubeVideoUrl "${youtube_video_url}" \
    '{
      title: $title,
      artistName: $artistName,
      artistDisplayName: $artistDisplayName,
      artistGenre: $artistGenre,
      artistDebutDate: $artistDebutDate,
      entertainmentName: $entertainmentName,
      entertainmentCountryCode: $entertainmentCountryCode,
      entertainmentHomepageUrl: $entertainmentHomepageUrl,
      promoterName: $promoterName,
      promoterCountryCode: $promoterCountryCode,
      promoterHomepageUrl: $promoterHomepageUrl,
      venueName: $venueName,
      venueCity: $venueCity,
      venueCountryCode: $venueCountryCode,
      venueAddress: $venueAddress,
      concertDate: $concertDate,
      seatCount: $seatCount,
      optionCount: $optionCount,
      youtubeVideoUrl: $youtubeVideoUrl
    }')"

  echo -ne "${YELLOW}[${index}] ${artist_display_name}... ${NC}"
  raw_response="$(curl -sS -w '\n%{http_code}' -X POST "${SETUP_API}" -H "${CONTENT_TYPE}" -d "${payload}")"
  response_body="$(printf '%s' "${raw_response}" | sed '$d')"
  status_code="$(printf '%s' "${raw_response}" | tail -n 1)"
  if [[ ! "${status_code}" =~ ^2 ]]; then
    echo -e "${RED}FAILED (${status_code})${NC}"
    echo "Response: ${response_body}"
    exit 1
  fi

  concert_id="$(echo "${response_body}" | grep -oP 'ConcertID=\K\d+' | head -n 1 || true)"
  option_id="$(echo "${response_body}" | grep -oP 'OptionID=\K\d+' | head -n 1 || true)"
  if [[ -z "${concert_id}" || -z "${option_id}" ]]; then
    echo -e "${RED}FAILED (id parse)${NC}"
    echo "Response: ${response_body}"
    exit 1
  fi

  created_concert_ids+=("${concert_id}")
  created_option_ids+=("${option_id}")
  echo -e "${GREEN}OK concertId=${concert_id}, optionId=${option_id}${NC}"
  if [[ "${REQUEST_DELAY_SEC}" != "0" ]]; then
    sleep "${REQUEST_DELAY_SEC}"
  fi
done

echo -e "${BLUE}====================================================${NC}"
echo -e "${GREEN}Ready for testing! Seeded=${#created_concert_ids[@]}${NC}"
echo "Setup completed: ConcertID=${created_concert_ids[0]}, OptionID=${created_option_ids[0]}, SeededCount=${#created_concert_ids[@]}"
echo -e "${BLUE}====================================================${NC}"
