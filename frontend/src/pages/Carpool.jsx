import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { RefreshCw, X } from 'lucide-react';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './Carpool.css';

const MIN_SEATS = 1;
const MAX_SEATS = 4;

/** Compact (830), colon (8:30 / 14:30), optional am/pm → normalized "h:mm AM|PM". */
function format12h(h24, m) {
  const isPm = h24 >= 12;
  let h12 = h24 % 12;
  if (h12 === 0) h12 = 12;
  return `${h12}:${String(m).padStart(2, '0')} ${isPm ? 'PM' : 'AM'}`;
}

function parseDepartureTime(raw) {
  const s = String(raw ?? '').trim();
  if (!s) return { ok: false };

  const colon = s.match(/^(\d{1,2})\s*:\s*(\d{2})\s*(am|pm)?$/i);
  if (colon) {
    const h = parseInt(colon[1], 10);
    const m = parseInt(colon[2], 10);
    const ap = colon[3]?.toLowerCase();
    if (Number.isNaN(h) || Number.isNaN(m) || m > 59 || m < 0) return { ok: false };

    let h24;
    if (ap === 'am') {
      if (h < 1 || h > 12) return { ok: false };
      h24 = h === 12 ? 0 : h;
    } else if (ap === 'pm') {
      if (h < 1 || h > 12) return { ok: false };
      h24 = h === 12 ? 12 : h + 12;
    } else {
      if (h < 0 || h > 23) return { ok: false };
      h24 = h;
    }
    return { ok: true, formatted: format12h(h24, m) };
  }

  const digits = s.replace(/\D/g, '');
  if (digits.length !== 3 && digits.length !== 4) return { ok: false };

  const n = parseInt(digits, 10);
  const h = Math.floor(n / 100);
  const m = n % 100;
  if (m > 59 || h > 23 || h < 0) return { ok: false };

  return { ok: true, formatted: format12h(h, m) };
}

function clampSeats(value) {
  const v = typeof value === 'number' ? value : parseInt(String(value), 10);
  if (Number.isNaN(v)) return MIN_SEATS;
  return Math.min(MAX_SEATS, Math.max(MIN_SEATS, v));
}

const VEHICLE_OPTIONS = [
  { value: 'Car', label: 'Car' },
  { value: 'Bike', label: 'Bike' },
  { value: 'InDrive with stop', label: 'InDrive with stop' },
];

/**
 * Carpool Page
 * 
 * Logic Highlights:
 * 1. SRS NFR 4.2.3: Max 5 checkpoints.
 * 2. SRS NFR 4.2.1: Real-time filtering.
 */
