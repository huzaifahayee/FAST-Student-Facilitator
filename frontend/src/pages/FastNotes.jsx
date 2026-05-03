import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './FastNotes.css';

const API_BASE_URL = 'http://localhost:8080/api/notes';

function FastNotes({ user }) {
  const { showAlert } = useFsfDialog();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [notes, setNotes] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('');
  const [courseCodeFilter, setCourseCodeFilter] = useState('');
  const [subjects, setSubjects] = useState([]);
  const [courseCodes, setCourseCodes] = useState([]);
  
  const [showModal, setShowModal] = useState(false);
  const [flashNoteId, setFlashNoteId] = useState(null);
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

        // Populate dropdowns from all notes when no filter is active.
        // Deduplicate case-insensitively: "OOP" and "oop" → one entry.
        if (!subjectFilter && !searchKeyword && !courseCodeFilter) {
          const subjectMap = new Map();
          data.forEach(n => {
            if (n.subjectName) {
              const key = n.subjectName.toLowerCase();
              if (!subjectMap.has(key)) subjectMap.set(key, n.subjectName.toUpperCase());
            }
          });
          setSubjects([...subjectMap.values()]);

          const codeMap = new Map();
          data.forEach(n => {
            if (n.courseCode) {
              const key = n.courseCode.toLowerCase();
              if (!codeMap.has(key)) codeMap.set(key, n.courseCode.toUpperCase());
            }
          });
          setCourseCodes([...codeMap.values()]);
        }
      }
    } catch (err) {
      console.error('Failed to fetch notes', err);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- notes reload
    fetchNotes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchKeyword, subjectFilter, courseCodeFilter]);

  useEffect(() => {
    if (searchParams.get('note')) return;
    const q = searchParams.get('q');
    // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
    if (q !== null) setSearchKeyword((prev) => (q !== prev ? q : prev));
  }, [searchParams]);

  useEffect(() => {
    const raw = searchParams.get('note');
    if (!raw) return;
    const id = parseInt(raw, 10);
    if (!Number.isFinite(id)) return;
    /* eslint-disable react-hooks/set-state-in-effect -- deep-link scroll target */
    setSearchKeyword('');
    setFlashNoteId(id);
    /* eslint-enable react-hooks/set-state-in-effect */
    const next = new URLSearchParams(searchParams);
    next.delete('note');
    const qs = next.toString();
    navigate(`/notes${qs ? `?${qs}` : ''}`, { replace: true });
  }, [searchParams, navigate]);

  useEffect(() => {
    if (!flashNoteId) return;
    const t = window.setTimeout(() => {
      const el = document.getElementById(`note-card-${flashNoteId}`);
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el?.classList.add('deep-link-highlight');
      window.setTimeout(() => el?.classList.remove('deep-link-highlight'), 2200);
      setFlashNoteId(null);
    }, 140);
    return () => window.clearTimeout(t);
  }, [notes, flashNoteId]);

  const subjectOptions = useMemo(
    () => [{ value: '', label: 'All Subjects' }, ...subjects.map((s) => ({ value: s, label: s }))],
    [subjects]
  );
  const courseCodeOptions = useMemo(
    () => [{ value: '', label: 'All Course Codes' }, ...courseCodes.map((c) => ({ value: c, label: c }))],
    [courseCodes]
  );

  const MAX_FILE_MB = 20;

  const handleSubmit = async (e) => {
    e.preventDefault();

    // --- Client-side validation (runs instantly, no network needed) ---
    const file = formData.file;
    if (!file) {
      await showAlert({ title: 'No file selected', message: 'Please choose a PDF or DOCX file to upload.' });
      return;
    }

    const nameLower = file.name.toLowerCase();
    if (!nameLower.endsWith('.pdf') && !nameLower.endsWith('.docx')) {
      await showAlert({
        title: 'Invalid file type',
        message: '🚫 Only PDF or DOCX files are accepted.',
      });
      return;
    }

    if (file.size > MAX_FILE_MB * 1024 * 1024) {
      await showAlert({
        title: 'File too large',
        message: `⚠️ File exceeds the ${MAX_FILE_MB} MB limit. Please upload a smaller file.`,
      });
      return;
    }

    // --- Send to backend ---
    try {
      const payload = new FormData();
      payload.append('title', formData.title);
      payload.append('subjectName', formData.subjectName);
      payload.append('courseCode', formData.courseCode);
      payload.append('studentEmail', user?.email);
      payload.append('file', file);

      const res = await fetch(API_BASE_URL, {
        method: 'POST',
        body: payload
      });

      if (res.ok) {
        setShowModal(false);
        setFormData({ title: '', subjectName: '', courseCode: '', file: null });
        fetchNotes();
      } else {
        let errorData;
        try { errorData = await res.json(); } catch { /* not JSON */ }
        const backendMsg = errorData?.message || errorData?.detail;

        if (res.status === 413) {
          await showAlert({
            title: 'File too large',
            message: backendMsg || `⚠️ File exceeds the ${MAX_FILE_MB} MB limit. Please upload a smaller file.`,
          });
        } else if (res.status === 400) {
          await showAlert({
            title: 'Invalid file type',
            message: backendMsg || '🚫 Only PDF or DOCX files are accepted.',
          });
        } else {
          await showAlert({
            title: 'Upload failed',
            message: backendMsg || '⚠️ Something went wrong. Please try again.',
          });
        }
      }
    } catch (err) {
      console.error(err);
      await showAlert({
        title: 'Network error',
        message: 'Could not reach the server. Please check your connection.',
      });
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

      <div className="fn-controls controls glass-card">
        <input 
          type="text" 
          placeholder="Search course code or subject..." 
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="search-input"
        />
        <IosPickerField
          className="fn-picker"
          value={subjectFilter}
          onChange={(v) => { setSubjectFilter(v); setCourseCodeFilter(''); }}
          options={subjectOptions}
          sheetTitle="Subject"
          minWidth={200}
        />
        <IosPickerField
          className="fn-picker"
          value={courseCodeFilter}
          onChange={(v) => { setCourseCodeFilter(v); setSubjectFilter(''); }}
          options={courseCodeOptions}
          sheetTitle="Course code"
          minWidth={200}
        />
        
        <button type="button" className="primary-btn fn-toolbar-primary" onClick={() => setShowModal(true)}>
          Upload Note
        </button>
      </div>

      <div className="notes-list">
        {notes.length === 0 ? (
          <p className="no-items">No notes found.</p>
        ) : (
          notes.map(note => (
            <div key={note.id} id={`note-card-${note.id}`} className="glass-card note-card">
              
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
        <div className="fn-modal-overlay" role="presentation" onClick={() => setShowModal(false)}>
          <div className="fn-modal-content glass-card" role="dialog" aria-labelledby="fn-upload-title" onClick={(e) => e.stopPropagation()}>
            <h2 id="fn-upload-title">Upload Note</h2>
            <form className="fsf-form" onSubmit={handleSubmit}>
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
                <span className="fn-field-label">Upload note (PDF or DOCX)</span>
                <label className="ios-file-field">
                  <input
                    required
                    className="ios-file-field-input"
                    type="file"
                    accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    onChange={(e) => setFormData({ ...formData, file: e.target.files?.[0] ?? null })}
                  />
                  <span className="ios-file-field-btn">Choose Media</span>
                  <span className="ios-file-field-name">{formData.file?.name || 'No file chosen'}</span>
                </label>
              </div>
              
              <div className="fn-modal-actions">
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
