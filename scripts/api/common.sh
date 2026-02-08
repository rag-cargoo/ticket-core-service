#!/usr/bin/env bash

resolve_latest_option_id() {
    local concert_api="$1"
    local concert_id
    local option_id

    concert_id=$(curl -s "${concert_api}" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
    if [[ -z "${concert_id}" ]]; then
        return 1
    fi

    option_id=$(curl -s "${concert_api}/${concert_id}/options" | grep -oP '"id":\s*\K\d+' | tail -n 1 || true)
    if [[ -z "${option_id}" ]]; then
        return 1
    fi

    echo "${option_id}"
}

ensure_test_data() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    bash "${script_dir}/setup-test-data.sh" >/dev/null
}

find_available_seat_id() {
    local concert_api="$1"
    local option_id
    local seat_id

    option_id=$(resolve_latest_option_id "${concert_api}" || true)
    if [[ -z "${option_id}" ]]; then
        ensure_test_data || true
        option_id=$(resolve_latest_option_id "${concert_api}" || true)
    fi

    if [[ -z "${option_id}" ]]; then
        return 1
    fi

    seat_id=$(curl -s "${concert_api}/options/${option_id}/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1 || true)
    if [[ -z "${seat_id}" ]]; then
        ensure_test_data || true
        option_id=$(resolve_latest_option_id "${concert_api}" || true)
        seat_id=$(curl -s "${concert_api}/options/${option_id}/seats" | grep -oP '"status":"AVAILABLE".*?"id":\s*\K\d+' | head -n 1 || true)
    fi

    if [[ -z "${seat_id}" ]]; then
        return 1
    fi

    echo "${seat_id}"
}
