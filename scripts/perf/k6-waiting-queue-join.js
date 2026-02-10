import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const apiHost = __ENV.API_HOST || 'http://127.0.0.1:8080';
const concertId = Number(__ENV.CONCERT_ID || 1);
const vus = Number(__ENV.VUS || 60);
const duration = __ENV.DURATION || '60s';
const sleepMaxSec = Number(__ENV.SLEEP_MAX_SEC || 0.2);
const userIdBase = Number(__ENV.USER_ID_BASE || 900000000);

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<700', 'p(99)<1200'],
    checks: ['rate>0.95'],
    join_valid_status_rate: ['rate>0.95'],
  },
};

const joinAccepted = new Counter('join_accepted');
const joinRejected = new Counter('join_rejected');
const joinUnknown = new Counter('join_unknown');
const joinHttpError = new Counter('join_http_error');
const joinValidStatusRate = new Rate('join_valid_status_rate');

function buildUserId() {
  return userIdBase + __VU * 100000 + __ITER;
}

export default function () {
  const userId = buildUserId();
  const payload = JSON.stringify({ userId, concertId });
  const params = { headers: { 'Content-Type': 'application/json' } };

  const response = http.post(`${apiHost}/api/v1/waiting-queue/join`, payload, params);

  const requestOk = check(response, {
    'join HTTP 200': (r) => r.status === 200,
  });

  if (!requestOk) {
    joinHttpError.add(1);
    joinValidStatusRate.add(false);
    sleep(Math.random() * sleepMaxSec);
    return;
  }

  let queueStatus = '';
  try {
    queueStatus = response.json('status');
  } catch (_) {
    queueStatus = '';
  }

  const isKnownStatus =
    queueStatus === 'WAITING' || queueStatus === 'ACTIVE' || queueStatus === 'REJECTED';
  joinValidStatusRate.add(isKnownStatus);

  if (queueStatus === 'WAITING' || queueStatus === 'ACTIVE') {
    joinAccepted.add(1);
  } else if (queueStatus === 'REJECTED') {
    joinRejected.add(1);
  } else {
    joinUnknown.add(1);
  }

  sleep(Math.random() * sleepMaxSec);
}
