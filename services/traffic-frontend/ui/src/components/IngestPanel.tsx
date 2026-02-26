import { FormEvent, useState } from 'react'
import type { RegionCode, TrafficEventIngestRequest } from '../types'
import {
  DEFAULT_REGION_CODE,
  formatRegionLabel,
  mergeRegionCodes,
  REGION_DEFINITIONS,
} from '../constants/regions'

interface IngestPanelProps {
  onSubmit: (request: TrafficEventIngestRequest) => Promise<void>
  busy: boolean
  knownRegions: string[]
}

interface IngestFormState {
  region: RegionCode
  roadName: string
  averageSpeedKph: number
  congestionLevel: number
  observedAt: string
}

const DEFAULT_STATE: IngestFormState = {
  region: DEFAULT_REGION_CODE,
  roadName: '',
  averageSpeedKph: 40,
  congestionLevel: 3,
  observedAt: '',
}

interface IngestPreset {
  label: string
  averageSpeedKph: number
  congestionLevel: number
  description: string
}

const INGEST_PRESETS: IngestPreset[] = [
  { label: '원활', averageSpeedKph: 70, congestionLevel: 1, description: '정체 거의 없음' },
  { label: '보통', averageSpeedKph: 45, congestionLevel: 3, description: '평시 수준' },
  { label: '혼잡', averageSpeedKph: 20, congestionLevel: 5, description: '출퇴근/사고 구간' },
]

export default function IngestPanel({ onSubmit, busy, knownRegions }: IngestPanelProps) {
  const [form, setForm] = useState<IngestFormState>(DEFAULT_STATE)
  const regionCodes = mergeRegionCodes(REGION_DEFINITIONS.map((region) => region.code), knownRegions)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const observedDate = form.observedAt.trim().length > 0 ? new Date(form.observedAt) : null
    const observedAt =
      observedDate && !Number.isNaN(observedDate.getTime())
        ? observedDate.toISOString()
        : null
    await onSubmit({
      ...form,
      roadName: form.roadName,
      observedAt,
    })
    setForm((previous) => ({ ...previous, roadName: '', observedAt: '' }))
  }

  return (
    <article className="panel ingest-panel" data-reveal="true">
      <h2>이벤트 수집</h2>

      <section className="ingest-guide">
        <p className="ingest-guide-title">입력 가이드</p>
        <div className="ingest-guide-list">
          <p>1) 지역/도로를 선택한 뒤</p>
          <p>2) 평균 속도와 혼잡도를 입력하고</p>
          <p>3) 필요하면 관측 시각을 지정하세요(비우면 서버 현재 시각 적용).</p>
        </div>
      </section>

      <section className="preset-grid" aria-label="혼잡도 프리셋">
        {INGEST_PRESETS.map((preset) => (
          <button
            key={preset.label}
            className="preset-btn"
            type="button"
            onClick={() =>
              setForm((previous) => ({
                ...previous,
                averageSpeedKph: preset.averageSpeedKph,
                congestionLevel: preset.congestionLevel,
              }))
            }
          >
            <span>{preset.label}</span>
            <span className="preset-caption">
              {preset.averageSpeedKph}km/h · 혼잡도 {preset.congestionLevel} ({preset.description})
            </span>
          </button>
        ))}
      </section>

      <form className="stack-form" onSubmit={(event) => void submit(event)}>
        <label>
          지역
          <select
            value={form.region}
            onChange={(event) => setForm((previous) => ({ ...previous, region: event.target.value as IngestFormState['region'] }))}
          >
            {regionCodes.map((regionCode) => (
              <option key={regionCode} value={regionCode}>
                {formatRegionLabel(regionCode)}
              </option>
            ))}
          </select>
        </label>
        <label>
          도로명
          <input
            type="text"
            value={form.roadName}
            placeholder="예: 강변북로, 올림픽대로"
            list="road-name-samples"
            onChange={(event) => setForm((previous) => ({ ...previous, roadName: event.target.value }))}
            required
          />
        </label>
        <datalist id="road-name-samples">
          <option value="강변북로" />
          <option value="올림픽대로" />
          <option value="경부고속도로" />
          <option value="동부간선도로" />
          <option value="자유로" />
          <option value="수영로" />
          <option value="동서대로" />
          <option value="번영로" />
        </datalist>
        <p className="input-hint">도로명은 실제 도로 기준으로 입력하세요. 예: 강변북로, 경부고속도로, 수영로</p>
        <label>
          평균 속도 (km/h)
          <input
            type="number"
            min={0}
            max={200}
            value={form.averageSpeedKph}
            onChange={(event) => setForm((previous) => ({ ...previous, averageSpeedKph: Number(event.target.value) }))}
            required
          />
        </label>
        <label>
          혼잡도 (1~5)
          <input
            type="number"
            min={1}
            max={5}
            value={form.congestionLevel}
            onChange={(event) => setForm((previous) => ({ ...previous, congestionLevel: Number(event.target.value) }))}
            required
          />
        </label>
        <label>
          관측 시각 (선택)
          <input
            type="datetime-local"
            value={form.observedAt}
            onChange={(event) => setForm((previous) => ({ ...previous, observedAt: event.target.value }))}
          />
        </label>
        <button className="btn" type="submit" disabled={busy}>
          이벤트 적재
        </button>
      </form>
    </article>
  )
}
