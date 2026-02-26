import type { TrafficEventMessage } from '../types'

interface RecentPanelProps {
  events: TrafficEventMessage[]
  limit: number
  onLimitChange: (limit: number) => void
  refreshTick: number
  refreshing: boolean
}

export default function RecentPanel({ events, limit, onLimitChange, refreshTick, refreshing }: RecentPanelProps) {
  return (
    <article className={`panel recent-panel${refreshing ? ' is-refreshing' : ''}`} data-reveal="true">
      <div className="panel-title-row">
        <h2>최근 이벤트</h2>
        <label>
          조회 개수
          <input
            type="number"
            min={5}
            max={100}
            value={limit}
            onChange={(event) => onLimitChange(Number(event.target.value))}
          />
        </label>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Observed At</th>
              <th>Region</th>
              <th>Road</th>
              <th>Speed</th>
              <th>Congestion</th>
            </tr>
          </thead>
          <tbody key={refreshTick}>
            {events.length === 0 ? (
              <tr>
                <td colSpan={5} className="empty">
                  최근 이벤트가 없습니다.
                </td>
              </tr>
            ) : (
              events.map((event, index) => (
                <tr key={event.eventId} className="row-reveal" style={{ animationDelay: `${index * 35}ms` }}>
                  <td>{formatDate(event.observedAt)}</td>
                  <td>{event.region}</td>
                  <td>{event.roadName}</td>
                  <td>{event.averageSpeedKph}</td>
                  <td>
                    <span className={`congestion level-${Number(event.congestionLevel ?? 1)}`}>
                      {event.congestionLevel}
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
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
