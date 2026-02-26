import { useCallback, useEffect, useRef, useState } from 'react'
import AuthPanel from './components/AuthPanel'
import FlashStack from './components/FlashStack'
import HeroHeader from './components/HeroHeader'
import IngestPanel from './components/IngestPanel'
import KpiPanel from './components/KpiPanel'
import RecentPanel from './components/RecentPanel'
import SummaryPanel from './components/SummaryPanel'
import {
  getDashboard,
  getSession,
  ingestTrafficEvent,
  login,
  logout,
  register,
} from './api/client'
import type {
  FlashMessage,
  RegionCode,
  SessionSnapshotResponse,
  TrafficEventIngestRequest,
  TrafficEventMessage,
  TrafficSummaryResponse,
} from './types'

const REFRESH_INTERVAL_MS = 5_000

const EMPTY_SESSION: SessionSnapshotResponse = {
  authenticated: false,
  username: null,
  expiresAt: null,
}

function toErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return '요청 처리 중 알 수 없는 오류가 발생했습니다.'
}

function clampLimit(value: number): number {
  if (!Number.isFinite(value)) {
    return 20
  }
  return Math.max(5, Math.min(100, Math.round(value)))
}

export default function App() {
  const [summary, setSummary] = useState<TrafficSummaryResponse | null>(null)
  const [recentEvents, setRecentEvents] = useState<TrafficEventMessage[]>([])
  const [generatedAt, setGeneratedAt] = useState<string | null>(null)
  const [session, setSession] = useState<SessionSnapshotResponse>(EMPTY_SESSION)
  const [snapshotTick, setSnapshotTick] = useState(0)

  const [selectedRegion, setSelectedRegion] = useState<RegionCode>('ALL')
  const [limit, setLimit] = useState<number>(20)
  const [refreshing, setRefreshing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [flashMessages, setFlashMessages] = useState<FlashMessage[]>([])

  const refreshInFlightRef = useRef(false)

  const dismissFlash = useCallback((id: string) => {
    setFlashMessages((previous) => previous.filter((message) => message.id !== id))
  }, [])

  const pushFlash = useCallback((type: FlashMessage['type'], text: string) => {
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`
    setFlashMessages((previous) => [{ id, type, text }, ...previous].slice(0, 4))
    window.setTimeout(() => {
      setFlashMessages((previous) => previous.filter((message) => message.id !== id))
    }, 4_000)
  }, [])

  const refreshDashboard = useCallback(async () => {
    if (refreshInFlightRef.current) {
      return
    }

    refreshInFlightRef.current = true
    setRefreshing(true)

    try {
      const snapshot = await getDashboard(selectedRegion, clampLimit(limit))
      setSummary(snapshot.summary)
      setRecentEvents(snapshot.recentEvents)
      setGeneratedAt(snapshot.generatedAt)
      setSession({
        authenticated: snapshot.authenticated,
        username: snapshot.username,
        expiresAt: snapshot.tokenExpiresAt,
      })
      setSnapshotTick((previous) => previous + 1)
    } catch (error) {
      pushFlash('error', toErrorMessage(error))
    } finally {
      refreshInFlightRef.current = false
      setRefreshing(false)
    }
  }, [limit, pushFlash, selectedRegion])

  const runMutation = useCallback(
    async (action: () => Promise<void>) => {
      if (busy) {
        return
      }
      setBusy(true)
      try {
        await action()
      } finally {
        setBusy(false)
      }
    },
    [busy],
  )

  const handleRegister = useCallback(
    async (request: { username: string; password: string }) => {
      await runMutation(async () => {
        try {
          const response = await register({
            username: request.username.trim(),
            password: request.password,
          })
          pushFlash('success', response.message)
        } catch (error) {
          pushFlash('error', toErrorMessage(error))
          throw error
        }
      })
    },
    [pushFlash, runMutation],
  )

  const handleLogin = useCallback(
    async (request: { username: string; password: string }) => {
      await runMutation(async () => {
        try {
          const response = await login({
            username: request.username.trim(),
            password: request.password,
          })
          setSession({
            authenticated: true,
            username: response.username,
            expiresAt: response.expiresAt,
          })
          pushFlash('success', response.message)
          await refreshDashboard()
        } catch (error) {
          pushFlash('error', toErrorMessage(error))
          throw error
        }
      })
    },
    [pushFlash, refreshDashboard, runMutation],
  )

  const handleLogout = useCallback(async () => {
    await runMutation(async () => {
      try {
        const response = await logout()
        setSession(EMPTY_SESSION)
        pushFlash('success', response.message)
        await refreshDashboard()
      } catch (error) {
        pushFlash('error', toErrorMessage(error))
        throw error
      }
    })
  }, [pushFlash, refreshDashboard, runMutation])

  const handleIngest = useCallback(
    async (request: TrafficEventIngestRequest) => {
      await runMutation(async () => {
        try {
          const response = await ingestTrafficEvent({
            ...request,
            roadName: request.roadName.trim(),
          })
          pushFlash('success', `${response.message} eventId=${response.eventId}`)
          await refreshDashboard()
        } catch (error) {
          pushFlash('error', toErrorMessage(error))
          throw error
        }
      })
    },
    [pushFlash, refreshDashboard, runMutation],
  )

  useEffect(() => {
    void getSession()
      .then((snapshot) => {
        setSession(snapshot)
      })
      .catch(() => {
        setSession(EMPTY_SESSION)
      })
  }, [])

  useEffect(() => {
    void refreshDashboard()

    const timerId = window.setInterval(() => {
      void refreshDashboard()
    }, REFRESH_INTERVAL_MS)

    const onVisibilityChange = () => {
      if (!document.hidden) {
        void refreshDashboard()
      }
    }

    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      window.clearInterval(timerId)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [refreshDashboard])

  return (
    <>
      <div className="ambient" aria-hidden="true">
        <div className="blob blob-a" />
        <div className="blob blob-b" />
        <div className="blob blob-c" />
      </div>

      <main className="layout">
        <HeroHeader refreshing={refreshing} />
        <FlashStack messages={flashMessages} onDismiss={dismissFlash} />

        <section className="grid">
          <KpiPanel summary={summary} generatedAt={generatedAt} session={session} refreshing={refreshing} />
          <AuthPanel
            session={session}
            onRegister={handleRegister}
            onLogin={handleLogin}
            onLogout={handleLogout}
            busy={busy}
          />
          <IngestPanel onSubmit={handleIngest} busy={busy} />
          <SummaryPanel
            summary={summary}
            selectedRegion={selectedRegion}
            onRegionChange={setSelectedRegion}
            onRefresh={refreshDashboard}
            refreshing={refreshing}
            refreshTick={snapshotTick}
          />
          <RecentPanel
            events={recentEvents}
            limit={limit}
            onLimitChange={(nextLimit) => setLimit(clampLimit(nextLimit))}
            refreshTick={snapshotTick}
            refreshing={refreshing}
          />
        </section>
      </main>
    </>
  )
}
