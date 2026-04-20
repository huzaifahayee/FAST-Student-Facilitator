import React, { useState, useEffect } from 'react';
import './PastPapers.css';

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
  const [papers, setPapers] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterExam, setFilterExam] = useState('ALL');
  
  const [showUploadForm, setShowUploadForm] = useState(false);
  const [formData, setFormData] = useState({
    courseName: '', courseCode: '', semesterYear: '', examType: 'MIDTERM',
    instructorName: '', googleDriveLink: ''
  });
  const [formErrors, setFormErrors] = useState({});

  const [selectedPaper, setSelectedPaper] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [reportReason, setReportReason] = useState('');
  const [showReportForm, setShowReportForm] = useState(false);
  const [reportError, setReportError] = useState('');
  
  useEffect(() => {
    fetchPapers();
  }, [searchQuery]);

  const fetchPapers = () => {
    const url = searchQuery.trim() 
      ? `http://localhost:8080/api/past-papers/search?query=${encodeURIComponent(searchQuery)}`
      : `http://localhost:8080/api/past-papers`;
      
    fetch(url)
      .then(res => res.json())
      .then(data => {
        setPapers(Array.isArray(data) ? data : []);
      })
      .catch(err => console.error("Error fetching papers: ", err));
  };

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
    if (!formData.instructorName.trim()) errors.instructorName = "Instructor name is required";
    
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
        instructorName: '', googleDriveLink: ''
      });
      fetchPapers(); // refresh
    })
    .catch(err => {
      alert("Error: " + err.message);
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
      .catch(err => alert("Could not fetch details: " + err));
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
      .catch(err => alert("Error: " + err.message));
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
    .catch(err => alert(err.message));
  };

  const postComment = () => {
    if(!newComment.trim()) {
      alert("Comment cannot be empty"); 
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
    .catch(err => alert(err.message));
  };

  const deleteComment = (commentId, ownerEmail) => {
    if(ownerEmail !== user.email) {
      alert("You can only delete your own comments");
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
    }).catch(err => alert(err.message));
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
      alert("Report submitted.");
    })
    .catch(err => setReportError(err.message));
  };

  const filteredPapers = filterExam === 'ALL' 
    ? papers 
    : papers.filter(p => p.examType === filterExam);

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
            <select name="examType" value={formData.examType} onChange={handleInputChange}>
              <option value="MIDTERM">Midterm</option>
              <option value="FINAL">Final</option>
              <option value="QUIZ">Quiz/Assignment</option>
            </select>
          </div>

          <div className="form-group">
            <input type="text" name="instructorName" placeholder="Instructor Name" 
              value={formData.instructorName} onChange={handleInputChange} />
            {formErrors.instructorName && <span className="error-text">{formErrors.instructorName}</span>}
          </div>

          <div className="form-group">
            <input type="text" name="googleDriveLink" placeholder="Google Drive Link (https://...)" 
              value={formData.googleDriveLink} onChange={handleInputChange} />
            {formErrors.googleDriveLink && <span className="error-text">{formErrors.googleDriveLink}</span>}
            <small className="hint-text">For standard courses, the permanent link will be used automatically.</small>
          </div>

          <button type="submit" className="btn-submit">Submit for Approval</button>
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
        <select value={filterExam} onChange={(e) => setFilterExam(e.target.value)} className="exam-filter">
          <option value="ALL">All Exams</option>
          <option value="MIDTERM">Midterms</option>
          <option value="FINAL">Finals</option>
          <option value="QUIZ">Quizzes</option>
        </select>
      </div>

      <div className="papers-grid">
        {filteredPapers.length === 0 ? (
          <p className="empty-msg">No approved papers found.</p>
        ) : (
          filteredPapers.map(paper => (
            <div key={paper.id} className="glass-card paper-item" onClick={() => loadDetails(paper)}>
              <div className="card-top">
                <span className={`exam-badge ${paper.examType.toLowerCase()}`}>{paper.examType}</span>
                {paper.flagged && <span className="flag-badge" title="Flagged for review">🚩</span>}
              </div>
              <h3 className="course-title">{paper.courseName}</h3>
              <p className="course-code">{paper.courseCode} &bull; {paper.semesterYear}</p>
              <p className="instructor">Instructor: {paper.instructorName}</p>
              <div className="card-bot">
                <span className="rating">⭐ {paper.averageRating} ({paper.ratingCount})</span>
                <span className="uploader">By {paper.ownerName.split(' ')[0]}</span>
              </div>
            </div>
          ))
        )}
      </div>

      {selectedPaper && (
        <div className="modal-backdrop" onClick={() => { setSelectedPaper(null); setShowReportForm(false); }}>
          <div className="modal paper-detail-modal" onClick={e => e.stopPropagation()}>
            <button className="close-btn" onClick={() => setSelectedPaper(null)}>&times;</button>
            
            <h2>{selectedPaper.courseName} ({selectedPaper.courseCode})</h2>
            <p className="metadata">
              {selectedPaper.semesterYear} • {selectedPaper.examType} • Inst: {selectedPaper.instructorName}
            </p>

            <button className="btn-primary open-drive" onClick={() => openDriveFolder(selectedPaper.id)}>
              Open Google Drive Folder
            </button>

            <div className="rating-section">
              <h4>Rate this paper</h4>
              <div className="stars">
                {[1,2,3,4,5].map(v => (
                   <span key={v} className="star-btn" onClick={() => ratePaper(v)}>⭐</span>
                ))}
              </div>
              <p className="avg-rating">Current Average: {selectedPaper.averageRating} from {selectedPaper.ratingCount} reviews</p>
            </div>

            <div className="comments-section">
              <h4>Comments & Tips</h4>
              <div className="comments-list">
                {comments.map(c => (
                  <div key={c.id} className="comment">
                    <div className="comm-head">
                      <strong>{c.studentEmail.split('@')[0]}</strong> 
                      <span className="date">{new Date(c.postedAt).toLocaleDateString()}</span>
                    </div>
                    <p>{c.content}</p>
                    {c.studentEmail === user.email && (
                      <span className="delete-comm" onClick={() => deleteComment(c.id, c.studentEmail)}>Delete</span>
                    )}
                  </div>
                ))}
              </div>
              
              <div className="add-comment">
                <input 
                   type="text" 
                   value={newComment} 
                   onChange={e => setNewComment(e.target.value)} 
                   placeholder="Add a comment..." 
                />
                <button onClick={postComment}>Post</button>
              </div>
            </div>

            <div className="report-action">
               <button className="btn-sm-danger" onClick={() => setShowReportForm(!showReportForm)}>Report this paper</button>
               {showReportForm && (
                 <div className="report-box">
                    <textarea 
                      value={reportReason} 
                      onChange={e => setReportReason(e.target.value)}
                      placeholder="Why are you reporting this? (e.g. Broken link, irrelevant file)" 
                    />
                    {reportError && <span className="error-text">{reportError}</span>}
                    <button className="btn-submit" onClick={reportPaper}>Submit Report</button>
                 </div>
               )}
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
