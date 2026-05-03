import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './AdminPanel.css';

/**
 * AdminPanel Component
 * Enhanced with User Management and Hard Deletion.
 */
const AdminPanel = () => {
  const { showConfirm, showPrompt } = useFsfDialog();
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
      const [resRides, resPapers, resBooks, resLocations] = await Promise.all([
        fetch('http://localhost:8080/api/rides/flagged'),
        fetch('http://localhost:8080/api/past-papers/flagged'),
        fetch('http://localhost:8080/api/books/flagged'),
        fetch('http://localhost:8080/api/campus-map/locations/flagged')
      ]);
      if (!resRides.ok || !resPapers.ok || !resBooks.ok || !resLocations.ok) throw new Error("CORS or Server Error (Flagged)");
      const dataRides = await resRides.json();
      const dataPapers = await resPapers.json();
      const dataBooks = await resBooks.json();
      const dataLocations = await resLocations.json();
      const mappedRides = dataRides.map(r => ({ ...r, entityType: 'Ride' }));
      const mappedPapers = dataPapers.map(p => ({ ...p, entityType: 'Paper' }));
      const mappedBooks = dataBooks.map(b => ({ ...b, entityType: 'Book' }));
      const mappedLocations = dataLocations.map(l => ({ ...l, entityType: 'Location' }));
      setFlaggedItems([...mappedRides, ...mappedPapers, ...mappedBooks, ...mappedLocations]);
    } catch (err) {
      console.error(err);
      setError("Moderation connectivity lost. Check backend.");
    } finally {
      setLoading(false);
    }
  };

  const fetchPending = async () => {
    try {
      const [resRides, resPapers, resBooks, resLocations] = await Promise.all([
        fetch('http://localhost:8080/api/rides/pending'),
        fetch('http://localhost:8080/api/past-papers/pending'),
        fetch('http://localhost:8080/api/books/pending'),
        fetch('http://localhost:8080/api/campus-map/locations/pending')
      ]);
      if (!resRides.ok || !resPapers.ok || !resBooks.ok || !resLocations.ok) throw new Error("Server error (Pending)");
      const dataRides = await resRides.json();
      const dataPapers = await resPapers.json();
      const dataBooks = await resBooks.json();
      const dataLocations = await resLocations.json();
      const mappedRides = dataRides.map(r => ({ ...r, entityType: 'Ride' }));
      const mappedPapers = dataPapers.map(p => ({ ...p, entityType: 'Paper' }));
      const mappedBooks = dataBooks.map(b => ({ ...b, entityType: 'Book' }));
      const mappedLocations = dataLocations.map(l => ({ ...l, entityType: 'Location' }));
      setPendingItems([...mappedRides, ...mappedPapers, ...mappedBooks, ...mappedLocations]);
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

  const handleApprove = async (id, entityType) => {
    const reasonRaw = await showPrompt({
      title: 'Approve listing',
      message: "Optional: add a message for the student (e.g. 'Verified').",
      placeholder: 'Message (optional)',
      required: false,
      confirmText: 'Approve',
      cancelText: 'Cancel',
    });
    const reason = reasonRaw ?? '';
    const endpoint = entityType === 'Ride' ? 'rides' : entityType === 'Paper' ? 'past-papers' : entityType === 'Location' ? 'campus-map/locations' : 'books';
    try {
      await fetch(`http://localhost:8080/api/${endpoint}/${id}/approve?reason=${encodeURIComponent(reason || '')}`, { method: 'PUT' });
      refreshData();
    } catch (err) {
      console.error("Approve failed", err);
    }
  };

  const handleResolve = async (id, entityType) => {
    const ok = await showConfirm({
      title: 'Clear flag',
      message: 'Clear the flag on this item and keep it live?',
      confirmText: 'Clear flag',
      cancelText: 'Cancel',
    });
    if (!ok) return;
    const endpoint = entityType === 'Ride' ? 'rides' : entityType === 'Paper' ? 'past-papers' : entityType === 'Location' ? 'campus-map/locations' : 'books';
    try {
      const res = await fetch(`http://localhost:8080/api/${endpoint}/${id}/resolve`, { method: 'PUT' });
      if (!res.ok) throw new Error(`Resolve failed with status ${res.status}`);
      refreshData();
    } catch (err) {
      console.error("Resolve failed", err);
      setError("Could not clear the flag. Please retry.");
    }
  };

  const handleDelete = async (id, entityType) => {
    const reason = await showPrompt({
      title: 'Deletion reason',
      message: 'Why is this content being removed?',
      placeholder: 'Reason (required)',
      required: true,
      confirmText: 'Continue',
      cancelText: 'Cancel',
    });
    if (reason == null || !reason.trim()) return;
    const endpoint = entityType === 'Ride' ? 'rides' : entityType === 'Paper' ? 'past-papers' : entityType === 'Location' ? 'campus-map/locations' : 'books';

    const proceed = await showConfirm({
      title: 'Permanent deletion',
      message: 'CRITICAL: This will permanently delete this item. Proceed?',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!proceed) return;
    try {
      await fetch(`http://localhost:8080/api/${endpoint}/${id}?reason=${encodeURIComponent(reason.trim())}`, { method: 'DELETE' });
      refreshData();
    } catch (err) {
      console.error("Delete failed", err);
    }
  };

  const handleToggleBan = async (id, name) => {
    const ok = await showConfirm({
      title: 'Change access',
      message: `Are you sure you want to change access status for ${name}?`,
      confirmText: 'Continue',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!ok) return;
    try {
      await fetch(`http://localhost:8080/api/users/${id}/ban`, { method: 'PUT' });
      refreshData();
    } catch (err) {
      console.error("Ban toggle failed", err);
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
                    <tr key={`${item.entityType}-${item.id}`}>
                      <td>{item.entityType} #{item.id}</td>
                      <td>
                        {item.entityType === 'Ride' 
                          ? `${item.origin} → ${item.destination}` 
                          : item.entityType === 'Paper'
                          ? `${item.courseName} (${item.courseCode})`
                          : item.entityType === 'Location'
                          ? `${item.locationName} (${item.category})`
                          : `${item.bookTitle} - ${item.listingType}`}
                      </td>
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
                            <button className="approve-btn" onClick={() => handleApprove(item.id, item.entityType)}>Approve</button>
                          ) : (
                            <button className="approve-btn" onClick={() => handleResolve(item.id, item.entityType)}>Resolve</button>
                          )}
                          <button className="reject-btn" onClick={() => handleDelete(item.id, item.entityType)}>Delete</button>
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
