import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import Dashboard from './components/Dashboard';
import Login from './pages/Login';
import Carpool from './pages/Carpool';
import AdminPanel from './pages/AdminPanel';
import Stats from './pages/Stats';
import LostAndFound from './pages/LostAndFound';
import FastNotes from './pages/FastNotes';
import ServiceSkeleton from './components/ServiceSkeleton';
import PastPapers from './pages/PastPapers';
import TimetableManager from './pages/TimetableManager';
import BookExchange from './pages/BookExchange';
import './App.css';

/**
 * App.jsx
 * 
 * We now use 'react-router-dom' to handle navigation between features.
 */
function App() {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('fsf-user');
    try {
      return savedUser ? JSON.parse(savedUser) : null;
    } catch (e) {
      return null;
    }
  }); 
  const [theme, setTheme] = useState(localStorage.getItem('fsf-theme') || 'dark');

  React.useEffect(() => {
    if (user) {
      localStorage.setItem('fsf-user', JSON.stringify(user));
    } else {
      localStorage.removeItem('fsf-user');
    }
  }, [user]);

  React.useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('fsf-theme', theme);
  }, [theme]);

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  const ProtectedRoute = ({ children }) => {
    if (!user) return <Navigate to="/login" />;
    return children;
  };

  const AdminRoute = ({ children }) => {
    if (!user || user.role !== 'ADMIN') return <Navigate to="/" />;
    return children;
  };

  return (
    <Router>
      <div className="app-shell">
        <Routes>
          {/* Login Page: Outside of the main shell */}
          <Route path="/login" element={<Login onLogin={(userData) => setUser(userData)} />} />

          {/* Main Shell: Only accessible if authenticated */}
          <Route path="/*" element={
            <ProtectedRoute>
              <div className="app-shell">
                <Sidebar user={user} />
                <main className="main-viewport">
                  <Topbar theme={theme} toggleTheme={toggleTheme} user={user} />
                  <div className="content-area">
                    <Routes>
                      <Route path="/" element={<Dashboard user={user} />} />
                      <Route path="/carpool" element={<Carpool user={user} />} />
                      <Route path="/admin" element={<AdminRoute><AdminPanel /></AdminRoute>} />
                      <Route path="/stats" element={<AdminRoute><Stats /></AdminRoute>} />
                      
                      {/* Placeholders for the other 8 features */}
                      <Route path="/lost-found" element={<LostAndFound user={user} />} />
                      <Route path="/past-papers" element={<PastPapers user={user} />} />
                      <Route path="/events" element={<ServiceSkeleton featureName="Campus Events" />} />
                      <Route path="/reminders" element={<ServiceSkeleton featureName="Pop Reminders" />} />
                      <Route path="/map" element={<ServiceSkeleton featureName="Campus Map" />} />
                      <Route path="/timetable" element={<TimetableManager user={user} />} />
                      <Route path="/marketplace" element={<BookExchange user={user} />} />
                      <Route path="/notes" element={<FastNotes user={user} />} />
                    </Routes>
                  </div>
                </main>
              </div>
            </ProtectedRoute>
          } />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
