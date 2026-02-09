import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    ingest_and_query: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '20s', target: 40 },
        { duration: '40s', target: 40 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1800', 'p(99)<2800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const REGIONS = ['SEOUL', 'BUSAN', 'INCHEON', 'DAEJEON', 'GWANGJU'];

function randomRegion() {
  return REGIONS[Math.floor(Math.random() * REGIONS.length)];
}

function randomRoad(region) {
  return `${region}-road-${Math.floor(Math.random() * 200)}`;
}

export default function () {
  const suffix = `${__VU}-${__ITER}-${Date.now()}`;
  const region = randomRegion();

  const eventPayload = JSON.stringify({
    region,
    roadName: randomRoad(region),
    averageSpeedKph: Math.floor(Math.random() * 90),
    congestionLevel: 1 + Math.floor(Math.random() * 5),
    observedAt: new Date().toISOString(),
  });

  const ingestRes = http.post(
    `${BASE_URL}/api/traffic/events`,
    eventPayload,
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Trace-Id': `k6-ingest-${suffix}`,
      },
    },
  );

  check(ingestRes, {
    'ingest status is 202': (r) => r.status === 202,
  });

  const summaryRes = http.get(`${BASE_URL}/api/traffic/summary?region=${region}`, {
    headers: {
      'X-Trace-Id': `k6-summary-${suffix}`,
    },
  });

  check(summaryRes, {
    'summary status is 200': (r) => r.status === 200,
  });

  const recentRes = http.get(`${BASE_URL}/api/traffic/events/recent?limit=20`, {
    headers: {
      'X-Trace-Id': `k6-recent-${suffix}`,
    },
  });

  check(recentRes, {
    'recent status is 200': (r) => r.status === 200,
  });

  sleep(0.15);
}