const Carpool = ({ user }) => {
  const { showAlert, showConfirm } = useFsfDialog();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [rides, setRides] = useState([]);
  const [isOffering, setIsOffering] = useState(false);
  const [newRide, setNewRide] = useState({
    origin: '', destination: '', availableSeats: 1, 
    departureTime: '', contactInfo: '', checkpoints: [],
    vehicleType: 'Car' // Default value
  });
  const [currentCheckpoint, setCurrentCheckpoint] = useState('');
  const [validationError, setValidationError] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [routeFilter, setRouteFilter] = useState('');

  useEffect(() => {
    if (searchParams.get('ride')) return;
    const q = searchParams.get('q');
    // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
    if (q !== null) setRouteFilter(q);
  }, [searchParams]);

  useEffect(() => {
    const raw = searchParams.get('ride');
    if (!raw) return;
    const rideId = parseInt(raw, 10);
    if (!Number.isFinite(rideId)) return;

    const t = window.setTimeout(() => {
      const el = document.getElementById(`ride-card-${rideId}`);
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el?.classList.add('deep-link-highlight');
      window.setTimeout(() => el?.classList.remove('deep-link-highlight'), 2200);

      const next = new URLSearchParams(searchParams);
      next.delete('ride');
      const qs = next.toString();
      navigate(`/carpool${qs ? `?${qs}` : ''}`, { replace: true });
    }, 160);

    return () => window.clearTimeout(t);
  }, [searchParams, rides, navigate]);

  const filteredRides = useMemo(() => {
    const t = routeFilter.trim().toLowerCase();
    if (!t) return rides;
    return rides.filter((r) => {
      const parts = [
        r.origin,
        r.destination,
        r.departureTime,
        ...(Array.isArray(r.checkpoints) ? r.checkpoints : []),
      ];
      return parts.some((p) => String(p || '').toLowerCase().includes(t));
    });
  }, [rides, routeFilter]);

  const loadRides = useCallback(async () => {
    const res = await fetch('http://localhost:8080/api/rides');
    const data = await res.json();
    setRides(Array.isArray(data) ? data : []);
  }, []);

  // Fetch rides on mount
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial data load
    loadRides().catch((err) => console.error('Failed to fetch rides', err));
  }, [loadRides]);

  const fetchRides = () => {
    loadRides().catch((err) => console.error('Failed to fetch rides', err));
  };

  const handleRefresh = async () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    const started = Date.now();
    try {
      await loadRides();
    } catch (err) {
      console.error('Failed to fetch rides', err);
    } finally {
      const minMs = 520;
      const wait = Math.max(0, minMs - (Date.now() - started));
      await new Promise((r) => setTimeout(r, wait));
      setIsRefreshing(false);
    }
  };

  const addCheckpoint = () => {
    const label = currentCheckpoint.trim();
    if (!label) return;
    setNewRide((prev) => {
      if (prev.checkpoints.length >= 5) return prev;
      return { ...prev, checkpoints: [...prev.checkpoints, label] };
    });
    setCurrentCheckpoint('');
  };

  const removeCheckpoint = async (index, label) => {
    const message = label
      ? `Remove checkpoint "${label}"?`
      : 'Remove this checkpoint?';
    const ok = await showConfirm({
      title: 'Remove checkpoint',
      message,
      confirmText: 'Remove',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!ok) return;
    setNewRide((prev) => ({
      ...prev,
      checkpoints: prev.checkpoints.filter((_, i) => i !== index),
    }));
  };

  const normalizeDepartureTimeOnBlur = () => {
    setNewRide((prev) => {
      const raw = prev.departureTime.trim();
      if (!raw) return prev;
      const parsed = parseDepartureTime(raw);
      if (!parsed.ok) return prev;
      return { ...prev, departureTime: parsed.formatted };
    });
  };

  const handleFlag = async (id) => {
    const confirmed = await showConfirm({
      title: 'Report ride',
      message:
        'Are you sure you want to report this ride? Misuse of the reporting system may lead to an account ban.',
      confirmText: 'Report',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!confirmed) return;

    try {
      await fetch(`http://localhost:8080/api/rides/${id}/flag`, { method: 'PUT' });
      await showAlert({
        title: 'Report submitted',
        message:
          'This ride has been reported for review. Thank you for keeping the community safe!',
      });
      fetchRides();
    } catch (err) {
      console.error("Failed to flag ride:", err);
    }
  };

  const handleOfferSubmit = async (e) => {
    e.preventDefault();

    if (!user?.email || !user?.name) {
      setValidationError("Your session is missing identity details. Please sign in again.");
      return;
    }

    const timeParsed = parseDepartureTime(newRide.departureTime);
    if (!timeParsed.ok) {
      setValidationError(
        'Enter a valid departure time (e.g. 830 → 8:30 AM, 8:30, 14:45, or 2:30 pm).'
      );
      return;
    }

    const seats = clampSeats(newRide.availableSeats);

    // VALIDATION: Phone number must be exactly 11 digits
    const phoneRegex = /^\d{11}$/;
    if (!phoneRegex.test(newRide.contactInfo.trim())) {
      setValidationError("Please enter a valid 11-digit phone number.");
      return;
    }

    setValidationError(''); // Clear errors if valid

    try {
      const res = await fetch('http://localhost:8080/api/rides', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...newRide,
          departureTime: timeParsed.formatted,
          availableSeats: seats,
          driverName: user.name,
          driverEmail: user.email,
        })
      });
      if (res.ok) {
        setIsOffering(false);
        await showAlert({
          title: 'Ride submitted',
          message:
            'Ride offer submitted! 👋 It will appear on the portal once an Admin verifies and approves it.',
        });
        fetchRides();
        // Reset form
        setNewRide({
          origin: '', destination: '', availableSeats: 1, 
          departureTime: '', contactInfo: '', checkpoints: [], vehicleType: 'Car'
        });
      } else {
        const body = await res.text();
        setValidationError(`Could not post ride (status ${res.status}). ${body || ''}`.trim());
      }
    } catch {
      setValidationError("Network error while posting ride. Is the backend running?");
    }
  };

  return (
    <div className="carpool-page">
      <header className="carpool-header">
        <h2>Carpool Portal</h2>
        <p>Share a ride with your fellow FASTians to campus.</p>
      </header>

      <div className="carpool-actions">
        <button className="primary-btn" onClick={() => setIsOffering(true)}>Offer a Ride</button>
        <button
          type="button"
          className={`secondary-btn cp-refresh-btn${isRefreshing ? ' is-refreshing' : ''}`}
          onClick={handleRefresh}
          disabled={isRefreshing}
          aria-busy={isRefreshing}
        >
          <RefreshCw className="cp-refresh-icon" size={18} strokeWidth={2.4} aria-hidden />
          {isRefreshing ? 'Refreshing…' : 'Refresh List'}
        </button>
      </div>

      {isOffering && (
        <div className="offer-form-container glass-card">
          <div className="form-header">
            <h3>Offer a New Ride</h3>
            {validationError && <p className="error-text">{validationError}</p>}
          </div>
          <form onSubmit={handleOfferSubmit}>
            <div className="form-grid">
              <input
                type="text"
                placeholder="Origin (e.g., Johar Town)"
                required
                value={newRide.origin}
                onChange={(e) => setNewRide({ ...newRide, origin: e.target.value })}
              />
              <input
                type="text"
                placeholder="Destination (e.g., FAST)"
                required
                value={newRide.destination}
                onChange={(e) => setNewRide({ ...newRide, destination: e.target.value })}
              />
              <input
                type="text"
                placeholder="Time (e.g., 830 or 8:30)"
                required
                value={newRide.departureTime}
                onChange={(e) => setNewRide({ ...newRide, departureTime: e.target.value })}
                onBlur={normalizeDepartureTimeOnBlur}
                aria-describedby="cp-time-hint"
              />
              <input
                type="number"
                placeholder={`Seats (${MIN_SEATS}–${MAX_SEATS})`}
                min={MIN_SEATS}
                max={MAX_SEATS}
                step={1}
                required
                value={newRide.availableSeats}
                onChange={(e) => {
                  const raw = e.target.value;
                  if (raw === '') {
                    setNewRide({ ...newRide, availableSeats: '' });
                    return;
                  }
                  const n = parseInt(raw, 10);
                  if (Number.isNaN(n)) return;
                  setNewRide({ ...newRide, availableSeats: clampSeats(n) });
                }}
                onBlur={() => {
                  setNewRide((prev) => {
                    if (prev.availableSeats === '' || Number(prev.availableSeats) < MIN_SEATS) {
                      return { ...prev, availableSeats: MIN_SEATS };
                    }
                    return prev;
                  });
                }}
              />
              
              <IosPickerField
                className="cp-vehicle-picker"
                value={newRide.vehicleType}
                onChange={(v) => setNewRide({ ...newRide, vehicleType: v })}
                options={VEHICLE_OPTIONS}
                sheetTitle="Vehicle type"
              />

              <input
                type="text"
                placeholder="Phone (e.g., 03001234567)"
                required
                value={newRide.contactInfo}
                onChange={(e) => setNewRide({ ...newRide, contactInfo: e.target.value })}
              />
            </div>
            <p id="cp-time-hint" className="cp-field-hint">
              Leaving the time field formats compact numbers (e.g. 830 → 8:30 AM). You can also use 24h (14:30).
            </p>

            <div className="checkpoint-section">
              <div className="cp-input">
                <input 
                  type="text" 
                  placeholder="Add Checkpoint Path" 
                  value={currentCheckpoint}
                  onChange={e => setCurrentCheckpoint(e.target.value)}
                  disabled={newRide.checkpoints.length >= 5}
                />
                <button type="button" className="add-btn" onClick={addCheckpoint}>Add</button>
              </div>
              <ul className="cp-tags" aria-label="Added checkpoints">
                {newRide.checkpoints.map((cp, i) => (
                  <li key={i} className="cp-tag-row">
                    <span className="cp-tag">{cp}</span>
                    <button
                      type="button"
                      className="cp-tag-remove"
                      onClick={() => removeCheckpoint(i, cp)}
                      aria-label={`Remove checkpoint ${cp}`}
                      title="Remove"
                    >
                      <X size={14} strokeWidth={2.5} aria-hidden />
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            <div className="form-btns">
              <button type="submit" className="post-btn">Post Ride</button>
              <button type="button" className="cancel-btn" onClick={() => setIsOffering(false)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <section className="rides-explorer">
        <div className="section-header">
          <h3>Available Rides</h3>
        </div>

        <div className={`rides-list${isRefreshing ? ' rides-list--refreshing' : ''}`}>
          {filteredRides.length > 0 ? (
            filteredRides.map((ride) => (
              <div key={ride.id} id={`ride-card-${ride.id}`} className="ride-card glass-card">
                <div className="ride-main">
                  <div className="route">
                    <span className="origin">{ride.origin}</span>
                    {ride.checkpoints?.map((cp, idx) => (
                      <React.Fragment key={idx}>
                        <span className="arrow">→</span>
                        <span className="checkpoint">{cp}</span>
                      </React.Fragment>
                    ))}
                    <span className="arrow">→</span>
                    <span className="dest">{ride.destination}</span>
                  </div>
                  <div className="ride-info-group">
                    <div className="seats-badge">
                      {ride.availableSeats} {ride.availableSeats === 1 ? 'Seat' : 'Seats'}
                    </div>
                    <div className={`status-badge ${ride.approved ? 'approved' : 'pending'}`}>
                      {ride.approved ? 'Approved' : 'Pending Review'}
                    </div>
                  </div>
                </div>
                <div className="ride-footer">
                  <div className="offerer-info">
                    <span className="driver-label">Offered by:</span>
                    <span className="driver-name">{ride.driverName}</span>
                  </div>
                  <div className="contact-info">
                    <span className="contact-label">Contact:</span>
                    <span className="contact-value">{ride.contactInfo}</span>
                  </div>
                  <button className="report-btn" onClick={() => handleFlag(ride.id)}>Report</button>
                </div>
              </div>
            ))
          ) : (
            <div className="ride-card glass-card empty-state">
              <p>
                {routeFilter.trim()
                  ? 'No rides match your search.'
                  : 'No active rides currently available.'}
              </p>
              <p className="subtext">Be the first to offer a ride today!</p>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default Carpool;
