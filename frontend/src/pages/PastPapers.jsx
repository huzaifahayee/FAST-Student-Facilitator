import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './PastPapers.css';
import '../styles/IosMenuPicker.css';

const EXAM_FILTER_OPTIONS = [
  { value: 'ALL', label: 'All Exams' },
  { value: 'MIDTERM', label: 'Midterms' },
  { value: 'FINAL', label: 'Finals' },
  { value: 'QUIZ', label: 'Quizzes' },
];

const EXAM_UPLOAD_OPTIONS = [
  { value: 'MIDTERM', label: 'Midterm' },
  { value: 'FINAL', label: 'Final' },
  { value: 'QUIZ', label: 'Quiz / Assignment' },
];

const GOOGLE_DRIVE_LINKS = {
  "Database Systems": "https://drive.google.com/drive/folders/1b8syVaHAJ1jCM70t8LvxRqeaAoGeHyK9",
  "Applied Physics": "https://drive.google.com/drive/folders/1Iy6uJGHFmvTd3pMe1jkKuEFUkCOc0IJN",
  "Calculus": "https://drive.google.com/drive/folders/1PvyVrVdYE5DaMN1LGM-Zk5UmECXbcPvd",
  "Discrete Structures": "https://drive.google.com/drive/folders/1VhK2MaXjLo-O5oGzOM6v5-kDYg94Ry54",
  "Cloud Computing": "https://drive.google.com/drive/folders/1qHoYQsuz-jkgLdozkh1HQb_DcTbPdWBR",
  "Digital Logic Design": "https://drive.google.com/drive/folders/1SZ2HkZJ02xq9oy5_RdFOeAur7IiSvHaN",
  "Digital Logic Design Lab": "https://drive.google.com/drive/folders/1MtjPz-sLc0WhQFeQHmsnRUUxwpBdfjAv",
  "Islamic Studies": "https://drive.google.com/drive/folders/1mw8pSWsPhIFM9rRcSQQWF-OfYKvqz8WE",
  "Linear Algebra": "https://drive.google.com/drive/folders/1SUkRnSiQkyVHohHoIDXOZ6T_gWkFHyrF",
  "Probability and Statistics": "https://drive.google.com/drive/folders/1knOsNuexBD1a86aFrgHUp4gym6U6ja1V"
};

