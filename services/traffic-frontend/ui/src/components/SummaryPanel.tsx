import type { RegionCode, TrafficSummaryResponse } from '../types'
import {
  ALL_REGION_CODE,
  formatRegionLabel,
  mergeRegionCodes,
  REGION_DEFINITIONS,
} from '../constants/regions'

interface SummaryPanelProps {
  summary: TrafficSummaryResponse | null
  selectedRegion: RegionCode
  onRegionChange: (region: RegionCode) => void
  onRefresh: () => Promise<void>
  refreshing: boolean
  refreshTick: number
}

export default function SummaryPanel({
  summary,
  selectedRegion,
  onRegionChange,
  onRefresh,
  refreshing,
  refreshTick,
}: SummaryPanelProps) {
  const rows = summary?.regions ?? []
  const filterRegionCodes: RegionCode[] = [
    ALL_REGION_CODE,
    ...mergeRegionCodes(
      REGION_DEFINITIONS.map((region) => region.code),
      rows.map((row) => row.region),
    ),
  ]

  return (
    <article className={`panel summary-panel${refreshing ? ' is-refreshing' : ''}`} data-reveal="true">
      <div className="panel-title-row">
        <h2>지역별 요약</h2>
        <div className="inline-controls">
          <label>
            지역 필터
            <select value={selectedRegion} onChange={(event) => onRegionChange(event.target.value as RegionCode)}>
              {filterRegionCodes.map((region) => (
                <option key={region} value={region}>
                  {formatRegionLabel(region)}
                </option>
              ))}
            </select>
          </label>
          <button className="btn secondary" type="button" onClick={() => void onRefresh()} disabled={refreshing}>
            {refreshing ? '갱신 중...' : '즉시 새로고침'}
          </button>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Region</th>
              <th>Total Events</th>
              <th>Avg Speed</th>
              <th>Congestion</th>
            </tr>
          </thead>
          <tbody key={refreshTick}>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={4} className="empty">
                  표시할 데이터가 없습니다.
                </td>
              </tr>
            ) : (
              rows.map((row, index) => (
                <tr key={row.region} className="row-reveal" style={{ animationDelay: `${index * 45}ms` }}>
                  <td>{formatRegionLabel(row.region)}</td>
                  <td>{row.totalEvents}</td>
                  <td>{Number(row.averageSpeedKph ?? 0).toFixed(1)}</td>
                  <td>
                    <span className={`congestion level-${Number(row.latestCongestionLevel ?? 1)}`}>
                      {row.latestCongestionLevel}
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
