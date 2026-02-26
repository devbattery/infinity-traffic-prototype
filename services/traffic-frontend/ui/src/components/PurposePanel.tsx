import type { PlatformOverviewResponse } from '../types'

interface PurposePanelProps {
  overview: PlatformOverviewResponse | null
}

export default function PurposePanel({ overview }: PurposePanelProps) {
  if (!overview) {
    return (
      <article className="panel purpose-panel" data-reveal="true">
        <h2>이 사이트는 무엇을 하나요?</h2>
        <p className="muted">서비스 설명을 불러오는 중입니다.</p>
      </article>
    )
  }

  return (
    <article className="panel purpose-panel" data-reveal="true">
      <div className="panel-title-row">
        <h2>이 사이트는 무엇을 하나요?</h2>
      </div>

      <p className="purpose-lead">
        <strong>{overview.productName}</strong>는 {overview.corePurpose}입니다.
      </p>
      <p className="muted">주요 사용자: {overview.primaryUsers}</p>

      <div className="purpose-capability-list">
        {overview.keyCapabilities.map((capability) => (
          <p key={capability} className="purpose-capability-item">
            {capability}
          </p>
        ))}
      </div>

      <div className="purpose-flow-list">
        {overview.valueFlow.map((step) => (
          <article key={step.order} className="purpose-flow-item">
            <p className="purpose-flow-order">STEP {step.order}</p>
            <h3>{step.title}</h3>
            <p className="muted">{step.description}</p>
          </article>
        ))}
      </div>
    </article>
  )
}
