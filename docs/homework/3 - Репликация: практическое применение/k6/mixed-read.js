import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8075';
const searchDataFile = __ENV.SEARCH_DATA_FILE || './user-search-data.json';
const fixedFirstName = __ENV.FIRST_NAME || '';
const fixedLastName = __ENV.LAST_NAME || '';
const summaryFile = __ENV.SUMMARY_FILE || './results/mixed-read-summary.json';

const vus = Number.parseInt(__ENV.K6_VUS || '10', 10);
const duration = __ENV.K6_DURATION || '30s';
const minUserId = Number.parseInt(__ENV.USER_ID_MIN || '1', 10);
const maxUserId = Number.parseInt(__ENV.USER_ID_MAX || '999999', 10);
const userGetRatio = Number.parseFloat(__ENV.USER_GET_RATIO || '0.5');
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || 'rate<0.01';
const userGetP95ThresholdMs = __ENV.USER_GET_P95_THRESHOLD_MS || '500';
const userSearchP95ThresholdMs = __ENV.USER_SEARCH_P95_THRESHOLD_MS || '500';
const searchPairs = JSON.parse(open(searchDataFile));

const userGetDuration = new Trend('user_get_duration', true);
const userSearchDuration = new Trend('user_search_duration', true);

function resolveSearchPair() {
  if (fixedFirstName && fixedLastName) {
    return {
      firstName: fixedFirstName,
      lastName: fixedLastName,
    };
  }

  return searchPairs[exec.scenario.iterationInTest % searchPairs.length];
}

function randomUserId() {
  return Math.floor(Math.random() * (maxUserId - minUserId + 1)) + minUserId;
}

function callUserGet() {
  const userId = randomUserId();
  const response = http.get(`${baseUrl}/user/get/${userId}`, {
    tags: { name: 'user_get' },
    headers: { Accept: 'application/json' },
  });

  userGetDuration.add(response.timings.duration);

  check(response, {
    'user get status is 200': (r) => r.status === 200,
    'user get content type is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });
}

function callUserSearch() {
  const searchPair = resolveSearchPair();
  const response = http.get(
    `${baseUrl}/user/search?firstName=${encodeURIComponent(searchPair.firstName)}&lastName=${encodeURIComponent(searchPair.lastName)}`,
    {
      tags: { name: 'user_search' },
      headers: { Accept: 'application/json' },
    }
  );

  userSearchDuration.add(response.timings.duration);

  check(response, {
    'user search status is 200': (r) => r.status === 200,
    'user search content type is json': (r) => (r.headers['Content-Type'] || '').includes('application/json'),
  });
}

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [errorRateThreshold],
    user_get_duration: [`p(95)<${userGetP95ThresholdMs}`],
    user_search_duration: [`p(95)<${userSearchP95ThresholdMs}`],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  if (Math.random() < userGetRatio) {
    callUserGet();
    return;
  }

  callUserSearch();
}

export function handleSummary(data) {
  return {
    [summaryFile]: JSON.stringify(data, null, 2),
    stdout: '\n' + JSON.stringify({
      vus,
      duration,
      userGetRatio,
      userIdRange: {
        min: minUserId,
        max: maxUserId,
      },
      searchMode: fixedFirstName && fixedLastName ? 'single-pair' : 'dataset',
      fixedFirstName: fixedFirstName || null,
      fixedLastName: fixedLastName || null,
      searchDataFile,
      searchPairs: searchPairs.length,
      http_req_failed: data.metrics.http_req_failed,
      user_get_duration: data.metrics.user_get_duration,
      user_search_duration: data.metrics.user_search_duration,
      checks: data.metrics.checks,
    }, null, 2) + '\n',
  };
}
