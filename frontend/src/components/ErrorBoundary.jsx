import React from 'react';

/**
 * ErrorBoundary catches unhandled render errors and shows a visible message
 * instead of a blank white page.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info);
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{
          padding: 40,
          background: '#020617',
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          fontFamily: 'sans-serif',
        }}>
          <div style={{
            background: '#0f172a',
            border: '1px solid #ef4444',
            borderRadius: 12,
            padding: 32,
            maxWidth: 600,
            width: '100%',
          }}>
            <h1 style={{ color: '#ef4444', margin: '0 0 12px' }}>Something went wrong</h1>
            <p style={{ color: '#94a3b8', margin: '0 0 16px', fontSize: 14 }}>
              The application encountered an error. Check the browser console for details.
            </p>
            <pre style={{
              color: '#fbbf24',
              fontSize: 13,
              background: '#1e293b',
              padding: 16,
              borderRadius: 8,
              overflowX: 'auto',
              whiteSpace: 'pre-wrap',
            }}>
              {this.state.error.message}
            </pre>
            <button
              onClick={() => window.location.reload()}
              style={{
                marginTop: 20,
                padding: '10px 24px',
                background: '#3b82f6',
                color: '#fff',
                border: 'none',
                borderRadius: 8,
                cursor: 'pointer',
                fontSize: 14,
              }}
            >
              Reload Page
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
