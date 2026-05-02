import { useEffect, useMemo, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import StatusBanner from "./components/StatusBanner";
import AnalysisPage from "./pages/AnalysisPage";
import AssistantPage from "./pages/AssistantPage";
import FamilyPage from "./pages/FamilyPage";
import OverviewPage from "./pages/OverviewPage";
import ProactivePage from "./pages/ProactivePage";
import RecordsPage from "./pages/RecordsPage";
import { authAccountTypeOptions, familyPermissionOptions, trendOptions } from "./constants/options";
import useTongfengApp from "./hooks/useTongfengApp";
import { formatDateTime } from "./utils/format";
import { emptyToNull, getMedicationPeriods, parseMedicationText, splitCsv, toIsoString } from "./utils/forms";

function HeroMetrics({ overview }) {
  const metrics = overview
    ? [
        {
          label: "今日重点",
          value: `${overview.todayFocus?.length || 0} 项`,
          description: overview.todayFocus?.slice(0, 2).join(" / ") || "今天先维持当前节奏",
        },
        {
          label: "当前风险",
          value: overview.stage || "待评估",
          description: overview.latestRiskSummary || "登录后会自动生成今天的风险摘要",
        },
        {
          label: "今日记录",
          value: `${overview.mealsCount || 0} 餐 / ${overview.uricAcidCount || 0} 次尿酸`,
          description: `高风险饮食 ${overview.highRiskMealsCount || 0} 次，发作 ${overview.flareCount || 0} 次`,
        },
      ]
    : [
        {
          label: "今日重点",
          value: "等待登录",
          description: "登录后直接生成今天的行动建议",
        },
        {
          label: "当前风险",
          value: "数据未加载",
          description: "风险分流、提醒和趋势会在这里汇总",
        },
        {
          label: "今日记录",
          value: "等待同步",
          description: "当前版本先聚焦饮食、用药、指标和复盘四件事",
        },
      ];

  return (
    <div className="hero-metrics">
      {metrics.map((item) => (
        <article className="metric-card" key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
          <p>{item.description}</p>
        </article>
      ))}
    </div>
  );
}

export default function App() {
  const app = useTongfengApp();
  const { data, busyMap, session } = app;

  const [authMode, setAuthMode] = useState("login");
  const [loginDraft, setLoginDraft] = useState({
    accountType: "EMAIL",
    account: "",
    password: "",
  });
  const [registerDraft, setRegisterDraft] = useState({
    nickname: "",
    accountType: "EMAIL",
    account: "",
    password: "",
    confirmPassword: "",
    consentVersion: "v1.0",
    privacyPolicyVersion: "privacy-v1.0",
    privacyAccepted: true,
    termsAccepted: true,
    medicalDataAuthorized: true,
    familyCollaborationAuthorized: true,
    notificationAuthorized: true,
  });
  const [mockNickname, setMockNickname] = useState("");
  const [profileDraft, setProfileDraft] = useState(app.profileForm);
  const [medicationDraft, setMedicationDraft] = useState(app.medicationForm);
  const [medicationCheckinDraft, setMedicationCheckinDraft] = useState({
    medicationName: "",
    scheduledPeriod: "MORNING",
    status: "TAKEN",
    note: "",
  });
  const [proactiveDraft, setProactiveDraft] = useState({
    monitoringCity: "",
    countryCode: "CN",
    weatherAlertsEnabled: true,
  });
  const [inviteDraft, setInviteDraft] = useState({
    relationType: "SPOUSE",
    inviteMessage: "",
    expiresInDays: "7",
    caregiverPermission: familyPermissionOptions[1].value,
    weeklyReportEnabled: true,
    notifyOnHighRisk: true,
  });
  const [familyTaskDraft, setFamilyTaskDraft] = useState({
    bindingCode: "",
    title: "",
    description: "",
    dueAt: "",
  });
  const [acceptInviteCode, setAcceptInviteCode] = useState("");
  const [deviceDraft, setDeviceDraft] = useState({
    deviceType: "URIC_ACID_METER",
    vendorName: "",
    deviceModel: "",
    serialNumber: "",
    aliasName: "",
  });
  const [deviceSyncDraft, setDeviceSyncDraft] = useState({
    deviceCode: "",
    metricType: "URIC_ACID",
    externalEventId: "",
    measuredAt: "",
    value: "",
    diastolicPressure: "",
    pulseRate: "",
    unit: "umol/L",
    waterIntakeMl: "",
    urineColorLevel: "",
    note: "",
  });
  const [deviceSyncQueue, setDeviceSyncQueue] = useState([]);
  const [recordCenterType, setRecordCenterType] = useState("ALL");
  const [recordEditDraft, setRecordEditDraft] = useState({});
  const [recordChangeReason, setRecordChangeReason] = useState("");
  const [recordRestoreReason, setRecordRestoreReason] = useState("回滚到此前版本");

  useEffect(() => {
    setProfileDraft(app.profileForm);
  }, [app.profileForm]);

  useEffect(() => {
    setMedicationDraft(app.medicationForm);
  }, [app.medicationForm]);

  useEffect(() => {
    const medications = data.medication?.currentMedications || [];

    if (!medications.length) {
      setMedicationCheckinDraft((current) => (
        current.medicationName || current.scheduledPeriod !== "MORNING"
          ? { ...current, medicationName: "", scheduledPeriod: "MORNING" }
          : current
      ));
      return;
    }

    const selectedMedication = medications.find((item) => item.name === medicationCheckinDraft.medicationName) || medications[0];
    const periods = getMedicationPeriods(selectedMedication.frequency);
    const nextPeriod = periods.includes(medicationCheckinDraft.scheduledPeriod)
      ? medicationCheckinDraft.scheduledPeriod
      : periods[0];

    setMedicationCheckinDraft((current) => {
      if (current.medicationName === selectedMedication.name && current.scheduledPeriod === nextPeriod) {
        return current;
      }

      return {
        ...current,
        medicationName: selectedMedication.name,
        scheduledPeriod: nextPeriod,
      };
    });
  }, [data.medication, medicationCheckinDraft.medicationName, medicationCheckinDraft.scheduledPeriod]);

  useEffect(() => {
    if (data.proactiveSettings) {
      setProactiveDraft({
        monitoringCity: data.proactiveSettings.monitoringCity || "",
        countryCode: data.proactiveSettings.countryCode || "CN",
        weatherAlertsEnabled: Boolean(data.proactiveSettings.weatherAlertsEnabled),
      });
    }
  }, [data.proactiveSettings]);

  useEffect(() => {
    if (!session) {
      setMockNickname("");
      return;
    }
    setMockNickname(session.nickname || "");
  }, [session]);

  useEffect(() => {
    if (!deviceSyncDraft.deviceCode && data.devices?.length) {
      setDeviceSyncDraft((current) => ({ ...current, deviceCode: data.devices[0].deviceCode }));
    }
  }, [data.devices, deviceSyncDraft.deviceCode]);

  useEffect(() => {
    if (!data.recordDetail) {
      setRecordEditDraft({});
      return;
    }

    const nextDraft = (data.recordDetail.fields || []).reduce((accumulator, field) => {
      accumulator[field.key] = field.value || "";
      return accumulator;
    }, {});

    nextDraft.note = data.recordDetail.note || "";
    nextDraft.source = data.recordDetail.source || "";
    setRecordEditDraft(nextDraft);
  }, [data.recordDetail]);

  useEffect(() => {
    const taskBindings = (data.familyMembers?.asPatient || []).filter((item) => item.caregiverPermission === "TASK" && item.status === "ACTIVE");
    if (!taskBindings.length) {
      setFamilyTaskDraft((current) => (current.bindingCode ? { ...current, bindingCode: "" } : current));
      return;
    }

    if (taskBindings.some((item) => item.bindingCode === familyTaskDraft.bindingCode)) {
      return;
    }

    setFamilyTaskDraft((current) => ({ ...current, bindingCode: taskBindings[0].bindingCode }));
  }, [data.familyMembers, familyTaskDraft.bindingCode]);

  const familySummaryTargetName = useMemo(() => data.familyPatientSummary?.patientNickname || "", [data.familyPatientSummary]);
  const familyWeeklyReportTargetName = useMemo(() => data.familyWeeklyReport?.patientNickname || "", [data.familyWeeklyReport]);
  const familyFeatureEnabled = useMemo(
    () => (data.capabilities?.features || []).some((item) => item.featureKey === "family-care" && item.enabled),
    [data.capabilities],
  );
  const navItems = useMemo(
    () => {
      const items = [
        { to: "/overview", label: "总览" },
        { to: "/records", label: "记录" },
        { to: "/analysis", label: "分析" },
        { to: "/proactive", label: "主动关怀" },
      ];

      if (familyFeatureEnabled) {
        items.push({ to: "/family", label: "家庭协同" });
      }

      items.push({ to: "/assistant", label: "问答与档案" });
      return items;
    },
    [familyFeatureEnabled],
  );

  async function withErrorHandling(task) {
    try {
      await task();
    } catch (error) {
      app.setBanner({ tone: "danger", message: error.message });
    }
  }

  function buildRecordMapper(formData, keys) {
    return keys(formData);
  }

  async function handlePasswordLogin(event) {
    event.preventDefault();
    if (!loginDraft.account.trim() || !loginDraft.password.trim()) {
      app.setBanner({ tone: "warning", message: "请先填写完整的账号和密码。" });
      return;
    }

    await withErrorHandling(() => app.login({
      accountType: loginDraft.accountType,
      account: loginDraft.account.trim(),
      password: loginDraft.password,
    }));
  }

  async function handleRegister(event) {
    event.preventDefault();
    if (!registerDraft.nickname.trim() || !registerDraft.account.trim()) {
      app.setBanner({ tone: "warning", message: "请先填写昵称和账号。" });
      return;
    }

    if (!registerDraft.password.trim() || !registerDraft.confirmPassword.trim()) {
      app.setBanner({ tone: "warning", message: "请先填写并确认密码。" });
      return;
    }

    await withErrorHandling(() => app.register({
      nickname: registerDraft.nickname.trim(),
      accountType: registerDraft.accountType,
      account: registerDraft.account.trim(),
      password: registerDraft.password,
      confirmPassword: registerDraft.confirmPassword,
      consent: {
        consentVersion: registerDraft.consentVersion,
        privacyPolicyVersion: registerDraft.privacyPolicyVersion,
        privacyAccepted: registerDraft.privacyAccepted,
        termsAccepted: registerDraft.termsAccepted,
        medicalDataAuthorized: registerDraft.medicalDataAuthorized,
        familyCollaborationAuthorized: registerDraft.familyCollaborationAuthorized,
        notificationAuthorized: registerDraft.notificationAuthorized,
      },
    }));
  }

  async function handleMockLogin(event) {
    event.preventDefault();
    const nickname = mockNickname.trim();

    if (!nickname) {
      app.setBanner({ tone: "warning", message: "请输入昵称后再进入开发体验环境。" });
      return;
    }

    await withErrorHandling(() => app.loginDemo(nickname));
  }

  async function handlePasswordChange(payload) {
    try {
      await app.submitPasswordChange(payload);
      return true;
    } catch (error) {
      app.setBanner({ tone: "danger", message: error.message });
      return false;
    }
  }

  async function handleRevokeSession(sessionCode) {
    await withErrorHandling(() => app.revokeAuthSession(sessionCode));
  }

  async function handleRefresh() {
    await withErrorHandling(() => app.hydrate(session, app.trendDays, false));
  }

  async function handleProfileSubmit(event) {
    event.preventDefault();
    await withErrorHandling(() =>
      app.submitProfile({
        name: profileDraft.name.trim(),
        gender: profileDraft.gender,
        birthday: emptyToNull(profileDraft.birthday),
        heightCm: profileDraft.heightCm ? Number(profileDraft.heightCm) : null,
        targetUricAcid: profileDraft.targetUricAcid ? Number(profileDraft.targetUricAcid) : null,
        allergies: splitCsv(profileDraft.allergies),
        comorbidities: splitCsv(profileDraft.comorbidities),
        emergencyContact: emptyToNull(profileDraft.emergencyContact),
      }),
    );
  }

  async function handleMedicationSubmit(event) {
    event.preventDefault();
    await withErrorHandling(() =>
      app.submitMedication({
        currentMedications: parseMedicationText(medicationDraft.lines),
        followUpNote: emptyToNull(medicationDraft.followUpNote),
      }),
    );
  }

  async function handleMedicationCheckinSubmit(event) {
    event.preventDefault();

    if (!medicationCheckinDraft.medicationName) {
      app.setBanner({ tone: "warning", message: "请先补齐用药计划，再进行用药打卡。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.submitMedicationCheckin({
        medicationName: medicationCheckinDraft.medicationName,
        scheduledPeriod: medicationCheckinDraft.scheduledPeriod,
        status: medicationCheckinDraft.status,
        note: emptyToNull(medicationCheckinDraft.note),
      });
      setMedicationCheckinDraft((current) => ({ ...current, status: "TAKEN", note: "" }));
    });
  }

  async function handleRecordSubmit(event, busyKey, path, successMessage, mapper) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    await withErrorHandling(async () => {
      await app.submitSimpleRecord(busyKey, path, buildRecordMapper(formData, mapper), successMessage);
      event.currentTarget.reset();
    });
  }

  async function handleMealSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const file = formData.get("file");

    if (!(file instanceof File) || !file.size) {
      app.setBanner({ tone: "warning", message: "请先选择餐盘图片。" });
      return;
    }

    const payload = new FormData();
    payload.append("file", file);
    payload.append("mealType", formData.get("mealType"));

    const takenAt = emptyToNull(formData.get("takenAt"));
    const note = emptyToNull(formData.get("note"));
    if (takenAt) {
      payload.append("takenAt", toIsoString(takenAt));
    }
    if (note) {
      payload.append("note", note);
    }

    await withErrorHandling(async () => {
      await app.submitMealAnalysis(payload);
      event.currentTarget.reset();
    });
  }

  async function handleLabSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const file = formData.get("file");

    if (!(file instanceof File) || !file.size) {
      app.setBanner({ tone: "warning", message: "请先选择化验单文件。" });
      return;
    }

    const payload = new FormData();
    payload.append("file", file);
    const reportDate = emptyToNull(formData.get("reportDate"));
    if (reportDate) {
      payload.append("reportDate", reportDate);
    }

    await withErrorHandling(async () => {
      await app.submitLabAnalysis(payload);
      event.currentTarget.reset();
    });
  }

  async function handleKnowledgeSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const question = String(formData.get("question") || "").trim();

    if (!question) {
      app.setBanner({ tone: "warning", message: "请输入问题后再提交。" });
      return;
    }

    await withErrorHandling(() =>
      app.submitKnowledgeQuestion({
        question,
        scene: emptyToNull(formData.get("scene")),
      }),
    );
  }

  async function handleProactiveSubmit(event) {
    event.preventDefault();
    await withErrorHandling(() =>
      app.submitProactiveSettings({
        monitoringCity: proactiveDraft.monitoringCity.trim(),
        countryCode: emptyToNull(proactiveDraft.countryCode),
        weatherAlertsEnabled: proactiveDraft.weatherAlertsEnabled,
      }),
    );
  }

  async function handleInviteSubmit(event) {
    event.preventDefault();
    await withErrorHandling(() =>
      app.submitFamilyInvite({
        relationType: inviteDraft.relationType,
        inviteMessage: emptyToNull(inviteDraft.inviteMessage),
        expiresInDays: Number(inviteDraft.expiresInDays),
        caregiverPermission: inviteDraft.caregiverPermission,
        weeklyReportEnabled: inviteDraft.weeklyReportEnabled,
        notifyOnHighRisk: inviteDraft.notifyOnHighRisk,
      }),
    );
  }

  async function handlePrivacyConsentSubmit(payload) {
    await withErrorHandling(() => app.submitPrivacyConsent(payload));
  }

  async function handleAcceptInvite(event) {
    event.preventDefault();
    const inviteCode = acceptInviteCode.trim();
    if (!inviteCode) {
      app.setBanner({ tone: "warning", message: "请输入邀请码后再接受。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.acceptInvite(inviteCode);
      setAcceptInviteCode("");
    });
  }

  async function handleFamilyTaskSubmit(event) {
    event.preventDefault();

    if (!familyTaskDraft.bindingCode) {
      app.setBanner({ tone: "warning", message: "请先选择具备共同照护权限的家属成员。" });
      return;
    }

    if (!familyTaskDraft.title.trim()) {
      app.setBanner({ tone: "warning", message: "请输入代办标题后再创建。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.submitFamilyTask(familyTaskDraft.bindingCode, {
        title: familyTaskDraft.title.trim(),
        description: emptyToNull(familyTaskDraft.description),
        dueAt: familyTaskDraft.dueAt ? toIsoString(familyTaskDraft.dueAt) : null,
      });
      setFamilyTaskDraft((current) => ({
        ...current,
        title: "",
        description: "",
        dueAt: "",
      }));
    });
  }

  async function handleDeviceBindingSubmit(event) {
    event.preventDefault();
    await withErrorHandling(() =>
      app.submitDeviceBinding({
        ...deviceDraft,
        vendorName: deviceDraft.vendorName.trim(),
        serialNumber: deviceDraft.serialNumber.trim(),
        deviceModel: emptyToNull(deviceDraft.deviceModel),
        aliasName: emptyToNull(deviceDraft.aliasName),
      }),
    );
  }

  async function handleDeviceSyncSubmit(event) {
    event.preventDefault();

    if (!deviceSyncDraft.deviceCode) {
      app.setBanner({ tone: "warning", message: "请先选择设备后再同步。" });
      return;
    }

    const metricType = deviceSyncDraft.metricType;
    const item = {
      metricType,
      externalEventId: deviceSyncDraft.externalEventId.trim(),
      measuredAt: toIsoString(deviceSyncDraft.measuredAt),
      note: emptyToNull(deviceSyncDraft.note),
    };

    if (!item.externalEventId) {
      app.setBanner({ tone: "warning", message: "请填写外部事件 ID。" });
      return;
    }

    if (deviceSyncQueue.length && deviceSyncQueue[0].deviceCode !== deviceSyncDraft.deviceCode) {
      app.setBanner({ tone: "warning", message: "当前队列中已存在其他设备的数据，请先提交或清空队列。" });
      return;
    }

    if (metricType === "HYDRATION") {
      item.waterIntakeMl = Number(deviceSyncDraft.waterIntakeMl || 0);
      item.urineColorLevel = Number(deviceSyncDraft.urineColorLevel || 0);
    } else if (metricType === "BLOOD_PRESSURE") {
      item.systolicPressure = Number(deviceSyncDraft.value || 0);
      item.diastolicPressure = Number(deviceSyncDraft.diastolicPressure || 0);
      item.pulseRate = deviceSyncDraft.pulseRate ? Number(deviceSyncDraft.pulseRate) : null;
      item.unit = emptyToNull(deviceSyncDraft.unit) || "mmHg";
    } else {
      item.value = Number(deviceSyncDraft.value || 0);
      item.unit = emptyToNull(deviceSyncDraft.unit);
    }

    setDeviceSyncQueue((current) => [
      ...current,
      {
        ...item,
        deviceCode: deviceSyncDraft.deviceCode,
      },
    ]);
    setDeviceSyncDraft((current) => ({
      ...current,
      externalEventId: "",
      measuredAt: "",
      value: "",
      diastolicPressure: "",
      pulseRate: "",
      waterIntakeMl: "",
      urineColorLevel: "",
      note: "",
    }));
    app.setBanner({ tone: "success", message: "同步草稿已加入队列，可继续添加或批量提交。" });
  }

  async function handleSubmitSyncQueue() {
    if (!deviceSyncQueue.length) {
      app.setBanner({ tone: "warning", message: "当前没有待同步的数据。" });
      return;
    }

    const deviceCode = deviceSyncQueue[0].deviceCode;
    const items = deviceSyncQueue
      .filter((item) => item.deviceCode === deviceCode)
      .map(({ deviceCode: _deviceCode, ...item }) => item);

    await withErrorHandling(async () => {
      await app.submitDeviceSync(deviceCode, { items });
      setDeviceSyncQueue([]);
    });
  }

  function handleRemoveSyncQueueItem(index) {
    setDeviceSyncQueue((current) => current.filter((_, itemIndex) => itemIndex !== index));
  }

  async function handleFileUpload(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const file = formData.get("file");

    if (!(file instanceof File) || !file.size) {
      app.setBanner({ tone: "warning", message: "请先选择要上传的文件。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.submitFileUpload(file);
      event.currentTarget.reset();
    });
  }

  async function handleOpenFile(fileId) {
    if (!session?.token) {
      app.setBanner({ tone: "warning", message: "请先登录后再查看文件。" });
      return;
    }

    await withErrorHandling(async () => {
      const response = await fetch(`/api/v1/files/${encodeURIComponent(fileId)}`, {
        headers: {
          Authorization: `Bearer ${session.token}`,
        },
      });

      if (!response.ok) {
        throw new Error(`文件打开失败：${response.status}`);
      }

      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      window.open(blobUrl, "_blank", "noopener,noreferrer");
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60_000);
    });
  }

  async function handleRecordFilterChange(nextType) {
    setRecordCenterType(nextType);
    await withErrorHandling(() => app.loadRecordCenterData({ type: nextType }));
  }

  async function handleRecordUpdate(event) {
    event.preventDefault();

    if (!data.recordDetail) {
      return;
    }

    const detailType = data.recordDetail.type;

    const payload = {
      value: detailType === "URIC_ACID" && recordEditDraft.value ? Number(recordEditDraft.value) : recordEditDraft.integerValue ? Number(recordEditDraft.integerValue) : null,
      decimalValue: detailType === "WEIGHT"
        ? (recordEditDraft.value ? Number(recordEditDraft.value) : null)
        : (recordEditDraft.decimalValue ? Number(recordEditDraft.decimalValue) : null),
      unit: emptyToNull(recordEditDraft.unit),
      measuredAt: toIsoString(emptyToNull(recordEditDraft.measuredAt)),
      source: emptyToNull(recordEditDraft.source),
      systolicPressure: recordEditDraft.systolicPressure ? Number(recordEditDraft.systolicPressure) : null,
      diastolicPressure: recordEditDraft.diastolicPressure ? Number(recordEditDraft.diastolicPressure) : null,
      pulseRate: recordEditDraft.pulseRate ? Number(recordEditDraft.pulseRate) : null,
      waterIntakeMl: recordEditDraft.waterIntakeMl ? Number(recordEditDraft.waterIntakeMl) : null,
      urineColorLevel: recordEditDraft.urineColorLevel ? Number(recordEditDraft.urineColorLevel) : null,
      checkedAt: toIsoString(emptyToNull(recordEditDraft.checkedAt)),
      joint: emptyToNull(recordEditDraft.joint),
      painLevel: recordEditDraft.painLevel ? Number(recordEditDraft.painLevel) : null,
      startedAt: toIsoString(emptyToNull(recordEditDraft.startedAt)),
      durationNote: emptyToNull(recordEditDraft.durationNote),
      note: emptyToNull(recordEditDraft.note),
      changeReason: recordChangeReason.trim(),
    };

    if (!payload.changeReason) {
      app.setBanner({ tone: "warning", message: "请填写更正原因后再保存记录。" });
      return;
    }

    await withErrorHandling(async () => {
      await app.submitRecordUpdate(data.recordDetail.type, data.recordDetail.recordId, payload);
      setRecordChangeReason("");
    });
  }

  async function handleRecordRestore(auditId) {
    if (!data.recordDetail) {
      return;
    }

    const changeReason = recordRestoreReason.trim();
    if (!changeReason) {
      app.setBanner({ tone: "warning", message: "请填写恢复原因。" });
      return;
    }

    await withErrorHandling(() =>
      app.restoreRecordFromAudit(data.recordDetail.type, data.recordDetail.recordId, auditId, changeReason),
    );
  }

  return (
    <div className="page-shell">
      <header className="site-header">
        <div className="site-header__inner">
          <NavLink className="brand" to="/overview">
            <span className="brand__mark">T</span>
            <span className="brand__text">
              <strong>痛风主动管理平台</strong>
              <small>Warmly structured care</small>
            </span>
          </NavLink>
          <nav className="site-nav">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `site-nav__link${isActive ? " site-nav__link--active" : ""}`}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="site-header__actions">
            <button className="pill-button" type="button" onClick={handleRefresh} disabled={!session || app.isHydrating}>
              {app.isHydrating ? "刷新中..." : "刷新数据"}
            </button>
            <button className="ghost-button" type="button" onClick={app.logout} disabled={!session}>
              退出登录
            </button>
          </div>
        </div>
      </header>

      <main id="top">
        <section className="hero-section">
          <div className="hero-copy">
            <p className="section-kicker">痛风主动管理</p>
            <h1>先告诉患者今天该做什么，而不是先展示一块研发味很重的仪表盘。</h1>
            <p className="hero-copy__lead">
              这一版把首页改成“今日行动页”，优先交付风险分流、行动清单和 AI 使用边界，让患者先完成关键任务，再查看趋势、档案和分析细节。
            </p>
            <HeroMetrics overview={data.overview} />
          </div>

          <aside className="hero-panel">
            <div className="panel-head">
              <p className="section-kicker">真实登录与授权</p>
              <h2>先完成正式登录，再进入今天的主动管理任务流</h2>
            </div>
            <div className="action-row">
              <button className={authMode === "login" ? "pill-button" : "ghost-button"} type="button" onClick={() => setAuthMode("login")}>
                账号登录
              </button>
              <button className={authMode === "register" ? "pill-button" : "ghost-button"} type="button" onClick={() => setAuthMode("register")}>
                新用户注册
              </button>
            </div>
            {authMode === "login" ? (
              <form className="stack-form" onSubmit={handlePasswordLogin}>
                <label>
                  <span>账号类型</span>
                  <select value={loginDraft.accountType} onChange={(event) => setLoginDraft((current) => ({ ...current, accountType: event.target.value }))}>
                    {authAccountTypeOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>账号</span>
                  <input
                    value={loginDraft.account}
                    onChange={(event) => setLoginDraft((current) => ({ ...current, account: event.target.value }))}
                    placeholder={loginDraft.accountType === "EMAIL" ? "例如：zhangsan@example.com" : "例如：13800138000"}
                  />
                </label>
                <label>
                  <span>密码</span>
                  <input
                    type="password"
                    value={loginDraft.password}
                    onChange={(event) => setLoginDraft((current) => ({ ...current, password: event.target.value }))}
                    placeholder="至少 8 位"
                  />
                </label>
                <button className="primary-button" type="submit" disabled={busyMap.login}>
                  {busyMap.login ? "登录中..." : "登录并进入"}
                </button>
              </form>
            ) : (
              <form className="stack-form" onSubmit={handleRegister}>
                <label>
                  <span>昵称</span>
                  <input value={registerDraft.nickname} onChange={(event) => setRegisterDraft((current) => ({ ...current, nickname: event.target.value }))} />
                </label>
                <label>
                  <span>账号类型</span>
                  <select value={registerDraft.accountType} onChange={(event) => setRegisterDraft((current) => ({ ...current, accountType: event.target.value }))}>
                    {authAccountTypeOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  <span>账号</span>
                  <input
                    value={registerDraft.account}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, account: event.target.value }))}
                    placeholder={registerDraft.accountType === "EMAIL" ? "例如：zhangsan@example.com" : "例如：13800138000"}
                  />
                </label>
                <label>
                  <span>密码</span>
                  <input
                    type="password"
                    value={registerDraft.password}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, password: event.target.value }))}
                    placeholder="至少 8 位"
                  />
                </label>
                <label>
                  <span>确认密码</span>
                  <input
                    type="password"
                    value={registerDraft.confirmPassword}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, confirmPassword: event.target.value }))}
                  />
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={registerDraft.privacyAccepted}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, privacyAccepted: event.target.checked }))}
                  />
                  <span>我已阅读并同意隐私政策</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={registerDraft.termsAccepted}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, termsAccepted: event.target.checked }))}
                  />
                  <span>我已阅读并同意服务条款</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={registerDraft.medicalDataAuthorized}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, medicalDataAuthorized: event.target.checked }))}
                  />
                  <span>同意平台使用健康数据生成主动管理建议</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={registerDraft.familyCollaborationAuthorized}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, familyCollaborationAuthorized: event.target.checked }))}
                  />
                  <span>同意后续启用家庭协同授权能力</span>
                </label>
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={registerDraft.notificationAuthorized}
                    onChange={(event) => setRegisterDraft((current) => ({ ...current, notificationAuthorized: event.target.checked }))}
                  />
                  <span>同意接收随访与风险提醒通知</span>
                </label>
                <button className="primary-button" type="submit" disabled={busyMap.register}>
                  {busyMap.register ? "注册中..." : "注册并进入"}
                </button>
              </form>
            )}
            <div className="session-card">
              <strong>开发体验入口</strong>
              <form className="stack-form compact-form" onSubmit={handleMockLogin}>
                <label>
                  <span>Mock 昵称</span>
                  <input value={mockNickname} onChange={(event) => setMockNickname(event.target.value)} placeholder="例如：开发联调用户" />
                </label>
                <button className="ghost-button" type="submit" disabled={busyMap.mockLogin}>
                  {busyMap.mockLogin ? "进入中..." : "进入开发体验环境"}
                </button>
              </form>
            </div>
            <div className="session-card">
              {session ? (
                <>
                  <strong>{session.nickname}</strong>
                  <p>用户 ID：{session.userId}</p>
                  <p>登录方式：{session.authMode || "UNKNOWN"} / {session.accountType || "DEMO"}</p>
                  <p>账号标识：{session.accountIdentifier || "开发体验账号"}</p>
                  <p>过期时间：{formatDateTime(session.expiresAt)}</p>
                </>
              ) : (
                <p>当前优先展示真实登录骨架，同时保留 mock 入口用于本地联调。</p>
              )}
            </div>
            <StatusBanner tone={app.banner.tone} message={app.banner.message} />
          </aside>
        </section>

        <Routes>
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage app={app} data={data} trendOptions={trendOptions} familyFeatureEnabled={familyFeatureEnabled} />} />
          <Route
            path="/records"
            element={(
              <RecordsPage
                app={app}
                data={data}
                busyMap={busyMap}
                session={session}
                recordCenterType={recordCenterType}
                handleRecordSubmit={handleRecordSubmit}
                handleRecordFilterChange={handleRecordFilterChange}
                withErrorHandling={withErrorHandling}
                recordEditDraft={recordEditDraft}
                setRecordEditDraft={setRecordEditDraft}
                recordChangeReason={recordChangeReason}
                setRecordChangeReason={setRecordChangeReason}
                handleRecordUpdate={handleRecordUpdate}
                recordRestoreReason={recordRestoreReason}
                setRecordRestoreReason={setRecordRestoreReason}
                handleRecordRestore={handleRecordRestore}
              />
            )}
          />
          <Route
            path="/analysis"
            element={<AnalysisPage data={data} busyMap={busyMap} session={session} handleMealSubmit={handleMealSubmit} />}
          />
          <Route
            path="/proactive"
            element={(
              <ProactivePage
                data={data}
                busyMap={busyMap}
                session={session}
                proactiveDraft={proactiveDraft}
                setProactiveDraft={setProactiveDraft}
                handleProactiveSubmit={handleProactiveSubmit}
              />
            )}
          />
          <Route
            path="/family"
            element={familyFeatureEnabled ? (
              <FamilyPage
                app={app}
                data={data}
                busyMap={busyMap}
                session={session}
                inviteDraft={inviteDraft}
                setInviteDraft={setInviteDraft}
                handleInviteSubmit={handleInviteSubmit}
                familyTaskDraft={familyTaskDraft}
                setFamilyTaskDraft={setFamilyTaskDraft}
                handleFamilyTaskSubmit={handleFamilyTaskSubmit}
                acceptInviteCode={acceptInviteCode}
                setAcceptInviteCode={setAcceptInviteCode}
                handleAcceptInvite={handleAcceptInvite}
                familySummaryTargetName={familySummaryTargetName}
                familyWeeklyReportTargetName={familyWeeklyReportTargetName}
                withErrorHandling={withErrorHandling}
              />
            ) : <Navigate to="/overview" replace />}
          />
          <Route path="/devices" element={<Navigate to="/overview" replace />} />
          <Route path="/growth" element={<Navigate to="/overview" replace />} />
          <Route
            path="/assistant"
            element={(
              <AssistantPage
                app={app}
                data={data}
                busyMap={busyMap}
                session={session}
                handleKnowledgeSubmit={handleKnowledgeSubmit}
                profileDraft={profileDraft}
                setProfileDraft={setProfileDraft}
                handleProfileSubmit={handleProfileSubmit}
                handlePrivacyConsentSubmit={handlePrivacyConsentSubmit}
                handlePasswordChange={handlePasswordChange}
                handleRevokeSession={handleRevokeSession}
                medicationDraft={medicationDraft}
                setMedicationDraft={setMedicationDraft}
                handleMedicationSubmit={handleMedicationSubmit}
                medicationCheckinDraft={medicationCheckinDraft}
                setMedicationCheckinDraft={setMedicationCheckinDraft}
                handleMedicationCheckinSubmit={handleMedicationCheckinSubmit}
                handleFileUpload={handleFileUpload}
                handleOpenFile={handleOpenFile}
                handleLabSubmit={handleLabSubmit}
                familyFeatureEnabled={familyFeatureEnabled}
                inviteDraft={inviteDraft}
                setInviteDraft={setInviteDraft}
                handleInviteSubmit={handleInviteSubmit}
                acceptInviteCode={acceptInviteCode}
                setAcceptInviteCode={setAcceptInviteCode}
                handleAcceptInvite={handleAcceptInvite}
                familySummaryTargetName={familySummaryTargetName}
                familyWeeklyReportTargetName={familyWeeklyReportTargetName}
                withErrorHandling={withErrorHandling}
              />
            )}
          />
          <Route path="*" element={<Navigate to="/overview" replace />} />
        </Routes>
      </main>
    </div>
  );
}

