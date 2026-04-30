import { useCallback, useEffect, useMemo, useState } from "react";
import { clearSession, readSession, writeSession } from "../api/client";
import {
  analyzeLab,
  analyzeMeal,
  acceptFamilyInvite,
  askKnowledge,
  bindDevice,
  cancelFamilyInvite,
  claimGrowthReward,
  createFamilyInvite,
  getDeviceSyncEvents,
  getBootstrapData,
  getDashboardBundle,
  getExtendedData,
  getFamilyPatientSummary,
  getRecordSnapshots,
  getRecordAudits,
  getRecordCenter,
  getRecordDetail,
  mockLogin,
  removeFamilyBinding,
  restoreRecord,
  syncDeviceData,
  submitRecord,
  unbindDevice,
  updateRecordDetail,
  updateProactiveSettings,
  updateMedications,
  updateProfile,
  uploadFile,
  deleteRecordDetail,
} from "../api/health";
import { mapMedicationToText, mapProfileToForm } from "../utils/forms";

const initialData = {
  capabilities: { features: [] },
  overview: null,
  trends: null,
  summaries: [],
  reminders: [],
  meals: [],
  mealResult: null,
  timeline: [],
  labs: [],
  labResult: null,
  knowledge: null,
  persona: null,
  uricAcidCauseAnalysis: null,
  mvpMetricsSummary: null,
  profile: null,
  medication: null,
  proactiveSettings: null,
  proactiveBrief: null,
  flareReview: null,
  familyInvites: [],
  familyMembers: { asPatient: [], asCaregiver: [] },
  familyAlerts: [],
  familyPatientSummary: null,
  devices: [],
  deviceSyncEvents: [],
  growthOverview: null,
  growthTasks: [],
  growthPoints: [],
  growthBadges: [],
  growthWeeklyPlan: null,
  growthRewards: [],
  growthRewardClaims: [],
  recordCenter: {
    types: [],
    totalCount: 0,
    returnedCount: 0,
    limit: 20,
    items: [],
    nextCursor: null,
    hasMore: false,
  },
  recordDetail: null,
  recordAudits: [],
  uploadedFiles: [],
  deviceCatalog: [],
  deviceOverview: null,
  recordSnapshots: {
    uricAcid: [],
    weight: [],
    hydration: [],
    flares: [],
  },
};

