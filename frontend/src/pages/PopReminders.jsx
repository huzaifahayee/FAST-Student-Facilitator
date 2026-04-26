import React, { useState, useEffect } from 'react';
import './PopReminders.css';

const PopReminders = ({ user }) => {
    const [reminders, setReminders] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [formData, setFormData] = useState({
        title: '', reminderTime: '', category: 'GENERAL'
    });

    useEffect(() => {
        loadReminders();
    }, []);

    const loadReminders = async () => {
        try {
            const res = await fetch(`http://localhost:8080/api/reminders?email=${user.email}`);
            const data = await res.json();
            setReminders(data);
        } catch (err) {
            console.error("Error loading reminders", err);
        }
    };

    const handleAdd = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch('http://localhost:8080/api/reminders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...formData, studentEmail: user.email })
            });
            if (res.ok) {
                setShowModal(false);
                setFormData({ title: '', reminderTime: '', category: 'GENERAL' });
                loadReminders();
            }
        } catch (err) {
            alert("Error adding reminder");
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Dismiss this reminder?")) return;
        try {
            const res = await fetch(`http://localhost:8080/api/reminders/${id}`, { method: 'DELETE' });
            if (res.ok) loadReminders();
        } catch (err) {
            alert("Error deleting reminder");
        }
    };

    const isUpcoming = (time) => new Date(time) > new Date();

    return (
        <div className="reminders-page">
            <div className="header-actions">
                <h1>Pop Reminders</h1>
                <button className="add-btn" onClick={() => setShowModal(true)}>+ New Reminder</button>
            </div>

            <div className="reminder-list">
                {reminders.map(r => (
                    <div key={r.id} className={`reminder-card glass-card ${!isUpcoming(r.reminderTime) ? 'overdue' : ''}`}>
                        <div className="status-indicator"></div>
                        <div className="reminder-content">
                            <span className="category">{r.category}</span>
                            <h3>{r.title}</h3>
                            <span className="time">🔔 {new Date(r.reminderTime).toLocaleString()}</span>
                        </div>
                        <button className="dismiss-btn" onClick={() => handleDelete(r.id)}>Dismiss</button>
                    </div>
                ))}
                {reminders.length === 0 && <p className="empty-msg">No reminders set. Add one to stay on track!</p>}
            </div>

            {showModal && (
                <div className="modal-backdrop">
                    <div className="modal glass-card">
                        <h2>Set New Reminder</h2>
                        <form onSubmit={handleAdd}>
                            <input type="text" placeholder="What to remember?" value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} required />
                            <input type="datetime-local" value={formData.reminderTime} onChange={e => setFormData({...formData, reminderTime: e.target.value})} required />
                            <select value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})}>
                                <option value="GENERAL">General</option>
                                <option value="QUIZ">Quiz</option>
                                <option value="ASSIGNMENT">Assignment</option>
                                <option value="MEETING">Meeting</option>
                            </select>
                            <div className="form-actions">
                                <button type="button" onClick={() => setShowModal(false)}>Cancel</button>
                                <button type="submit" className="submit-btn">Set Reminder</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PopReminders;
