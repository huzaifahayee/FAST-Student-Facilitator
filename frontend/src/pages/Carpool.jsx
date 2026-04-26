import React, { useState, useEffect, useCallback } from 'react';
import { RefreshCw } from 'lucide-react';
import IosPickerField from '../components/IosPickerField';
import './Carpool.css';

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

  const loadRides = useCallback(async () => {
    const res = await fetch('http://localhost:8080/api/rides');
    const data = await res.json();
    setRides(Array.isArray(data) ? data : []);
  }, []);

  // Fetch rides on mount
  useEffect(() => {
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
    if (newRide.checkpoints.length < 5 && currentCheckpoint) {
      setNewRide({...newRide, checkpoints: [...newRide.checkpoints, currentCheckpoint]});
      setCurrentCheckpoint('');
    }
  };

  const handleFlag = async (id) => {
    const confirmed = window.confirm("Are you sure you want to report this ride? Misuse of the reporting system may lead to an account ban.");
    if (!confirmed) return;

    try {
      await fetch(`http://localhost:8080/api/rides/${id}/flag`, { method: 'PUT' });
      alert("This ride has been reported for review. Thank you for keeping the community safe!");
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

    // VALIDATION: Phone number must be exactly 11 digits
    const phoneRegex = /^\d{11}$/;
    if (!phoneRegex.test(newRide.contactInfo)) {
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
          driverName: user.name,
          driverEmail: user.email,
        })
      });
      if (res.ok) {
        setIsOffering(false);
        alert("Ride offer submitted! 👋 It will appear on the portal once an Admin verifies and approves it.");
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
    } catch (err) {
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
              <input type="text" placeholder="Origin (e.g., Johar Town)" required 
                onChange={e => setNewRide({...newRide, origin: e.target.value})} />
              <input type="text" placeholder="Destination (e.g., FAST)" required 
                onChange={e => setNewRide({...newRide, destination: e.target.value})} />
              <input type="text" placeholder="Time (e.g., 08:30 AM)" required 
                onChange={e => setNewRide({...newRide, departureTime: e.target.value})} />
              <input type="number" placeholder="Seats" min="1" max="4" required 
                onChange={e => setNewRide({...newRide, availableSeats: e.target.value})} />
              
              <IosPickerField
                className="cp-vehicle-picker"
                value={newRide.vehicleType}
                onChange={(v) => setNewRide({ ...newRide, vehicleType: v })}
                options={VEHICLE_OPTIONS}
                sheetTitle="Vehicle type"
              />

              <input type="text" placeholder="Phone (e.g., 03001234567)" required 
                onChange={e => setNewRide({...newRide, contactInfo: e.target.value})} />
            </div>

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
              <div className="cp-tags">
                {newRide.checkpoints.map((cp, i) => (
                  <span key={i} className="cp-tag">{cp}</span>
                ))}
              </div>
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
          {rides.length > 0 ? (
            rides.map((ride, i) => (
              <div key={i} className="ride-card glass-card">
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
              <p>No active rides currently available.</p>
              <p className="subtext">Be the first to offer a ride today!</p>
            </div>
          )}
        </div>
      </section>
    </div>
  );
};

export default Carpool;
