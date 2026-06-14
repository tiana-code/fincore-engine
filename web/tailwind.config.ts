import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)',
        'bg-2': 'var(--bg-2)',
        surface: 'var(--surface)',
        'surface-2': 'var(--surface-2)',
        'surface-3': 'var(--surface-3)',
        border: 'var(--border)',
        'border-strong': 'var(--border-strong)',
        text: 'var(--text)',
        'text-2': 'var(--text-2)',
        'text-3': 'var(--text-3)',
        'text-mute': 'var(--text-mute)',
        accent: 'var(--accent)',
        credit: 'var(--credit)',
        debit: 'var(--debit)',
        amber: 'var(--amber)',
        violet: 'var(--violet)',
      },
      fontFamily: {
        sans: 'var(--sans)',
        mono: 'var(--mono)',
      },
    },
  },
  plugins: [],
} satisfies Config
