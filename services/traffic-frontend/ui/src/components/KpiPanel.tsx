import type { SessionSnapshotResponse, TrafficSummaryResponse } from '../types'

interface KpiPanelProps {
  summary: TrafficSummaryResponse | null
  generatedAt: string | null
  session: SessionSnapshotResponse
}

function formatDate(value: string | null): string {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(parsed)
}

export default function KpiPanel({ summary, generatedAt, session }: KpiPanelProps) {
  const regions = summary?.regions ?? []
  const averageSpeed = regions.length > 0
    ? regions.reduce((acc, region) => acc + Number(region.averageSpeedKph ?? 0), 0) / regions.length
    : null

  const maxCongestion = regions.reduce(
    (acc, region) => Math.max(acc, Number(region.latestCongestionLevel ?? 0)),
    0,
  )

  return (
    <article className="panel kpi-panel" data-reveal="true">
      <div className="panel-title-row">
        <h2>운영 KPI</h2>
        <p>{formatDate(generatedAt)}</p>
      </div>
      <div className="kpi-grid">
        <div className="kpi-card">
          <p className="kpi-label">총 이벤트 수</p>
          <p className="kpi-value">{summary?.totalEvents ?? 0}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">평균 속도(km/h)</p>
          <p className="kpi-value">{averageSpeed == null ? '-' : averageSpeed.toFixed(1)}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">최고 혼잡도</p>
          <p className="kpi-value">{maxCongestion === 0 ? '-' : maxCongestion}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">로그인 사용자</p>
          <p className="kpi-value">{session.authenticated ? (session.username ?? '운영자') : '익명 모드'}</p>
        </div>
      </div>
    </article>
  )
}
