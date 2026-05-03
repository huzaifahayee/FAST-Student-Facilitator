import { useEffect, useRef } from 'react';

/**
 * ReminderNotifier — UC-25 Alternate Course of Action 2.
 *
 * While the FSF tab is open, polls the backend every 30s for the student's
 * pending reminders and fires a browser notification at the scheduled
 * Date & Time of each one (within a small grace window). Notifications are
 * deduplicated per browser via localStorage so the same reminder is not
 * surfaced twice.
 *
 * Note: full email + true server-side push at exact times require a
 * backend scheduler / mail service / Web Push subscription, which is out
 * of scope for this prototype. This component fulfils the in-app push
 * portion of the use case while the user is logged in.
 */

const POLL_INTERVAL_MS = 30 * 1000;
const FIRE_WINDOW_MS = 5 * 60 * 1000;
const STORAGE_KEY = 'fsf-fired-reminders';

const loadFired = () => {
    try {
        return new Set(JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'));
    } catch {
        return new Set();
    }
};
const saveFired = (set) => {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify([...set])); } catch { /* ignore */ }
};

const ReminderNotifier = ({ user }) => {
    const firedRef = useRef(loadFired());
    const permissionAskedRef = useRef(false);

    useEffect(() => {
        if (!user?.email) return;

        if (
            typeof window !== 'undefined' &&
            'Notification' in window &&
            Notification.permission === 'default' &&
            !permissionAskedRef.current
        ) {
            permissionAskedRef.current = true;
            Notification.requestPermission().catch(() => { /* ignore */ });
        }

        const tick = async () => {
            try {
                const res = await fetch(
                    `http://localhost:8080/api/reminders/pending?email=${encodeURIComponent(user.email)}`
                );
                if (!res.ok) return;
                const list = await res.json();
                if (!Array.isArray(list)) return;

                const now = Date.now();
                const fired = firedRef.current;
                let changed = false;

                for (const r of list) {
                    const when = new Date(r.reminderTime).getTime();
                    if (Number.isNaN(when)) continue;
                    if (fired.has(r.id)) continue;
                    if (when <= now && now - when <= FIRE_WINDOW_MS) {
                        notify(r);
                        fired.add(r.id);
                        changed = true;
                    }
                }
                if (changed) saveFired(fired);
            } catch {
                /* ignore polling errors */
            }
        };

        tick();
        const id = setInterval(tick, POLL_INTERVAL_MS);
        return () => clearInterval(id);
    }, [user?.email]);

    return null;
};

const notify = (r) => {
    const body = `${r.category} • ${new Date(r.reminderTime).toLocaleString()}`;
    if (
        typeof window !== 'undefined' &&
        'Notification' in window &&
        Notification.permission === 'granted'
    ) {
        try {
            new Notification(`Reminder: ${r.title}`, { body });
            return;
        } catch { /* fall through to in-page banner */ }
    }
    showInPageBanner(r.title, body);
};

const showInPageBanner = (title, body) => {
    if (typeof document === 'undefined') return;
    const el = document.createElement('div');
    el.style.cssText = [
        'position:fixed', 'right:1rem', 'bottom:1rem', 'z-index:2000',
        'background:#1f2230', 'color:#fff', 'padding:0.9rem 1.1rem',
        'border-left:4px solid #FFC107', 'border-radius:8px',
        'box-shadow:0 6px 24px rgba(0,0,0,0.35)', 'max-width:320px',
        'font-family:inherit',
    ].join(';');
    el.innerHTML = `<strong style="display:block;margin-bottom:4px;">🔔 ${title}</strong><span style="font-size:0.85rem;opacity:0.85;">${body}</span>`;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 8000);
};

export default ReminderNotifier;
