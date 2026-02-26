import type { FlashMessage } from '../types'

interface FlashStackProps {
  messages: FlashMessage[]
  onDismiss: (id: string) => void
}

export default function FlashStack({ messages, onDismiss }: FlashStackProps) {
  if (messages.length === 0) {
    return <section className="flash-stack" aria-live="polite" />
  }

  return (
    <section className="flash-stack" aria-live="polite">
      {messages.map((message) => (
        <button
          key={message.id}
          type="button"
          className={`flash ${message.type}`}
          onClick={() => onDismiss(message.id)}
          title="메시지 닫기"
        >
          {message.text}
        </button>
      ))}
    </section>
  )
}
