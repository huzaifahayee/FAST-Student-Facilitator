import React, { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Search, Bell, Sun, Moon } from 'lucide-react';
import './Topbar.css';

/**
 * Topbar — Quantix-styled application header.
 *
 * Layout (left to right):
 *   - Crimson "active page" pill
 *   - Pill search (muted icon + input + type-ahead)
 *   - Theme toggle, notification bell with red dot, user avatar pill
 */

const FEATURES = [
    { name: 'Dashboard',     path: '/',           tags: ['home', 'main', 'overview'] },
    { name: 'Carpool',       path: '/carpool',    tags: ['ride', 'transport', 'sharing'] },
    { name: 'Lost & Found',  path: '/lost-found', tags: ['items', 'missing', 'found'] },
    { name: 'Past Papers',   path: '/past-papers',tags: ['exams', 'study', 'papers'] },
    { name: 'Campus Events', path: '/events',     tags: ['activities', 'dates', 'semester'] },
    { name: 'Reminders',     path: '/reminders',  tags: ['pop', 'alerts', 'deadlines'] },
    { name: 'Campus Map',    path: '/map',        tags: ['guide', 'directions', 'rooms'] },
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
    '/map': 'Map Guide',
    '/timetable': 'Timetable',
    '/marketplace': 'Book Exchange',
    '/notes': 'FastNotes',
    '/admin': 'Moderation',
    '/stats': 'Analytics',
};

const Topbar = ({ theme, toggleTheme, user }) => {
    const [query, setQuery] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const [showDropdown, setShowDropdown] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const dropdownRef = useRef(null);

    const activeLabel = ROUTE_LABELS[location.pathname] || 'FSF Portal';

    useEffect(() => {
        if (query.length > 0) {
            const q = query.toLowerCase();
            setSuggestions(
                FEATURES.filter(f =>
                    f.name.toLowerCase().includes(q) ||
                    f.tags.some(tag => tag.includes(q))
                )
            );
            setShowDropdown(true);
        } else {
            setShowDropdown(false);
        }
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

    const handleSelect = (path) => {
        navigate(path);
        setQuery('');
        setShowDropdown(false);
    };

    const initials = (user?.name || 'U')
        .split(' ')
        .map(s => s[0])
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
                    placeholder="Search features…"
                    className="search-input"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    autoComplete="off"
                />
                {showDropdown && (
                    <div className="search-dropdown glass-card">
                        {suggestions.length > 0 ? (
                            suggestions.map((s, i) => (
                                <div
                                    key={i}
                                    className="search-result-item"
                                    onClick={() => handleSelect(s.path)}
                                >
                                    <span className="result-name">{s.name}</span>
                                    <span className="result-category">Feature</span>
                                </div>
                            ))
                        ) : (
                            <div className="search-no-results">No features found</div>
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
                    {theme === 'dark'
                        ? <Sun size={18} strokeWidth={2} />
                        : <Moon size={18} strokeWidth={2} />}
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
