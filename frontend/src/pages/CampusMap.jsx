import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './CampusMap.css';

const API = 'http://localhost:8080/api/campus-map';

const VALID_CATEGORIES = [
  'Academic Buildings',
  'Administrative Offices',
  'Facilities',
  'Parking Areas',
  'Sports Areas',
  'Faculty Offices',
];

const CATEGORY_ICONS = {
  'Academic Buildings':     '🏛️',
  'Administrative Offices': '🏢',
  'Facilities':             '🏫',
  'Parking Areas':          '🅿️',
  'Sports Areas':           '⚽',
  'Faculty Offices':        '👨‍🏫',
};

// ─────────────────────────────────────────────────────────────────────────────
// Sub-component: Destination Info Card
// ─────────────────────────────────────────────────────────────────────────────
function DestInfoCard({ location }) {
  if (!location) return null;

  const faculty = location.facultyOffices
    ? location.facultyOffices.split(',').map(s => s.trim()).filter(Boolean)
    : [];
  const rooms = location.classroomNumbers
    ? location.classroomNumbers.split(',').map(s => s.trim()).filter(Boolean)
    : [];

  return (
    <div className="dest-info-card">
      <h4>{location.locationName}</h4>
      {location.description && <p>{location.description}</p>}

      <div className="dest-info-section">
        <span className="dest-info-section-label">Faculty Offices</span>
        {faculty.length > 0
          ? <div className="chips-row">{faculty.map((f, i) => <span key={i} className="chip">{f}</span>)}</div>
          : <span className="dest-info-empty">No faculty offices in this location</span>}
      </div>

      <div className="dest-info-section">
        <span className="dest-info-section-label">Classroom Numbers</span>
        {rooms.length > 0
          ? <div className="chips-row">{rooms.map((r, i) => <span key={i} className="chip">{r}</span>)}</div>
          : <span className="dest-info-empty">No classrooms in this location</span>}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-component: Event Popup
// ─────────────────────────────────────────────────────────────────────────────
function EventPopup({ events, onDismiss }) {
  if (!events || events.length === 0) return null;
  return (
    <div className="event-popup" role="dialog" aria-label="Events at destination">
      <div className="event-popup-header">
        <h5>🎉 Events happening here!</h5>
        <button
          className="event-popup-close"
          onClick={onDismiss}
          aria-label="Dismiss event popup"
        >×</button>
      </div>
      <ul className="event-popup-list" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {events.map((ev, i) => (
          <li key={i} className="event-popup-item">
            <div className="event-popup-item-title">{ev.title}</div>
            <div className="event-popup-item-meta">
              {ev.eventDate}
              {ev.venue ? ` • ${ev.venue}` : ''}
            </div>
            <div className="event-popup-item-desc">{ev.description}</div>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-component: Location Detail Panel (bottom-sheet modal)
// ─────────────────────────────────────────────────────────────────────────────
function LocationDetailPanel({ location, onClose, onGoHere }) {
  if (!location) return null;

  const faculty = location.facultyOffices
    ? location.facultyOffices.split(',').map(s => s.trim()).filter(Boolean)
    : [];
  const rooms = location.classroomNumbers
    ? location.classroomNumbers.split(',').map(s => s.trim()).filter(Boolean)
    : [];

  return (
    <div
      className="location-detail-overlay"
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
      role="dialog"
      aria-modal="true"
      aria-label={`Details for ${location.locationName}`}
    >
      <div className="location-detail-panel">
        <div className="location-detail-top">
          <h3>{location.locationName}</h3>
          <button className="location-detail-close" onClick={onClose} aria-label="Close">×</button>
        </div>

        <span className="location-detail-category">{location.category}</span>

        {location.description && (
          <p className="location-detail-desc">{location.description}</p>
        )}

        <div className="dest-info-section">
          <span className="dest-info-section-label">Faculty Offices</span>
          {faculty.length > 0
            ? <div className="chips-row">{faculty.map((f, i) => <span key={i} className="chip">{f}</span>)}</div>
            : <span className="dest-info-empty">No faculty offices in this location</span>}
        </div>

        <div className="dest-info-section">
          <span className="dest-info-section-label">Classrooms</span>
          {rooms.length > 0
            ? <div className="chips-row">{rooms.map((r, i) => <span key={i} className="chip">{r}</span>)}</div>
            : <span className="dest-info-empty">No classrooms in this location</span>}
        </div>

        <button
          id="go-here-btn"
          className="go-here-btn"
          onClick={() => onGoHere(location)}
        >
          🧭 Get Directions Here
        </button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN COMPONENT
// ─────────────────────────────────────────────────────────────────────────────
const CampusMap = ({ user }) => {
  const { showAlert, showConfirm } = useFsfDialog();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const mapSearchDeepLink = searchParams.get('q');

  // ── Refs ────────────────────────────────────────────────────────────────────
  const directionTopRef = useRef(null);
  const categoryBrowserRef = useRef(null);
  const debounceRef = useRef(null);

  // ── Global location data ────────────────────────────────────────────────────
  const [allLocations, setAllLocations]       = useState([]);
  const [groupedLocations, setGroupedLocations] = useState({});

  // ── Direction Finder state ──────────────────────────────────────────────────
  const [searchType, setSearchType]         = useState('');
  const [fromLocation, setFromLocation]     = useState('');
  const [toLocation, setToLocation]         = useState('');
  const [directionsResult, setDirectionsResult] = useState(null);
  const [currentStep, setCurrentStep]       = useState(0);
  const [activeEvents, setActiveEvents]     = useState([]);
  const [showEventPopup, setShowEventPopup] = useState(false);
  const [validationErrors, setValidationErrors] = useState({ from: '', to: '' });
  const [loading, setLoading]               = useState(false);

  // ── Search bar state ────────────────────────────────────────────────────────
  const [searchQuery, setSearchQuery]     = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [showResults, setShowResults]     = useState(false);

  // ── Accordion state ─────────────────────────────────────────────────────────
  const [expandedCategory, setExpandedCategory] = useState(null);
  const [selectedLocation, setSelectedLocation] = useState(null);

  // ── Suggest form state ──────────────────────────────────────────────────────
  const [showSuggestForm, setShowSuggestForm] = useState(false);
  const [suggestData, setSuggestData]         = useState({ locationName: '', category: '', description: '' });
  const [suggestErrors, setSuggestErrors]     = useState({});
  const [suggestSuccess, setSuggestSuccess]   = useState(false);
  const [suggestSubmitting, setSuggestSubmitting] = useState(false);
  const [showWholeMap, setShowWholeMap] = useState(false);

  useEffect(() => {
    if (!mapSearchDeepLink?.trim()) return;

    let cancelled = false;
    const decoded = decodeURIComponent(mapSearchDeepLink.replace(/\+/g, ' ')).trim();
    if (!decoded) return;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
    setSearchQuery(decoded);

    (async () => {
      try {
        const res = await fetch(`${API}/locations/search?query=${encodeURIComponent(decoded)}`);
        const data = await res.json();
        if (cancelled) return;
        const arr = Array.isArray(data) ? data : [];
        setSearchResults(arr);
        setShowResults(arr.length > 0);
        const exact =
          arr.find((l) => (l.locationName || '').toLowerCase() === decoded.toLowerCase()) ?? arr[0];
        if (exact) {
          setExpandedCategory(exact.category);
          setSelectedLocation(exact);
        }
      } catch {
        /* ignore */
      }
      if (!cancelled) navigate('/campus-map', { replace: true });
    })();

    return () => {
      cancelled = true;
    };
  }, [mapSearchDeepLink, navigate]);

  // ── Admin Route Manager state ───────────────────────────────────────────────
  const [adminRoutes, setAdminRoutes] = useState([]);
  const [adminRouteForm, setAdminRouteForm] = useState({
    fromLocation: '', toLocation: '', stepOrder: 1, imageFileName: '', stepDescription: ''
  });
  const [adminRouteErrors, setAdminRouteErrors] = useState({});
  const [adminRouteSuccess, setAdminRouteSuccess] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [adminToType, setAdminToType] = useState('BLOCK'); // BLOCK, ROOM, FACULTY
  const [newRoomInput, setNewRoomInput] = useState('');
  const [newFacultyInput, setNewFacultyInput] = useState('');
  const [newDescriptionInput, setNewDescriptionInput] = useState('');

  const [definedRoutes, setDefinedRoutes] = useState([]);

  const fetchDefinedRoutes = useCallback(() => {
    // Fetching all routes so we know which paths are actually set up
    fetch(`${API}/admin/routes/all`)
      .then(r => r.json())
      .then(data => {
        setDefinedRoutes(data);
        if (user?.role === 'ADMIN') {
          setAdminRoutes(data);
        }
      })
      .catch(err => console.error('Failed to fetch defined routes', err));
  }, [user]);

  useEffect(() => {
    fetchDefinedRoutes();
  }, [fetchDefinedRoutes]);

  const handleAdminRouteSubmit = async (e) => {
    e.preventDefault();
    const errs = {};
    if (!adminRouteForm.fromLocation) errs.fromLocation = 'Please select From location';
    if (!adminRouteForm.toLocation) errs.toLocation = 'Please select To location';
    if (adminRouteForm.fromLocation && adminRouteForm.toLocation && adminRouteForm.fromLocation === adminRouteForm.toLocation) {
      errs.toLocation = 'From and To cannot be the same';
    }
    if (adminRouteForm.stepOrder < 1) errs.stepOrder = 'Step number must be 1 or greater';
    if (!adminRouteForm.stepDescription.trim()) errs.stepDescription = 'Step description is required';
    
    if (Object.keys(errs).length > 0) {
      setAdminRouteErrors(errs);
      return;
    }

    // ── Duplicate Check ──
    const isDuplicate = definedRoutes.some(r => 
      r.fromLocation === adminRouteForm.fromLocation && 
      r.toLocation === adminRouteForm.toLocation && 
      r.stepOrder === parseInt(adminRouteForm.stepOrder, 10)
    );
    if (isDuplicate) {
      setAdminRouteErrors({ api: `A step #${adminRouteForm.stepOrder} already exists for this path.` });
      return;
    }

    setAdminRouteErrors({});
    
    try {
      let finalImageName = adminRouteForm.imageFileName.trim() || null;

      // If a file is selected, upload it first
      if (selectedFile) {
        setUploading(true);
        const formData = new FormData();
        formData.append('file', selectedFile);

        const uploadRes = await fetch(`${API}/admin/upload-image`, {
          method: 'POST',
          body: formData
        });

        if (uploadRes.ok) {
          const uploadData = await uploadRes.json();
          finalImageName = uploadData.fileName;
        } else {
          const msg = await uploadRes.text();
          setAdminRouteErrors({ api: `Upload failed: ${msg}` });
          setUploading(false);
          return;
        }
        setUploading(false);
      }

      // ── Step 1: Add/Update Metadata if provided ──────────────────────────────
      // We assume the route is being added to the PARENT block.
      // If the admin selected a block and provided new rooms/faculty/description, we update that block.
      const targetBlock = allLocations.find(l => l.locationName === adminRouteForm.toLocation);
      if (targetBlock && (newRoomInput.trim() || newFacultyInput.trim() || newDescriptionInput.trim())) {
        const updatedLoc = { ...targetBlock };
        
        if (newDescriptionInput.trim()) {
          updatedLoc.description = newDescriptionInput.trim();
        }

        if (newRoomInput.trim()) {
          const existing = updatedLoc.classroomNumbers ? updatedLoc.classroomNumbers.split(',').map(s => s.trim()) : [];
          const newOnes = newRoomInput.split(',').map(s => s.trim()).filter(s => s && !existing.includes(s));
          if (newOnes.length > 0) {
            updatedLoc.classroomNumbers = [...existing, ...newOnes].join(', ');
          }
        }
        if (newFacultyInput.trim()) {
          const existing = updatedLoc.facultyOffices ? updatedLoc.facultyOffices.split(',').map(s => s.trim()) : [];
          const newOnes = newFacultyInput.split(',').map(s => s.trim()).filter(s => s && !existing.includes(s));
          if (newOnes.length > 0) {
            updatedLoc.facultyOffices = [...existing, ...newOnes].join(', ');
          }
        }

        await fetch(`${API}/locations/${targetBlock.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(updatedLoc)
        });
      }

      // ── Step 2: Add Route Step ──────────────────────────────────────────────
      const res = await fetch(`${API}/admin/routes/step`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fromLocation: adminRouteForm.fromLocation,
          toLocation: adminRouteForm.toLocation,
          stepOrder: parseInt(adminRouteForm.stepOrder, 10),
          imageFileName: finalImageName,
          stepDescription: adminRouteForm.stepDescription.trim(),
          ownerEmail: user?.email || '',
          ownerName: user?.name || ''
        })
      });
      if (res.ok) {
        setAdminRouteSuccess('Route step added successfully!');
        setAdminRouteForm({ fromLocation: '', toLocation: '', stepOrder: 1, imageFileName: '', stepDescription: '' });
        setSelectedFile(null);
        setNewRoomInput('');
        setNewFacultyInput('');
        setNewDescriptionInput('');
        // Reset file input manually
        const fileInput = document.getElementById('admin-route-file');
        if (fileInput) fileInput.value = '';

        fetchDefinedRoutes();
        // Refresh locations to show new rooms/faculty in dropdowns
        fetch(`${API}/all-locations`).then(r => r.json()).then(data => setAllLocations(data.locations || []));
        
        setTimeout(() => setAdminRouteSuccess(''), 4000);
      } else {
        const msg = await res.text();
        setAdminRouteErrors({ api: msg || 'Failed to add route' });
      }
    } catch {
      setAdminRouteErrors({ api: 'Network error submitting route' });
      setUploading(false);
    }
  };

  const handleDeleteAdminRoute = async (id) => {
    const ok = await showConfirm({
      title: 'Delete route step',
      message: 'Are you sure you want to delete this route step?',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!ok) return;
    try {
      const res = await fetch(`${API}/admin/routes/step/${id}`, { method: 'DELETE' });
      if (res.ok) {
        fetchDefinedRoutes();
      } else {
        await showAlert({ title: 'Delete failed', message: 'Failed to delete route step.' });
      }
    } catch {
      await showAlert({
        title: 'Network error',
        message: 'Network error deleting route step.',
      });
    }
  };

  // ── On mount: fetch flat list + grouped list ────────────────────────────────
  useEffect(() => {
    fetch(`${API}/all-locations`)
      .then(r => r.json())
      .then(data => setAllLocations(data.locations || []))
      .catch(err => console.error('Failed to fetch locations', err));

    fetch(`${API}/locations`)
      .then(r => r.json())
      .then(data => setGroupedLocations(data || {}))
      .catch(err => console.error('Failed to fetch grouped locations', err));
  }, []);

  // ── Derived dropdown options ────────────────────────────────────────────────
  const blockOptions = allLocations.filter(l => l.locationType === 'BLOCK');

  const destinationOptions = React.useMemo(() => {
    if (!searchType) return [];

    // ── Student Filtering Logic ──
    // If not admin, only show destinations that have a defined route from 'fromLocation'
    const isAdmin = user?.role === 'ADMIN';
    const filterByRoute = (locName) => {
      if (isAdmin) return true; // Admins see everything
      if (!fromLocation) return false; // Don't show any destinations until starting point is selected
      
      // Check if any route exists: From -> To (case-insensitive)
      return definedRoutes.some(r => 
        r.fromLocation.toLowerCase() === fromLocation.toLowerCase() && 
        r.toLocation.toLowerCase() === locName.toLowerCase()
      );
    };

    if (searchType === 'BLOCK') {
      return allLocations
        .filter(l => l.locationType === 'BLOCK')
        .filter(l => filterByRoute(l.locationName));
    }
    
    if (searchType === 'FACULTY_OFFICE') {
      const faculties = [];
      allLocations.forEach(loc => {
        if (loc.facultyOffices && filterByRoute(loc.locationName)) {
          loc.facultyOffices.split(',').forEach(fac => {
            const f = fac.trim();
            if (f) faculties.push({ name: f, block: loc.locationName, blockId: loc.blockId });
          });
        }
      });
      return faculties.sort((a, b) => a.name.localeCompare(b.name));
    }

    if (searchType === 'ROOM') {
      const rooms = [];
      allLocations.forEach(loc => {
        if (loc.classroomNumbers && filterByRoute(loc.locationName)) {
          loc.classroomNumbers.split(',').forEach(room => {
            const r = room.trim();
            if (r) rooms.push({ name: r, block: loc.locationName, blockId: loc.blockId });
          });
        }
      });
      return rooms.sort((a, b) => a.name.localeCompare(b.name));
    }
    return [];
  }, [searchType, allLocations, definedRoutes, fromLocation, user]);

  // ── Direction Finder handlers ───────────────────────────────────────────────
  const handleSearchTypeChange = (e) => {
    setSearchType(e.target.value);
    setFromLocation('');
    setToLocation('');
    setDirectionsResult(null);
    setValidationErrors({ from: '', to: '' });
  };

  const handleToChange = (e) => {
    setToLocation(e.target.value);
    setDirectionsResult(null);
    setValidationErrors(prev => ({ ...prev, to: '' }));
  };

  const handleFromChange = (e) => {
    setFromLocation(e.target.value);
    setValidationErrors(prev => ({ ...prev, from: '' }));
  };

  const handleGetDirections = async () => {
    const errors = { from: '', to: '' };
    if (!fromLocation) errors.from = 'Please select your current location';
    if (!toLocation)   errors.to   = 'Please select a destination';
    if (errors.from || errors.to) {
      setValidationErrors(errors);
      return;
    }
    setValidationErrors({ from: '', to: '' });
    setLoading(true);
    setDirectionsResult(null);
    setCurrentStep(0);
    setActiveEvents([]);
    setShowEventPopup(false);

    try {
      const res = await fetch(`${API}/directions?from=${encodeURIComponent(fromLocation)}&to=${encodeURIComponent(toLocation)}`);
      const data = await res.json();
      setDirectionsResult(data);
      if (data.activeEvents && data.activeEvents.length > 0) {
        setActiveEvents(data.activeEvents);
        setShowEventPopup(true);
      }
    } catch (err) {
      console.error('Directions fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  // ── Step navigation ─────────────────────────────────────────────────────────
  const steps = directionsResult?.steps || [];
  const totalSteps = steps.length;
  const currentStepData = steps[currentStep] || null;

  // ── Search bar with 300ms debounce ──────────────────────────────────────────
  const handleSearchChange = (e) => {
    const val = e.target.value;
    setSearchQuery(val);
    clearTimeout(debounceRef.current);
    if (!val.trim()) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await fetch(`${API}/locations/search?query=${encodeURIComponent(val)}`);
        const data = await res.json();
        setSearchResults(Array.isArray(data) ? data : []);
        setShowResults(true);
      } catch (err) {
        console.error('Search error:', err);
      }
    }, 300);
  };

  const clearSearch = () => {
    setSearchQuery('');
    setSearchResults([]);
    setShowResults(false);
  };

  const handleSearchResultClick = (loc) => {
    clearSearch();
    setSelectedLocation(loc);
    // Open correct accordion category
    setExpandedCategory(loc.category);
    // Scroll to browser
    setTimeout(() => {
      categoryBrowserRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 80);
  };

  // ── Accordion ───────────────────────────────────────────────────────────────
  const toggleCategory = (cat) => {
    setExpandedCategory(prev => prev === cat ? null : cat);
  };

  // ── "Get Directions Here" from detail panel ─────────────────────────────────
  const handleGoHere = useCallback((location) => {
    setSelectedLocation(null);
    setSearchType('BLOCK'); // pre-fill type as BLOCK for simple routing
    setToLocation(location.locationName);
    setFromLocation('');
    setDirectionsResult(null);
    directionTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  // ── Suggest form ─────────────────────────────────────────────────────────────
  const validateSuggest = () => {
    const errs = {};
    if (!suggestData.locationName.trim()) errs.locationName = 'Location name is required';
    if (!suggestData.category)            errs.category     = 'Please select a category';
    if (!suggestData.description.trim())  errs.description  = 'Please describe the location';
    return errs;
  };

  const handleSuggestSubmit = async (e) => {
    e.preventDefault();
    const errs = validateSuggest();
    if (Object.keys(errs).length > 0) { setSuggestErrors(errs); return; }
    setSuggestErrors({});
    setSuggestSubmitting(true);
    try {
      const res = await fetch(`${API}/locations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          locationName: suggestData.locationName.trim(),
          category: suggestData.category,
          description: suggestData.description.trim(),
          locationType: 'ROOM',
          ownerEmail: user?.email || '',
          ownerName: user?.name || '',
        }),
      });
      if (res.ok) {
        setSuggestSuccess(true);
        setShowSuggestForm(false);
        setSuggestData({ locationName: '', category: '', description: '' });
      } else {
        const msg = await res.text();
        setSuggestErrors({ api: msg || 'Submission failed. Please try again.' });
      }
    } catch {
      setSuggestErrors({ api: 'Network error. Is the backend running?' });
    } finally {
      setSuggestSubmitting(false);
    }
  };

  // ─────────────────────────────────────────────────────────────────────────
  // RENDER
  // ─────────────────────────────────────────────────────────────────────────
  return (
    <div className="campus-map-page">

      {/* ── Page Header ─────────────────────────────────────────────────── */}
      <header className="campus-map-header" ref={directionTopRef}>
        <h1>🗺️ Campus Map Guide</h1>
        <p>Find your way around FAST-NUCES Lahore campus — directions, locations, and more.</p>
      </header>

      {/* ── Global Search Bar ───────────────────────────────────────────── */}
      <div className="map-search-wrapper">
        <div className="map-search-bar" role="search">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden>
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            id="campus-map-search-input"
            type="text"
            placeholder="Search by block name, room number, or faculty name…"
            value={searchQuery}
            onChange={handleSearchChange}
            aria-label="Search campus locations"
            autoComplete="off"
          />
          {searchQuery && (
            <button className="clear-search-btn" onClick={clearSearch} aria-label="Clear search">
              ✕
            </button>
          )}
        </div>

        {showResults && (
          <div className="map-search-results" role="listbox">
            {searchResults.length === 0 ? (
              <div className="map-search-empty">
                No locations found. Try block name, room number, or faculty name.
              </div>
            ) : (
              searchResults.map((loc, i) => (
                <div
                  key={i}
                  className="map-search-result-item"
                  role="option"
                  onClick={() => handleSearchResultClick(loc)}
                  id={`search-result-${i}`}
                >
                  <span className="map-search-result-name">{loc.locationName}</span>
                  <div className="map-search-result-meta">
                    <span className="map-cat-badge">{loc.category}</span>
                    {loc.blockId && <span className="map-block-id">{loc.blockId}</span>}
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>

      <div className="whole-map-trigger-container">
        <button 
          id="whole-campus-map-btn"
          className="whole-map-btn"
          onClick={() => setShowWholeMap(true)}
        >
          🗺️ Whole Campus Map
        </button>
      </div>

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 1 — DIRECTION FINDER  (UC-32)
      ═══════════════════════════════════════════════════════════════════ */}
      <div className="map-section-card glass-card">
        <div className="map-section-header">
          <span className="map-section-icon">🧭</span>
          <span className="map-section-title">Direction Finder</span>
        </div>

        <div className="map-section-body">
          <div className="direction-finder-grid">
            {/* Step 1 — What are you looking for? */}
            <div className="map-field-group">
              <label className="map-field-label" htmlFor="search-type-select">
                What are you looking for?
              </label>
              <select
                id="search-type-select"
                className="map-select"
                value={searchType}
                onChange={handleSearchTypeChange}
              >
                <option value="">Select type…</option>
                <option value="BLOCK">Block</option>
                <option value="FACULTY_OFFICE">Faculty Office</option>
                <option value="ROOM">Room Number</option>
              </select>
            </div>

            {/* Step 2 — Where are you now? */}
            <div className="map-field-group">
              <label className="map-field-label" htmlFor="from-location-select">
                Where are you now?
              </label>
              <select
                id="from-location-select"
                className="map-select"
                value={fromLocation}
                onChange={handleFromChange}
                disabled={!searchType}
              >
                <option value="">{searchType ? 'Choose your starting point…' : 'Select what you seek first'}</option>
                {blockOptions.map((loc, i) => (
                  <option key={i} value={loc.locationName}>{loc.locationName}</option>
                ))}
              </select>
              {validationErrors.from && <span className="map-field-error">{validationErrors.from}</span>}
            </div>

            {/* Step 3 — Select Destination */}
            <div className="map-field-group">
              <label className="map-field-label" htmlFor="to-location-select">
                Select Destination
              </label>
              <select
                id="to-location-select"
                className="map-select"
                value={toLocation}
                onChange={handleToChange}
                disabled={!fromLocation}
              >
                <option value="">
                  {fromLocation ? 'Choose destination…' : 'Select starting point first'}
                </option>
                {searchType === 'BLOCK' && destinationOptions.map((loc, i) => (
                  <option key={i} value={loc.locationName}>{loc.locationName}</option>
                ))}
                {searchType === 'FACULTY_OFFICE' && destinationOptions.map((opt, i) => (
                  <option key={i} value={opt.name}>
                    {opt.name} — {opt.block}
                  </option>
                ))}
                {searchType === 'ROOM' && destinationOptions.map((opt, i) => (
                  <option key={i} value={opt.name}>
                    {opt.name} — {opt.block}
                  </option>
                ))}
              </select>
              {validationErrors.to && <span className="map-field-error">{validationErrors.to}</span>}
            </div>
          </div>

          {/* Get Directions button */}
          <div className="get-directions-row">
            <button
              id="get-directions-btn"
              className="get-directions-btn"
              onClick={handleGetDirections}
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner-ring" style={{ width: 16, height: 16, borderWidth: 2 }} />
                  Finding route…
                </>
              ) : (
                <> 🧭 Get Directions </>
              )}
            </button>

            {/* Inline same-location hint */}
            {fromLocation && toLocation && fromLocation === toLocation && (
              <span className="same-location-msg">⚠️ You are already at your destination!</span>
            )}
          </div>

          {/* ── Direction Result ──────────────────────────────────────── */}
          {directionsResult && (
            <div className="directions-result">

              {/* Same location */}
              {directionsResult.sameLocation && (
                <div className="directions-info-banner success">
                  <span className="directions-info-banner-icon">🎉</span>
                  <div className="directions-info-banner-text">
                    <h4>Already at destination!</h4>
                    <p>{directionsResult.message}</p>
                  </div>
                </div>
              )}

              {/* Route not found */}
              {directionsResult.routeFound === false && !directionsResult.sameLocation && (
                <div className="directions-info-banner">
                  <span className="directions-info-banner-icon">📍</span>
                  <div className="directions-info-banner-text">
                    <h4>Directions Not Available</h4>
                    <p>{directionsResult.message}</p>
                  </div>
                </div>
              )}

              {/* Step-by-step navigation */}
              {directionsResult.routeFound && steps.length > 0 && (
                <div className="step-viewer">
                  <span className="step-counter">
                    Step {currentStep + 1} of {totalSteps}
                  </span>

                  {/* Image or placeholder */}
                  {currentStepData?.hasImage ? (
                    <div className="step-image-area">
                      <img
                        src={`http://localhost:8080${currentStepData.imageUrl}`}
                        alt={`Step ${currentStep + 1}`}
                        onError={e => {
                          e.target.style.display = 'none';
                          e.target.nextSibling.style.display = 'flex';
                        }}
                      />
                      {/* Fallback shown on error (hidden by default) */}
                      <div className="step-placeholder" style={{ display: 'none' }}>
                        <span className="step-placeholder-icon">🗺️</span>
                        <p>{currentStepData.stepDescription}</p>
                      </div>
                    </div>
                  ) : (
                    <div className="step-placeholder">
                      <span className="step-placeholder-icon">🗺️</span>
                      <p>{currentStepData?.stepDescription}</p>
                    </div>
                  )}

                  <p className="step-description">{currentStepData?.stepDescription}</p>

                  {/* Navigation controls */}
                  <div className="step-nav-row">
                    <button
                      id="step-prev-btn"
                      className="step-nav-btn"
                      onClick={() => setCurrentStep(s => s - 1)}
                      disabled={currentStep === 0}
                      aria-label="Previous step"
                    >
                      ← Previous
                    </button>

                    <button
                      id="step-next-btn"
                      className="step-nav-btn"
                      onClick={() => setCurrentStep(s => s + 1)}
                      disabled={currentStep === totalSteps - 1}
                      aria-label="Next step"
                    >
                      Next →
                    </button>

                    {currentStep === totalSteps - 1 && (
                      <span className="arrival-badge">You have arrived! 🎉</span>
                    )}

                    {/* Step dots */}
                    <div className="step-dots" aria-hidden>
                      {steps.map((_, i) => (
                        <span
                          key={i}
                          className={`step-dot${i === currentStep ? ' active' : ''}`}
                        />
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Destination Info Card (always shown when there's a result) */}
              {directionsResult.destinationInfo && (
                <DestInfoCard location={directionsResult.destinationInfo} />
              )}
            </div>
          )}
        </div>
      </div>

      {/* Campus Event Popup — floating top-right */}
      {showEventPopup && activeEvents.length > 0 && (
        <EventPopup events={activeEvents} onDismiss={() => setShowEventPopup(false)} />
      )}

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 2 — CATEGORY BROWSER  (UC-33)
      ═══════════════════════════════════════════════════════════════════ */}
      <div className="map-section-card glass-card" ref={categoryBrowserRef}>
        <div className="map-section-header">
          <span className="map-section-icon">📂</span>
          <span className="map-section-title">Browse by Category</span>
        </div>

        <div className="category-browser">
          {VALID_CATEGORIES.map(cat => {
            const locsInCat = groupedLocations[cat];
            if (!locsInCat || locsInCat.length === 0) return null; // hide empty categories

            const isOpen = expandedCategory === cat;
            return (
              <div key={cat} className="accordion-item">
                <div
                  className="accordion-header"
                  role="button"
                  tabIndex={0}
                  aria-expanded={isOpen}
                  onClick={() => toggleCategory(cat)}
                  onKeyDown={e => e.key === 'Enter' && toggleCategory(cat)}
                  id={`accordion-${cat.replace(/\s+/g, '-').toLowerCase()}`}
                >
                  <div className="accordion-header-left">
                    <span className="accordion-cat-icon">{CATEGORY_ICONS[cat] || '📍'}</span>
                    <span className="accordion-cat-name">{cat}</span>
                    <span className="accordion-count">{locsInCat.length}</span>
                  </div>
                  <svg
                    className={`accordion-arrow${isOpen ? ' open' : ''}`}
                    width="16" height="16" viewBox="0 0 24 24"
                    fill="none" stroke="currentColor" strokeWidth="2.5"
                    aria-hidden
                  >
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </div>

                <div className={`accordion-body${isOpen ? ' open' : ''}`}>
                  <div className="accordion-inner">
                    <div className="location-grid">
                      {locsInCat.map((loc, i) => (
                        <div
                          key={i}
                          className="location-tile"
                          role="button"
                          tabIndex={0}
                          onClick={() => setSelectedLocation(loc)}
                          onKeyDown={e => e.key === 'Enter' && setSelectedLocation(loc)}
                          id={`location-tile-${loc.id}`}
                        >
                          <div className="location-tile-name">{loc.locationName}</div>
                          <div className="location-tile-type">{loc.locationType}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Location Detail Panel */}
      {selectedLocation && (
        <LocationDetailPanel
          location={selectedLocation}
          onClose={() => setSelectedLocation(null)}
          onGoHere={handleGoHere}
        />
      )}

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 3 — SUGGEST A LOCATION  (UC-35)
      ═══════════════════════════════════════════════════════════════════ */}
      <div className="map-section-card glass-card">
        <div className="map-section-header">
          <span className="map-section-icon">💡</span>
          <span className="map-section-title">Suggest a Location</span>
        </div>

        <div className="map-section-body">
          {suggestSuccess && !showSuggestForm && (
            <div className="suggest-success">
              ✅ Your suggestion has been submitted! The admin will review it.
            </div>
          )}

          {!showSuggestForm ? (
            <>
              <p style={{ color: 'var(--text-on-card-secondary)', fontSize: '0.9rem' }}>
                Know a place on campus that's hard to find? Help your fellow FASTians by suggesting it.
              </p>
              <button
                id="toggle-suggest-form-btn"
                className="suggest-toggle-btn"
                onClick={() => {
                  if (!user) return;
                  setShowSuggestForm(true);
                  setSuggestSuccess(false);
                }}
                disabled={!user}
                aria-label="Open suggest location form"
              >
                + Suggest a Location
              </button>
              {!user && (
                <span className="suggest-not-logged">Please log in to suggest a location.</span>
              )}
            </>
          ) : (
            <form
              className="suggest-form"
              onSubmit={handleSuggestSubmit}
              noValidate
              id="suggest-location-form"
            >
              {/* Location Name */}
              <div className="suggest-field">
                <label htmlFor="suggest-name">Location Name</label>
                <input
                  id="suggest-name"
                  className={`suggest-input${suggestErrors.locationName ? ' error' : ''}`}
                  type="text"
                  placeholder="e.g. New Computer Lab"
                  value={suggestData.locationName}
                  onChange={e => {
                    setSuggestData(d => ({ ...d, locationName: e.target.value }));
                    setSuggestErrors(err => ({ ...err, locationName: '' }));
                  }}
                />
                {suggestErrors.locationName && (
                  <span className="field-error-text">{suggestErrors.locationName}</span>
                )}
              </div>

              {/* Category */}
              <div className="suggest-field">
                <label htmlFor="suggest-category">Category</label>
                <select
                  id="suggest-category"
                  className={`suggest-select${suggestErrors.category ? ' error' : ''}`}
                  value={suggestData.category}
                  onChange={e => {
                    setSuggestData(d => ({ ...d, category: e.target.value }));
                    setSuggestErrors(err => ({ ...err, category: '' }));
                  }}
                >
                  <option value="">Select a category…</option>
                  {VALID_CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
                {suggestErrors.category && (
                  <span className="field-error-text">{suggestErrors.category}</span>
                )}
              </div>

              {/* Description */}
              <div className="suggest-field">
                <label htmlFor="suggest-desc">Description / Reason</label>
                <textarea
                  id="suggest-desc"
                  className={`suggest-textarea${suggestErrors.description ? ' error' : ''}`}
                  placeholder="Describe the location and why it should be added…"
                  value={suggestData.description}
                  onChange={e => {
                    setSuggestData(d => ({ ...d, description: e.target.value }));
                    setSuggestErrors(err => ({ ...err, description: '' }));
                  }}
                />
                {suggestErrors.description && (
                  <span className="field-error-text">{suggestErrors.description}</span>
                )}
              </div>

              {suggestErrors.api && (
                <span className="field-error-text">{suggestErrors.api}</span>
              )}

              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                <button
                  id="submit-suggestion-btn"
                  type="submit"
                  className="suggest-submit-btn"
                  disabled={suggestSubmitting || !user}
                >
                  {suggestSubmitting ? 'Submitting…' : 'Submit Suggestion'}
                </button>
                <button
                  type="button"
                  className="suggest-toggle-btn"
                  onClick={() => {
                    setShowSuggestForm(false);
                    setSuggestErrors({});
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </div>

      {/* ════════════════════════════════════════════════════════════════════
          SECTION 4 — ADMIN ROUTE MANAGER
      ═══════════════════════════════════════════════════════════════════ */}
      {user?.role === 'ADMIN' && (
        <div className="map-section-card glass-card admin-routes-panel" style={{ border: '2px solid var(--glass-border-accent)' }}>
          <div className="map-section-header">
            <span className="map-section-icon">⚙️</span>
            <span className="map-section-title">Manage Route Steps (Admin Only)</span>
          </div>

          <div className="map-section-body">
            {adminRouteSuccess && (
              <div className="directions-info-banner success" style={{ marginBottom: '1rem', padding: '0.75rem' }}>
                <span className="directions-info-banner-icon">✅</span>
                <div className="directions-info-banner-text">{adminRouteSuccess}</div>
              </div>
            )}

            <form onSubmit={handleAdminRouteSubmit} className="admin-route-form">
              <div className="direction-finder-grid">
                <div className="map-field-group">
                  <label className="map-field-label">From Location</label>
                  <select
                    className={`map-select${adminRouteErrors.fromLocation ? ' error' : ''}`}
                    value={adminRouteForm.fromLocation}
                    onChange={e => setAdminRouteForm(f => ({ ...f, fromLocation: e.target.value }))}
                  >
                    <option value="">Select From…</option>
                    {blockOptions.map((loc, i) => (
                      <option key={i} value={loc.locationName}>{loc.locationName}</option>
                    ))}
                  </select>
                  {adminRouteErrors.fromLocation && <span className="field-error-text">{adminRouteErrors.fromLocation}</span>}
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Destination Category</label>
                  <select
                    className="map-select"
                    value={adminToType}
                    onChange={e => setAdminToType(e.target.value)}
                  >
                    <option value="BLOCK">Block / Building</option>
                    <option value="ROOM">Classroom / Lab</option>
                    <option value="FACULTY">Faculty Office</option>
                  </select>
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Target Block (Route Endpoint)</label>
                  <select
                    className={`map-select${adminRouteErrors.toLocation ? ' error' : ''}`}
                    value={adminRouteForm.toLocation}
                    onChange={e => setAdminRouteForm(f => ({ ...f, toLocation: e.target.value }))}
                  >
                    <option value="">Select Target Block…</option>
                    {blockOptions.map((loc, i) => (
                      <option key={i} value={loc.locationName}>{loc.locationName} ({loc.blockId})</option>
                    ))}
                  </select>
                  {adminRouteErrors.toLocation && <span className="field-error-text">{adminRouteErrors.toLocation}</span>}
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Step Number</label>
                  <input
                    type="number"
                    min="1"
                    className={`map-select${adminRouteErrors.stepOrder ? ' error' : ''}`}
                    value={adminRouteForm.stepOrder}
                    onChange={e => setAdminRouteForm(f => ({ ...f, stepOrder: e.target.value }))}
                    style={{ backgroundColor: 'var(--bg-card-hover)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', padding: '0.65rem 1rem', borderRadius: '12px' }}
                  />
                  {adminRouteErrors.stepOrder && <span className="field-error-text">{adminRouteErrors.stepOrder}</span>}
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Or Upload Photo</label>
                  <input
                    id="admin-route-file"
                    type="file"
                    accept="image/*"
                    onChange={e => setSelectedFile(e.target.files[0])}
                    className="map-select"
                    style={{ backgroundColor: 'var(--bg-card-hover)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', padding: '0.65rem 1rem', borderRadius: '12px' }}
                  />
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Update Description (Optional)</label>
                  <input
                    type="text"
                    placeholder="e.g. Near the main entrance..."
                    className="map-select"
                    value={newDescriptionInput}
                    onChange={e => setNewDescriptionInput(e.target.value)}
                    style={{ backgroundColor: 'var(--bg-card-hover)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', padding: '0.65rem 1rem', borderRadius: '12px' }}
                  />
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Add Classrooms (Optional, comma separated)</label>
                  <input
                    type="text"
                    placeholder="e.g. C-15, Lab-4"
                    className="map-select"
                    value={newRoomInput}
                    onChange={e => setNewRoomInput(e.target.value)}
                    style={{ backgroundColor: 'var(--bg-card-hover)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', padding: '0.65rem 1rem', borderRadius: '12px' }}
                  />
                </div>

                <div className="map-field-group">
                  <label className="map-field-label">Add Faculty Names (Optional, comma separated)</label>
                  <input
                    type="text"
                    placeholder="e.g. Dr. Salman (CS)"
                    className="map-select"
                    value={newFacultyInput}
                    onChange={e => setNewFacultyInput(e.target.value)}
                    style={{ backgroundColor: 'var(--bg-card-hover)', border: '1px solid var(--border-color)', color: 'var(--text-primary)', padding: '0.65rem 1rem', borderRadius: '12px' }}
                  />
                </div>
              </div>

              <div className="map-field-group" style={{ marginTop: '1rem' }}>
                <label className="map-field-label">Step Description</label>
                <textarea
                  className={`suggest-textarea${adminRouteErrors.stepDescription ? ' error' : ''}`}
                  placeholder="Describe this step of the route…"
                  value={adminRouteForm.stepDescription}
                  onChange={e => setAdminRouteForm(f => ({ ...f, stepDescription: e.target.value }))}
                  style={{ minHeight: '80px' }}
                />
                {adminRouteErrors.stepDescription && <span className="field-error-text">{adminRouteErrors.stepDescription}</span>}
              </div>

              {adminRouteErrors.api && <div className="field-error-text" style={{ color: 'var(--accent-teal)', marginBottom: '0.5rem', fontWeight: 'bold' }}>{adminRouteErrors.api}</div>}

              <div style={{ marginTop: '1rem' }}>
                <button type="submit" className="primary-btn" disabled={uploading}>
                  {uploading ? 'Uploading Image...' : '+ Add Route Step'}
                </button>
              </div>
            </form>

            <div style={{ marginTop: '2rem', overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', color: 'var(--text-primary)' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <th style={{ padding: '0.5rem' }}>From</th>
                    <th style={{ padding: '0.5rem' }}>To</th>
                    <th style={{ padding: '0.5rem' }}>Step #</th>
                    <th style={{ padding: '0.5rem' }}>Description</th>
                    <th style={{ padding: '0.5rem' }}>Image</th>
                    <th style={{ padding: '0.5rem' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {adminRoutes.length === 0 ? (
                    <tr>
                      <td colSpan="6" style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No routes defined yet.</td>
                    </tr>
                  ) : (
                    adminRoutes.map(route => (
                      <tr key={route.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '0.5rem' }}>{route.fromLocation}</td>
                        <td style={{ padding: '0.5rem' }}>{route.toLocation}</td>
                        <td style={{ padding: '0.5rem' }}>{route.stepOrder}</td>
                        <td style={{ padding: '0.5rem', maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{route.stepDescription}</td>
                        <td style={{ padding: '0.5rem' }}>{route.imageFileName || <span style={{color: 'var(--text-secondary)'}}>Text-only</span>}</td>
                        <td style={{ padding: '0.5rem' }}>
                          <button
                            type="button"
                            className="secondary-btn admin-route-delete-btn"
                            onClick={() => handleDeleteAdminRoute(route.id)}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

          </div>
        </div>
      )}
      {/* ── Whole Campus Map Modal ────────────────────────────────────── */}
      {showWholeMap && (
        <div className="whole-map-overlay" onClick={() => setShowWholeMap(false)} role="dialog" aria-modal="true">
          <div className="whole-map-modal" onClick={e => e.stopPropagation()}>
            <div className="whole-map-header">
              <h3>FAST Lahore - Complete Campus Map</h3>
              <button className="whole-map-close" onClick={() => setShowWholeMap(false)} aria-label="Close modal">×</button>
            </div>
            <div className="whole-map-body">
              <img 
                src="http://localhost:8080/api/campus-map/images/whole_campus_map.jpg" 
                alt="Whole Campus Map" 
                className="whole-map-img"
                onError={(e) => {
                  e.target.src = 'https://via.placeholder.com/1200x800?text=Campus+Map+Image+Not+Found+in+Backend';
                }}
              />
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default CampusMap;
