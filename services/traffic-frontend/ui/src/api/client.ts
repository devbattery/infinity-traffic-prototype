import type {
  DashboardSnapshotResponse,
  FrontendMessageResponse,
  LoginRequest,
  LoginSuccessResponse,
  MonitoringSnapshotResponse,
  PlatformOverviewResponse,
  RegisterRequest,
  SessionSnapshotResponse,
  TrafficEventIngestRequest,
  TrafficEventIngestSuccessResponse,
} from '../types'

interface ApiErrorPayload {
  message?: string
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    let message = `요청 처리에 실패했습니다. status=${response.status}`
    try {
      const payload = (await response.json()) as ApiErrorPayload
      if (payload.message) {
        message = payload.message
      }
    } catch {
      const text = await response.text()
      if (text.trim().length > 0) {
        message = text
      }
    }
    throw new Error(message)
  }

  return (await response.json()) as T
}

export function getDashboard(region: string, limit: number): Promise<DashboardSnapshotResponse> {
  const params = new URLSearchParams()
  if (region !== 'ALL') {
    params.set('region', region)
  }
  params.set('limit', String(limit))

  return requestJson<DashboardSnapshotResponse>(`/ui/api/dashboard?${params.toString()}`)
}

export function getSession(): Promise<SessionSnapshotResponse> {
  return requestJson<SessionSnapshotResponse>('/ui/api/session')
}

export function getPlatformOverview(): Promise<PlatformOverviewResponse> {
  return requestJson<PlatformOverviewResponse>('/ui/api/platform/overview')
}

export function getMonitoringSnapshot(): Promise<MonitoringSnapshotResponse> {
  return requestJson<MonitoringSnapshotResponse>('/ui/api/monitoring/snapshot')
}

export function register(request: RegisterRequest): Promise<FrontendMessageResponse> {
  return requestJson<FrontendMessageResponse>('/ui/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function login(request: LoginRequest): Promise<LoginSuccessResponse> {
  return requestJson<LoginSuccessResponse>('/ui/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export function logout(): Promise<FrontendMessageResponse> {
  return requestJson<FrontendMessageResponse>('/ui/api/auth/logout', {
    method: 'POST',
  })
}

export function ingestTrafficEvent(request: TrafficEventIngestRequest): Promise<TrafficEventIngestSuccessResponse> {
  return requestJson<TrafficEventIngestSuccessResponse>('/ui/api/traffic/events', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
