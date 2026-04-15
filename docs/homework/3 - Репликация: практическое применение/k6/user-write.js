import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8075';
const writeDataFile = __ENV.WRITE_DATA_FILE || './user-write-data.json';
const summaryFile = __ENV.SUMMARY_FILE || './results/user-write-summary.json';
const loginPrefix = __ENV.WRITE_LOGIN_PREFIX || 'k6w';
const runId = __ENV.WRITE_RUN_ID || `${Date.now()}`;
const password = __ENV.WRITE_PASSWORD || 'test_password';
const dbSchema = __ENV.DB_SCHEMA || 'highload_social_web';

const vus = Number.parseInt(__ENV.K6_VUS || '10', 10);
const duration = __ENV.K6_DURATION || '30s';
const errorRateThreshold = __ENV.K6_ERROR_RATE_THRESHOLD || 'rate<0.01';
const writeP95ThresholdMs = __ENV.USER_WRITE_P95_THRESHOLD_MS || '1000';
const successRateThreshold = __ENV.USER_WRITE_SUCCESS_RATE_THRESHOLD || 'rate>0.99';
const data = JSON.parse(open(writeDataFile));

const successfulWrites = new Counter('successful_writes');
const failedWrites = new Counter('failed_writes');
const userWriteDuration = new Trend('user_write_duration', true);
const writeSuccessRate = new Rate('write_success_rate');

function randomInt(minInclusive, maxInclusive) {
  return Math.floor(Math.random() * (maxInclusive - minInclusive + 1)) + minInclusive;
}

function randomItem(items) {
  return items[randomInt(0, items.length - 1)];
}

function randomBirthDate() {
  const year = randomInt(1960, 2005);
  const month = `${randomInt(1, 12)}`.padStart(2, '0');
  const day = `${randomInt(1, 28)}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function randomInterests() {
  const count = randomInt(1, 3);
  const selected = [];

  while (selected.length < count) {
    const interest = randomItem(data.interests);
    if (!selected.includes(interest)) {
      selected.push(interest);
    }
  }

  return selected.join(', ');
}

function uniqueLogin() {
  const vu = exec.vu.idInTest;
  const iteration = exec.scenario.iterationInTest;
  const suffix = randomInt(100000, 999999);
  return `${loginPrefix}-${runId}-${vu}-${iteration}-${suffix}`;
}

function generateUser() {
  const male = Math.random() < 0.5;
  return {
    login: uniqueLogin(),
    password,
    firstName: male ? randomItem(data.maleFirstNames) : randomItem(data.femaleFirstNames),
    lastName: male ? randomItem(data.maleLastNames) : randomItem(data.femaleLastNames),
    birthDate: randomBirthDate(),
    gender: male ? 'male' : 'female',
    interests: randomInterests(),
    city: randomItem(data.cities),
  };
}

function hasNumericId(response) {
  try {
    const id = response.json('id');
    return typeof id === 'number' && id > 0;
  } catch (_) {
    return false;
  }
}

export const options = {
  vus,
  duration,
  thresholds: {
    http_req_failed: [errorRateThreshold],
    user_write_duration: [`p(95)<${writeP95ThresholdMs}`],
    write_success_rate: [successRateThreshold],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  const payload = JSON.stringify(generateUser());
  const response = http.post(`${baseUrl}/user/register`, payload, {
    tags: { name: 'user_register_write' },
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  });

  userWriteDuration.add(response.timings.duration);

  const ok = response.status === 201 && hasNumericId(response);
  writeSuccessRate.add(ok);

  if (ok) {
    successfulWrites.add(1);
  } else {
    failedWrites.add(1);
  }

  check(response, {
    'user register status is 201': (r) => r.status === 201,
    'user register response has id': (r) => hasNumericId(r),
  });
}

function metricCount(summary, name) {
  return summary.metrics[name]?.values?.count || 0;
}

export function handleSummary(summary) {
  const successfulWriteCount = metricCount(summary, 'successful_writes');
  const failedWriteCount = metricCount(summary, 'failed_writes');

  return {
    [summaryFile]: JSON.stringify(summary, null, 2),
    stdout: '\n' + JSON.stringify({
      vus,
      duration,
      baseUrl,
      endpoint: 'POST /user/register',
      writeDataFile,
      loginPrefix,
      runId,
      loginLikePattern: `${loginPrefix}-${runId}-%`,
      successfulWrites: successfulWriteCount,
      failedWrites: failedWriteCount,
      countSql: `SELECT count(*) FROM ${dbSchema}.users WHERE login LIKE '${loginPrefix}-${runId}-%';`,
      http_req_failed: summary.metrics.http_req_failed,
      user_write_duration: summary.metrics.user_write_duration,
      write_success_rate: summary.metrics.write_success_rate,
      checks: summary.metrics.checks,
    }, null, 2) + '\n',
  };
}
