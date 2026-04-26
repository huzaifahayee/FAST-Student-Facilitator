import React from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css';

/**
 * Login Page
 * 
 * What is this?
 * This is the landing gate for FSF. 
 * As per NFR 4.1.2, only @nu.edu.pk accounts will be allowed.
 */
const Login = ({ onLogin }) => {
  const navigate = useNavigate();

  const handleLogin = (role) => {
    const mockUser = {
      name: role === 'ADMIN' ? 'Admin User' : 'M. Huzaifa',
      email: role === 'ADMIN' ? 'admin@nu.edu.pk' : 'l224151@lhr.nu.edu.pk',
      role: role
    };
    onLogin(mockUser);
    navigate('/');
  };

  return (
    <div className="login-page">
      <div className="login-card glass-card">
        <div className="login-header">
          <div className="logo-badge">F</div>
          <h1>FAST Student Facilitator</h1>
          <p>The centralized hub for FAST-NUCES Lahore</p>
        </div>

        <div className="login-body">
          <p className="login-hint">Simulate a session to explore the portal features</p>
          
          <div className="login-actions">
            <button className="primary-btn login-btn" onClick={() => handleLogin('STUDENT')}>
              Sign in as Student
            </button>
            <button className="secondary-btn login-btn" onClick={() => handleLogin('ADMIN')}>
              Sign in as Portal Admin
            </button>
          </div>

          <p className="login-note">
            In production, this will use Google OAuth2 exclusively @nu.edu.pk.
          </p>
        </div>

        <div className="login-footer">
          <p>© 2026 Team 2 - BSSE 4B</p>
        </div>
      </div>
    </div>
  );
};

export default Login;
