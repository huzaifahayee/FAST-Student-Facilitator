import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Search, Bell, Sun, Moon } from 'lucide-react';
import { fetchGlobalSearch } from '../utils/globalSearch';
import './Topbar.css';

/**
 * Topbar — Quantix-styled application header.
 *
 * Search: feature shortcuts + debounced API aggregate (past papers, rides, books, …).
 */

const FEATURES = [
    { name: 'Dashboard',     path: '/',           tags: ['home', 'main', 'overview'] },
    { name: 'Carpool',       path: '/carpool',    tags: ['ride', 'transport', 'sharing'] },
    { name: 'Lost & Found',  path: '/lost-found', tags: ['items', 'missing', 'found'] },
    { name: 'Past Papers',   path: '/past-papers',tags: ['exams', 'study', 'papers'] },
    { name: 'Campus Events', path: '/events',     tags: ['activities', 'dates', 'semester'] },
    { name: 'Reminders',     path: '/reminders',  tags: ['pop', 'alerts', 'deadlines'] },
    { name: 'Campus Map',    path: '/campus-map', tags: ['guide', 'directions', 'rooms'] },
    { name: 'Timetable',     path: '/timetable',  tags: ['classes', 'schedule', 'weekly'] },
    { name: 'Book Exchange', path: '/marketplace',tags: ['books', 'buy', 'sell'] },
    { name: 'FastNotes',     path: '/notes',      tags: ['pdfs', 'study', 'sharing'] },
];

const ROUTE_LABELS = {
    '/': 'Dashboard',
    '/carpool': 'Carpool',
    '/lost-found': 'Lost & Found',
    '/past-papers': 'Past Papers',
    '/events': 'Events',
    '/reminders': 'Reminders',
    '/campus-map': 'Map Guide',
    '/timetable': 'Timetable',
    '/marketplace': 'Book Exchange',
    '/notes': 'FastNotes',
    '/admin': 'Moderation',
    '/stats': 'Analytics',
};

const MIN_REMOTE_LEN = 2;
const DEBOUNCE_MS = 320;

function SearchSection({ label, children }) {
    const nodes = React.Children.toArray(children).filter(Boolean);
    if (nodes.length === 0) return null;
    return (
        <>
            <div className="search-section-label">{label}</div>
            {children}
        </>
    );
}

