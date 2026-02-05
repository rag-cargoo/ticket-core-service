#!/bin/bash
source "$(dirname "$0")/../common/env.sh"

CONCURRENCY=5
echo -e "${BLUE}Firing ${CONCURRENCY} concurrent requests to [v3] Distributed Lock...${NC}"

for i in $(seq 1 ${CONCURRENCY})
do
    curl -s -X POST "${BASE_URL}/v3/distributed-lock" \
         -H "${CONTENT_TYPE}" \
         -d "{\"userId\": $i, \"seatId\": ${DEFAULT_SEAT_ID}}" &
done

wait
echo -e "\n${GREEN}All requests finished.${NC}"