export default function PastPapers({ user }) {
  const { showAlert } = useFsfDialog();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [papers, setPapers] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterExam, setFilterExam] = useState('ALL');
  const [examMenuOpen, setExamMenuOpen] = useState(false);
  const examMenuRef = useRef(null);

  const [showUploadForm, setShowUploadForm] = useState(false);
  const [formData, setFormData] = useState({
    courseName: '', courseCode: '', semesterYear: '', examType: 'MIDTERM',
    googleDriveLink: ''
  });
  const [formErrors, setFormErrors] = useState({});

  const [selectedPaper, setSelectedPaper] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [reportReason, setReportReason] = useState('');
  const [showReportForm, setShowReportForm] = useState(false);
  const [reportError, setReportError] = useState('');

  const fetchPapers = () => {
    const url = searchQuery.trim()
      ? `http://localhost:8080/api/past-papers/search?query=${encodeURIComponent(searchQuery)}`
      : `http://localhost:8080/api/past-papers`;

    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        setPapers(Array.isArray(data) ? data : []);
      })
      .catch((err) => console.error('Error fetching papers: ', err));
  };

  useEffect(() => {
    fetchPapers();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reload list when query changes
  }, [searchQuery]);

  useEffect(() => {
    if (searchParams.get('paper')) return;
    const q = searchParams.get('q');
    // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
    if (q !== null) setSearchQuery((prev) => (q !== prev ? q : prev));
  }, [searchParams]);

  useEffect(() => {
    const raw = searchParams.get('paper');
    if (!raw) return;
    const id = parseInt(raw, 10);
    if (!Number.isFinite(id)) return;

    fetch(`http://localhost:8080/api/past-papers/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error('Paper not found');
        return res.json();
      })
      .then((data) => {
        setSelectedPaper(data.paper);
        setComments(data.comments || []);
      })
      .catch((err) =>
        void showAlert({
          title: 'Could not open paper',
          message: String(err?.message || err),
        })
      )
      .finally(() => {
        const next = new URLSearchParams(searchParams);
        next.delete('paper');
        const qs = next.toString();
        navigate(`/past-papers${qs ? `?${qs}` : ''}`, { replace: true });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- showAlert stable from dialog provider
  }, [searchParams, navigate]);

  useEffect(() => {
    if (!examMenuOpen) return;
    const onDoc = (e) => {
      if (examMenuRef.current && !examMenuRef.current.contains(e.target)) {
        setExamMenuOpen(false);
      }
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setExamMenuOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [examMenuOpen]);

  const handleInputChange = (e) => {
    setFormData({...formData, [e.target.name]: e.target.value});
    setFormErrors({...formErrors, [e.target.name]: ''}); // clear error as they type
  };

  const handleUpload = (e) => {
    e.preventDefault();
    const errors = {};
    if (!formData.courseName.trim()) errors.courseName = "Course name is required";
    if (!formData.courseCode.trim()) errors.courseCode = "Course code is required";
    if (!formData.semesterYear.trim()) errors.semesterYear = "Semester/Year is required";
    
    if (!formData.googleDriveLink.trim()) {
      errors.googleDriveLink = "Link is required";
    } else if (!formData.googleDriveLink.startsWith("https://")) {
      errors.googleDriveLink = "Link must start with https://";
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    const payload = {
      ...formData,
      instructorName: 'N/A',
      ownerEmail: user.email,
      ownerName: user.name
    };

    fetch('http://localhost:8080/api/past-papers', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    .then(async (res) => {
      if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Upload failed");
      }
      return res.json();
    })
    .then(() => {
      setShowUploadForm(false);
      setFormData({
        courseName: '', courseCode: '', semesterYear: '', examType: 'MIDTERM',
        googleDriveLink: ''
      });
      fetchPapers(); // refresh
    })
    .catch((err) => {
      void showAlert({ title: 'Upload failed', message: 'Error: ' + err.message });
    });
  };

  const loadDetails = (paper) => {
    fetch(`http://localhost:8080/api/past-papers/${paper.id}`)
      .then(res => {
        if (!res.ok) throw new Error("Paper not found");
        return res.json();
      })
      .then(data => {
        setSelectedPaper(data.paper);
        setComments(data.comments || []);
      })
      .catch((err) =>
        void showAlert({
          title: 'Could not load paper',
          message: 'Could not fetch details: ' + err,
        })
      );
  };

  const openDriveFolder = (paperId) => {
    fetch(`http://localhost:8080/api/past-papers/${paperId}/download`)
      .then(res => {
         if (!res.ok) throw new Error("Download tracking failed");
         return res.json();
      })
      .then(data => {
         window.open(data.googleDriveLink, '_blank');
      })
      .catch((err) =>
        void showAlert({ title: 'Error', message: 'Error: ' + err.message })
      );
  };

  const ratePaper = (rating) => {
    if (!selectedPaper) return;
    fetch(`http://localhost:8080/api/past-papers/${selectedPaper.id}/rate`, {
      method: 'POST',
      headers:{ 'Content-Type': 'application/json' },
      body: JSON.stringify({ studentEmail: user.email, rating })
    })
    .then(res => {
      if(!res.ok) throw new Error("Rating failed");
      return res.json();
    })
    .then(data => {
      setSelectedPaper(data);
      setPapers(papers.map(p => p.id === data.id ? data : p));
    })
    .catch((err) => void showAlert({ title: 'Rating failed', message: err.message }));
  };

  const postComment = () => {
    if (!newComment.trim()) {
      void showAlert({ title: 'Comment', message: 'Comment cannot be empty.' });
      return;
    }
    fetch(`http://localhost:8080/api/past-papers/${selectedPaper.id}/comments`, {
      method: 'POST',
      headers:{ 'Content-Type': 'application/json' },
      body: JSON.stringify({ studentEmail: user.email, content: newComment })
    })
    .then(res => {
      if(!res.ok) throw new Error("Failed to post comment");
      return res.json();
    })
    .then(data => {
      setComments([...comments, data]);
      setNewComment('');
    })
    .catch((err) =>
      void showAlert({ title: 'Comment failed', message: err.message })
    );
  };

  const deleteComment = (commentId, ownerEmail) => {
    if (ownerEmail !== user.email) {
      void showAlert({
        title: 'Cannot delete',
        message: 'You can only delete your own comments.',
      });
      return;
    }
    fetch(`http://localhost:8080/api/past-papers/comments/${commentId}?studentEmail=${encodeURIComponent(user.email)}`, {
      method: 'DELETE'
    }).then(res => {
      if(!res.ok) {
        if(res.status === 403) throw new Error("You can only delete your own comments");
        throw new Error("Failed to delete comment");
      }
      setComments(comments.filter(c => c.id !== commentId));
    }).catch((err) => void showAlert({ title: 'Error', message: err.message }));
  };

  const reportPaper = () => {
    if(!reportReason.trim()){
      setReportError("Reason cannot be empty");
      return;
    }
    fetch(`http://localhost:8080/api/past-papers/${selectedPaper.id}/report`, {
      method: 'POST',
      headers:{ 'Content-Type': 'application/json' },
      body: JSON.stringify({ reporterEmail: user.email, reason: reportReason })
    })
    .then(async res => {
      if(!res.ok) {
        const txt = await res.text();
        throw new Error(txt || "Failed to report");
      }
      return res.json();
    })
    .then(() => {
      setShowReportForm(false);
      setReportReason('');
      setReportError('');
      // Update local state to reflect it's flagged
      const updated = {...selectedPaper, flagged: true};
      setSelectedPaper(updated);
      setPapers(papers.map(p => p.id === updated.id ? updated : p));
      void showAlert({ title: 'Report submitted', message: 'Report submitted.' });
    })
    .catch(err => setReportError(err.message));
  };

  const filteredPapers = filterExam === 'ALL' 
    ? papers 
    : papers.filter(p => p.examType === filterExam);

  const examTriggerLabel =
    EXAM_FILTER_OPTIONS.find((o) => o.value === filterExam)?.label ?? 'All Exams';

  return (
    <div className="past-papers-container">
      <div className="header-actions">
        <h1>Past Papers</h1>
        <button className="btn-primary" onClick={() => setShowUploadForm(!showUploadForm)}>
          {showUploadForm ? 'Cancel' : 'Upload Paper'}
        </button>
      </div>

      {showUploadForm && (
        <form className="upload-form glass-card" onSubmit={handleUpload}>
          <h3>Upload a Past Paper</h3>
          
          <div className="form-group">
            <input type="text" name="courseName" placeholder="Course Name (e.g. Database Systems)" 
              value={formData.courseName} onChange={handleInputChange} />
            {formErrors.courseName && <span className="error-text">{formErrors.courseName}</span>}
          </div>

          <div className="form-group">
            <input type="text" name="courseCode" placeholder="Course Code (e.g. CS-201)" 
              value={formData.courseCode} onChange={handleInputChange} />
            {formErrors.courseCode && <span className="error-text">{formErrors.courseCode}</span>}
          </div>

          <div className="form-group">
            <input type="text" name="semesterYear" placeholder="Semester & Year (e.g. Fall 2023)" 
              value={formData.semesterYear} onChange={handleInputChange} />
            {formErrors.semesterYear && <span className="error-text">{formErrors.semesterYear}</span>}
          </div>

          <div className="form-group">
            <span className="upload-field-label">Exam type</span>
            <IosPickerField
              className="pp-exam-picker"
              value={formData.examType}
              onChange={(v) => setFormData({ ...formData, examType: v })}
              options={EXAM_UPLOAD_OPTIONS}
              sheetTitle="Exam type"
            />
          </div>


          <div className="form-group">
            <input type="text" name="googleDriveLink" placeholder="Google Drive Link (https://...)" 
              value={formData.googleDriveLink} onChange={handleInputChange} />
            {formErrors.googleDriveLink && <span className="error-text">{formErrors.googleDriveLink}</span>}
            <small className="hint-text">For standard courses, the permanent link will be used automatically.</small>
          </div>

          <div className="upload-form-actions">
            <button type="submit" className="btn-submit">Submit for Approval</button>
          </div>
        </form>
      )}

      <div className="filters-bar">
        <input 
          type="text" 
          placeholder="Search by course name or code..." 
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)} 
          className="search-input"
        />
        <div className="ios-category-dropdown" ref={examMenuRef}>
          <button
            type="button"
            className={`ios-category-dropdown-trigger${examMenuOpen ? ' is-open' : ''}`}
            aria-haspopup="listbox"
            aria-expanded={examMenuOpen}
            onClick={() => setExamMenuOpen((o) => !o)}
          >
            <span className="ios-category-trigger-label">{examTriggerLabel}</span>
            <span className="ios-category-trigger-chevron" aria-hidden>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 9l6 6 6-6" />
              </svg>
            </span>
          </button>
          {examMenuOpen && (
            <div className="ios-category-dropdown-sheet">
              <div className="ios-category-dropdown-panel" role="listbox" aria-label="Filter by exam type">
                <p className="ios-category-dropdown-title">Exams</p>
                <div className="ios-category-dropdown-list">
                  {EXAM_FILTER_OPTIONS.map(({ value, label }) => (
                    <button
                      key={value}
                      type="button"
                      role="option"
                      aria-selected={filterExam === value}
                      className={`ios-category-option${filterExam === value ? ' is-selected' : ''}`}
                      onClick={() => {
                        setFilterExam(value);
                        setExamMenuOpen(false);
                      }}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="papers-grid">
        {filteredPapers.length === 0 ? (
          <p className="empty-msg">No approved papers found.</p>
        ) : (
          filteredPapers.map((paper) => (
            <div
              key={paper.id}
              role="button"
              tabIndex={0}
              className="glass-card paper-item"
              onClick={() => loadDetails(paper)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  loadDetails(paper);
                }
              }}
            >
              <div className="card-top">
                <span className={`exam-badge ${paper.examType.toLowerCase()}`}>{paper.examType}</span>
                <div className="paper-card-top-actions">
                  {paper.flagged && (
                    <span className="flag-badge" title="Flagged for review">
                      🚩
                    </span>
                  )}
                  <span className="paper-card-chevron" aria-hidden>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.25" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9 18l6-6-6-6" />
                    </svg>
                  </span>
                </div>
              </div>
              <h3 className="course-title">{paper.courseName}</h3>
              <p className="course-code">{paper.courseCode} &bull; {paper.semesterYear}</p>
              <div className="card-bot">
                <span className="rating">⭐ {paper.averageRating} ({paper.ratingCount})</span>
                <span className="uploader">By {paper.ownerName.split(' ')[0]}</span>
              </div>
            </div>
          ))
        )}
      </div>

      {selectedPaper && (
        <div
          className="modal-backdrop paper-detail-backdrop"
          onClick={() => { setSelectedPaper(null); setShowReportForm(false); }}
          role="presentation"
        >
          <div
            className="modal paper-detail-modal"
            onClick={e => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="paper-detail-title"
          >
            <header className="paper-detail-modal-header">
              <span className="paper-detail-modal-grabber" aria-hidden />
              <button
                type="button"
                className="paper-detail-modal-done"
                onClick={() => { setSelectedPaper(null); setShowReportForm(false); }}
              >
                Done
              </button>
            </header>

            <div className="paper-detail-modal-body">
              <h2 id="paper-detail-title">{selectedPaper.courseName} ({selectedPaper.courseCode})</h2>
              <p className="metadata">
                {selectedPaper.semesterYear} • {selectedPaper.examType}
              </p>

              <button type="button" className="btn-primary open-drive" onClick={() => openDriveFolder(selectedPaper.id)}>
                Open Google Drive Folder
              </button>

              <div className="rating-section">
                <h4>Rate this paper</h4>
                <div className="stars" role="group" aria-label="Star rating">
                  {[1, 2, 3, 4, 5].map((v) => (
                    <button
                      key={v}
                      type="button"
                      className="star-btn"
                      onClick={() => ratePaper(v)}
                      aria-label={`Rate ${v} stars`}
                    >
                      ⭐
                    </button>
                  ))}
                </div>
                <p className="avg-rating">
                  Current Average: {selectedPaper.averageRating} from {selectedPaper.ratingCount} reviews
                </p>
              </div>

              <div className="comments-section">
                <h4>Comments & Tips</h4>
                <div className="comments-list">
                  {comments.map((c) => (
                    <div key={c.id} className="comment">
                      <div className="comm-head">
                        <strong>{c.studentEmail.split('@')[0]}</strong>
                        <span className="date">{new Date(c.postedAt).toLocaleDateString()}</span>
                      </div>
                      <p>{c.content}</p>
                      {c.studentEmail === user.email && (
                        <button
                          type="button"
                          className="delete-comm"
                          onClick={() => deleteComment(c.id, c.studentEmail)}
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  ))}
                </div>

                <div className="add-comment">
                  <input
                    type="text"
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="Add a comment..."
                    aria-label="Add a comment"
                  />
                  <button type="button" className="paper-detail-post-btn" onClick={postComment}>
                    Post
                  </button>
                </div>
              </div>

              <div className="report-action">
                <button
                  type="button"
                  className="paper-detail-report-link"
                  onClick={() => setShowReportForm(!showReportForm)}
                >
                  Report this paper
                </button>
                {showReportForm && (
                  <div className="report-box">
                    <textarea
                      value={reportReason}
                      onChange={(e) => setReportReason(e.target.value)}
                      placeholder="Why are you reporting this? (e.g. Broken link, irrelevant file)"
                    />
                    {reportError && <span className="error-text">{reportError}</span>}
                    <button type="button" className="btn-submit paper-detail-report-submit" onClick={reportPaper}>
                      Submit Report
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
