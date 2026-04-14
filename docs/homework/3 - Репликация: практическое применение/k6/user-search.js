import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8075';
const searchDataFile = __ENV.SEARCH_DATA_FILE || './user-search-data.json';
const fixedFirstName = __ENV.FIRST_NAME || '';
const fixedLastName = __ENV.LAST_NAME || '';
const summaryFile = __ENV.SUMMARY_FILE || './results/user-search-summary.json';

const vus = Number.parseInt(__ENV.K6_VUS || '10', 10);
const duration = __ENV.K6_DURATION || '30s';
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || 'rate<0.01';
const p95ThresholdMs = __ENV.K6_P95_THRESHOLD_MS || '500';
const searchPairs = JSON.parse(open(searchDataFile));

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [errorRateThreshold],
    http_req_duration: [`p(95)<${p95ThresholdMs}`],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

function resolveSearchPair() {
  if (fixedFirstName && fixedLastName) {
    return {
      firstName: fixedFirstName,
      lastName: fixedLastName,
    };
  }

  return searchPairs[exec.scenario.iterationInTest % searchPairs.length];
}

export default function () {
  const searchPair = resolveSearchPair();
  const response = http.get(
    `${baseUrl}/user/search?firstName=${encodeURIComponent(searchPair.firstName)}&lastName=${encodeURIComponent(searchPair.lastName)}`,
    {
      tags: { name: 'user_search' },
      headers: { Accept: 'application/json' },
    }
  );

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
      searchMode: fixedFirstName && fixedLastName ? 'single-pair' : 'dataset',
      fixedFirstName: fixedFirstName || null,
      fixedLastName: fixedLastName || null,
      searchDataFile,
      searchPairs: searchPairs.length,
      http_req_failed: data.metrics.http_req_failed,
      http_req_duration: data.metrics.http_req_duration,
      checks: data.metrics.checks,
    }, null, 2) + '\n',
  };
}
