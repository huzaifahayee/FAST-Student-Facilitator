import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useFsfDialog } from '../components/FsfDialogProvider';
import IosPickerField from '../components/IosPickerField';
import './CampusEventBoard.css';

const CampusEventBoard = ({ user }) => {
    const { showAlert, showConfirm } = useFsfDialog();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [events, setEvents] = useState([]);
    const [semesterPlan, setSemesterPlan] = useState([]);
    const [viewMode, setViewMode] = useState('BOARD'); // 'BOARD' or 'PLAN'
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [editingId, setEditingId] = useState(null);

    const blankForm = () => ({
        title: '',
        description: '',
        eventDate: '',
        venue: '',
        organizer: '',
        category: 'SOCIAL',
        semesterPlan: false,
    });
    const [formData, setFormData] = useState(blankForm());

    const [searchQuery, setSearchQuery] = useState('');
    const [flashEventId, setFlashEventId] = useState(null);

    const isAdmin = user?.role === 'ADMIN';

    const loadEvents = async () => {
        setLoading(true);
        try {
            const res = await fetch('http://localhost:8080/api/events');
            const data = await res.json();
            setEvents(data);
        } catch (err) {
            console.error('Error loading events', err);
        } finally {
            setLoading(false);
        }
    };

    const loadSemesterPlan = async () => {
        try {
            const res = await fetch('http://localhost:8080/api/events/semester-plan');
            const data = await res.json();
            setSemesterPlan(data);
        } catch (err) {
            console.error('Error loading semester plan', err);
        }
    };

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect -- initial board + plan load
        loadEvents();
        loadSemesterPlan();
    }, []);

    useEffect(() => {
        if (searchParams.get('event')) return;
        const q = searchParams.get('q');
        // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
        if (q !== null) setSearchQuery((prev) => (q !== prev ? q : prev));
    }, [searchParams]);

    useEffect(() => {
        const raw = searchParams.get('event');
        if (!raw || events.length === 0) return;
        const id = parseInt(raw, 10);

        const stripEventParam = () => {
            const next = new URLSearchParams(searchParams);
            next.delete('event');
            const qs = next.toString();
            navigate(`/events${qs ? `?${qs}` : ''}`, { replace: true });
        };

        if (!Number.isFinite(id)) {
            stripEventParam();
            return;
        }
        const exists = events.some((e) => e.id === id);
        if (!exists) {
            stripEventParam();
            return;
        }

        // eslint-disable-next-line react-hooks/set-state-in-effect -- open highlighted event card from global search
        setViewMode('BOARD');
        setSearchQuery('');
        setFlashEventId(id);
        stripEventParam();
    }, [searchParams, events, navigate]);

    useEffect(() => {
        if (!flashEventId) return;
        const t = window.setTimeout(() => {
            const el = document.getElementById(`event-card-${flashEventId}`);
            el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el?.classList.add('deep-link-highlight');
            window.setTimeout(() => el?.classList.remove('deep-link-highlight'), 2200);
            setFlashEventId(null);
        }, 160);
        return () => window.clearTimeout(t);
    }, [flashEventId]);

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
    };

    const openCreateModal = () => {
        setEditingId(null);
        setFormData(blankForm());
        setShowModal(true);
    };

    const openEditModal = (event) => {
        setEditingId(event.id);
        setFormData({
            title: event.title || '',
            description: event.description || '',
            eventDate: event.eventDate || '',
            venue: event.venue || '',
            organizer: event.organizer || '',
            category: event.category || 'SOCIAL',
            semesterPlan: !!event.semesterPlan,
        });
        setShowModal(true);
    };

    const formatPickedDate = (iso) => {
        if (!iso) return '';
        const d = new Date(iso + 'T00:00:00');
        if (isNaN(d.getTime())) return '';
        return d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!user || !user.email) {
            await showAlert({
                title: 'Session error',
                message: 'User session error. Please log in again.',
            });
            return;
        }

        const isEditing = editingId !== null;
        const url = isEditing
            ? `http://localhost:8080/api/events/${editingId}?requesterEmail=${encodeURIComponent(user.email)}&requesterRole=${encodeURIComponent(user.role || '')}`
            : 'http://localhost:8080/api/events';
        const method = isEditing ? 'PUT' : 'POST';

        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    ...formData,
                    ownerEmail: user.email,
                    approved: isAdmin,
                }),
            });

            if (res.ok) {
                if (isEditing) {
                    await showAlert({ title: 'Saved', message: 'Event updated.' });
                } else {
                    const dest = formData.semesterPlan
                        ? 'Event Board (also added to Semester Plan)'
                        : 'Event Board';
                    await showAlert({ title: 'Event added', message: `Event added to ${dest}.` });
                }
                setShowModal(false);
                setEditingId(null);
                setFormData(blankForm());
                loadEvents();
                loadSemesterPlan();
            } else {
                const err = await res.text();
                console.error("Submission failed:", err);
                await showAlert({ title: 'Error', message: 'Error: ' + err });
            }
        } catch (err) {
            console.error("Network Error:", err);
            await showAlert({
                title: 'Network error',
                message: 'Could not connect to backend.',
            });
        }
    };

    const handleDelete = async (event) => {
        const ok = await showConfirm({
            title: 'Delete event',
            message: `Delete "${event.title}"?`,
            confirmText: 'Delete',
            cancelText: 'Cancel',
            danger: true,
        });
        if (!ok) return;
        try {
            const res = await fetch(`http://localhost:8080/api/events/${event.id}`, { method: 'DELETE' });
            if (res.ok) {
                loadEvents();
                loadSemesterPlan();
            } else {
                await showAlert({ title: 'Delete failed', message: 'Delete failed.' });
            }
        } catch (err) {
            console.error(err);
            await showAlert({
                title: 'Network error',
                message: 'Could not connect to backend.',
            });
        }
    };

    const handleUploadPlan = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const formDataUpload = new FormData();
        formDataUpload.append('file', file);
        formDataUpload.append('ownerEmail', user.email);

        try {
            const res = await fetch('http://localhost:8080/api/events/upload-plan', {
                method: 'POST',
                body: formDataUpload,
            });
            if (res.ok) {
                await showAlert({
                    title: 'Upload complete',
                    message: 'Semester Plan uploaded successfully!',
                });
                loadSemesterPlan();
                loadEvents();
            } else {
                await showAlert({ title: 'Upload failed', message: 'Upload failed.' });
            }
        } catch (err) {
            console.error(err);
            await showAlert({
                title: 'Network error',
                message: 'Error connecting to server.',
            });
        }
    };

    const filteredEvents = (viewMode === 'BOARD' ? events : semesterPlan).filter(event => {
        const query = searchQuery.toLowerCase().trim();
        if (!query) return true;
        const hay = [
            event.title,
            event.description,
            event.eventDate,
            event.category,
            event.venue,
            event.organizer,
        ].map((v) => String(v || '').toLowerCase());
        return hay.some((s) => s.includes(query));
    });

    /** Imported calendar holidays use Administration + HOLIDAY; Add Event never does. */
    const isImportedCalendarHoliday = (event) =>
        (event.category || '').toUpperCase() === 'HOLIDAY' &&
        (event.organizer || '').trim() === 'Administration';

    const semesterPlanRowClassName = (event) => {
        return 'academic-row';
    };

    /** 'holiday' only for plan-upload holidays; 'event' for Add Event (and other non-academic). */
    const semesterPlanHeroKind = (event) => {
        return null;
    };

    const canEdit = (event) => isAdmin;

    return (
        <div className="events-page">
            <div className="header-actions">
                <div className="title-section">
                    <h1>{viewMode === 'BOARD' ? 'Campus Event Board' : 'Semester Plan'}</h1>
                    <div className="btn-group">
                        <button className={`toggle-btn ${viewMode === 'BOARD' ? 'active' : ''}`} onClick={() => setViewMode('BOARD')}>Event Board</button>
                        <button className={`toggle-btn ${viewMode === 'PLAN' ? 'active' : ''}`} onClick={() => setViewMode('PLAN')}>Semester Plan</button>
                    </div>
                </div>

                <div className="search-bar-container">
                    <input
                        type="text"
                        placeholder="Search by name or date (YYYY-MM-DD)..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="search-input"
                    />
                </div>

                {isAdmin && (
                    <div className="admin-actions">
                        <label className="upload-label">
                            📁 Upload Plan (XLS)
                            <input type="file" onChange={handleUploadPlan} hidden accept=".xls,.xlsx" />
                        </label>
                        <button className="add-btn" onClick={openCreateModal}>+ Post Event</button>
                    </div>
                )}
            </div>

            {loading ? <p>Loading...</p> : (
                viewMode === 'BOARD' ? (
                    <div className="event-grid">
                        {filteredEvents.map(event => (
                            <div key={event.id} id={`event-card-${event.id}`} className="event-card glass-card">
                                <div className="card-header">
                                    <span className={`category-tag ${event.category.toLowerCase()}`}>{event.category}</span>
                                    <span className="event-date">{new Date(event.eventDate).toLocaleDateString()}</span>
                                </div>
                                <h3>{event.title}</h3>
                                <p className="description">{event.description}</p>
                                <div className="card-footer">
                                    <span>📍 {event.venue}</span>
                                    <span>👤 {event.organizer}</span>
                                </div>
                                {event.semesterPlan && (
                                    <div className="card-badges">
                                        <span className="badge-plan">Also on Semester Plan</span>
                                    </div>
                                )}
                                {canEdit(event) && (
                                    <div className="card-actions">
                                        <button type="button" className="card-btn edit" onClick={() => openEditModal(event)}>Edit</button>
                                        {isAdmin && (
                                            <button type="button" className="card-btn delete" onClick={() => handleDelete(event)}>Delete</button>
                                        )}
                                    </div>
                                )}
                            </div>
                        ))}
                        {events.length === 0 && <p className="empty-msg">No approved events found.</p>}
                    </div>
                ) : (
                    <div className="semester-plan-table-container glass-card ios-grouped-plan">
                        <table className="semester-plan-table">
                            <thead>
                                <tr>
                                    <th scope="col">Date</th>
                                    <th scope="col">Course / Event</th>
                                    <th scope="col">Time / Details</th>
                                    <th scope="col">Venue</th>
                                    {isAdmin && <th scope="col" className="semester-plan-th-actions">Actions</th>}
                                </tr>
                            </thead>
                            <tbody>
                                {filteredEvents.map((event) => {
                                    const heroKind = semesterPlanHeroKind(event);
                                    return (
                                    <tr key={event.id} className={semesterPlanRowClassName(event)}>
                                        <td>{new Date(event.eventDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                                        <td className="bold-cell">
                                            {heroKind && (
                                                <span
                                                    className={`semester-kind-pill${heroKind === 'holiday' ? ' semester-kind-pill--holiday' : ''}`}
                                                >
                                                    {heroKind === 'holiday' ? 'Holiday' : 'Event'}
                                                </span>
                                            )}
                                            <span className="semester-title-text">{event.title}</span>
                                        </td>
                                        <td>{event.description}</td>
                                        <td>
                                            <span className="venue-cell">
                                                <span className="venue-pin-icon" aria-hidden>
                                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                        <path d="M12 21s7-4.35 7-10a7 7 0 1 0-14 0c0 5.65 7 10 7 10z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
                                                        <circle cx="12" cy="11" r="2.5" fill="currentColor" />
                                                    </svg>
                                                </span>
                                                <span className="venue-text">{event.venue}</span>
                                            </span>
                                        </td>
                                        {isAdmin && (
                                            <td>
                                                {canEdit(event) && (
                                                    <div className="row-actions">
                                                        <button type="button" className="card-btn edit small" onClick={() => openEditModal(event)}>Edit</button>
                                                        <button type="button" className="card-btn delete small" onClick={() => handleDelete(event)}>Delete</button>
                                                    </div>
                                                )}
                                            </td>
                                        )}
                                    </tr>
                                    );
                                })}
                                {semesterPlan.length === 0 && (
                                    <tr>
                                        <td colSpan={isAdmin ? 5 : 4} className="empty-row">No semester plan items found. Upload or propose one!</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )
            )}

            {showModal && (
                <div
                    className="modal-backdrop events-modal-backdrop"
                    role="presentation"
                    onClick={() => { setShowModal(false); setEditingId(null); }}
                >
                    <div
                        className="modal glass-card campus-event-modal"
                        role="dialog"
                        aria-modal="true"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h2>{editingId ? 'Edit Event / Plan Item' : 'Post New Event / Plan Item'}</h2>
                        <form onSubmit={handleSubmit} className="campus-event-form">
                            <div className="form-section">
                                <label className="field-label">Event Details</label>
                                <input type="text" name="title" placeholder="Event Title" value={formData.title} onChange={handleInputChange} required className="full-width" />
                                <textarea name="description" placeholder="Describe the event..." value={formData.description} onChange={handleInputChange} required className="full-width" />
                            </div>

                            <div className="form-row two-cols">
                                <div className="field-group">
                                    <label className="field-label">Venue</label>
                                    <input type="text" name="venue" placeholder="e.g. Auditorium" value={formData.venue} onChange={handleInputChange} required />
                                </div>
                                <div className="field-group">
                                    <label className="field-label">Organizer</label>
                                    <input type="text" name="organizer" placeholder="Society / Dept" value={formData.organizer} onChange={handleInputChange} required />
                                </div>
                            </div>

                            <div className="form-row two-cols">
                                <div className="field-group">
                                    <label className="field-label">Event Date</label>
                                    <input
                                        type="date"
                                        name="eventDate"
                                        value={formData.eventDate}
                                        onChange={handleInputChange}
                                        required
                                    />
                                </div>
                                <div className="field-group">
                                    <label className="field-label">Category</label>
                                    <IosPickerField
                                        value={formData.category}
                                        onChange={(val) => setFormData({ ...formData, category: val })}
                                        options={[
                                            { value: 'ACADEMIC', label: 'Academic' },
                                            { value: 'SOCIAL', label: 'Social' },
                                            { value: 'SPORTS', label: 'Sports' },
                                            { value: 'HOLIDAY', label: 'Holiday' },
                                        ]}
                                        sheetTitle="Select Category"
                                    />
                                </div>
                            </div>

                            <div className="semester-plan-checkbox-row">
                                <label className="checkbox-label">
                                    <input
                                        type="checkbox"
                                        name="semesterPlan"
                                        checked={formData.semesterPlan}
                                        onChange={handleInputChange}
                                    />
                                    <span>Also add to Semester Plan</span>
                                </label>
                            </div>

                            <div className="form-actions">
                                <button type="button" className="cancel-btn" onClick={() => { setShowModal(false); setEditingId(null); }}>Cancel</button>
                                <button type="submit" className="submit-btn">{editingId ? 'Save Changes' : 'Propose Event'}</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CampusEventBoard;
