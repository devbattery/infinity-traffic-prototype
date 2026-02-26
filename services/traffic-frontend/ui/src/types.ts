export type RegionCode = 'ALL' | 'SEOUL' | 'BUSAN' | 'INCHEON' | 'DAEJEON' | 'GWANGJU'

export interface RegionSummary {
  region: string
  totalEvents: number
  averageSpeedKph: number
  latestCongestionLevel: number
}

export interface TrafficSummaryResponse {
  generatedAt: string
  totalEvents: number
  regions: RegionSummary[]
}

export interface TrafficEventMessage {
  eventId: string
  traceId: string
  region: string
  roadName: string
  averageSpeedKph: number
  congestionLevel: number
  observedAt: string
}

export interface DashboardSnapshotResponse {
  generatedAt: string
  summary: TrafficSummaryResponse
  recentEvents: TrafficEventMessage[]
  authenticated: boolean
  username: string | null
  tokenExpiresAt: string | null
}

export interface PlatformValueStep {
  order: number
  title: string
  description: string
}

export interface PlatformOverviewResponse {
  productName: string
  corePurpose: string
  primaryUsers: string
  keyCapabilities: string[]
  valueFlow: PlatformValueStep[]
}

export interface MonitoringLink {
  name: string
  url: string
  description: string
}

export interface MonitoringTargetStatus {
  name: string
  healthUrl: string
  status: string
  responseTimeMs: number | null
  detail: string | null
  checkedAt: string
}

export interface MonitoringSnapshotResponse {
  generatedAt: string
  links: MonitoringLink[]
  targets: MonitoringTargetStatus[]
}

export interface SessionSnapshotResponse {
  authenticated: boolean
  username: string | null
  expiresAt: string | null
}

export interface FrontendMessageResponse {
  message: string
  timestamp: string
}

export interface LoginSuccessResponse {
  message: string
  username: string
  expiresAt: string
}

export interface TrafficEventIngestSuccessResponse {
  message: string
  eventId: string
  status: string
  observedAt: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface TrafficEventIngestRequest {
  region: Exclude<RegionCode, 'ALL'>
  roadName: string
  averageSpeedKph: number
  congestionLevel: number
  observedAt: string | null
}

export interface FlashMessage {
  id: string
  type: 'success' | 'error'
  text: string
}
