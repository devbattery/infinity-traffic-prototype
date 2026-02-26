import { FormEvent, useState } from 'react'
import type { RegionCode, TrafficEventIngestRequest } from '../types'

interface IngestPanelProps {
  onSubmit: (request: TrafficEventIngestRequest) => Promise<void>
  busy: boolean
}

interface IngestFormState {
  region: Exclude<RegionCode, 'ALL'>
  roadName: string
  averageSpeedKph: number
  congestionLevel: number
}

const DEFAULT_STATE: IngestFormState = {
  region: 'SEOUL',
  roadName: '',
  averageSpeedKph: 40,
  congestionLevel: 3,
}

export default function IngestPanel({ onSubmit, busy }: IngestPanelProps) {
  const [form, setForm] = useState<IngestFormState>(DEFAULT_STATE)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await onSubmit({
      ...form,
      roadName: form.roadName,
      observedAt: null,
    })
    setForm((previous) => ({ ...previous, roadName: '' }))
  }

  return (
    <article className="panel ingest-panel" data-reveal="true">
      <h2>이벤트 수집</h2>
      <form className="stack-form" onSubmit={(event) => void submit(event)}>
        <label>
          지역
          <select
            value={form.region}
            onChange={(event) => setForm((previous) => ({ ...previous, region: event.target.value as IngestFormState['region'] }))}
          >
            <option value="SEOUL">서울 (SEOUL)</option>
            <option value="BUSAN">부산 (BUSAN)</option>
            <option value="INCHEON">인천 (INCHEON)</option>
            <option value="DAEJEON">대전 (DAEJEON)</option>
            <option value="GWANGJU">광주 (GWANGJU)</option>
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
        </datalist>
        <p className="input-hint">한글 도로명 입력을 지원합니다. 예: 강변북로, 올림픽대로</p>
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
        <button className="btn" type="submit" disabled={busy}>
          이벤트 적재
        </button>
      </form>
    </article>
  )
}
