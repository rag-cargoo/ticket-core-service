#!/bin/bash
source "$(dirname "$0")/../common/env.sh"
[ -f "$(dirname "$0")/../common/last_setup.sh" ] && source "$(dirname "$0")/../common/last_setup.sh"

SEAT_ID=${LATEST_SEAT_ID}
USER_ID=1

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}[v4] Async Queue Polling Test (Seat: ${SEAT_ID})${NC}"
echo -e "${BLUE}====================================================${NC}"

# 1. 예약 요청 (Enqueued)
echo -e "${YELLOW}[Step 1] Sending Async Reservation Request...${NC}"
curl -s -X POST "http://localhost:8080/api/reservations/v4-opt/queue-polling" -H "Content-Type: application/json" -d "{\"userId\": ${USER_ID}, \"seatId\": ${SEAT_ID}}"

echo -e "\n\n${YELLOW}[Step 2] Polling Status...${NC}"
for i in {1..5}
do
    STATUS=$(curl -s "http://localhost:8080/api/reservations/v4/status?userId=${USER_ID}&seatId=${SEAT_ID}")
    echo -e " - Current Status (Attempt $i): ${GREEN}${STATUS}${NC}"
    if [ "$STATUS" == "SUCCESS" ]; then
        echo -e "\n${GREEN}Test Passed! Reservation successfully processed via Kafka.${NC}"
        break
    fi
    sleep 1
done

echo -e "${BLUE}====================================================${NC}"
