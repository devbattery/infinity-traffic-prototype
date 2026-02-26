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
          React + Spring Boot + Kafka 기반으로 실시간 도로 혼잡도를 모니터링하고,
          이벤트를 즉시 수집/조회합니다.
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
