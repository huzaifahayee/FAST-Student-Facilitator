import { Link, useLocation } from 'react-router-dom';
import './Sidebar.css';

/**
 * Sidebar Component
 * 
 * What is this component?
 * This is the vertical navigation bar on the left.
 * We now use 'Link' from react-router-dom instead of plain divs. 
 * This allows us to navigate to different components WITHOUT refreshing the page.
 */
const Sidebar = ({ user }) => {
  const location = useLocation();
  const isAdmin = user?.role === 'ADMIN';
  const isLinkActive = (path) => location.pathname === path;

  return (
    <aside className="sidebar glass-card">
      <Link to="/" style={{ textDecoration: 'none', color: 'inherit' }}>
        <div className="sidebar-logo">
          <div className="logo-icon">F</div>
          <div className="logo-text">
            <span className="brand">FSF</span>
            <span className="campus">FAST Lahore</span>
          </div>
        </div>
      </Link>

      <nav className="sidebar-nav">
        <div className="nav-group">
          <p className="nav-label">Main</p>
          <Link to="/" className={`nav-item ${isLinkActive('/') ? 'active' : ''}`}>
            <span className="nav-icon">🏠</span>
            Dashboard
          </Link>
          <Link to="/search" className={`nav-item ${isLinkActive('/search') ? 'active' : ''}`}>
            <span className="nav-icon">🔍</span>
            Global Search
          </Link>
        </div>

        {isAdmin && (
          <div className="nav-group">
            <p className="nav-label">Admin Panel</p>
            <Link to="/admin" className={`nav-item ${isLinkActive('/admin') ? 'active' : ''}`}>
              <span className="nav-icon">🛡️</span>
              Moderation
            </Link>
            <Link to="/stats" className={`nav-item ${isLinkActive('/stats') ? 'active' : ''}`}>
              <span className="nav-icon">📊</span>
              Analytics
            </Link>
          </div>
        )}

        <div className="nav-group">
          <p className="nav-label">Services</p>
          <Link to="/carpool" className={`nav-item ${isLinkActive('/carpool') ? 'active' : ''}`}>
            <span className="nav-icon">🚗</span>
            Carpool
          </Link>
          <Link to="/lost-found" className={`nav-item ${isLinkActive('/lost-found') ? 'active' : ''}`}>
            <span className="nav-icon">📦</span>
            Lost & Found
          </Link>
          <Link to="/past-papers" className={`nav-item ${isLinkActive('/past-papers') ? 'active' : ''}`}>
            <span className="nav-icon">📝</span>
            Past Papers
          </Link>
          <Link to="/events" className={`nav-item ${isLinkActive('/events') ? 'active' : ''}`}>
            <span className="nav-icon">📅</span>
            Events
          </Link>
          <Link to="/reminders" className={`nav-item ${isLinkActive('/reminders') ? 'active' : ''}`}>
            <span className="nav-icon">🔔</span>
            Reminders
          </Link>
          <Link to="/campus-map" className={`nav-item ${isLinkActive('/campus-map') ? 'active' : ''}`}>
            <span className="nav-icon">🗺️</span>
            Map Guide
          </Link>
          <Link to="/timetable" className={`nav-item ${isLinkActive('/timetable') ? 'active' : ''}`}>
            <span className="nav-icon">🕒</span>
            Timetable
          </Link>
          <Link to="/marketplace" className={`nav-item ${isLinkActive('/marketplace') ? 'active' : ''}`}>
            <span className="nav-icon">📚</span>
            Book Exchange
          </Link>
          <Link to="/notes" className={`nav-item ${isLinkActive('/notes') ? 'active' : ''}`}>
            <span className="nav-icon">📄</span>
            FastNotes
          </Link>
        </div>
      </nav>

      <div className="sidebar-footer">
        <div className="user-badge">
          <div className="user-avatar">{user?.name?.charAt(0) || 'U'}</div>
          <div className="user-details">
            <p className="user-name">{user?.name || 'Guest User'}</p>
            <p className="user-role">{user?.role === 'ADMIN' ? 'Portal Admin' : 'Student'}</p>
          </div>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
