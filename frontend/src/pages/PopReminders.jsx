import React, { useState, useEffect, useMemo } from 'react';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './PopReminders.css';

/**
 * PopReminders — implements UC-24 (My Reminders view), UC-25 (Add) and
 * UC-26 (Manage: mark completed / edit / delete).
 *
 * Pending reminders are listed first (sorted by Date & Time ascending);
 * Completed reminders appear at the bottom, grayed out. Overdue pending
 * reminders are highlighted in red.
 */

const API_BASE = 'http://localhost:8080/api/reminders';

const CATEGORIES = [
  { value: 'ASSIGNMENT', label: 'Assignment' },
  { value: 'EXAM',       label: 'Exam' },
  { value: 'QUIZ',       label: 'Quiz' },
  { value: 'PROJECT',    label: 'Project' },
  { value: 'OTHER',      label: 'Other' },
];

const emptyForm = { title: '', reminderTime: '', category: 'ASSIGNMENT' };

const PopReminders = ({ user }) => {
    const { showConfirm } = useFsfDialog();
    const [reminders, setReminders] = useState([]);
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [formData, setFormData] = useState(emptyForm);
    const [formError, setFormError] = useState('');

    useEffect(() => {
        if (user?.email) loadReminders();
    }, [user?.email]);

    const loadReminders = async () => {
        setLoading(true);
        try {
            const res = await fetch(`${API_BASE}?email=${encodeURIComponent(user.email)}`);
            const data = await res.json();
            setReminders(Array.isArray(data) ? data : []);
            // Signal topbar to update notification dot
            window.dispatchEvent(new CustomEvent('remindersUpdated'));
        } catch (err) {
            console.error('Error loading reminders', err);
        } finally {
            setLoading(false);
        }
    };

    const { pending, completed } = useMemo(() => {
        const p = [], c = [];
        for (const r of reminders) {
            ((r.status || 'PENDING').toUpperCase() === 'COMPLETED' ? c : p).push(r);
        }
        return { pending: p, completed: c };
    }, [reminders]);

    const isOverdue = (r) =>
        (r.status || 'PENDING').toUpperCase() === 'PENDING' &&
        new Date(r.reminderTime).getTime() < Date.now();

    const formatWhen = (iso) => {
        const d = new Date(iso);
        return d.toLocaleString(undefined, {
            weekday: 'short', day: '2-digit', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const openAddModal = () => {
        setEditingId(null);
        setFormData(emptyForm);
        setFormError('');
        setShowModal(true);
    };

    const openEditModal = (r) => {
        setEditingId(r.id);
        setFormData({
            title: r.title,
            // datetime-local needs YYYY-MM-DDTHH:mm without seconds
            reminderTime: (r.reminderTime || '').slice(0, 16),
            category: (r.category || 'OTHER').toUpperCase(),
        });
        setFormError('');
        setShowModal(true);
    };

    const setNow = () => {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const h = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        setFormData(prev => ({ ...prev, reminderTime: `${y}-${m}-${day}T${h}:${min}` }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError('');

        if (!formData.title.trim()) { setFormError('Title is required.'); return; }
        if (!formData.reminderTime) { setFormError('Date & Time is required.'); return; }
        if (!formData.category)     { setFormError('Category is required.'); return; }

        const body = {
            title: formData.title.trim(),
            reminderTime: formData.reminderTime,
            category: formData.category,
            studentEmail: user.email,
        };

        try {
            const url = editingId ? `${API_BASE}/${editingId}` : API_BASE;
            const method = editingId ? 'PUT' : 'POST';
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });
            if (!res.ok) {
                const msg = await res.text();
                setFormError(msg || 'Failed to save reminder.');
                return;
            }
            setShowModal(false);
            setEditingId(null);
            setFormData(emptyForm);
            loadReminders();
        } catch (err) {
            setFormError('Network error. Please try again.');
        }
    };

    const handleComplete = async (id) => {
        try {
            const res = await fetch(`${API_BASE}/${id}/complete`, { method: 'PUT' });
            if (res.ok) loadReminders();
        } catch (err) { /* swallow */ }
    };

    const handleDelete = async (id) => {
        const ok = await showConfirm({
            title: 'Delete reminder',
            message: 'Delete this reminder permanently?',
            confirmText: 'Delete',
            cancelText: 'Cancel',
            danger: true,
        });
        if (!ok) return;
        try {
            const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
            if (res.ok) loadReminders();
        } catch (err) { /* swallow */ }
    };

    const renderCard = (r, { grayed = false } = {}) => {
        const overdue = isOverdue(r);
        const cardClass = [
            'reminder-card glass-card',
            overdue ? 'overdue' : '',
            grayed ? 'completed' : '',
        ].filter(Boolean).join(' ');

        return (
            <div key={r.id} className={cardClass}>
                <div className="status-indicator" />
                <div className="reminder-content">
                    <div className="meta-line">
                        <span className="category">{r.category}</span>
                        {overdue && <span className="overdue-badge">OVERDUE</span>}
                        {grayed && <span className="completed-badge">COMPLETED</span>}
                    </div>
                    <h3 className={overdue ? 'title-overdue' : ''}>{r.title}</h3>
                    <span className="time">🔔 {formatWhen(r.reminderTime)}</span>
                </div>
                <div className="card-actions">
                    {!grayed && (
                        <>
                            <button className="action-btn complete-btn" onClick={() => handleComplete(r.id)}>
                                Mark Completed
                            </button>
                            <button className="action-btn edit-btn" onClick={() => openEditModal(r)}>
                                Edit
                            </button>
                        </>
                    )}
                    <button className="action-btn delete-btn" onClick={() => handleDelete(r.id)}>
                        Delete
                    </button>
                </div>
            </div>
        );
    };

    return (
        <div className={`reminders-page${showModal ? ' reminders-page--modal-open' : ''}`}>
            <div className="header-actions">
                <div>
                    <h1>My Reminders</h1>
                    <p className="subtitle">Pending reminders appear first; completed ones are grayed out below.</p>
                </div>
                <button className="add-btn" onClick={openAddModal}>+ Add Reminder</button>
            </div>

            {loading && <p className="empty-msg">Loading…</p>}

            {!loading && reminders.length === 0 && (
                <p className="empty-msg">No reminders yet. Click <strong>Add Reminder</strong> to create one.</p>
            )}

            {pending.length > 0 && (
                <section className="reminder-section">
                    <h2 className="section-title">Pending <span className="count-pill">{pending.length}</span></h2>
                    <div className="reminder-list">
                        {pending.map(r => renderCard(r))}
                    </div>
                </section>
            )}

            {completed.length > 0 && (
                <section className="reminder-section">
                    <h2 className="section-title section-title-muted">
                        Completed <span className="count-pill">{completed.length}</span>
                    </h2>
                    <div className="reminder-list reminder-list-muted">
                        {completed.map(r => renderCard(r, { grayed: true }))}
                    </div>
                </section>
            )}

            {showModal && (
                <div className="modal-backdrop reminders-modal-backdrop" onClick={() => setShowModal(false)}>
                    <div className="modal glass-card reminders-modal-sheet" onClick={(e) => e.stopPropagation()}>
                        <h2>{editingId ? 'Edit Reminder' : 'Add Reminder'}</h2>
                        <form onSubmit={handleSubmit}>
                            <label className="form-label">
                                <span>Title <span className="req">*</span></span>
                                <input
                                    type="text"
                                    placeholder="e.g. OOP Assignment 3"
                                    value={formData.title}
                                    onChange={e => setFormData({ ...formData, title: e.target.value })}
                                    required
                                />
                            </label>

                            <div className="form-label">
                                <div className="label-with-action">
                                    <span>Date &amp; Time <span className="req">*</span></span>
                                    <button 
                                        type="button" 
                                        className="set-now-btn"
                                        onClick={setNow}
                                    >
                                        🕒 Add current date and time
                                    </button>
                                </div>
                                <input
                                    type="datetime-local"
                                    value={formData.reminderTime}
                                    onChange={e => setFormData({ ...formData, reminderTime: e.target.value })}
                                    required
                                />
                            </div>

                            <div className="form-label">
                                <span>Category <span className="req">*</span></span>
                                <IosPickerField
                                    className="pr-category-picker"
                                    value={formData.category}
                                    onChange={(v) => setFormData({ ...formData, category: v })}
                                    options={CATEGORIES}
                                    sheetTitle="Category"
                                />
                            </div>

                            {formError && <div className="form-error">{formError}</div>}

                            <div className="form-actions">
                                <button type="button" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="submit-btn">
                                    {editingId ? 'Save Changes' : 'Add Reminder'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PopReminders;
