import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8075';
const summaryFile = __ENV.SUMMARY_FILE || './results/user-get-summary.json';

const vus = Number.parseInt(__ENV.K6_VUS || '10', 10);
const duration = __ENV.K6_DURATION || '30s';
const minUserId = Number.parseInt(__ENV.USER_ID_MIN || '1', 10);
const maxUserId = Number.parseInt(__ENV.USER_ID_MAX || '999999', 10);
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || 'rate<0.01';
const p95ThresholdMs = __ENV.K6_P95_THRESHOLD_MS || '500';

const userGetDuration = new Trend('user_get_duration', true);

function randomUserId() {
  return Math.floor(Math.random() * (maxUserId - minUserId + 1)) + minUserId;
}

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [errorRateThreshold],
    user_get_duration: [`p(95)<${p95ThresholdMs}`],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  const userId = randomUserId();
  const response = http.get(`${baseUrl}/user/get/${userId}`, {
    tags: { name: 'user_get' },
    headers: { Accept: 'application/json' },
  });

  userGetDuration.add(response.timings.duration);

  check(response, {
    'status is 200': (r) => r.status === 200,
    'content type is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });
}

export function handleSummary(data) {
  return {
    [summaryFile]: JSON.stringify(data, null, 2),
    stdout: '\n' + JSON.stringify({
      vus,
      duration,
      userIdRange: {
        min: minUserId,
        max: maxUserId,
      },
      http_req_failed: data.metrics.http_req_failed,
      user_get_duration: data.metrics.user_get_duration,
      checks: data.metrics.checks,
    }, null, 2) + '\n',
  };
}
