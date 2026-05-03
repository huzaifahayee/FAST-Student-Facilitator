import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import IosPickerField from '../components/IosPickerField';
import { useFsfDialog } from '../components/FsfDialogProvider';
import './LostAndFound.css';
import '../styles/IosMenuPicker.css';

const API_BASE_URL = 'http://localhost:8080/api/lost-found';

const LF_CATEGORIES = ['Electronics', 'Wallet/ID', 'Books', 'Keys', 'Clothing', 'Other'];

function LostAndFound({ user }) {
  const { showAlert } = useFsfDialog();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('Lost'); // 'Lost' or 'Found'
  const [listings, setListings] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  
  const [categoryMenuOpen, setCategoryMenuOpen] = useState(false);
  const categoryMenuRef = useRef(null);

  const [showModal, setShowModal] = useState(false);
  const [flashListingId, setFlashListingId] = useState(null);
  const [formData, setFormData] = useState({
    itemName: '',
    category: '',
    description: '',
    location: '',
    date: ''
  });

  const modalCategoryOptions = useMemo(
    () => [{ value: '', label: 'Select Category' }, ...LF_CATEGORIES.map((c) => ({ value: c, label: c }))],
    []
  );

  const fetchListings = async () => {
    try {
      const url = new URL(API_BASE_URL);
      url.searchParams.append('type', activeTab);
      if (searchKeyword) url.searchParams.append('keyword', searchKeyword);
      if (categoryFilter) url.searchParams.append('category', categoryFilter);

      const response = await fetch(url.toString());
      if (response.ok) {
        const data = await response.json();
        setListings(data);
      }
    } catch (err) {
      console.error('Failed to fetch listings', err);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- listings reload
    fetchListings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, searchKeyword, categoryFilter]);

  useEffect(() => {
    if (searchParams.get('item')) return;
    const q = searchParams.get('q');
    // eslint-disable-next-line react-hooks/set-state-in-effect -- deep-link ?q= from global search
    if (q !== null) setSearchKeyword((prev) => (q !== prev ? q : prev));
  }, [searchParams]);

  useEffect(() => {
    const raw = searchParams.get('item');
    if (!raw) return;
    const id = parseInt(raw, 10);
    if (!Number.isFinite(id)) return;

    fetch(`${API_BASE_URL}/${id}`)
      .then((res) => {
        if (!res.ok) throw new Error('not found');
        return res.json();
      })
      .then((listing) => {
        setActiveTab(listing.type || 'Lost');
        setSearchKeyword('');
        setFlashListingId(id);
      })
      .catch(() => {})
      .finally(() => {
        const next = new URLSearchParams(searchParams);
        next.delete('item');
        const qs = next.toString();
        navigate(`/lost-found${qs ? `?${qs}` : ''}`, { replace: true });
      });
  }, [searchParams, navigate]);

  useEffect(() => {
    if (!flashListingId) return;
    const t = window.setTimeout(() => {
      const el = document.getElementById(`lf-card-${flashListingId}`);
      el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el?.classList.add('deep-link-highlight');
      window.setTimeout(() => el?.classList.remove('deep-link-highlight'), 2200);
      setFlashListingId(null);
    }, 140);
    return () => window.clearTimeout(t);
  }, [listings, flashListingId]);

  useEffect(() => {
    if (!categoryMenuOpen) return;
    const onDoc = (e) => {
      if (categoryMenuRef.current && !categoryMenuRef.current.contains(e.target)) {
        setCategoryMenuOpen(false);
      }
    };
    const onKey = (e) => {
      if (e.key === 'Escape') setCategoryMenuOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDoc);
      document.removeEventListener('keydown', onKey);
    };
  }, [categoryMenuOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.category) {
      await showAlert({ title: 'Category required', message: 'Please select a category.' });
      return;
    }
    try {
      const payload = {
        ...formData,
        type: activeTab,
        studentEmail: user?.email
      };

      const res = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        // Close modal and reset form first
        setShowModal(false);
        setFormData({ itemName: '', category: '', description: '', location: '', date: '' });

        // Re-fetch with current filters — use activeTab directly to avoid stale closure
        const url = new URL(API_BASE_URL);
        url.searchParams.append('type', activeTab);
        if (searchKeyword) url.searchParams.append('keyword', searchKeyword);
        if (categoryFilter) url.searchParams.append('category', categoryFilter);
        const refreshRes = await fetch(url.toString());
        if (refreshRes.ok) {
          setListings(await refreshRes.json());
        }
      } else {
        await showAlert({
          title: 'Server error',
          message: 'Could not submit the listing. Please check the backend connection.',
        });
      }
    } catch (err) {
      console.error(err);
      await showAlert({
        title: 'Network error',
        message: 'Backend is unreachable.',
      });
    }
  };

  const markResolved = async (id) => {
    try {
      const res = await fetch(`${API_BASE_URL}/${id}/resolve?studentEmail=${user?.email}`, {
        method: 'PUT'
      });
      if (res.ok) {
        fetchListings();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const deleteListing = async (id) => {
    try {
      const res = await fetch(`${API_BASE_URL}/${id}`, { method: 'DELETE' });
      if (res.ok) {
        fetchListings();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const activeListings = listings.filter(l => l.status !== 'Resolved');
  const resolvedListings = listings.filter(l => l.status === 'Resolved');
  
  const sortListings = (list) => [...list].sort((a, b) => new Date(b.date) - new Date(a.date));

  return (
    <div className="lost-found-page">
      <header className="lf-portal-header">
        <h2>Lost and Found portal</h2>
        <p>Report or browse lost and found listings to help the FAST community recover belongings.</p>
      </header>

      <div className="tab-control">
        <button 
          className={activeTab === 'Lost' ? 'active-tab' : ''} 
          onClick={() => setActiveTab('Lost')}
        >Lost Items</button>
        <button 
          className={activeTab === 'Found' ? 'active-tab' : ''} 
          onClick={() => setActiveTab('Found')}
        >Found Items</button>
      </div>

      <div className="controls glass-card">
        <input 
          type="text" 
          placeholder="Search items..." 
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          className="search-input"
        />
        <div className="ios-category-dropdown" ref={categoryMenuRef}>
          <button
            type="button"
            className={`ios-category-dropdown-trigger${categoryMenuOpen ? ' is-open' : ''}`}
            aria-haspopup="listbox"
            aria-expanded={categoryMenuOpen}
            onClick={() => setCategoryMenuOpen((o) => !o)}
          >
            <span className="ios-category-trigger-label">
              {categoryFilter === '' ? 'All Categories' : categoryFilter}
            </span>
            <span className="ios-category-trigger-chevron" aria-hidden>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 9l6 6 6-6" />
              </svg>
            </span>
          </button>
          {categoryMenuOpen && (
            <div className="ios-category-dropdown-sheet">
              <div className="ios-category-dropdown-panel" role="listbox" aria-label="Filter by category">
                <p className="ios-category-dropdown-title">Categories</p>
                <div className="ios-category-dropdown-list">
                  <button
                    type="button"
                    role="option"
                    aria-selected={categoryFilter === ''}
                    className={`ios-category-option${categoryFilter === '' ? ' is-selected' : ''}`}
                    onClick={() => {
                      setCategoryFilter('');
                      setCategoryMenuOpen(false);
                    }}
                  >
                    All Categories
                  </button>
                  {LF_CATEGORIES.map((c) => (
                    <button
                      key={c}
                      type="button"
                      role="option"
                      aria-selected={categoryFilter === c}
                      className={`ios-category-option${categoryFilter === c ? ' is-selected' : ''}`}
                      onClick={() => {
                        setCategoryFilter(c);
                        setCategoryMenuOpen(false);
                      }}
                    >
                      {c}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
        
        <button className="primary-btn" onClick={() => setShowModal(true)}>
          Report {activeTab} Item
        </button>
      </div>

      <div className="listings-section">
        <h2>Active {activeTab} Listings</h2>
        <div className="listings-grid">
          {activeListings.length === 0 ? (
            <p className="no-items">No active items found.</p>
          ) : (
            sortListings(activeListings).map(listing => (
              <div key={listing.id} id={`lf-card-${listing.id}`} className="glass-card listing-card">
                <h3>{listing.itemName}</h3>
                <p className="category-badge">{listing.category}</p>
                <p className="desc">{listing.description}</p>
                <p className="meta"><strong>Location:</strong> {listing.location}</p>
                <p className="meta"><strong>Date:</strong> {listing.date}</p>
                <p className="meta"><strong>Contact:</strong> {listing.studentEmail}</p>
                
                <div className="card-actions">
                  {user && (listing.studentEmail === user.email || user.role === 'ADMIN') && (
                    <button className="resolve-btn" onClick={() => markResolved(listing.id)}>Mark Resolved</button>
                  )}
                  {user && (listing.studentEmail === user.email || user.role === 'ADMIN') && (
                    <button className="delete-btn" onClick={() => deleteListing(listing.id)}>Delete</button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="resolved-section">
        <h2>Resolved {activeTab} Listings</h2>
        <div className="listings-grid">
          {resolvedListings.length === 0 ? (
            <p className="no-items">No resolved items yet.</p>
          ) : (
            sortListings(resolvedListings).map(listing => (
              <div key={listing.id} id={`lf-card-${listing.id}`} className="glass-card listing-card resolved">
                <div className="resolved-header">
                  <h3>{listing.itemName}</h3>
                  <span className="tick-mark">✔</span>
                </div>
                <p className="category-badge">{listing.category}</p>
                <p className="desc">{listing.description}</p>
                <p className="meta"><strong>Location:</strong> {listing.location}</p>
                <p className="meta"><strong>Date:</strong> {listing.date}</p>
                <p className="meta"><strong>Resolved by:</strong> {listing.studentEmail}</p>
                
                <div className="card-actions">
                  {user && user.role === 'ADMIN' && (
                    <button className="delete-btn" onClick={() => deleteListing(listing.id)}>Delete</button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content glass-card">
            <h2>Report {activeTab} Item</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Item Name</label>
                <input required type="text" value={formData.itemName} onChange={e => setFormData({...formData, itemName: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Category</label>
                <IosPickerField
                  className="lf-modal-category-picker"
                  value={formData.category}
                  onChange={(v) => setFormData({ ...formData, category: v })}
                  options={modalCategoryOptions}
                  sheetTitle="Category"
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea required value={formData.description} onChange={e => setFormData({...formData, description: e.target.value})}></textarea>
              </div>
              <div className="form-group">
                <label>Location</label>
                <input required type="text" value={formData.location} onChange={e => setFormData({...formData, location: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Date Lost/Found</label>
                <input required type="date" value={formData.date} onChange={e => setFormData({...formData, date: e.target.value})} />
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

export default LostAndFound;
