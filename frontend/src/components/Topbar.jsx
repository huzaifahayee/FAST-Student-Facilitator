import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './Topbar.css';

/**
 * Topbar Component with Global Search
 * 
 * What's new here?
 * We've added 'Global Search' logic (Feature #6). 
 * As you type, the system filters the 10 services of FSF.
 */
const Topbar = ({ theme, toggleTheme }) => {
  const [query, setQuery] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const navigate = useNavigate();
  const dropdownRef = useRef(null);

  // The 10 features defined in your proposal
  const features = [
    { name: 'Carpool', path: '/carpool', tags: ['ride', 'transport', 'sharing'] },
    { name: 'Lost & Found', path: '/lost-found', tags: ['items', 'missing', 'found'] },
    { name: 'Past Papers', path: '/past-papers', tags: ['exams', 'study', 'papers'] },
    { name: 'Campus Events', path: '/events', tags: ['activities', 'dates', 'semester'] },
    { name: 'Reminders', path: '/reminders', tags: ['pop', 'alerts', 'deadlines'] },
    { name: 'Campus Map', path: '/map', tags: ['guide', 'directions', 'rooms'] },
    { name: 'Timetable', path: '/timetable', tags: ['classes', 'schedule', 'weekly'] },
    { name: 'Book Exchange', path: '/marketplace', tags: ['books', 'buy', 'sell'] },
    { name: 'FastNotes', path: '/notes', tags: ['pdfs', 'study', 'sharing'] },
    { name: 'Dashboard', path: '/', tags: ['home', 'main', 'overview'] }
  ];

  // Logic to filter features as the user types
  useEffect(() => {
    if (query.length > 0) {
      const filtered = features.filter(f => 
        f.name.toLowerCase().includes(query.toLowerCase()) || 
        f.tags.some(tag => tag.includes(query.toLowerCase()))
      );
      setSuggestions(filtered);
      setShowDropdown(true);
    } else {
      setShowDropdown(false);
    }
  }, [query]);

  // Logic to close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (path) => {
    navigate(path);
    setQuery('');
    setShowDropdown(false);
  };

  return (
    <header className="topbar glass-card">
      <h2 className="page-title">FSF Portal</h2>
      
      <div className="search-container" ref={dropdownRef}>
        <span className="search-icon">🔍</span>
        <input 
          type="text" 
          placeholder="Type 'Ride', 'Papers', or 'Map'..." 
          className="search-input"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
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
        <div className={`theme-switch ${theme}`} onClick={toggleTheme}>
          <div className="switch-track">
            <span className="switch-icon">☀️</span>
            <span className="switch-icon">🌙</span>
          </div>
          <div className="switch-thumb"></div>
        </div>
        <button className="notif-btn">
          <span>🔔</span>
          <div className="notif-dot"></div>
        </button>
        <div className="date-badge">
          Apr 17, 2026
        </div>
      </div>
    </header>
  );
};

export default Topbar;
