import { useEffect, useRef, useState } from 'react'
import type { SessionSnapshotResponse, TrafficSummaryResponse } from '../types'

interface KpiPanelProps {
  summary: TrafficSummaryResponse | null
  generatedAt: string | null
  session: SessionSnapshotResponse
  refreshing: boolean
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

function useAnimatedNumber(target: number, durationMs = 420): number {
  const [value, setValue] = useState(target)
  const currentRef = useRef(target)

  useEffect(() => {
    const from = currentRef.current
    const to = target

    if (Math.abs(to - from) < 0.0001) {
      setValue(to)
      currentRef.current = to
      return
    }

    const startedAt = performance.now()
    let rafId = 0

    const tick = (now: number) => {
      const progress = Math.min(1, (now - startedAt) / durationMs)
      const eased = 1 - (1 - progress) ** 3
      const nextValue = from + (to - from) * eased
      setValue(nextValue)
      currentRef.current = nextValue

      if (progress < 1) {
        rafId = window.requestAnimationFrame(tick)
      } else {
        setValue(to)
        currentRef.current = to
      }
    }

    rafId = window.requestAnimationFrame(tick)
    return () => window.cancelAnimationFrame(rafId)
  }, [durationMs, target])

  return value
}

export default function KpiPanel({ summary, generatedAt, session, refreshing }: KpiPanelProps) {
  const regions = summary?.regions ?? []
  const totalEvents = summary?.totalEvents ?? 0
  const averageSpeed = regions.length > 0
    ? regions.reduce((acc, region) => acc + Number(region.averageSpeedKph ?? 0), 0) / regions.length
    : null

  const maxCongestion = regions.reduce(
    (acc, region) => Math.max(acc, Number(region.latestCongestionLevel ?? 0)),
    0,
  )

  const animatedTotalEvents = useAnimatedNumber(totalEvents)
  const animatedAverageSpeed = useAnimatedNumber(averageSpeed ?? 0)
  const animatedMaxCongestion = useAnimatedNumber(maxCongestion)

  return (
    <article className={`panel kpi-panel${refreshing ? ' is-refreshing' : ''}`} data-reveal="true">
      <div className="panel-title-row">
        <h2>운영 KPI</h2>
        <p>{formatDate(generatedAt)}</p>
      </div>
      <div className="kpi-grid">
        <div className="kpi-card">
          <p className="kpi-label">총 이벤트 수</p>
          <p className="kpi-value">{Math.round(animatedTotalEvents)}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">평균 속도(km/h)</p>
          <p className="kpi-value">{averageSpeed == null ? '-' : animatedAverageSpeed.toFixed(1)}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">최고 혼잡도</p>
          <p className="kpi-value">{maxCongestion === 0 ? '-' : Math.round(animatedMaxCongestion)}</p>
        </div>
        <div className="kpi-card">
          <p className="kpi-label">로그인 사용자</p>
          <p className="kpi-value">{session.authenticated ? (session.username ?? '운영자') : '익명 모드'}</p>
        </div>
      </div>
    </article>
  )
}
