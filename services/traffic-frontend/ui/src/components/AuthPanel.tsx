import { FormEvent, useEffect, useState } from 'react'
import type { LoginRequest, RegisterRequest, SessionSnapshotResponse } from '../types'

interface AuthPanelProps {
  session: SessionSnapshotResponse
  onRegister: (request: RegisterRequest) => Promise<void>
  onLogin: (request: LoginRequest) => Promise<void>
  onLogout: () => Promise<void>
  busy: boolean
}

type AuthMode = 'login' | 'register'

export default function AuthPanel({ session, onRegister, onLogin, onLogout, busy }: AuthPanelProps) {
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false)
  const [registerForm, setRegisterForm] = useState<RegisterRequest>({
    username: '',
    password: '',
  })
  const [loginForm, setLoginForm] = useState<LoginRequest>({
    username: '',
    password: '',
  })

  const submitRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      await onRegister(registerForm)
      setRegisterForm((previous) => ({ ...previous, password: '' }))
      setAuthMode('login')
    } catch {
      // 오류 메시지는 상위(App)에서 플래시로 표시한다.
    }
  }

  const submitLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      await onLogin(loginForm)
      setLoginForm((previous) => ({ ...previous, password: '' }))
      setIsAuthModalOpen(false)
    } catch {
      // 오류 메시지는 상위(App)에서 플래시로 표시한다.
    }
  }

  useEffect(() => {
    if (!isAuthModalOpen) {
      return
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) {
        setIsAuthModalOpen(false)
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [busy, isAuthModalOpen])

  const openAuthModal = (mode: AuthMode) => {
    setAuthMode(mode)
    setIsAuthModalOpen(true)
  }

  const closeAuthModal = () => {
    if (!busy) {
      setIsAuthModalOpen(false)
    }
  }

  return (
    <article className="panel auth-panel" data-reveal="true">
      <h2>인증 센터</h2>

      {session.authenticated ? (
        <div className="auth-state">
          <p>
            <strong>{session.username ?? '운영자'}</strong> 님이 로그인 중입니다.
          </p>
          <p className="muted">토큰 만료: {formatDate(session.expiresAt)}</p>
          <button className="btn secondary" type="button" onClick={() => void onLogout()} disabled={busy}>
            로그아웃
          </button>
        </div>
      ) : (
        <div className="auth-entry">
          <p className="muted">로그인/회원가입은 분리된 인증 창에서 진행합니다.</p>
          <div className="auth-entry-actions">
            <button className="btn" type="button" disabled={busy} onClick={() => openAuthModal('login')}>
              로그인 열기
            </button>
            <button className="btn secondary" type="button" disabled={busy} onClick={() => openAuthModal('register')}>
              회원가입 열기
            </button>
          </div>

          {isAuthModalOpen && (
            <div className="auth-modal-backdrop" role="presentation" onClick={closeAuthModal}>
              <section
                className="auth-modal"
                role="dialog"
                aria-modal="true"
                aria-label="인증 창"
                onClick={(event) => event.stopPropagation()}
              >
                <div className="auth-modal-header">
                  <h3>계정 인증</h3>
                  <button className="icon-btn" type="button" onClick={closeAuthModal} disabled={busy} aria-label="인증 창 닫기">
                    닫기
                  </button>
                </div>

                <div className="auth-tabs" role="tablist" aria-label="인증 방식 선택">
                  <button
                    type="button"
                    className={`auth-tab ${authMode === 'login' ? 'active' : ''}`}
                    onClick={() => setAuthMode('login')}
                    role="tab"
                    aria-selected={authMode === 'login'}
                  >
                    로그인
                  </button>
                  <button
                    type="button"
                    className={`auth-tab ${authMode === 'register' ? 'active' : ''}`}
                    onClick={() => setAuthMode('register')}
                    role="tab"
                    aria-selected={authMode === 'register'}
                  >
                    회원가입
                  </button>
                </div>

                {authMode === 'login' ? (
                  <form className="stack-form auth-form" onSubmit={(event) => void submitLogin(event)}>
                    <label>
                      아이디
                      <input
                        type="text"
                        value={loginForm.username}
                        placeholder="traffic_operator"
                        onChange={(event) => setLoginForm((previous) => ({ ...previous, username: event.target.value }))}
                        required
                      />
                    </label>
                    <label>
                      비밀번호
                      <input
                        type="password"
                        value={loginForm.password}
                        placeholder="비밀번호"
                        onChange={(event) => setLoginForm((previous) => ({ ...previous, password: event.target.value }))}
                        required
                      />
                    </label>
                    <button className="btn" type="submit" disabled={busy}>
                      로그인
                    </button>
                  </form>
                ) : (
                  <form className="stack-form auth-form" onSubmit={(event) => void submitRegister(event)}>
                    <label>
                      아이디
                      <input
                        type="text"
                        value={registerForm.username}
                        placeholder="traffic_operator"
                        onChange={(event) => setRegisterForm((previous) => ({ ...previous, username: event.target.value }))}
                        required
                      />
                    </label>
                    <label>
                      비밀번호
                      <input
                        type="password"
                        value={registerForm.password}
                        placeholder="최소 8자 이상"
                        onChange={(event) => setRegisterForm((previous) => ({ ...previous, password: event.target.value }))}
                        minLength={8}
                        required
                      />
                    </label>
                    <button className="btn secondary" type="submit" disabled={busy}>
                      회원가입
                    </button>
                  </form>
                )}
              </section>
            </div>
          )}
        </div>
      )}
    </article>
  )
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
