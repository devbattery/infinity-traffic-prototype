import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    auth_flow: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '20s', target: 30 },
        { duration: '30s', target: 30 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1200', 'p(99)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const suffix = `${__VU}-${__ITER}-${Date.now()}`;
  const username = `k6_user_${suffix}`;
  const password = 'Password!1234';

  const registerPayload = JSON.stringify({ username, password });
  const commonHeaders = {
    'Content-Type': 'application/json',
    'X-Trace-Id': `k6-trace-${suffix}`,
  };

  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    registerPayload,
    { headers: commonHeaders },
  );

  check(registerRes, {
    'register status is 200': (r) => r.status === 200,
  });

  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    registerPayload,
    { headers: commonHeaders },
  );

  const loginOk = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => {
      try {
        return !!r.json('accessToken');
      } catch (e) {
        return false;
      }
    },
  });

  if (loginOk) {
    const token = loginRes.json('accessToken');
    const validateRes = http.get(`${BASE_URL}/api/auth/validate`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Trace-Id': `k6-validate-${suffix}`,
      },
    });

    check(validateRes, {
      'validate status is 200': (r) => r.status === 200,
      'validate response valid true': (r) => r.json('valid') === true,
    });
  }

  sleep(0.2);
}
