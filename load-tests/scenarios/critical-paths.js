import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { getBaseUrl, getHealthUrl, getCredentials } from '../lib/config.js';
import { login } from '../lib/auth.js';

function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

function expectApiOk(res, label) {
  return check(res, {
    [`${label} status 200`]: (r) => r.status === 200,
    [`${label} success true`]: (r) => {
      try {
        return r.json('success') === true;
      } catch (_) {
        return false;
      }
    },
  });
}

export function criticalPaths() {
  const baseUrl = getBaseUrl();
  const healthUrl = getHealthUrl();
  const creds = getCredentials();

  group('public_health', () => {
    const res = http.get(healthUrl, { tags: { name: 'health' } });
    check(res, {
      'health status 200': (r) => r.status === 200,
      'health body UP': (r) => {
        try {
          const status = r.json('status');
          return status === 'UP' || status === undefined;
        } catch (_) {
          return r.body && r.body.includes('UP');
        }
      },
    });
  });

  group('auth_public_config', () => {
    const res = http.get(`${baseUrl}/auth/otp-delivery-config`, {
      tags: { name: 'otp_config' },
    });
    expectApiOk(res, 'otp_config');
  });

  group('authenticated_critical_paths', () => {
    const token = login(baseUrl, creds.identifier, creds.password);
    if (!token) {
      return;
    }

    const headers = authHeaders(token);

    group('list_products', () => {
      const published = http.get(`${baseUrl}/products?page=0&size=10`, {
        headers,
        tags: { name: 'products_list' },
      });
      expectApiOk(published, 'products_list');

      const tenant = http.get(`${baseUrl}/products/tenant?page=0&size=10`, {
        headers,
        tags: { name: 'products_tenant' },
      });
      check(tenant, {
        'products_tenant ok': (r) => r.status === 200 || r.status === 403,
      });
    });

    group('list_policies', () => {
      const tenantPolicies = http.get(`${baseUrl}/policies/tenant?page=0&size=10`, {
        headers,
        tags: { name: 'policies_tenant' },
      });
      expectApiOk(tenantPolicies, 'policies_tenant');
    });

    group('list_claims', () => {
      const claims = http.get(`${baseUrl}/claims?page=0&size=10`, {
        headers,
        tags: { name: 'claims_list' },
      });
      expectApiOk(claims, 'claims_list');
    });

    group('protected_smoke', () => {
      const staffMe = http.get(`${baseUrl}/staff/me`, {
        headers,
        tags: { name: 'staff_me' },
      });
      check(staffMe, {
        'staff_me reachable': (r) => r.status === 200 || r.status === 403,
      });

      const reports = http.get(`${baseUrl}/reports/overview`, {
        headers,
        tags: { name: 'reports_overview' },
      });
      check(reports, {
        'reports_overview ok': (r) => r.status === 200 || r.status === 403 || r.status === 404,
      });
    });
  });

  sleep(0.5);
}
