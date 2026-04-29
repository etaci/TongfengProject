const SESSION_STORAGE_KEY = "tongfeng-web-session";

export function readSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function writeSession(session) {
  window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_STORAGE_KEY);
}

export async function apiRequest(path, options = {}, session = null) {
  const headers = new Headers(options.headers || {});
  const isFormData = options.body instanceof FormData;

  if (!isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json; charset=UTF-8");
  }

  if (!options.skipAuth) {
    if (!session?.token) {
      throw new Error("请先登录后再继续。");
    }
    headers.set("Authorization", `Bearer ${session.token}`);
  }

  const response = await fetch(path, {
    ...options,
    headers,
  });

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json") ? await response.json() : null;

  if (!response.ok || payload?.success === false) {
    throw new Error(payload?.message || `请求失败：${response.status}`);
  }

  return payload;
}
