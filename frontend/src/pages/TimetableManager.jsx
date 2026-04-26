import React, { useState, useEffect } from 'react';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import './TimetableManager.css';

const TimetableManager = ({ user }) => {
  const [department, setDepartment] = useState('CS');
  const [batch, setBatch] = useState('24');
  const [section, setSection] = useState('A');
  const [day, setDay] = useState('Monday');
  const [timetable, setTimetable] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Auto-load when filters change
  useEffect(() => {
    loadTimetable();
  }, [department, batch, section]);

  // Admin upload state
  const [uploadUrl, setUploadUrl] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"];
  const timeSlots = ["08:30-09:50", "10:00-11:20", "11:30-12:50", "01:00-02:20", "02:30-03:50", "04:00-05:20"];

  const loadTimetable = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`http://localhost:8080/api/timetable/section?department=${department}&batch=${batch}&section=${section}`);
      if (!res.ok) throw new Error('Failed to fetch timetable');
      const data = await res.json();
      setTimetable(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!uploadUrl && !selectedFile) {
      setUploadError("Please provide a valid Google Sheet CSV URL or select a local CSV file.");
      return;
    }
    setUploading(true);
    setUploadError(null);
    
    try {
      let res;
      if (selectedFile) {
        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('ownerName', user.name);
        formData.append('ownerEmail', user.email);
        
        res = await fetch('http://localhost:8080/api/timetable/upload-file', {
          method: 'POST',
          body: formData
        });
      } else {
        res = await fetch(`http://localhost:8080/api/timetable/upload?url=${encodeURIComponent(uploadUrl)}&ownerName=${encodeURIComponent(user.name)}&ownerEmail=${encodeURIComponent(user.email)}`, {
          method: 'POST'
        });
      }

      if (!res.ok) {
        const errText = await res.text();
        throw new Error(errText || 'Failed to upload timetable');
      }
      alert("Timetable successfully uploaded and parsed!");
      setUploadUrl('');
      setSelectedFile(null);
      loadTimetable(); // reload after upload
    } catch (err) {
      setUploadError(err.message);
    } finally {
      setUploading(false);
    }
  };

  const exportAsImage = async () => {
    const element = document.getElementById('timetable-weekly-export');
    if (!element) return;
    
    // Ensure the element is temporarily visible for capture
    element.style.display = 'block';
    const canvas = await html2canvas(element, { scale: 2 });
    element.style.display = 'none';
    
    const image = canvas.toDataURL('image/png');
    const link = document.createElement('a');
    link.href = image;
    link.download = `Timetable_${department}_${batch}_${section}_Weekly.png`;
    link.click();
  };

  const exportAsPDF = async () => {
    const element = document.getElementById('timetable-weekly-export');
    if (!element) return;
    
    element.style.display = 'block';
    const canvas = await html2canvas(element, { scale: 2 });
    element.style.display = 'none';
    
    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF('landscape', 'pt', 'a4');
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
    
    pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
    pdf.save(`Timetable_${department}_${batch}_${section}_Weekly.pdf`);
  };

  const getEntryForSlot = (day, slot) => {
    return timetable.find(t => 
      t.dayOfWeek.toLowerCase() === day.toLowerCase() && 
      (t.startTime + "-" + t.endTime) === slot
    );
  };

  // Helper to check if a class is currently ongoing
  const isOngoing = (day, slot) => {
    return false;
  };

  return (
    <div className="timetable-page custom-theme">
      <header className="timetable-header">
        <h2>Timetable Manager</h2>
        <p>View and export your official weekly class schedule.</p>
      </header>

      {user?.role === 'ADMIN' && (
        <section className="admin-upload-section glass-card">
          <h3>Admin Controls: Upload Timetable</h3>
          {uploadError && <p className="error-text">{uploadError}</p>}
          <form onSubmit={handleUpload} className="upload-form">
            <div className="upload-inputs" style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
              <div className="input-row">
                <label style={{color: '#fff', fontSize: '0.9rem'}}>Option 1: Paste CSV URL</label>
                <input 
                  type="url" 
                  placeholder="Google Sheet CSV Link" 
                  value={uploadUrl} 
                  onChange={e => { setUploadUrl(e.target.value); if(e.target.value) setSelectedFile(null); }} 
                  className="url-input"
                  style={{marginTop: '0.5rem'}}
                />
              </div>
              
              <div className="divider" style={{color: '#666', textAlign: 'center', fontSize: '0.8rem'}}>— OR —</div>

              <div className="input-row">
                <label style={{color: '#fff', fontSize: '0.9rem'}}>Option 2: Upload local CSV or Excel (.xlsx/.xls)</label>
                <input 
                  type="file" 
                  accept=".csv, .xlsx, .xls"
                  onChange={e => { setSelectedFile(e.target.files[0]); if(e.target.files[0]) setUploadUrl(''); }}
                  style={{marginTop: '0.5rem', color: '#fff'}}
                  className="file-input"
                />
              </div>
            </div>
            <button type="submit" className="primary-btn" disabled={uploading} style={{marginTop: '1.5rem'}}>
              {uploading ? "Uploading..." : "Upload / Update Global Timetable"}
            </button>
          </form>
          <small className="help-text">CSV columns must be: Department, Batch, Section, Day, Time, Course, Room, Instructor</small>
        </section>
      )}

      <section className="timetable-viewer">
        <div className="custom-selectors">
          <div className="select-group">
            <label>Department</label>
            <select value={department} onChange={(e) => setDepartment(e.target.value)}>
              <option value="CS">BS CS</option>
              <option value="SE">BS SE</option>
              <option value="DS">BS DS</option>
              <option value="AI">BS AI</option>
              <option value="CYS">BS CYS</option>
            </select>
          </div>
          <div className="select-group">
            <label>Batch Year</label>
            <select value={batch} onChange={(e) => setBatch(e.target.value)}>
              <option value="23">2023</option>
              <option value="24">2024</option>
              <option value="25">2025</option>
              <option value="26">2026</option>
            </select>
          </div>
          <div className="select-group">
            <label>Day</label>
            <select value={day} onChange={(e) => setDay(e.target.value)}>
              <option value="Monday">Monday</option>
              <option value="Tuesday">Tuesday</option>
              <option value="Wednesday">Wednesday</option>
              <option value="Thursday">Thursday</option>
              <option value="Friday">Friday</option>
            </select>
          </div>
          <div className="select-group">
            <label>Section</label>
            <select value={section} onChange={(e) => setSection(e.target.value)}>
              {['A','B','C','D','E','F','G'].map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        </div>

        <button className="get-timetable-btn" onClick={loadTimetable} disabled={loading}>
          {loading ? "Loading..." : "Get Timetable"}
        </button>
        
        {timetable.length > 0 && (
          <div className="export-actions" style={{marginBottom: '1rem'}}>
            <button className="secondary-btn" onClick={exportAsImage}>📸 Export Weekly Image</button>
            <button className="secondary-btn" onClick={exportAsPDF}>📄 Export Weekly PDF</button>
          </div>
        )}

        {error && <p className="error-text">{error}</p>}

        <div className="custom-table-container">
          {timetable.length > 0 ? (
            <table className="custom-timetable">
              <thead>
                <tr>
                  <th>Course</th>
                  <th>Location</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {timetable
                  .filter(t => t.dayOfWeek.toLowerCase() === day.toLowerCase())
                  .sort((a, b) => a.startTime.localeCompare(b.startTime))
                  .map(t => (
                  <tr key={t.id}>
                    <td>{t.courseName}</td>
                    <td>{t.roomNumber}</td>
                    <td>{t.startTime}-{t.endTime}</td>
                  </tr>
                ))}
                {timetable.filter(t => t.dayOfWeek.toLowerCase() === day.toLowerCase()).length === 0 && (
                  <tr>
                    <td colSpan="3" style={{textAlign: 'center', padding: '2rem'}}>No classes found for {day}.</td>
                  </tr>
                )}
              </tbody>
            </table>
          ) : (
            !loading && <div className="empty-state">
              <p style={{color: '#fff'}}>No timetable found for the selected section.</p>
            </div>
          )}
        </div>

        {/* HIDDEN WEEKLY GRID FOR EXPORT */}
        <div id="timetable-weekly-export" style={{ display: 'none', padding: '40px', backgroundColor: '#121212', width: '1200px' }}>
          <div style={{ textAlign: 'center', marginBottom: '30px' }}>
            <h1 style={{ color: '#FFC107', margin: '0' }}>{department} - Batch {batch} - Section {section}</h1>
            <p style={{ color: '#fff', fontSize: '18px' }}>Weekly Class Schedule</p>
          </div>
          <table className="weekly-export-table" style={{ width: '100%', borderCollapse: 'collapse', border: '2px solid #FFC107' }}>
            <thead>
              <tr style={{ backgroundColor: '#1a1a1a' }}>
                <th style={{ border: '1px solid #FFC107', padding: '15px', color: '#FFC107' }}>Time / Day</th>
                {days.map(d => (
                  <th key={d} style={{ border: '1px solid #FFC107', padding: '15px', color: '#FFC107' }}>{d}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {timeSlots
                .filter(slot => days.some(day => getEntryForSlot(day, slot)))
                .map(slot => (
                <tr key={slot}>
                  <td style={{ border: '1px solid #FFC107', padding: '15px', color: '#FFC107', fontWeight: 'bold', backgroundColor: '#1a1a1a' }}>{slot}</td>
                  {days.map(d => {
                    const entry = getEntryForSlot(d, slot);
                    return (
                      <td key={d} style={{ border: '1px solid #FFC107', padding: '10px', color: '#fff', backgroundColor: entry ? '#2a2a2a' : 'transparent', textAlign: 'center', fontSize: '14px' }}>
                        {entry ? (
                          <>
                            <div style={{ fontWeight: 'bold', color: '#FFC107', marginBottom: '5px' }}>{entry.courseName}</div>
                            <div style={{ fontSize: '12px', opacity: 0.8 }}>{entry.roomNumber}</div>
                            <div style={{ fontSize: '11px', fontStyle: 'italic', marginTop: '3px' }}>{entry.instructorName}</div>
                          </>
                        ) : '-'}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: '20px', textAlign: 'right', color: '#888', fontSize: '12px' }}>
            Generated by FAST Student Facilitator (FSF)
          </div>
        </div>
      </section>
    </div>
  );
};

export default TimetableManager;
