import Card from "../components/Card";
import EmptyState from "../components/EmptyState";
import SectionHeader from "../components/SectionHeader";
import { BulletList } from "../components/HealthBlocks";
import { formatDateTime } from "../utils/format";

export default function GrowthPage({ app, data, busyMap, withErrorHandling }) {
  return (
    <section className="content-section" id="growth">
      <SectionHeader
        kicker="07 / 成长"
        title="把积分、任务、周计划和奖励体系整合成成长中心，为后续增加更多激励玩法保留空间。"
      />

      <div className="overview-grid">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Growth overview</p>
              <h3>成长概览</h3>
            </div>
          </div>
          {data.growthOverview ? (
            <>
              <div className="stats-grid">
                {[
                  ["当前等级", data.growthOverview.level],
                  ["累计积分", data.growthOverview.totalPoints],
                  ["今日积分", data.growthOverview.todayPoints],
                  ["徽章数量", data.growthOverview.badgesCount],
                ].map(([label, value]) => (
                  <div className="stat-line" key={label}>
                    <span>{label}</span>
                    <strong>{value}</strong>
                  </div>
                ))}
              </div>
              <div className="split-block">
                <div>
                  <h4>{data.growthOverview.levelTitle}</h4>
                  <p className="narrative-text">
                    当前连续天数 {data.growthOverview.currentStreakDays} 天 / 最长连续 {data.growthOverview.longestStreakDays} 天
                  </p>
                  <p className="narrative-text">
                    当前等级积分区间 {data.growthOverview.currentLevelMinPoints} - {data.growthOverview.nextLevelPoints ?? "已满级"}
                  </p>
                  <p className="narrative-text">可兑换积分：{data.growthOverview.redeemablePoints}</p>
                </div>
                <div>
                  <h4>成长亮点</h4>
                  <BulletList title="最近表现" items={data.growthOverview.highlights} />
                </div>
              </div>
            </>
          ) : (
            <EmptyState message="暂无成长概览数据。" />
          )}
        </Card>

        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Tasks</p>
              <h3>成长任务</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.growthTasks?.length ? (
              data.growthTasks.map((item) => (
                <article className="list-card" key={item.taskCode}>
                  <div className="result-header">
                    <strong>{item.title}</strong>
                    <span className={`inline-tag ${item.completed ? "risk-green" : ""}`}>{item.completed ? "已完成" : "进行中"}</span>
                  </div>
                  <p>{item.description}</p>
                  <div className="list-card__meta">
                    <span>奖励 {item.rewardPoints} 积分</span>
                    <span>{item.completedCount} / {item.targetCount}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无成长任务。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Point logs</p>
              <h3>积分记录</h3>
            </div>
          </div>
          <div className="stack-list">
            {data.growthPoints?.length ? (
              data.growthPoints.map((item) => (
                <article className="list-card" key={item.pointId}>
                  <div className="result-header">
                    <strong>{item.summary}</strong>
                    <span className="inline-tag">+{item.points}</span>
                  </div>
                  <p>{item.actionType}</p>
                  <div className="list-card__meta">
                    <span>{item.awardedDate}</span>
                    <span>{formatDateTime(item.createdAt)}</span>
                  </div>
                </article>
              ))
            ) : (
              <EmptyState message="暂无积分记录。" />
            )}
          </div>
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Badges</p>
              <h3>已获得徽章</h3>
            </div>
          </div>
          <div className="token-list">
            {data.growthBadges?.length ? (
              data.growthBadges.map((item) => (
                <div className="token" key={item.badgeKey}>
                  <strong>{item.badgeName}</strong>
                  <p className="narrative-text">{item.badgeDescription}</p>
                  <small className="meta-text">{formatDateTime(item.awardedAt)}</small>
                </div>
              ))
            ) : (
              <EmptyState message="暂无徽章记录。" />
            )}
          </div>
        </Card>
      </div>

      <div className="overview-grid overview-grid--secondary">
        <Card>
          <div className="card-head">
            <div>
              <p className="eyebrow">Weekly plan</p>
              <h3>每周计划</h3>
            </div>
          </div>
          {data.growthWeeklyPlan ? (
            <>
              <div className="stats-grid stats-grid--compact">
                {[
                  ["已得积分", data.growthWeeklyPlan.weeklyEarnedPoints],
                  ["目标积分", data.growthWeeklyPlan.targetPoints],
                  ["完成度", `${data.growthWeeklyPlan.progressPercent}%`],
                ].map(([label, value]) => (
                  <div className="stat-line" key={label}>
                    <span>{label}</span>
                    <strong>{value}</strong>
                  </div>
                ))}
              </div>
              <p className="narrative-text">
                周期：{data.growthWeeklyPlan.weekStartDate} 至 {data.growthWeeklyPlan.weekEndDate}
              </p>
              <div className="stack-list">
                {(data.growthWeeklyPlan.challenges || []).map((item) => (
                  <article className="list-card" key={item.challengeCode}>
                    <div className="result-header">
                      <strong>{item.title}</strong>
                      <span className="inline-tag">{item.priority}</span>
                    </div>
                    <p>{item.description}</p>
                    <div className="list-card__meta">
                      <span>{item.completedCount} / {item.targetCount}</span>
                      <span>奖励 {item.rewardPoints} 积分</span>
                    </div>
                  </article>
                ))}
              </div>
            </>
          ) : (
            <EmptyState message="暂无周计划。" />
          )}
        </Card>

        <Card className="span-2">
          <div className="card-head">
            <div>
              <p className="eyebrow">Rewards</p>
              <h3>奖励与兑换</h3>
            </div>
          </div>
          <div className="split-block">
            <div>
              <h4>可兑换奖励</h4>
              <div className="stack-list">
                {data.growthRewards?.length ? (
                  data.growthRewards.map((item) => (
                    <article className="list-card" key={item.rewardKey}>
                      <div className="result-header">
                        <strong>{item.rewardName}</strong>
                        <span className="inline-tag">{item.pointsCost} 积分</span>
                      </div>
                      <p>{item.rewardDescription}</p>
                      <div className="list-card__meta">
                        <span>{item.rewardType}</span>
                        <span>剩余可领 {item.remainingClaims} 次</span>
                      </div>
                      <div className="action-row">
                        <span className="token token--tiny">{item.claimHint}</span>
                        <button
                          className="ghost-button action-button"
                          type="button"
                          disabled={!item.claimable || busyMap[`reward-${item.rewardKey}`]}
                          onClick={() => withErrorHandling(() => app.claimReward(item.rewardKey))}
                        >
                          {busyMap[`reward-${item.rewardKey}`] ? "兑换中..." : item.claimable ? "立即兑换" : "积分不足"}
                        </button>
                      </div>
                    </article>
                  ))
                ) : (
                  <EmptyState message="暂无可兑换奖励。" />
                )}
              </div>
            </div>
            <div>
              <h4>兑换记录</h4>
              <div className="stack-list">
                {data.growthRewardClaims?.length ? (
                  data.growthRewardClaims.map((item) => (
                    <article className="list-card" key={item.claimCode}>
                      <div className="result-header">
                        <strong>{item.rewardName}</strong>
                        <span className="inline-tag">{item.status}</span>
                      </div>
                      <p>{item.claimNote || "暂无备注。"}</p>
                      <div className="list-card__meta">
                        <span>消耗 {item.pointsCost} 积分</span>
                        <span>剩余 {item.remainingPoints} 积分</span>
                      </div>
                    </article>
                  ))
                ) : (
                  <EmptyState message="暂无兑换记录。" />
                )}
              </div>
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
}
