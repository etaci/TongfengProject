import { apiRequest } from "./client";

export function mockLogin(nickname) {
  return apiRequest(
    "/api/v1/auth/mock-login",
    {
      method: "POST",
      body: JSON.stringify({ nickname }),
      skipAuth: true,
    },
    null,
  );
}

export function getBootstrapData(session, days) {
  return Promise.all([
    apiRequest("/api/v1/app/capabilities", {}, session),
    apiRequest("/api/v1/profile", {}, session),
    apiRequest("/api/v1/dashboard/overview", {}, session),
    apiRequest(`/api/v1/dashboard/trends?days=${days}`, {}, session),
    apiRequest(`/api/v1/dashboard/daily-summaries?days=${days}`, {}, session),
    apiRequest("/api/v1/reminders", {}, session),
    apiRequest("/api/v1/meals", {}, session),
    apiRequest("/api/v1/records/timeline", {}, session),
    apiRequest("/api/v1/lab-reports", {}, session),
    apiRequest("/api/v1/persona/summary", {}, session),
    apiRequest("/api/v1/analysis/uric-acid-causes?lookbackDays=7", {}, session),
    apiRequest("/api/v1/medications", {}, session),
    apiRequest("/api/v1/mvp/metrics/summary?days=7", {}, session),
  ]);
}

export function getRecordSnapshots(session) {
  return Promise.all([
    apiRequest("/api/v1/records/uric-acid", {}, session),
    apiRequest("/api/v1/records/weight", {}, session),
    apiRequest("/api/v1/records/hydration", {}, session),
    apiRequest("/api/v1/records/flares", {}, session),
  ]);
}

export async function getExtendedData(session, { familyEnabled = false } = {}) {
  const requestEntries = [
    ["proactiveSettings", () => apiRequest("/api/v1/proactive-care/settings", {}, session)],
    ["proactiveBrief", () => apiRequest("/api/v1/proactive-care/brief", {}, session)],
    ["flareReview", () => apiRequest("/api/v1/flares/reports/latest", {}, session)],
    ["recordCenter", () => apiRequest("/api/v1/records/center?limit=20", {}, session)],
  ];

  if (familyEnabled) {
    requestEntries.push(
      ["familyInvites", () => apiRequest("/api/v1/family/invitations", {}, session)],
      ["familyMembers", () => apiRequest("/api/v1/family/members", {}, session)],
      ["familyAlerts", () => apiRequest("/api/v1/family/alerts", {}, session)],
    );
  }

  const results = await Promise.allSettled(requestEntries.map((entry) => entry[1]()));

  return requestEntries.reduce((accumulator, [name], index) => {
    const result = results[index];

    if (result.status === "fulfilled") {
      accumulator[name] = result.value.data;
      return accumulator;
    }

    if (name === "familyMembers") {
      accumulator[name] = { asPatient: [], asCaregiver: [] };
      return accumulator;
    }

    if (["familyInvites", "familyAlerts"].includes(name)) {
      accumulator[name] = [];
      return accumulator;
    }

    if (name === "recordCenter") {
      accumulator[name] = {
        types: [],
        totalCount: 0,
        returnedCount: 0,
        limit: 20,
        items: [],
        nextCursor: null,
        hasMore: false,
      };
      return accumulator;
    }

    accumulator[name] = null;
    return accumulator;
  }, {
    familyInvites: [],
    familyMembers: { asPatient: [], asCaregiver: [] },
    familyAlerts: [],
    devices: [],
    deviceCatalog: [],
    deviceOverview: null,
    growthOverview: null,
    growthTasks: [],
    growthPoints: [],
    growthBadges: [],
    growthWeeklyPlan: null,
    growthRewards: [],
    growthRewardClaims: [],
  });
}

export function getDashboardBundle(session, days) {
  return Promise.all([
    apiRequest("/api/v1/app/capabilities", {}, session),
    apiRequest("/api/v1/dashboard/overview", {}, session),
    apiRequest(`/api/v1/dashboard/trends?days=${days}`, {}, session),
    apiRequest(`/api/v1/dashboard/daily-summaries?days=${days}`, {}, session),
    apiRequest("/api/v1/reminders", {}, session),
    apiRequest("/api/v1/meals", {}, session),
    apiRequest("/api/v1/records/timeline", {}, session),
    apiRequest("/api/v1/lab-reports", {}, session),
    apiRequest("/api/v1/persona/summary", {}, session),
    apiRequest("/api/v1/analysis/uric-acid-causes?lookbackDays=7", {}, session),
    apiRequest("/api/v1/mvp/metrics/summary?days=7", {}, session),
  ]);
}

