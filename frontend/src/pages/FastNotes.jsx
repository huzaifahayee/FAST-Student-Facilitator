import React, { useState, useEffect } from 'react';
import './FastNotes.css';

const API_BASE_URL = 'http://localhost:8080/api/notes';

function FastNotes({ user }) {
  const [notes, setNotes] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');
  const [courseCodeFilter, setCourseCodeFilter] = useState('');
  const [subjects, setSubjects] = useState([]);
  const [courseCodes, setCourseCodes] = useState([]);
  
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    subjectName: '',
    courseCode: '',
    file: null
  });

  const fetchNotes = async () => {
    try {
      const url = new URL(API_BASE_URL);
      if (searchKeyword) url.searchParams.append('keyword', searchKeyword);
      else if (courseCodeFilter) url.searchParams.append('keyword', courseCodeFilter);
      if (subjectFilter) url.searchParams.append('subject', subjectFilter);
      if (user?.email) url.searchParams.append('studentEmail', user.email);

      const response = await fetch(url.toString());
      if (response.ok) {
        const data = await response.json();
        setNotes(data);

        // Populate dropdowns from all notes when no filter is active
        if (!subjectFilter && !searchKeyword && !courseCodeFilter) {
          const uniqueSubjects = [...new Set(data.map(n => n.subjectName))];
          setSubjects(uniqueSubjects);
          const uniqueCodes = [...new Set(data.map(n => n.courseCode))];
          setCourseCodes(uniqueCodes);
        }
      }
    } catch (err) {
      console.error('Failed to fetch notes', err);
    }
  };

  useEffect(() => {
    fetchNotes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchKeyword, subjectFilter, courseCodeFilter]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = new FormData();
      payload.append('title', formData.title);
      payload.append('subjectName', formData.subjectName);
      payload.append('courseCode', formData.courseCode);
      payload.append('studentEmail', user?.email);
      payload.append('file', formData.file);

      const res = await fetch(API_BASE_URL, {
        method: 'POST',
        body: payload
      });

      if (res.ok) {
        setShowModal(false);
        setFormData({ title: '', subjectName: '', courseCode: '', file: null });
        fetchNotes();
      } else {
        // Read the error message from the backend
        let errorData;
        try {
          errorData = await res.json();
        } catch (e) {
          // Fallback if not JSON
        }
        
        const errorMessage = errorData?.message || "Please upload PDF or DOCX only.";
        
        if (res.status === 400) {
          alert(`🚫 CRITICAL ERROR\n\n${errorMessage}`);
        } else {
          alert(`⚠️ Server Error: ${errorMessage}`);
        }
      }
    } catch (err) {
      console.error(err);
      alert("Network Error: Backend is unreachable.");
    }
  };

  const handleVote = async (id, type) => {
    try {
      const email = user?.email || 'test@nu.edu.pk';
      const res = await fetch(`${API_BASE_URL}/${id}/vote?studentEmail=${email}&type=${type}`, { method: 'PUT' });
      if (res.ok) {
        fetchNotes();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const deleteNote = async (id) => {
    try {
      const res = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE' });
      if (res.ok) {
        fetchNotes();
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="fast-notes-container">
      <div className="page-header">
        <h1>FAST-Notes</h1>
        <p>Student-driven PDF note-sharing platform</p>
      </div>

      <div className="controls glass-card">
        <input 
          type="text" 
          placeholder="Search course code or subject..." 
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="search-input"
        />
        <select 
          value={subjectFilter} 
          onChange={(e) => { setSubjectFilter(e.target.value); setCourseCodeFilter(''); }}
          className="filter-select"
        >
          <option value="">All Subjects</option>
          {subjects.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
        <select 
          value={courseCodeFilter} 
          onChange={(e) => { setCourseCodeFilter(e.target.value); setSubjectFilter(''); }}
          className="filter-select"
        >
          <option value="">All Course Codes</option>
          {courseCodes.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        
        <button className="primary-btn" onClick={() => setShowModal(true)}>
          Upload Note
        </button>
      </div>

      <div className="notes-list">
        {notes.length === 0 ? (
          <p className="no-items">No notes found.</p>
        ) : (
          notes.map(note => (
            <div key={note.id} className="glass-card note-card">
              
              <div className="vote-controls">
                <div className="vote-item">
                  <button 
                    className={`vote-btn upvote ${note.userVoteType === 'UPVOTE' ? 'active-up' : ''}`} 
                    onClick={() => handleVote(note.id, 'UPVOTE')}
                  >▲</button>
                  <span className="vote-count">{note.upvotes}</span>
                </div>
                <div className="vote-item">
                  <span className="vote-count">{note.downvotes}</span>
                  <button 
                    className={`vote-btn downvote ${note.userVoteType === 'DOWNVOTE' ? 'active-down' : ''}`} 
                    onClick={() => handleVote(note.id, 'DOWNVOTE')}
                  >▼</button>
                </div>
              </div>

              <div className="note-details">
                <h3>{note.title}</h3>
                <div className="badges">
                  <span className="badge subject">{note.subjectName}</span>
                  <span className="badge course">{note.courseCode}</span>
                </div>
                <div className="meta-info">
                  <span>Uploaded by: {note.studentEmail}</span>
                  <span>Date: {note.uploadDate}</span>
                </div>
              </div>

              <div className="action-buttons">
                <a 
                  href={`${API_BASE_URL}/download/${note.fileUrl}`} 
                  target="_blank" 
                  rel="noreferrer" 
                  className="download-btn"
                >
                  Download
                </a>
                {user && user.role === 'ADMIN' && (
                  <button className="delete-btn" onClick={() => deleteNote(note.id)}>Delete</button>
                )}
              </div>

            </div>
          ))
        )}
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content glass-card">
            <h2>Upload Note</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Note Title</label>
                <input required type="text" value={formData.title} onChange={e => setFormData({...formData, title: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Subject Name (e.g. OOP)</label>
                <input required type="text" value={formData.subjectName} onChange={e => setFormData({...formData, subjectName: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Course Code (e.g. CS-1004)</label>
                <input required type="text" value={formData.courseCode} onChange={e => setFormData({...formData, courseCode: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Upload Note (PDF only)</label>
                <input required type="file" accept=".pdf" onChange={e => setFormData({...formData, file: e.target.files[0]})} />
              </div>
              
              <div className="modal-actions">
                <button type="button" className="cancel-btn" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="submit-btn">Submit</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default FastNotes;
