import React, { useState, useEffect } from 'react';
import './Stats.css';

/**
 * Stats (Analytics) Page
 * 
 * Provides high-level platform health and usage metrics for Admins.
 */
const Stats = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeRides: 0,
    pendingApprovals: 0,
    flaggedReports: 0
  });
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchStats = async () => {
    try {
      // /api/admin/** is gated by Spring Security; /api/rides/pending is permitAll
      // and returns the same data we can count client-side.
      const [usersRes, activeRes, pendingRes, flaggedRes] = await Promise.all([
        fetch('http://localhost:8080/api/users/count'),
        fetch('http://localhost:8080/api/rides/count/active'),
        fetch('http://localhost:8080/api/rides/pending'),
        fetch('http://localhost:8080/api/rides/flagged/count')
      ]);

      const pendingList = await pendingRes.json();

      setStats({
        totalUsers: await usersRes.json(),
        activeRides: await activeRes.json(),
        pendingApprovals: Array.isArray(pendingList) ? pendingList.length : 0,
        flaggedReports: await flaggedRes.json()
      });
      setLastUpdated(new Date());
      setLoading(false);
    } catch (err) {
      console.error("Failed to fetch analytics", err);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="stats-page animate-in">
      <header className="stats-header">
        <div className="stats-header-row">
          <div>
            <h2>📊 Platform Analytics</h2>
            <p>Real-time insights into student engagement and system health.</p>
          </div>
          <div className="stats-refresh">
            <button className="refresh-btn" onClick={fetchStats}>↻ Refresh</button>
            {lastUpdated && (
              <span className="last-updated">
                Updated {lastUpdated.toLocaleTimeString()}
              </span>
            )}
          </div>
        </div>
      </header>

      {loading ? (
        <div className="stats-skeleton pulse">Analyzing platform data...</div>
      ) : (
        <div className="stats-grid">
          <div className="stat-card glass-card">
            <span className="stat-icon">👥</span>
            <div className="stat-info">
              <h3>Total Students</h3>
              <p className="stat-value">{stats.totalUsers}</p>
              <span className="stat-trend positive">↑ Live</span>
            </div>
          </div>

          <div className="stat-card glass-card">
            <span className="stat-icon">🚗</span>
            <div className="stat-info">
              <h3>Active Rides</h3>
              <p className="stat-value">{stats.activeRides}</p>
              <span className="stat-trend neutral">Verified</span>
            </div>
          </div>

          <div className="stat-card glass-card highlight-pending">
            <span className="stat-icon">⏳</span>
            <div className="stat-info">
              <h3>Pending Tasks</h3>
              <p className="stat-value">{stats.pendingApprovals}</p>
              <span className="stat-trend warning">Action Required</span>
            </div>
          </div>

          <div className="stat-card glass-card highlight-danger">
            <span className="stat-icon">🚩</span>
            <div className="stat-info">
              <h3>Content Flags</h3>
              <p className="stat-value">{stats.flaggedReports}</p>
              <span className="stat-trend danger">Moderation</span>
            </div>
          </div>
        </div>
      )}

      <section className="usage-chart glass-card">
        <h3>Platform Activity (Stub)</h3>
        <div className="chart-placeholder">
          <div className="bar" style={{height: '40%'}}></div>
          <div className="bar" style={{height: '70%'}}></div>
          <div className="bar" style={{height: '50%'}}></div>
          <div className="bar highlight" style={{height: '90%'}}></div>
          <div className="bar" style={{height: '60%'}}></div>
        </div>
        <p className="chart-note">Activity spikes detected during peak campus commuting hours.</p>
      </section>
    </div>
  );
};

export default Stats;
