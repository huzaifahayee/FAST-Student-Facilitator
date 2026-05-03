import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGoogleLogin } from '@react-oauth/google';
import './Login.css';

/**
 * Login Page — Google OAuth 2.0
 *
 * Uses useGoogleLogin (implicit flow) which opens a real Google popup.
 * Access token is sent to the backend which verifies it with Google and
 * handles domain + role enforcement.
 */
const Login = ({ onLogin }) => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(null); // 'student' | 'admin' | null
  const [popup,   setPopup]   = useState(null); // { type, message }

  // ─── Send access_token + loginAs to our backend ──────────────────────────
  const handleToken = async (accessToken, loginAs) => {
    try {
      const res = await fetch('http://localhost:8080/api/auth/google', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ credential: accessToken, loginAs }),
      });

      const data = await res.json();

      if (res.ok) {
        onLogin(data);
        navigate('/');
      } else if (data.error === 'NOT_NUCES') {
        setPopup({ type: 'nuces',  message: data.message });
      } else if (data.error === 'NOT_ADMIN') {
        setPopup({ type: 'admin',  message: data.message });
      } else if (data.error === 'BANNED') {
        setPopup({ type: 'banned', message: data.message });
      } else {
        setPopup({ type: 'error',  message: data.message || 'Authentication failed.' });
      }
    } catch {
      setPopup({ type: 'error', message: 'Cannot reach server. Is the backend running on port 8080?' });
    } finally {
      setLoading(null);
    }
  };

  // ─── Google OAuth hooks ────────────────────────────────────────────────
  const loginAsStudent = useGoogleLogin({
    onSuccess: (resp) => handleToken(resp.access_token, 'STUDENT'),
    onError:   () => { setLoading(null); setPopup({ type: 'error', message: 'Google sign-in failed or was cancelled.' }); },
    flow: 'implicit',
  });

  const loginAsAdmin = useGoogleLogin({
    onSuccess: (resp) => handleToken(resp.access_token, 'ADMIN'),
    onError:   () => { setLoading(null); setPopup({ type: 'error', message: 'Google sign-in failed or was cancelled.' }); },
    flow: 'implicit',
  });

  // ─── Popup config ──────────────────────────────────────────────────────
  const POPUP_META = {
    nuces:  { icon: '🏫', title: 'Access Restricted',   color: 'var(--accent-fill)' },
    admin:  { icon: '🔒', title: 'Admin Access Denied',  color: '#f59e0b' },
    banned: { icon: '🚫', title: 'Account Banned',       color: '#ef4444' },
    error:  { icon: '⚠️', title: 'Something went wrong', color: '#ef4444' },
  };
  const meta = popup ? POPUP_META[popup.type] : null;

  return (
    <div className="login-page">

      {/* ── Error / Info Popup ─────────────────────────────────── */}
      {popup && (
        <div className="login-overlay" onClick={() => setPopup(null)}>
          <div className="login-popup glass-card" onClick={(e) => e.stopPropagation()}>
            <div className="popup-icon">{meta.icon}</div>
            <h2 className="popup-title" style={{ color: meta.color }}>{meta.title}</h2>
            <p className="popup-msg">{popup.message}</p>
            {popup.type === 'nuces' && (
              <p className="popup-sub">
                This portal is exclusively for <strong>FAST-NUCES</strong> students
                with a <strong>.nu.edu.pk</strong> Google account
                (e.g. @lhr.nu.edu.pk, @khi.nu.edu.pk, @nu.edu.pk).
              </p>
            )}
            <button className="popup-close-btn primary-btn" onClick={() => setPopup(null)}>
              Got it
            </button>
          </div>
        </div>
      )}

      {/* ── Login Card ─────────────────────────────────────────── */}
      <div className="login-card glass-card">

        {/* Header */}
        <div className="login-header">
          <div className="logo-badge">F</div>
          <h1>FAST Student Facilitator</h1>
          <p>The centralized hub for FAST-NUCES</p>
        </div>

        {/* ── Student Section ──────────────────────────────────── */}
        <div className="login-body">
          <p className="login-section-label">Student Portal</p>
          <p className="login-hint">
            Sign in with your <strong>.nu.edu.pk</strong> Google account
          </p>

          <button
            id="btn-student-login"
            className="google-btn login-btn"
            onClick={() => { setLoading('student'); loginAsStudent(); }}
            disabled={loading !== null}
          >
            {loading === 'student' ? <span className="btn-spinner" /> : <GoogleIcon />}
            {loading === 'student' ? 'Signing in…' : 'Continue as Student'}
          </button>
        </div>

        {/* Divider */}
        <div className="login-divider"><span>or</span></div>

        {/* ── Admin Section ────────────────────────────────────── */}
        <div className="login-body">
          <p className="login-section-label admin-label">Portal Admin</p>
          <p className="login-hint">Restricted to registered FSF developers</p>

          <button
            id="btn-admin-login"
            className="google-btn google-btn--admin login-btn"
            onClick={() => { setLoading('admin'); loginAsAdmin(); }}
            disabled={loading !== null}
          >
            {loading === 'admin' ? <span className="btn-spinner" /> : <GoogleIcon />}
            {loading === 'admin' ? 'Verifying…' : 'Sign in as Portal Admin'}
          </button>
        </div>

        {/* Footer */}
        <div className="login-footer">
          <p>© 2026 Team 2 · BSSE 4B · FAST-NUCES Lahore</p>
        </div>
      </div>
    </div>
  );
};

/** Google "G" SVG icon */
const GoogleIcon = () => (
  <svg className="google-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
  </svg>
);

export default Login;
