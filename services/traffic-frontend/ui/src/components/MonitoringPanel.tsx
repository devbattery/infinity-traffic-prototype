import type { MonitoringSnapshotResponse } from '../types'

interface MonitoringPanelProps {
  snapshot: MonitoringSnapshotResponse | null
  errorMessage: string | null
  onRefresh: () => Promise<void>
  refreshing: boolean
}

export default function MonitoringPanel({ snapshot, errorMessage, onRefresh, refreshing }: MonitoringPanelProps) {
  return (
    <article className={`panel monitoring-panel${refreshing ? ' is-refreshing' : ''}`} data-reveal="true">
      <div className="panel-title-row">
        <h2>모니터링 연동</h2>
        <button className="btn secondary" type="button" onClick={() => void onRefresh()} disabled={refreshing}>
          {refreshing ? '갱신 중...' : '상태 갱신'}
        </button>
      </div>

      {snapshot ? (
        <>
          <p className="muted">최근 점검 시각: {formatDate(snapshot.generatedAt)}</p>

          <div className="monitoring-links">
            {snapshot.links.map((link) => (
              <a
                key={link.name}
                className="monitoring-link-card"
                href={link.url}
                target="_blank"
                rel="noreferrer noopener"
              >
                <span className="monitoring-link-title">{link.name}</span>
                <span className="muted">{link.description}</span>
              </a>
            ))}
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Target</th>
                  <th>Status</th>
                  <th>Latency(ms)</th>
                  <th>Checked At</th>
                </tr>
              </thead>
              <tbody>
                {snapshot.targets.map((target) => (
                  <tr key={target.name}>
                    <td>
                      <div className="monitoring-target-cell">
                        <strong>{target.name}</strong>
                        <p className="muted">{target.healthUrl}</p>
                      </div>
                    </td>
                    <td>
                      <span className={`monitoring-badge ${toStatusClassName(target.status)}`}>{target.status}</span>
                    </td>
                    <td>{target.responseTimeMs ?? '-'}</td>
                    <td>{formatDate(target.checkedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {errorMessage ? <p className="muted monitoring-error">주의: {errorMessage}</p> : null}
        </>
      ) : (
        <p className="muted">모니터링 상태를 불러오는 중입니다.</p>
      )}
    </article>
  )
}

function formatDate(value: string): string {
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

function toStatusClassName(status: string): string {
  const normalized = status.trim().toUpperCase()
  if (normalized === 'UP') {
    return 'status-up'
  }
  if (normalized === 'DOWN') {
    return 'status-down'
  }
  return 'status-unknown'
}
