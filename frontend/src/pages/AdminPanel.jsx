import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import './AdminPanel.css';

/**
 * AdminPanel Component
 * Enhanced with User Management and Hard Deletion.
 */
const AdminPanel = () => {
  const [flaggedItems, setFlaggedItems] = useState([]);
  const [pendingItems, setPendingItems] = useState([]);
  const [users, setUsers] = useState([]);
  const [logs, setLogs] = useState([]);
  const [searchParams] = useSearchParams();
  const validTabs = ['approvals', 'moderation', 'users'];
  const initialTab = validTabs.includes(searchParams.get('tab'))
    ? searchParams.get('tab')
    : 'approvals';
  const [activeTab, setActiveTab] = useState(initialTab);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Keep the active tab in sync with the URL so deep-links from the Dashboard work.
  useEffect(() => {
    const tab = searchParams.get('tab');
    if (validTabs.includes(tab) && tab !== activeTab) {
      setActiveTab(tab);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    refreshData();
    const interval = setInterval(refreshData, 10000);
    return () => clearInterval(interval);
  }, []);

  const refreshData = () => {
    setError(null);
    fetchFlagged();
    fetchPending();
    fetchLogs();
    fetchUsers();
  };

  const fetchFlagged = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/rides/flagged');
      if (!res.ok) throw new Error("CORS or Server Error (Flagged)");
      const data = await res.json();
      setFlaggedItems(data);
    } catch (err) {
      console.error(err);
      setError("Moderation connectivity lost. Check backend.");
    } finally {
      setLoading(false);
    }
  };

  const fetchPending = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/rides/pending');
      if (!res.ok) throw new Error("Server error (Pending)");
      const data = await res.json();
      setPendingItems(data);
    } catch (err) {
      setError("Failed to sync with approval queue.");
    }
  };

  const fetchUsers = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/users');
      if (!res.ok) throw new Error("User data fetch failed");
      const data = await res.json();
      setUsers(data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchLogs = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/admin/logs');
      if (!res.ok) throw new Error("Log fetch failed");
      const data = await res.json();
      setLogs(data);
    } catch (err) {
      setError("System logs unreachable.");
    }
  };

  const handleApprove = async (id) => {
    const reason = prompt("Optional: Add a message for the student (e.g., 'Verified FASTian')");
    try {
      await fetch(`http://localhost:8080/api/rides/${id}/approve?reason=${encodeURIComponent(reason || '')}`, { method: 'PUT' });
      refreshData();
    } catch (err) {
      console.error("Approve failed", err);
    }
  };

  const handleResolve = async (id) => {
    if (!window.confirm("Clear the flag on this ride and keep it live?")) return;
    try {
      const res = await fetch(`http://localhost:8080/api/rides/${id}/resolve`, { method: 'PUT' });
      if (!res.ok) throw new Error(`Resolve failed with status ${res.status}`);
      refreshData();
    } catch (err) {
      console.error("Resolve failed", err);
      setError("Could not clear the flag. Please retry.");
    }
  };

  const handleDelete = async (id) => {
    const reason = prompt("REQUIRED: Why is this content being removed?");
    if (!reason) return; // Force a reason for deletion

    if (window.confirm("CRITICAL: This will permanently delete this ride. Proceed?")) {
      try {
        await fetch(`http://localhost:8080/api/rides/${id}?reason=${encodeURIComponent(reason)}`, { method: 'DELETE' });
        refreshData();
      } catch (err) {
        console.error("Delete failed", err);
      }
    }
  };

  const handleToggleBan = async (id, name) => {
    if (window.confirm(`Are you sure you want to change access status for ${name}?`)) {
      try {
        await fetch(`http://localhost:8080/api/users/${id}/ban`, { method: 'PUT' });
        refreshData();
      } catch (err) {
        console.error("Ban toggle failed", err);
      }
    }
  };

  return (
    <div className="admin-panel">
      {error && <div className="admin-error-banner pulse">{error}</div>}
      
      <header className="admin-header">
        <h2>🛡️ Moderation Center</h2>
        <p>Manage system integrity, user access, and content policy.</p>
        
        <div className="admin-tabs">
          <button 
            className={`tab-btn ${activeTab === 'approvals' ? 'active' : ''}`}
            onClick={() => setActiveTab('approvals')}
          >
            Approvals ({pendingItems.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'moderation' ? 'active' : ''}`}
            onClick={() => setActiveTab('moderation')}
          >
            Reported ({flaggedItems.length})
          </button>
          <button 
            className={`tab-btn ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
          >
            Users ({users.length})
          </button>
        </div>
      </header>

      <div className="admin-grid">
        <section className="admin-card glass-card">
          <h3>
            {activeTab === 'approvals' && 'Pending Verification'}
            {activeTab === 'moderation' && 'Inappropriate Content Flags'}
            {activeTab === 'users' && 'Student Access Control'}
          </h3>

          <div className="table-wrapper">
            {loading ? (
              <div className="skeleton-placeholder pulse">Syncing metadata...</div>
            ) : activeTab === 'users' ? (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(user => (
                    <tr key={user.id}>
                      <td>{user.name}</td>
                      <td>{user.email}</td>
                      <td>
                        <span className={`status-tag ${user.banned ? 'banned' : 'active'}`}>
                          {user.banned ? 'Banned' : 'Authorized'}
                        </span>
                      </td>
                      <td>
                        <button 
                          className={user.banned ? 'approve-btn' : 'reject-btn'}
                          onClick={() => handleToggleBan(user.id, user.name)}
                        >
                          {user.banned ? 'Restore' : 'Restrict'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (activeTab === 'approvals' ? pendingItems : flaggedItems).length > 0 ? (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Target</th>
                    <th>Details</th>
                    <th>{activeTab === 'moderation' ? 'Flag Reason' : 'Action'}</th>
                    <th>Control</th>
                  </tr>
                </thead>
                <tbody>
                  {(activeTab === 'approvals' ? pendingItems : flaggedItems).map(item => (
                    <tr key={item.id}>
                      <td>Ride #{item.id}</td>
                      <td>{item.origin} → {item.destination}</td>
                      <td>
                        {item.moderationReason ? (
                          <span className="reason-tag">{item.moderationReason}</span>
                        ) : (
                          <span className="text-muted">No flag note</span>
                        )}
                      </td>
                      <td>
                        <div className="action-btns">
                          {activeTab === 'approvals' ? (
                            <button className="approve-btn" onClick={() => handleApprove(item.id)}>Approve</button>
                          ) : (
                            <button className="approve-btn" onClick={() => handleResolve(item.id)}>Resolve</button>
                          )}
                          <button className="reject-btn" onClick={() => handleDelete(item.id)}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="empty-state pulse-subtle">
                <span className="check-icon">✅</span>
                <p>System is clean. No {activeTab} requires action.</p>
              </div>
            )}
          </div>
        </section>

        <section className="admin-card glass-card system-logs">
          <h3>System Audit Logs</h3>
          <div className="log-list">
            {logs.length > 0 ? logs.map(log => (
              <div className="log-entry" key={log.id}>
                <span className="log-time">{new Date(log.timestamp).toLocaleTimeString()}</span>
                <p>{log.message}</p>
                <span className={`log-tag ${log.logType.toLowerCase()}`}>{log.logType}</span>
              </div>
            )) : (
              <p className="no-logs">Quiet in the cockpit. No logs.</p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
};

export default AdminPanel;
