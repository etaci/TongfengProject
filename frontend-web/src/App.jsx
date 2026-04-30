import { useEffect, useMemo, useState } from "react";
import { Navigate, NavLink, Route, Routes } from "react-router-dom";
import StatusBanner from "./components/StatusBanner";
import AnalysisPage from "./pages/AnalysisPage";
import AssistantPage from "./pages/AssistantPage";
import FamilyPage from "./pages/FamilyPage";
import OverviewPage from "./pages/OverviewPage";
import ProactivePage from "./pages/ProactivePage";
import RecordsPage from "./pages/RecordsPage";
import { trendOptions } from "./constants/options";
import useTongfengApp from "./hooks/useTongfengApp";
import { formatDateTime } from "./utils/format";
import { emptyToNull, parseMedicationText, splitCsv, toIsoString } from "./utils/forms";

function HeroMetrics({ overview }) {
  const metrics = overview
    ? [
        {
          label: "今日重点",
          value: `${overview.todayFocus?.length || 0} 项`,
          description: overview.todayFocus?.slice(0, 2).join(" / ") || "今天暂无额外提醒",
        },
        {
          label: "风险阶段",
          value: overview.stage || "ACTIVE",
          description: overview.latestRiskSummary || "后端暂未生成风险摘要",
        },
        {
          label: "闭环动作",
          value: `${overview.mealsCount || 0} 餐 / ${overview.uricAcidCount || 0} 次尿酸`,
          description: `高风险餐 ${overview.highRiskMealsCount || 0} 次，发作 ${overview.flareCount || 0} 次`,
        },
      ]
    : [
        {
          label: "今日重点",
          value: "等待登录",
          description: "登录后自动读取当天的管理建议",
        },
        {
          label: "风险阶段",
          value: "数据未加载",
          description: "总览、提醒和趋势会在这里汇总",
        },
        {
          label: "闭环动作",
          value: "等待同步",
          description: "当前版本聚焦饮食识别、记录、分析与档案四条主链路",
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

  const [loginNickname, setLoginNickname] = useState("");
  const [profileDraft, setProfileDraft] = useState(app.profileForm);
  const [medicationDraft, setMedicationDraft] = useState(app.medicationForm);
  const [proactiveDraft, setProactiveDraft] = useState({
    monitoringCity: "",
    countryCode: "CN",
    weatherAlertsEnabled: true,
  });
  const [inviteDraft, setInviteDraft] = useState({
    relationType: "SPOUSE",
    inviteMessage: "",
    expiresInDays: "7",
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
      setLoginNickname("");
      return;
    }
    setLoginNickname(session.nickname || "");
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

  const familySummaryTargetName = useMemo(() => data.familyPatientSummary?.patientNickname || "", [data.familyPatientSummary]);
  const familyFeatureEnabled = useMemo(
    () => (data.capabilities?.features || []).some((item) => item.featureKey === "family-care" && item.enabled),
    [data.capabilities],
  );
  const navItems = useMemo(
    () => [
      { to: "/overview", label: "总览" },
      { to: "/records", label: "记录" },
      { to: "/analysis", label: "分析" },
      { to: "/assistant", label: "问答与档案" },
    ],
    [],
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

  async function handleLogin(event) {
    event.preventDefault();
    const nickname = loginNickname.trim();

    if (!nickname) {
      app.setBanner({ tone: "warning", message: "请输入昵称后再登录。" });
      return;
    }

    await withErrorHandling(() => app.login(nickname));
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
      }),
    );
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
            <p className="section-kicker">Zapier-inspired responsive dashboard</p>
            <h1>把每日指标、饮食风险、家庭协同与设备数据，放进一个可持续扩展的动态 Web 端里。</h1>
            <p className="hero-copy__lead">
              这个版本继续沿用暖色背景、卡片承重和橙色聚焦的设计语言，并把当前 MVP 主链路收敛到记录、趋势、原因分析、OCR 与家属轻协同几个核心面板中。
            </p>
            <HeroMetrics overview={data.overview} />
          </div>

          <aside className="hero-panel">
            <div className="panel-head">
              <p className="section-kicker">Mock login</p>
              <h2>快速进入体验环境</h2>
            </div>
            <form className="stack-form" onSubmit={handleLogin}>
              <label>
                <span>昵称</span>
                <input value={loginNickname} onChange={(event) => setLoginNickname(event.target.value)} placeholder="例如：张三" />
              </label>
              <button className="primary-button" type="submit" disabled={busyMap.login}>
                {busyMap.login ? "登录中..." : "立即登录并拉取数据"}
              </button>
            </form>
            <div className="session-card">
              {session ? (
                <>
                  <strong>{session.nickname}</strong>
                  <p>用户 ID：{session.userId}</p>
                  <p>过期时间：{formatDateTime(session.expiresAt)}</p>
                </>
              ) : (
                <p>当前尚未建立会话。</p>
              )}
            </div>
            <StatusBanner tone={app.banner.tone} message={app.banner.message} />
          </aside>
        </section>

        <Routes>
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage app={app} data={data} trendOptions={trendOptions} />} />
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
                acceptInviteCode={acceptInviteCode}
                setAcceptInviteCode={setAcceptInviteCode}
                handleAcceptInvite={handleAcceptInvite}
                familySummaryTargetName={familySummaryTargetName}
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
                medicationDraft={medicationDraft}
                setMedicationDraft={setMedicationDraft}
                handleMedicationSubmit={handleMedicationSubmit}
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

