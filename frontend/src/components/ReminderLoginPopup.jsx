import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../pages/PopReminders.css';

/**
 * ReminderLoginPopup — UC-24 (View Reminders and Login Pop-up).
 *
 * Triggered once after a fresh login. If the student has any PENDING
 * reminders, a modal listing them (sorted by Date & Time, with overdue
 * entries highlighted in red) is shown. The student can dismiss it or
 * jump straight to the My Reminders view.
 */
const ReminderLoginPopup = ({ user, justLoggedIn, onDismiss }) => {
    const [pending, setPending] = useState([]);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        if (!justLoggedIn || !user?.email) return;

        let cancelled = false;
        (async () => {
            try {
                const res = await fetch(
                    `http://localhost:8080/api/reminders/pending?email=${encodeURIComponent(user.email)}`
                );
                if (!res.ok) return;
                const data = await res.json();
                if (cancelled) return;
                if (Array.isArray(data) && data.length > 0) {
                    setPending(data);
                    setOpen(true);
                } else {
                    onDismiss && onDismiss();
                }
            } catch {
                /* network failure — silently skip pop-up */
            }
        })();

        return () => { cancelled = true; };
    }, [justLoggedIn, user?.email]);

    const close = () => {
        setOpen(false);
        onDismiss && onDismiss();
    };

    if (!open) return null;

    const formatWhen = (iso) =>
        new Date(iso).toLocaleString(undefined, {
            weekday: 'short', day: '2-digit', month: 'short',
            hour: '2-digit', minute: '2-digit'
        });

    const isOverdue = (r) => new Date(r.reminderTime).getTime() < Date.now();

    return (
        <div className="modal-backdrop reminders-modal-backdrop" onClick={close}>
            <div className="modal glass-card login-popup-modal reminders-modal-sheet" onClick={(e) => e.stopPropagation()}>
                <h2>You have {pending.length} pending reminder{pending.length === 1 ? '' : 's'}</h2>
                <p className="login-popup-subtitle">
                    Sorted by date &amp; time. Overdue items are highlighted in red.
                </p>

                <div className="login-popup-list">
                    {pending.map(r => {
                        const overdue = isOverdue(r);
                        return (
                            <div key={r.id} className={`popup-item ${overdue ? 'overdue' : ''}`}>
                                <div style={{ flex: 1, minWidth: 0 }}>
                                    <div>
                                        <span className="popup-cat">{r.category}</span>
                                        <span className="popup-title">{r.title}</span>
                                    </div>
                                    <div className="popup-meta">🔔 {formatWhen(r.reminderTime)}</div>
                                </div>
                            </div>
                        );
                    })}
                </div>

                <div className="login-popup-actions">
                    <button onClick={close}>Dismiss</button>
                    <button
                        className="primary"
                        onClick={() => { close(); navigate('/reminders'); }}
                    >
                        Open My Reminders
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ReminderLoginPopup;