export default function useTongfengApp() {
  const [session, setSession] = useState(() => readSession());
  const [trendDays, setTrendDays] = useState("7");
  const [banner, setBanner] = useState({
    tone: "neutral",
    message: "页面已准备就绪，登录后即可开始记录与分析。",
  });
  const [isHydrating, setIsHydrating] = useState(false);
  const [busyMap, setBusyMap] = useState({});
  const [data, setData] = useState(initialData);

  const setBusy = useCallback((key, value) => {
    setBusyMap((current) => ({ ...current, [key]: value }));
  }, []);

  const isFamilyEnabled = useCallback((capabilitiesPayload) => {
    const features = capabilitiesPayload?.features || [];
    return features.some((item) => item.featureKey === "family-care" && item.enabled);
  }, []);

  const applyExtendedPayload = useCallback((extended) => {
    setData((current) => ({
      ...current,
      proactiveSettings: extended.proactiveSettings,
      proactiveBrief: extended.proactiveBrief,
      flareReview: extended.flareReview,
      familyInvites: extended.familyInvites || [],
      familyMembers: extended.familyMembers || { asPatient: [], asCaregiver: [] },
      familyAlerts: extended.familyAlerts || [],
      devices: extended.devices || [],
      deviceCatalog: extended.deviceCatalog || [],
      deviceOverview: extended.deviceOverview,
      growthOverview: extended.growthOverview,
      growthTasks: extended.growthTasks || [],
      growthPoints: extended.growthPoints || [],
      growthBadges: extended.growthBadges || [],
      growthWeeklyPlan: extended.growthWeeklyPlan,
      growthRewards: extended.growthRewards || [],
      growthRewardClaims: extended.growthRewardClaims || [],
      recordCenter: extended.recordCenter || current.recordCenter,
    }));
  }, []);

  const applyRecordSnapshots = useCallback((responses) => {
    setData((current) => ({
      ...current,
      recordSnapshots: {
        uricAcid: responses[0].data || [],
        weight: responses[1].data || [],
        hydration: responses[2].data || [],
        flares: responses[3].data || [],
      },
    }));
  }, []);

  const applyBootstrapPayload = useCallback((responses) => {
    const capabilities = responses[0].data || { features: [] };
    setData((current) => ({
      ...current,
      capabilities,
      profile: responses[1].data,
      overview: responses[2].data,
      trends: responses[3].data,
      summaries: responses[4].data || [],
      reminders: responses[5].data || [],
      meals: responses[6].data || [],
      timeline: responses[7].data?.events || [],
      labs: responses[8].data || [],
      persona: responses[9].data,
      uricAcidCauseAnalysis: responses[10].data,
      medication: responses[11].data,
      mvpMetricsSummary: responses[12].data,
    }));
  }, []);

  const refreshDashboard = useCallback(
    async (nextSession = session, nextDays = trendDays, silent = false) => {
      if (!nextSession?.token) {
        return;
      }

      if (!silent) {
        setBanner({ tone: "neutral", message: "正在同步总览、趋势与记录列表。" });
      }

      const responses = await getDashboardBundle(nextSession, nextDays);
      const capabilities = responses[0].data || { features: [] };
      const [extended, snapshots] = await Promise.all([
        getExtendedData(nextSession, { familyEnabled: isFamilyEnabled(capabilities) }),
        getRecordSnapshots(nextSession),
      ]);
      setData((current) => ({
        ...current,
        capabilities,
        overview: responses[1].data,
        trends: responses[2].data,
        summaries: responses[3].data || [],
        reminders: responses[4].data || [],
        meals: responses[5].data || [],
        timeline: responses[6].data?.events || [],
        labs: responses[7].data || [],
        persona: responses[8].data,
        uricAcidCauseAnalysis: responses[9].data,
        mvpMetricsSummary: responses[10].data,
        proactiveSettings: extended.proactiveSettings,
        proactiveBrief: extended.proactiveBrief,
        flareReview: extended.flareReview,
        familyInvites: extended.familyInvites || [],
        familyMembers: extended.familyMembers || { asPatient: [], asCaregiver: [] },
        familyAlerts: extended.familyAlerts || [],
        devices: extended.devices || [],
        deviceCatalog: extended.deviceCatalog || [],
        deviceOverview: extended.deviceOverview,
        growthOverview: extended.growthOverview,
        growthTasks: extended.growthTasks || [],
        growthPoints: extended.growthPoints || [],
        growthBadges: extended.growthBadges || [],
        growthWeeklyPlan: extended.growthWeeklyPlan,
        growthRewards: extended.growthRewards || [],
        growthRewardClaims: extended.growthRewardClaims || [],
        recordCenter: extended.recordCenter || current.recordCenter,
        recordSnapshots: {
          uricAcid: snapshots[0].data || [],
          weight: snapshots[1].data || [],
          hydration: snapshots[2].data || [],
          flares: snapshots[3].data || [],
        },
      }));

      if (!silent) {
        setBanner({ tone: "success", message: "数据同步完成。" });
      }
    },
    [session, trendDays],
  );

  const hydrate = useCallback(
    async (nextSession = session, nextDays = trendDays, silent = true) => {
      if (!nextSession?.token) {
        return;
      }

      setIsHydrating(true);

      try {
        const responses = await getBootstrapData(nextSession, nextDays);
        const capabilities = responses[0].data || { features: [] };
        const [extended, snapshots] = await Promise.all([
          getExtendedData(nextSession, { familyEnabled: isFamilyEnabled(capabilities) }),
          getRecordSnapshots(nextSession),
        ]);
        applyBootstrapPayload(responses);
        applyExtendedPayload(extended);
        applyRecordSnapshots(snapshots);

        if (!silent) {
          setBanner({ tone: "success", message: "数据加载完成。" });
        }
      } catch (error) {
        clearSession();
        setSession(null);
        setData(initialData);
        setBanner({ tone: "danger", message: `会话同步失败：${error.message}` });
      } finally {
        setIsHydrating(false);
      }
    },
    [applyBootstrapPayload, applyExtendedPayload, applyRecordSnapshots, isFamilyEnabled, session, trendDays],
  );

  useEffect(() => {
    if (session?.token) {
      hydrate(session, trendDays, true);
    }
  }, [session, trendDays, hydrate]);

  const login = useCallback(async (nickname) => {
    setBusy("login", true);
    try {
      const response = await mockLogin(nickname);
      writeSession(response.data);
      setSession(response.data);
      setBanner({ tone: "success", message: `欢迎回来，${response.data.nickname}。正在同步你的健康数据。` });
    } finally {
      setBusy("login", false);
    }
  }, [setBusy]);

  const logout = useCallback(() => {
    clearSession();
    setSession(null);
    setData(initialData);
    setBanner({ tone: "neutral", message: "会话已清除，你可以重新登录继续体验。" });
  }, []);

  const submitProfile = useCallback(
    async (payload) => {
      setBusy("profile", true);
      try {
        const response = await updateProfile(session, payload);
        setData((current) => ({ ...current, profile: response.data }));
        setBanner({ tone: "success", message: "个人档案已保存。" });
      } finally {
        setBusy("profile", false);
      }
    },
    [session, setBusy],
  );

  const submitMedication = useCallback(
    async (payload) => {
      setBusy("medication", true);
      try {
        const response = await updateMedications(session, payload);
        setData((current) => ({ ...current, medication: response.data }));
        setBanner({ tone: "success", message: "用药计划已更新。" });
      } finally {
        setBusy("medication", false);
      }
    },
    [session, setBusy],
  );

  const submitSimpleRecord = useCallback(
    async (busyKey, path, payload, successMessage) => {
      setBusy(busyKey, true);
      try {
        await submitRecord(session, path, payload);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: successMessage });
      } finally {
        setBusy(busyKey, false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const submitMealAnalysis = useCallback(
    async (formData) => {
      setBusy("meal", true);
      try {
        const response = await analyzeMeal(session, formData);
        setData((current) => ({ ...current, mealResult: response.data }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "餐盘识别已完成。" });
      } finally {
        setBusy("meal", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const submitLabAnalysis = useCallback(
    async (formData) => {
      setBusy("lab", true);
      try {
        const response = await analyzeLab(session, formData);
        setData((current) => ({ ...current, labResult: response.data }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "化验单解析已完成。" });
      } finally {
        setBusy("lab", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const submitKnowledgeQuestion = useCallback(
    async (payload) => {
      setBusy("knowledge", true);
      try {
        const response = await askKnowledge(session, payload);
        setData((current) => ({ ...current, knowledge: response.data }));
        setBanner({ tone: "success", message: "知识问答结果已返回。" });
      } finally {
        setBusy("knowledge", false);
      }
    },
    [session, setBusy],
  );

  const submitProactiveSettings = useCallback(
    async (payload) => {
      setBusy("proactive", true);
      try {
        const response = await updateProactiveSettings(session, payload);
        setData((current) => ({ ...current, proactiveSettings: response.data }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "主动关怀设置已更新。" });
      } finally {
        setBusy("proactive", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const submitFamilyInvite = useCallback(
    async (payload) => {
      setBusy("familyInvite", true);
      try {
        await createFamilyInvite(session, payload);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "家庭邀请已创建。" });
      } finally {
        setBusy("familyInvite", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const acceptInvite = useCallback(
    async (inviteCode) => {
      setBusy("acceptInvite", true);
      try {
        await acceptFamilyInvite(session, inviteCode);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "家庭邀请已接受。" });
      } finally {
        setBusy("acceptInvite", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const cancelInvite = useCallback(
    async (inviteCode) => {
      setBusy(`cancel-${inviteCode}`, true);
      try {
        await cancelFamilyInvite(session, inviteCode);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "家庭邀请已取消。" });
      } finally {
        setBusy(`cancel-${inviteCode}`, false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const unbindFamilyMember = useCallback(
    async (bindingCode) => {
      setBusy(`family-binding-${bindingCode}`, true);
      try {
        await removeFamilyBinding(session, bindingCode);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "家庭绑定已解除。" });
      } finally {
        setBusy(`family-binding-${bindingCode}`, false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const loadFamilySummary = useCallback(
    async (patientUserId) => {
      setBusy("familySummary", true);
      try {
        const response = await getFamilyPatientSummary(session, patientUserId);
        setData((current) => ({ ...current, familyPatientSummary: response.data }));
        setBanner({ tone: "success", message: "家属患者摘要已加载。" });
      } finally {
        setBusy("familySummary", false);
      }
    },
    [session, setBusy],
  );

  const submitDeviceBinding = useCallback(
    async (payload) => {
      setBusy("deviceBind", true);
      try {
        await bindDevice(session, payload);
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "设备已绑定。" });
      } finally {
        setBusy("deviceBind", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const removeDeviceBinding = useCallback(
    async (deviceCode) => {
      setBusy(`device-unbind-${deviceCode}`, true);
      try {
        await unbindDevice(session, deviceCode);
        setData((current) => ({
          ...current,
          deviceSyncEvents: current.deviceSyncEvents.filter((item) => item.deviceCode !== deviceCode),
        }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "设备已解绑。" });
      } finally {
        setBusy(`device-unbind-${deviceCode}`, false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const submitDeviceSync = useCallback(
    async (deviceCode, payload) => {
      setBusy("deviceSync", true);
      try {
        const response = await syncDeviceData(session, deviceCode, payload);
        setData((current) => ({ ...current, deviceSyncEvents: response.data.results || [] }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "设备同步已完成。" });
      } finally {
        setBusy("deviceSync", false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const loadDeviceSyncHistory = useCallback(
    async (deviceCode) => {
      setBusy("deviceHistory", true);
      try {
        const response = await getDeviceSyncEvents(session, deviceCode);
        setData((current) => ({ ...current, deviceSyncEvents: response.data || [] }));
        setBanner({ tone: "success", message: "设备同步记录已加载。" });
      } finally {
        setBusy("deviceHistory", false);
      }
    },
    [session, setBusy],
  );

  const submitFileUpload = useCallback(
    async (file) => {
      setBusy("fileUpload", true);
      try {
        const response = await uploadFile(session, file);
        setData((current) => ({
          ...current,
          uploadedFiles: [response.data, ...current.uploadedFiles].slice(0, 12),
        }));
        setBanner({ tone: "success", message: "文件已上传。" });
      } finally {
        setBusy("fileUpload", false);
      }
    },
    [session, setBusy],
  );

  const loadRecordCenterData = useCallback(
    async ({ type = "ALL", cursor = null, append = false } = {}) => {
      setBusy("recordCenter", true);
      try {
        const response = await getRecordCenter(session, { type, cursor, limit: 20 });
        setData((current) => ({
          ...current,
          recordCenter: append
            ? {
                ...response.data,
                items: [...(current.recordCenter?.items || []), ...(response.data.items || [])],
              }
            : response.data,
        }));
        if (!append) {
          setBanner({ tone: "success", message: "记录中心已更新。" });
        }
      } finally {
        setBusy("recordCenter", false);
      }
    },
    [session, setBusy],
  );

  const loadRecordDetailData = useCallback(
    async (type, recordId) => {
      setBusy("recordDetail", true);
      try {
        const [detailResponse, auditsResponse] = await Promise.all([
          getRecordDetail(session, type, recordId),
          getRecordAudits(session, type, recordId, 20),
        ]);
        setData((current) => ({
          ...current,
          recordDetail: detailResponse.data,
          recordAudits: auditsResponse.data || [],
        }));
        setBanner({ tone: "success", message: "记录详情已加载。" });
      } finally {
        setBusy("recordDetail", false);
      }
    },
    [session, setBusy],
  );

  const submitRecordUpdate = useCallback(
    async (type, recordId, payload) => {
      setBusy("recordUpdate", true);
      try {
        const response = await updateRecordDetail(session, type, recordId, payload);
        const auditsResponse = await getRecordAudits(session, type, recordId, 20);
        setData((current) => ({
          ...current,
          recordDetail: response.data,
          recordAudits: auditsResponse.data || [],
        }));
        await refreshDashboard(session, trendDays, true);
        await loadRecordCenterData({ type: "ALL" });
        setBanner({ tone: "success", message: "记录已更新。" });
      } finally {
        setBusy("recordUpdate", false);
      }
    },
    [loadRecordCenterData, refreshDashboard, session, setBusy, trendDays],
  );

  const removeRecord = useCallback(
    async (type, recordId) => {
      setBusy("recordDelete", true);
      try {
        await deleteRecordDetail(session, type, recordId);
        setData((current) => ({
          ...current,
          recordDetail: null,
          recordAudits: [],
        }));
        await refreshDashboard(session, trendDays, true);
        await loadRecordCenterData({ type: "ALL" });
        setBanner({ tone: "success", message: "记录已删除。" });
      } finally {
        setBusy("recordDelete", false);
      }
    },
    [loadRecordCenterData, refreshDashboard, session, setBusy, trendDays],
  );

  const restoreRecordFromAudit = useCallback(
    async (type, recordId, auditId, changeReason) => {
      setBusy("recordRestore", true);
      try {
        const response = await restoreRecord(session, type, recordId, auditId, { changeReason });
        const auditsResponse = await getRecordAudits(session, type, recordId, 20);
        setData((current) => ({
          ...current,
          recordDetail: response.data.detail,
          recordAudits: auditsResponse.data || [],
        }));
        await refreshDashboard(session, trendDays, true);
        await loadRecordCenterData({ type: "ALL" });
        setBanner({ tone: "success", message: "记录已恢复到所选版本。" });
      } finally {
        setBusy("recordRestore", false);
      }
    },
    [loadRecordCenterData, refreshDashboard, session, setBusy, trendDays],
  );

  const claimReward = useCallback(
    async (rewardKey) => {
      setBusy(`reward-${rewardKey}`, true);
      try {
        const response = await claimGrowthReward(session, rewardKey);
        setData((current) => ({
          ...current,
          growthRewardClaims: [response.data, ...(current.growthRewardClaims || [])],
        }));
        await refreshDashboard(session, trendDays, true);
        setBanner({ tone: "success", message: "奖励领取成功。" });
      } finally {
        setBusy(`reward-${rewardKey}`, false);
      }
    },
    [refreshDashboard, session, setBusy, trendDays],
  );

  const profileForm = useMemo(
    () => mapProfileToForm(data.profile, session?.nickname || ""),
    [data.profile, session?.nickname],
  );

  const medicationForm = useMemo(
    () => mapMedicationToText(data.medication),
    [data.medication],
  );

  return {
    session,
    trendDays,
    setTrendDays,
    banner,
    setBanner,
    isHydrating,
    busyMap,
    data,
    login,
    logout,
    hydrate,
    refreshDashboard,
    submitProfile,
    submitMedication,
    submitSimpleRecord,
    submitMealAnalysis,
    submitLabAnalysis,
    submitKnowledgeQuestion,
    submitProactiveSettings,
    submitFamilyInvite,
    acceptInvite,
    cancelInvite,
    unbindFamilyMember,
    loadFamilySummary,
    submitDeviceBinding,
    removeDeviceBinding,
    submitDeviceSync,
    loadDeviceSyncHistory,
    submitFileUpload,
    loadRecordCenterData,
    loadRecordDetailData,
    submitRecordUpdate,
    removeRecord,
    restoreRecordFromAudit,
    claimReward,
    applyRecordSnapshots,
    profileForm,
    medicationForm,
  };
}
