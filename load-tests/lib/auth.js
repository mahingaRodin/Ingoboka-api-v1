import http from 'k6/http';
import { check } from 'k6';

let loginFailureLogged = false;

function snippet(body, maxLen = 300) {
  if (typeof body !== 'string') {
    return '';
  }
  return body.length <= maxLen ? body : `${body.slice(0, maxLen)}…`;
}

export function login(baseUrl, identifier, password) {
  const payload = JSON.stringify({ identifier, password });
  const res = http.post(`${baseUrl}/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'auth_login' },
  });

  const ok = check(res, {
    'login status 200': (r) => r.status === 200,
    'login success flag': (r) => {
      try {
        return r.json('success') === true;
      } catch (_) {
        return false;
      }
    },
    'login returns token': (r) => {
      try {
        const token = r.json('data.accessToken');
        return typeof token === 'string' && token.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  if (!ok) {
    if (!loginFailureLogged) {
      loginFailureLogged = true;
      console.warn(
        `[auth] login failed once (identifier=${identifier}): status=${res.status} body=${snippet(res.body)}`
      );
    }
    return null;
  }

  return res.json('data.accessToken');
}
