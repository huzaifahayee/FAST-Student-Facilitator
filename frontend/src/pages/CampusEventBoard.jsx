import React, { useState, useEffect } from 'react';
import './CampusEventBoard.css';

const CampusEventBoard = ({ user }) => {
    const [events, setEvents] = useState([]);
    const [semesterPlan, setSemesterPlan] = useState([]);
    const [viewMode, setViewMode] = useState('BOARD'); // 'BOARD' or 'PLAN'
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({
        title: '', description: '', eventDate: '', venue: '', organizer: '', category: 'SOCIAL', semesterPlan: false
    });

    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        loadEvents();
        loadSemesterPlan();
    }, []);

    const loadEvents = async () => {
        setLoading(true);
        try {
            const res = await fetch('http://localhost:8080/api/events');
            const data = await res.json();
            setEvents(data);
        } catch (err) {
            console.error("Error loading events", err);
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
            console.error("Error loading semester plan", err);
        }
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({ ...formData, [name]: type === 'checkbox' ? checked : value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        console.log("Submitting Event Data:", formData);
        
        if (!user || !user.email) {
            alert("User session error. Please log in again.");
            return;
        }

        try {
            const res = await fetch('http://localhost:8080/api/events', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ 
                    ...formData, 
                    ownerEmail: user.email, 
                    approved: user.role === 'ADMIN' // Auto-approve only if truly admin
                })
            });
            
            if (res.ok) {
                console.log("Submission successful!");
                alert("Event added successfully!");
                setShowModal(false);
                setFormData({ title: '', description: '', eventDate: '', venue: '', organizer: '', category: 'SOCIAL', semesterPlan: false });
                loadEvents();
                loadSemesterPlan();
            } else {
                const err = await res.text();
                console.error("Submission failed:", err);
                alert("Error: " + err);
            }
        } catch (err) {
            console.error("Network Error:", err);
            alert("Network Error: Could not connect to backend.");
        }
    };

    const handleUploadPlan = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const formData = new FormData();
        formData.append('file', file);
        formData.append('ownerEmail', user.email);

        try {
            const res = await fetch('http://localhost:8080/api/events/upload-plan', {
                method: 'POST',
                body: formData
            });
            if (res.ok) {
                alert("Semester Plan uploaded successfully!");
                loadSemesterPlan();
            } else {
                alert("Upload failed.");
            }
        } catch (err) {
            console.error(err);
            alert("Error connecting to server.");
        }
    };

    const filteredEvents = (viewMode === 'BOARD' ? events : semesterPlan).filter(event => {
        const query = searchQuery.toLowerCase();
        return event.title.toLowerCase().includes(query) || 
               event.eventDate.includes(query) ||
               event.category.toLowerCase().includes(query);
    });

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
                
                {user?.role === 'ADMIN' && (
                    <div className="admin-actions">
                        <label className="upload-label">
                            📁 Upload Plan (XLS)
                            <input type="file" onChange={handleUploadPlan} hidden accept=".xls,.xlsx" />
                        </label>
                        <button className="add-btn" onClick={() => setShowModal(true)}>+ Post Event</button>
                    </div>
                )}
            </div>

            {loading ? <p>Loading...</p> : (
                viewMode === 'BOARD' ? (
                    <div className="event-grid">
                        {filteredEvents.map(event => (
                            <div key={event.id} className="event-card glass-card">
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
                            </div>
                        ))}
                        {events.length === 0 && <p className="empty-msg">No approved events found.</p>}
                    </div>
                ) : (
                    <div className="semester-plan-table-container glass-card">
                        <table className="semester-plan-table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Course / Event</th>
                                    <th>Time / Details</th>
                                    <th>Venue</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredEvents.map(event => (
                                    <tr key={event.id} className={event.category === 'ACADEMIC' ? 'academic-row' : ''}>
                                        <td>{new Date(event.eventDate).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}</td>
                                        <td className="bold-cell">{event.title}</td>
                                        <td>{event.description}</td>
                                        <td>📍 {event.venue}</td>
                                    </tr>
                                ))}
                                {semesterPlan.length === 0 && (
                                    <tr>
                                        <td colSpan="4" className="empty-row">No semester plan items found. Upload or propose one!</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                )
            )}

            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal glass-card">
                        <h2>Post New Event / Plan Item</h2>
                        <form onSubmit={handleSubmit}>
                            <input type="text" name="title" placeholder="Event Title" value={formData.title} onChange={handleInputChange} required />
                            <textarea name="description" placeholder="Short Description" value={formData.description} onChange={handleInputChange} required />
                            <input type="date" name="eventDate" value={formData.eventDate} onChange={handleInputChange} required />
                            <input type="text" name="venue" placeholder="Venue" value={formData.venue} onChange={handleInputChange} required />
                            <input type="text" name="organizer" placeholder="Organizer" value={formData.organizer} onChange={handleInputChange} required />
                            <select name="category" value={formData.category} onChange={handleInputChange}>
                                <option value="ACADEMIC">Academic</option>
                                <option value="SOCIAL">Social</option>
                                <option value="SPORTS">Sports</option>
                                <option value="HOLIDAY">Holiday</option>
                            </select>
                            <label className="checkbox-label">
                                <input type="checkbox" name="semesterPlan" checked={formData.semesterPlan} onChange={handleInputChange} />
                                Add to Semester Plan
                            </label>
                            <div className="form-actions">
                                <button type="button" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="submit-btn">Propose</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CampusEventBoard;
