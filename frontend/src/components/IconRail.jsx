import { NavLink, useNavigate } from 'react-router-dom';
import {
    LayoutDashboard,
    Car,
    Package,
    FileText,
    Calendar,
    Bell,
    Map,
    Clock,
    BookOpen,
    StickyNote,
    Shield,
    BarChart3,
    LogOut,
} from 'lucide-react';
import './IconRail.css';

/**
 * IconRail — slim, icon-only navigation column that replaces the previous
 * wide labelled sidebar. Inspired by the Quantix dashboard floating action
 * rail. Search lives in the top bar. Each link has a title tooltip.
 */

const navItems = [
    { to: '/',            label: 'Dashboard',     Icon: LayoutDashboard },
    { to: '/carpool',     label: 'Carpool',       Icon: Car },
    { to: '/lost-found',  label: 'Lost & Found',  Icon: Package },
    { to: '/past-papers', label: 'Past Papers',   Icon: FileText },
    { to: '/events',      label: 'Events',        Icon: Calendar },
    { to: '/reminders',   label: 'Reminders',     Icon: Bell },
    { to: '/campus-map',  label: 'Map Guide',     Icon: Map },
    { to: '/timetable',   label: 'Timetable',     Icon: Clock },
    { to: '/marketplace', label: 'Book Exchange', Icon: BookOpen },
    { to: '/notes',       label: 'FastNotes',     Icon: StickyNote },
];

const adminItems = [
    { to: '/admin', label: 'Moderation', Icon: Shield },
    { to: '/stats', label: 'Analytics',  Icon: BarChart3 },
];

const IconRail = ({ user, onLogout }) => {
    const navigate = useNavigate();
    const isAdmin = user?.role === 'ADMIN';

    const handleLogout = () => {
        if (onLogout) onLogout();
        navigate('/login');
    };

    return (
        <aside className="icon-rail">
            <nav className="rail-section rail-nav" aria-label="Primary">
                {navItems.map(({ to, label, Icon }) => (
                    <NavLink
                        key={to}
                        to={to}
                        end={to === '/'}
                        title={label}
                        aria-label={label}
                        className={({ isActive }) =>
                            `rail-link ${isActive ? 'active' : ''}`
                        }
                    >
                        <Icon size={20} strokeWidth={2} />
                    </NavLink>
                ))}

                {isAdmin && (
                    <>
                        <div className="rail-divider" aria-hidden="true" />
                        {adminItems.map(({ to, label, Icon }) => (
                            <NavLink
                                key={to}
                                to={to}
                                title={label}
                                aria-label={label}
                                className={({ isActive }) =>
                                    `rail-link ${isActive ? 'active' : ''}`
                                }
                            >
                                <Icon size={20} strokeWidth={2} />
                            </NavLink>
                        ))}
                    </>
                )}
            </nav>

            <div className="rail-section rail-bottom">
                <button
                    className="rail-link rail-logout"
                    title="Log out"
                    aria-label="Log out"
                    onClick={handleLogout}
                >
                    <LogOut size={20} strokeWidth={2} />
                </button>
            </div>
        </aside>
    );
};

export default IconRail;
