/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        gpt: {
          // Theme-aware colors via CSS custom properties
          // Toggle between dark/light by adding .light to <html>
          bg:      'rgb(var(--color-bg) / <alpha-value>)',
          surface: 'rgb(var(--color-surface) / <alpha-value>)',
          border:  'rgb(var(--color-border) / <alpha-value>)',
          muted:   'rgb(var(--color-muted) / <alpha-value>)',
          accent:  'rgb(var(--color-accent) / <alpha-value>)',
          green:   'rgb(var(--color-green) / <alpha-value>)',
          text:    'rgb(var(--color-text) / <alpha-value>)',
          dim:     'rgb(var(--color-dim) / <alpha-value>)',
          red:     'rgb(var(--color-red) / <alpha-value>)',
          amber:   'rgb(var(--color-amber) / <alpha-value>)',
          purple:  'rgb(var(--color-purple) / <alpha-value>)',
        },
      },
      fontFamily: {
        sans: ['Inter', 'SF Pro Display', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'Consolas', 'monospace'],
      },
      borderRadius: {
        xl: '12px',
        '2xl': '16px',
        '3xl': '24px',
      },
      boxShadow: {
        glass: '0 8px 32px rgba(0,0,0,0.12)',
        'glass-sm': '0 4px 16px rgba(0,0,0,0.08)',
        glow: '0 0 20px rgba(59,130,246,0.15)',
        'glow-green': '0 0 20px rgba(16,185,129,0.15)',
      },
      animation: {
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.4s ease-out',
        'slide-down': 'slideDown 0.3s ease-out',
        'pulse-dot': 'pulseDot 2s ease-in-out infinite',
        'typewriter': 'typewriter 0.05s steps(1)',
        'spin-slow': 'spin 3s linear infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideDown: {
          '0%': { opacity: '0', transform: 'translateY(-10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        pulseDot: {
          '0%, 100%': { opacity: '0.4' },
          '50%': { opacity: '1' },
        },
        typewriter: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}
