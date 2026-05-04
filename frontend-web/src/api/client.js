const SESSION_STORAGE_KEY = "tongfeng-web-session";
const DEVICE_STORAGE_KEY = "tongfeng-web-device";

function getSessionStorage() {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function getLocalStorage() {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function readStorageJson(storage, key) {
  if (!storage) {
    return null;
  }
  const raw = storage.getItem(key);
  return raw ? JSON.parse(raw) : null;
}

function writeStorageJson(storage, key, value) {
  if (!storage) {
    return;
  }
  storage.setItem(key, JSON.stringify(value));
}

function removeStorageKey(storage, key) {
  if (!storage) {
    return;
  }
  storage.removeItem(key);
}

export function readSession() {
  try {
    const sessionStorage = getSessionStorage();
    const currentSession = readStorageJson(sessionStorage, SESSION_STORAGE_KEY);
    if (currentSession) {
      return currentSession;
    }

    const localStorage = getLocalStorage();
    const legacySession = readStorageJson(localStorage, SESSION_STORAGE_KEY);
    if (legacySession) {
      writeStorageJson(sessionStorage, SESSION_STORAGE_KEY, legacySession);
      removeStorageKey(localStorage, SESSION_STORAGE_KEY);
    }
    return legacySession;
  } catch {
    return null;
  }
}

export function writeSession(session) {
  const sessionStorage = getSessionStorage();
  const localStorage = getLocalStorage();
  writeStorageJson(sessionStorage || localStorage, SESSION_STORAGE_KEY, session);
  if (sessionStorage) {
    removeStorageKey(localStorage, SESSION_STORAGE_KEY);
  }
}

export function clearSession() {
  removeStorageKey(getSessionStorage(), SESSION_STORAGE_KEY);
  removeStorageKey(getLocalStorage(), SESSION_STORAGE_KEY);
}

function detectBrowserName(userAgent) {
  const ua = userAgent || "";
  if (ua.includes("Edg/")) {
    return "Edge";
  }
  if (ua.includes("Chrome/")) {
    return "Chrome";
  }
  if (ua.includes("Firefox/")) {
    return "Firefox";
  }
  if (ua.includes("Safari/") && !ua.includes("Chrome/")) {
    return "Safari";
  }
  return "Browser";
}

function readOrCreateDeviceProfile() {
  try {
    const cached = readStorageJson(getLocalStorage(), DEVICE_STORAGE_KEY);
    if (cached) {
      return cached;
    }
  } catch {
  }

  const userAgent = window.navigator?.userAgent || "";
  const platform = window.navigator?.userAgentData?.platform || window.navigator?.platform || "Web";
  const browser = detectBrowserName(userAgent);
  const deviceProfile = {
    deviceId: window.crypto?.randomUUID ? window.crypto.randomUUID() : `web-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    deviceLabel: `Web / ${browser} / ${platform}`.slice(0, 120),
  };

  try {
    writeStorageJson(getLocalStorage(), DEVICE_STORAGE_KEY, deviceProfile);
  } catch {
  }

  return deviceProfile;
}

export async function apiRequest(path, options = {}, session = null) {
  const headers = new Headers(options.headers || {});
  const isFormData = options.body instanceof FormData;
  const deviceProfile = readOrCreateDeviceProfile();

  if (!isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json; charset=UTF-8");
  }

  headers.set("X-Device-Fingerprint", deviceProfile.deviceId);
  headers.set("X-Device-Label", deviceProfile.deviceLabel);

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