export function getUricAcidCauseAnalysis(session, lookbackDays = 7) {
  return apiRequest(`/api/v1/analysis/uric-acid-causes?lookbackDays=${lookbackDays}`, {}, session);
}

export function getMvpMetricsSummary(session, days = 7) {
  return apiRequest(`/api/v1/mvp/metrics/summary?days=${days}`, {}, session);
}

export function updateProfile(session, payload) {
  return apiRequest(
    "/api/v1/profile",
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function updateMedications(session, payload) {
  return apiRequest(
    "/api/v1/medications",
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function submitRecord(session, path, payload) {
  return apiRequest(
    path,
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function analyzeMeal(session, payload) {
  return apiRequest(
    "/api/v1/meals/analyze",
    {
      method: "POST",
      body: payload,
    },
    session,
  );
}

export function analyzeLab(session, payload) {
  return apiRequest(
    "/api/v1/lab-reports/analyze",
    {
      method: "POST",
      body: payload,
    },
    session,
  );
}

export function askKnowledge(session, payload) {
  return apiRequest(
    "/api/v1/knowledge/ask",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function updateProactiveSettings(session, payload) {
  return apiRequest(
    "/api/v1/proactive-care/settings",
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function createFamilyInvite(session, payload) {
  return apiRequest(
    "/api/v1/family/invitations",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function acceptFamilyInvite(session, inviteCode) {
  return apiRequest(
    `/api/v1/family/invitations/${inviteCode}/accept`,
    {
      method: "POST",
    },
    session,
  );
}

export function cancelFamilyInvite(session, inviteCode) {
  return apiRequest(
    `/api/v1/family/invitations/${inviteCode}/cancel`,
    {
      method: "POST",
    },
    session,
  );
}

export function removeFamilyBinding(session, bindingCode) {
  return apiRequest(
    `/api/v1/family/members/${bindingCode}`,
    {
      method: "DELETE",
    },
    session,
  );
}

export function getFamilyPatientSummary(session, patientUserId) {
  return apiRequest(`/api/v1/family/patients/${patientUserId}/summary`, {}, session);
}

export function bindDevice(session, payload) {
  return apiRequest(
    "/api/v1/devices",
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function unbindDevice(session, deviceCode) {
  return apiRequest(
    `/api/v1/devices/${deviceCode}`,
    {
      method: "DELETE",
    },
    session,
  );
}

export function syncDeviceData(session, deviceCode, payload) {
  return apiRequest(
    `/api/v1/devices/${deviceCode}/sync`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function getDeviceSyncEvents(session, deviceCode) {
  return apiRequest(`/api/v1/devices/${deviceCode}/sync-events`, {}, session);
}

export function uploadFile(session, file) {
  const payload = new FormData();
  payload.append("file", file);
  return apiRequest(
    "/api/v1/files/upload",
    {
      method: "POST",
      body: payload,
    },
    session,
  );
}

export function getRecordCenter(session, { type = "ALL", cursor = null, limit = 20 } = {}) {
  const query = new URLSearchParams();
  if (type && type !== "ALL") {
    query.append("types", type);
  }
  if (cursor) {
    query.append("cursor", cursor);
  }
  query.append("limit", String(limit));
  return apiRequest(`/api/v1/records/center?${query.toString()}`, {}, session);
}

export function getRecordDetail(session, type, recordId) {
  return apiRequest(`/api/v1/records/detail?type=${encodeURIComponent(type)}&recordId=${encodeURIComponent(recordId)}`, {}, session);
}

export function updateRecordDetail(session, type, recordId, payload) {
  return apiRequest(
    `/api/v1/records/detail?type=${encodeURIComponent(type)}&recordId=${encodeURIComponent(recordId)}`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function deleteRecordDetail(session, type, recordId) {
  return apiRequest(
    `/api/v1/records/detail?type=${encodeURIComponent(type)}&recordId=${encodeURIComponent(recordId)}`,
    {
      method: "DELETE",
    },
    session,
  );
}

export function getRecordAudits(session, type, recordId, limit = 20) {
  return apiRequest(
    `/api/v1/records/audits?type=${encodeURIComponent(type)}&recordId=${encodeURIComponent(recordId)}&limit=${limit}`,
    {},
    session,
  );
}

export function restoreRecord(session, type, recordId, auditId, payload) {
  return apiRequest(
    `/api/v1/records/restore?type=${encodeURIComponent(type)}&recordId=${encodeURIComponent(recordId)}&auditId=${encodeURIComponent(auditId)}`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    },
    session,
  );
}

export function claimGrowthReward(session, rewardKey) {
  return apiRequest(
    `/api/v1/growth/rewards/${encodeURIComponent(rewardKey)}/claim`,
    {
      method: "POST",
    },
    session,
  );
}
