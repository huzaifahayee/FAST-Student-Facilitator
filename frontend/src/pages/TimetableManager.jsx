import React, { useState, useEffect } from 'react';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './TimetableManager.css';

const TIMETABLE_DEPT_OPTIONS = [
  { value: 'CS', label: 'BS CS' },
  { value: 'SE', label: 'BS SE' },
  { value: 'DS', label: 'BS DS' },
  { value: 'AI', label: 'BS AI' },
  { value: 'CYS', label: 'BS CYS' },
];

const TIMETABLE_BATCH_OPTIONS = [
  { value: '23', label: '2023' },
  { value: '24', label: '2024' },
  { value: '25', label: '2025' },
  { value: '26', label: '2026' },
];

const TIMETABLE_DAY_OPTIONS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].map((d) => ({
  value: d,
  label: d,
}));

const TIMETABLE_SECTION_OPTIONS = ['A', 'B', 'C', 'D', 'E', 'F', 'G'].map((s) => ({
  value: s,
  label: s,
}));

const TimetableManager = ({ user }) => {
  const { showAlert } = useFsfDialog();
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
      await showAlert({
        title: 'Upload complete',
        message: 'Timetable successfully uploaded and parsed!',
      });
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
    <div className="timetable-page">
      <header className="timetable-header">
        <h2>Timetable Manager</h2>
        <p>View and export your official weekly class schedule.</p>
      </header>

      {user?.role === 'ADMIN' && (
        <section className="admin-upload-section glass-card">
          <h3>Admin Controls: Upload Timetable</h3>
          {uploadError && <p className="error-text">{uploadError}</p>}
          <form onSubmit={handleUpload} className="upload-form">
            <div className="upload-inputs">
              <div className="input-row">
                <label className="upload-field-label">Option 1: Paste CSV URL</label>
                <input 
                  type="url" 
                  placeholder="Google Sheet CSV Link" 
                  value={uploadUrl} 
                  onChange={e => { setUploadUrl(e.target.value); if(e.target.value) setSelectedFile(null); }} 
                  className="url-input"
                />
              </div>
              
              <div className="upload-divider">— OR —</div>

              <div className="input-row">
                <span className="upload-field-label">Option 2: Upload local CSV or Excel (.xlsx/.xls)</span>
                <label className="ios-file-field tt-admin-file">
                  <input
                    className="ios-file-field-input"
                    type="file"
                    accept=".csv,.xlsx,.xls,application/vnd.ms-excel,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    onChange={(e) => {
                      const f = e.target.files?.[0];
                      setSelectedFile(f ?? null);
                      if (f) setUploadUrl('');
                    }}
                  />
                  <span className="ios-file-field-btn">Choose Media</span>
                  <span className="ios-file-field-name">{selectedFile?.name || 'No file chosen'}</span>
                </label>
              </div>
            </div>
            <button type="submit" className="primary-btn" disabled={uploading}>
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
            <IosPickerField
              className="tt-picker"
              value={department}
              onChange={setDepartment}
              options={TIMETABLE_DEPT_OPTIONS}
              sheetTitle="Department"
            />
          </div>
          <div className="select-group">
            <label>Batch Year</label>
            <IosPickerField
              className="tt-picker"
              value={batch}
              onChange={setBatch}
              options={TIMETABLE_BATCH_OPTIONS}
              sheetTitle="Batch year"
            />
          </div>
          <div className="select-group">
            <label>Day</label>
            <IosPickerField
              className="tt-picker"
              value={day}
              onChange={setDay}
              options={TIMETABLE_DAY_OPTIONS}
              sheetTitle="Day"
            />
          </div>
          <div className="select-group">
            <label>Section</label>
            <IosPickerField
              className="tt-picker"
              value={section}
              onChange={setSection}
              options={TIMETABLE_SECTION_OPTIONS}
              sheetTitle="Section"
            />
          </div>
        </div>

        <button type="button" className="get-timetable-btn" onClick={loadTimetable} disabled={loading}>
          {loading ? "Loading..." : "Get Timetable"}
        </button>
        
        {timetable.length > 0 && (
          <div className="export-actions">
            <button type="button" className="secondary-btn" onClick={exportAsImage}>📸 Export Weekly Image</button>
            <button type="button" className="secondary-btn" onClick={exportAsPDF}>📄 Export Weekly PDF</button>
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
              <p>No timetable found for the selected section.</p>
            </div>
          )}
        </div>

        {/* HIDDEN WEEKLY GRID FOR EXPORT */}
        <div id="timetable-weekly-export" style={{ display: 'none' }}>
          <div className="export-title">
            <h1>{department} - Batch {batch} - Section {section}</h1>
            <p>Weekly Class Schedule</p>
          </div>
          <table className="weekly-export-table">
            <thead>
              <tr>
                <th>Time / Day</th>
                {days.map(d => (
                  <th key={d}>{d}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {timeSlots
                .filter(slot => days.some(day => getEntryForSlot(day, slot)))
                .map(slot => (
                <tr key={slot}>
                  <td className="slot-label">{slot}</td>
                  {days.map(d => {
                    const entry = getEntryForSlot(d, slot);
                    return (
                      <td key={d} className={entry ? 'cell-filled' : ''}>
                        {entry ? (
                          <>
                            <div className="cell-course">{entry.courseName}</div>
                            <div className="cell-room">{entry.roomNumber}</div>
                            <div className="cell-instructor">{entry.instructorName}</div>
                          </>
                        ) : '-'}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
          <div className="export-footer">
            Generated by FAST Student Facilitator (FSF)
          </div>
        </div>
      </section>
    </div>
  );
};

export default TimetableManager;