const Topbar = ({ theme, toggleTheme, user }) => {
    const [query, setQuery] = useState('');
    const [featureHits, setFeatureHits] = useState([]);
    const [remote, setRemote] = useState(null);
    const [remoteLoading, setRemoteLoading] = useState(false);
    const [showDropdown, setShowDropdown] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const dropdownRef = useRef(null);

    const activeLabel = ROUTE_LABELS[location.pathname] || 'FSF Portal';

    useEffect(() => {
        /* eslint-disable react-hooks/set-state-in-effect -- derive dropdown lists from query */
        const q = query.trim();
        const qLower = q.toLowerCase();

        const feat = FEATURES.filter(
            (f) =>
                f.name.toLowerCase().includes(qLower) ||
                f.tags.some((tag) => tag.includes(qLower))
        );
        setFeatureHits(feat);

        if (q.length === 0) {
            setShowDropdown(false);
            setRemote(null);
            setRemoteLoading(false);
            return;
        }
        setShowDropdown(true);

        if (q.length < MIN_REMOTE_LEN) {
            setRemote(null);
            setRemoteLoading(false);
            return;
        }

        const ac = new AbortController();
        const t = window.setTimeout(async () => {
            setRemoteLoading(true);
            try {
                const data = await fetchGlobalSearch(q, ac.signal);
                if (!ac.signal.aborted) setRemote(data);
            } catch {
                if (!ac.signal.aborted) setRemote(null);
            } finally {
                if (!ac.signal.aborted) setRemoteLoading(false);
            }
        }, DEBOUNCE_MS);

        /* eslint-enable react-hooks/set-state-in-effect */

        return () => {
            window.clearTimeout(t);
            ac.abort();
        };
    }, [query]);

    useEffect(() => {
        const onClick = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setShowDropdown(false);
            }
        };
        document.addEventListener('mousedown', onClick);
        return () => document.removeEventListener('mousedown', onClick);
    }, []);

    const trimmed = query.trim();
    const encQ = encodeURIComponent(trimmed);

    const closeSearch = () => {
        setQuery('');
        setShowDropdown(false);
    };

    const goFeature = (path) => {
        navigate(path);
        closeSearch();
    };

    const hasRemote =
        remote &&
        [
            remote.papers?.length,
            remote.rides?.length,
            remote.books?.length,
            remote.locations?.length,
            remote.notes?.length,
            remote.lost?.length,
            remote.events?.length,
        ].some(Boolean);

    const initials = (user?.name || 'U')
        .split(' ')
        .map((s) => s[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();

    return (
        <header className="topbar">
            <div className="topbar-left">
                <span className="page-pill">{activeLabel}</span>
            </div>

            <div className="search-container" ref={dropdownRef}>
                <span className="search-icon" aria-hidden="true">
                    <Search size={18} strokeWidth={2} />
                </span>
                <input
                    type="text"
                    placeholder="Search features & content…"
                    className="search-input"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    autoComplete="off"
                    aria-expanded={showDropdown}
                    aria-controls="global-search-results"
                    role="combobox"
                />
                {showDropdown && (
                    <div
                        id="global-search-results"
                        className="search-dropdown glass-card"
                        role="listbox"
                    >
                        {featureHits.length > 0 && (
                            <>
                                <div className="search-section-label">Features</div>
                                {featureHits.map((s, i) => (
                                    <div
                                        key={`f-${s.path}-${i}`}
                                        role="option"
                                        className="search-result-item search-result-card"
                                        onClick={() => goFeature(s.path)}
                                    >
                                        <div className="search-result-main">
                                            <span className="result-title">{s.name}</span>
                                            <span className="result-meta">Open section</span>
                                        </div>
                                        <span className="result-category">Feature</span>
                                    </div>
                                ))}
                            </>
                        )}

                        {trimmed.length >= MIN_REMOTE_LEN && remoteLoading && (
                            <div className="search-loading">Searching catalog…</div>
                        )}

                        {trimmed.length >= MIN_REMOTE_LEN && !remoteLoading && remote && (
                            <>
                                <SearchSection label="Past Papers">
                                    {remote.papers?.map((p) => (
                                        <div
                                            key={`p-${p.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/past-papers?q=${encQ}&paper=${p.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{p.courseName}</span>
                                                <span className="result-meta">
                                                    {[p.courseCode, p.examType, p.semesterYear]
                                                        .filter(Boolean)
                                                        .join(' · ')}
                                                </span>
                                            </div>
                                            <span className="result-category">Past Paper</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="Carpool">
                                    {remote.rides?.map((r) => (
                                        <div
                                            key={`r-${r.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/carpool?q=${encQ}&ride=${r.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">
                                                    {r.origin} → {r.destination}
                                                </span>
                                                <span className="result-meta">
                                                    {r.departureTime} · {r.availableSeats} seats ·{' '}
                                                    {r.vehicleType}
                                                </span>
                                            </div>
                                            <span className="result-category">Ride</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="Book Exchange">
                                    {remote.books?.map((b) => (
                                        <div
                                            key={`b-${b.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/marketplace?q=${encQ}&book=${b.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{b.bookTitle}</span>
                                                <span className="result-meta">
                                                    {[b.courseCode, b.bookCondition, `Rs. ${b.price}`]
                                                        .filter(Boolean)
                                                        .join(' · ')}{' '}
                                                    · {b.listingType}
                                                </span>
                                            </div>
                                            <span className="result-category">Book</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="Campus Map">
                                    {remote.locations?.map((loc) => (
                                        <div
                                            key={`loc-${loc.id ?? loc.locationName}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                const locKey = encodeURIComponent(loc.locationName || '');
                                                navigate(`/campus-map?q=${locKey}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{loc.locationName}</span>
                                                <span className="result-meta">
                                                    {(loc.description || '').slice(0, 72)}
                                                    {(loc.description || '').length > 72 ? '…' : ''}
                                                </span>
                                            </div>
                                            <span className="result-category">Location</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="FastNotes">
                                    {remote.notes?.map((n) => (
                                        <div
                                            key={`n-${n.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/notes?q=${encQ}&note=${n.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{n.title}</span>
                                                <span className="result-meta">
                                                    {[n.subjectName, n.courseCode].filter(Boolean).join(' · ')}
                                                </span>
                                            </div>
                                            <span className="result-category">Note</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="Lost & Found">
                                    {remote.lost?.map((item) => (
                                        <div
                                            key={`l-${item.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/lost-found?q=${encQ}&item=${item.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{item.itemName}</span>
                                                <span className="result-meta">
                                                    {item.type} · {item.category}
                                                    {item.location ? ` · ${item.location}` : ''}
                                                </span>
                                            </div>
                                            <span className="result-category">Listing</span>
                                        </div>
                                    ))}
                                </SearchSection>

                                <SearchSection label="Events">
                                    {remote.events?.map((ev) => (
                                        <div
                                            key={`e-${ev.id}`}
                                            role="option"
                                            className="search-result-item search-result-card"
                                            onClick={() => {
                                                navigate(`/events?q=${encQ}&event=${ev.id}`);
                                                closeSearch();
                                            }}
                                        >
                                            <div className="search-result-main">
                                                <span className="result-title">{ev.title}</span>
                                                <span className="result-meta">
                                                    {[ev.eventDate, ev.venue, ev.category]
                                                        .filter(Boolean)
                                                        .join(' · ')}
                                                </span>
                                            </div>
                                            <span className="result-category">Event</span>
                                        </div>
                                    ))}
                                </SearchSection>
                            </>
                        )}

                        {trimmed.length >= MIN_REMOTE_LEN &&
                            !remoteLoading &&
                            !hasRemote &&
                            featureHits.length === 0 && (
                                <div className="search-no-results">No matches</div>
                            )}

                        {trimmed.length > 0 &&
                            trimmed.length < MIN_REMOTE_LEN &&
                            featureHits.length === 0 && (
                                <div className="search-hint">
                                    Type at least {MIN_REMOTE_LEN} characters to search uploads & listings.
                                </div>
                            )}
                    </div>
                )}
            </div>

            <div className="topbar-actions">
                <button
                    type="button"
                    className="icon-btn"
                    onClick={toggleTheme}
                    title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
                    aria-label="Toggle theme"
                >
                    {theme === 'dark' ? (
                        <Sun size={18} strokeWidth={2} />
                    ) : (
                        <Moon size={18} strokeWidth={2} />
                    )}
                </button>

                <button
                    type="button"
                    className="icon-btn notif-btn"
                    title="Reminders & alerts"
                    aria-label="Open reminders"
                    onClick={() => navigate('/reminders')}
                >
                    <Bell size={18} strokeWidth={2} />
                    <span className="notif-dot" aria-hidden="true" />
                </button>

                <div className="user-pill" title={user?.email || ''}>
                    <div className="user-avatar">{initials}</div>
                    <div className="user-info">
                        <span className="user-name">{user?.name || 'Guest'}</span>
                        <span className="user-role">
                            {user?.role === 'ADMIN' ? 'Admin' : 'Student'}
                        </span>
                    </div>
                </div>
            </div>
        </header>
    );
};

export default Topbar;
