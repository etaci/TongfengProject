import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import RiskBadge from "../components/RiskBadge";
import SectionHeader from "../components/SectionHeader";
import { BulletList } from "../components/HealthBlocks";
import { genderOptions } from "../constants/options";
import { formatDateTime } from "../utils/format";

export default function AssistantPage({
  data,
  busyMap,
  session,
  handleKnowledgeSubmit,
  profileDraft,
  setProfileDraft,
  handleProfileSubmit,
  medicationDraft,
  setMedicationDraft,
  handleMedicationSubmit,
  handleFileUpload,
  handleOpenFile,
}) {
  return (
    <section className="content-section" id="assistant">
      <SectionHeader
        kicker="08 / 问答与档案"
        title="把知识问答、个人档案、用药计划与文件管理放到同一页，方便后续继续扩展更多长期管理能力。"
      />

      <div className="overview-grid">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Knowledge assistant</p>
              <h3>知识问答</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleKnowledgeSubmit}>
            <label>
              <span>问题</span>
              <textarea name="question" rows="4" placeholder="例如：痛风发作期饮食要注意什么？" />
            </label>
            <label>
              <span>场景</span>
              <input name="scene" placeholder="例如：夜间疼痛、聚餐后、复查前" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.knowledge}>
              {busyMap.knowledge ? "提交中..." : "发起问答"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Answer panel</p>
              <h3>回答结果</h3>
            </div>
          </div>
          <div className={`result-panel ${data.knowledge ? "" : "empty-panel"}`}>
            {data.knowledge ? (
              <>
                <div className="result-header">
                  <div>
                    <h4>知识库回答</h4>
                    <p>
                      {data.knowledge.escalateToDoctor
                        ? "当前问题建议尽快线下就医或联系医生进一步确认。"
                        : "当前回答更适合作为日常健康管理参考。"}
                    </p>
                  </div>
                  <span className={`inline-tag ${data.knowledge.escalateToDoctor ? "risk-red" : "risk-green"}`}>
                    {data.knowledge.escalateToDoctor ? "建议就医" : "可先自我管理"}
                  </span>
                </div>
                <p>{data.knowledge.answer || "暂无回答内容。"}</p>
                <BulletList title="引用来源" items={data.knowledge.references} />
                <p className="narrative-text">{data.knowledge.disclaimer}</p>
              </>
            ) : (
              "登录后即可向本地知识库提问，这里会展示答案、引用来源与是否建议就医。"
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Activity timeline</p>
              <h3>活动时间线</h3>
            </div>
          </div>
          <div className="timeline-list">
            {data.timeline?.length ? (
              data.timeline.map((item) => (
                <article className="timeline-item" key={item.eventId}>
                  <div className="result-header">
                    <div>
                      <strong>{item.title || item.type}</strong>
                      <p>{item.detail || "暂无详细说明。"}</p>
                    </div>
                    <RiskBadge level={item.riskLevel} />
                  </div>
                  <div className="timeline-item__meta">
                    <span className="event-type">{item.type}</span>
                    <span>{formatDateTime(item.occurredAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无活动时间线数据。" />
            )}
          </div>
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Profile archive</p>
              <h3>个人档案</h3>
            </div>
          </div>
          <form className="stack-form compact-form" onSubmit={handleProfileSubmit}>
            <label>
              <span>姓名</span>
              <input value={profileDraft.name} onChange={(event) => setProfileDraft((current) => ({ ...current, name: event.target.value }))} />
            </label>
            <label>
              <span>性别</span>
              <select value={profileDraft.gender} onChange={(event) => setProfileDraft((current) => ({ ...current, gender: event.target.value }))}>
                {genderOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label>
              <span>生日</span>
              <input type="date" value={profileDraft.birthday} onChange={(event) => setProfileDraft((current) => ({ ...current, birthday: event.target.value }))} />
            </label>
            <label>
              <span>身高（cm）</span>
              <input
                type="number"
                min="100"
                max="240"
                value={profileDraft.heightCm}
                onChange={(event) => setProfileDraft((current) => ({ ...current, heightCm: event.target.value }))}
              />
            </label>
            <label>
              <span>目标尿酸（umol/L）</span>
              <input
                type="number"
                min="180"
                max="600"
                value={profileDraft.targetUricAcid}
                onChange={(event) => setProfileDraft((current) => ({ ...current, targetUricAcid: event.target.value }))}
              />
            </label>
            <label>
              <span>过敏信息</span>
              <input
                value={profileDraft.allergies}
                onChange={(event) => setProfileDraft((current) => ({ ...current, allergies: event.target.value }))}
                placeholder="多个条目用逗号分隔"
              />
            </label>
            <label>
              <span>合并症</span>
              <input
                value={profileDraft.comorbidities}
                onChange={(event) => setProfileDraft((current) => ({ ...current, comorbidities: event.target.value }))}
                placeholder="多个条目用逗号分隔"
              />
            </label>
            <label>
              <span>紧急联系人</span>
              <input
                value={profileDraft.emergencyContact}
                onChange={(event) => setProfileDraft((current) => ({ ...current, emergencyContact: event.target.value }))}
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.profile}>
              {busyMap.profile ? "保存中..." : "保存个人档案"}
            </button>
          </form>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Medication plan</p>
              <h3>用药计划</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleMedicationSubmit}>
            <label>
              <span>当前用药清单</span>
              <textarea
                rows="8"
                value={medicationDraft.lines}
                onChange={(event) => setMedicationDraft((current) => ({ ...current, lines: event.target.value }))}
                placeholder="每行一条，例如：非布司他 | 40mg | 每日一次 | 饭后"
              />
            </label>
            <label>
              <span>随访备注</span>
              <textarea
                rows="4"
                value={medicationDraft.followUpNote}
                onChange={(event) => setMedicationDraft((current) => ({ ...current, followUpNote: event.target.value }))}
                placeholder="记录不适反应、复查安排或医生建议"
              />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.medication}>
              {busyMap.medication ? "保存中..." : "保存用药计划"}
            </button>
          </form>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Files</p>
              <h3>文件上传</h3>
            </div>
          </div>
          <form className="stack-form" onSubmit={handleFileUpload}>
            <label>
              <span>选择文件</span>
              <input name="file" type="file" />
            </label>
            <button className="primary-button" type="submit" disabled={!session || busyMap.fileUpload}>
              {busyMap.fileUpload ? "上传中..." : "上传文件"}
            </button>
          </form>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Recent uploads</p>
              <h3>最近上传</h3>
            </div>
          </div>
          <div className="masonry-list">
            {data.uploadedFiles?.length ? (
              data.uploadedFiles.map((item) => (
                <article className="list-card" key={item.fileId}>
                  <div className="result-header">
                    <strong>{item.fileName}</strong>
                    <span className="inline-tag">{item.contentType || "file"}</span>
                  </div>
                  <p>文件大小：{item.size} bytes</p>
                  <div className="action-row">
                    <button className="ghost-button action-button" type="button" onClick={() => handleOpenFile(item.fileId)}>
                      打开文件
                    </button>
                    <span className="token token--tiny">{item.fileId}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无已上传文件。" />
            )}
          </div>
        </Card>
      </div>
    </section>
  );
}
