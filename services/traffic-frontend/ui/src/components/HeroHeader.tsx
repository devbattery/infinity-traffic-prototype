interface HeroHeaderProps {
  refreshing: boolean
}

export default function HeroHeader({ refreshing }: HeroHeaderProps) {
  return (
    <header className="hero" data-reveal="true">
      <div>
        <p className="eyebrow">REAL-TIME TRAFFIC MSA</p>
        <h1>Infinity Traffic Control Center</h1>
        <p className="hero-desc">
          이 대시보드는 교통 이벤트를 실시간으로 수집하고, 지역별 혼잡 현황을 집계해 운영 의사결정을 지원합니다.
          입력된 이벤트는 Kafka 파이프라인을 거쳐 즉시 요약/최근 이벤트 화면에 반영됩니다.
        </p>
      </div>
      <div className="hero-meta">
        <div className="meta-chip status-chip">
          <span className="meta-label">Snapshot</span>
          <strong className="live-status">
            <span className={`live-dot ${refreshing ? 'is-refreshing' : ''}`} />
            {refreshing ? 'SYNCING' : 'LIVE'}
          </strong>
        </div>
        <div className="meta-chip">
          <span className="meta-label">Gateway</span>
          <strong>api-gateway :8080</strong>
        </div>
        <div className="meta-chip">
          <span className="meta-label">Frontend</span>
          <strong>traffic-frontend :8084</strong>
        </div>
      </div>
    </header>
  )
}
