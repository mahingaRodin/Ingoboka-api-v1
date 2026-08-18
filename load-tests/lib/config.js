/**
 * Shared k6 configuration for Ingoboka API load tests.
 *
 * Env vars (required for login):
 *   BASE_URL  - API root (default http://localhost:8085/api/v1)
 *   EMAIL     - Login identifier (email or phone)
 *   PASSWORD  - Login password (no default — set via env or load-tests/.env)
 */

export function getBaseUrl() {
  return (__ENV.BASE_URL || 'http://localhost:8085/api/v1').replace(/\/$/, '');
}

export function getHealthUrl() {
  const origin = getBaseUrl().replace(/\/api\/v1$/i, '');
  return `${origin}/actuator/health`;
}

export function getCredentials() {
  const identifier = __ENV.EMAIL;
  const password = __ENV.PASSWORD;
  if (!identifier || !password) {
    throw new Error(
      'EMAIL and PASSWORD are required. Copy load-tests/.env.example to load-tests/.env ' +
        'or export them before running k6.',
    );
  }
  return { identifier, password };
}

const commonThresholds = {
  checks: ['rate>0.95'],
};

export const smokeThresholds = {
  ...commonThresholds,
  http_req_failed: ['rate<0.01'],
  'http_req_duration{name:health}': ['p(95)<1500'],
  'http_req_duration{name:auth_login}': ['p(95)<3000'],
  http_req_duration: ['p(95)<2500'],
};

export const loadThresholds = {
  ...commonThresholds,
  http_req_failed: ['rate<0.02'],
  'http_req_duration{name:health}': ['p(95)<2000'],
  'http_req_duration{name:auth_login}': ['p(95)<4000'],
  http_req_duration: ['p(95)<3500'],
};

export const stressThresholds = {
  ...commonThresholds,
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<6000'],
};

export const smokeOptions = {
  vus: 2,
  duration: '1m',
  thresholds: smokeThresholds,
};

export const loadOptions = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '3m', target: 20 },
    { duration: '1m', target: 0 },
  ],
  thresholds: loadThresholds,
};

export const stressOptions = {
  stages: [
    { duration: '1m', target: 20 },
    { duration: '2m', target: 40 },
    { duration: '2m', target: 60 },
    { duration: '1m', target: 0 },
  ],
  thresholds: stressThresholds,
};
