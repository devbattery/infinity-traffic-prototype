import { FormEvent, useState } from 'react'
import type { LoginRequest, RegisterRequest, SessionSnapshotResponse } from '../types'

interface AuthPanelProps {
  session: SessionSnapshotResponse
  onRegister: (request: RegisterRequest) => Promise<void>
  onLogin: (request: LoginRequest) => Promise<void>
  onLogout: () => Promise<void>
  busy: boolean
}

export default function AuthPanel({ session, onRegister, onLogin, onLogout, busy }: AuthPanelProps) {
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
    await onRegister(registerForm)
    setRegisterForm((previous) => ({ ...previous, password: '' }))
  }

  const submitLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await onLogin(loginForm)
    setLoginForm((previous) => ({ ...previous, password: '' }))
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
        <>
          <form className="stack-form" onSubmit={(event) => void submitRegister(event)}>
            <h3>회원가입</h3>
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
            <button className="btn" type="submit" disabled={busy}>
              회원가입
            </button>
          </form>

          <form className="stack-form" onSubmit={(event) => void submitLogin(event)}>
            <h3>로그인</h3>
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
        </>
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
