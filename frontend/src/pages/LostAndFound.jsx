import React, { useState, useEffect } from 'react';
import './LostAndFound.css';

const API_BASE_URL = 'http://localhost:8080/api/lost-found';

function LostAndFound({ user }) {
  const [activeTab, setActiveTab] = useState('Lost'); // 'Lost' or 'Found'
  const [showResolved, setShowResolved] = useState(false);
  const [listings, setListings] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    itemName: '',
    category: '',
    description: '',
    location: '',
    date: ''
  });

  const categories = ['Electronics', 'Wallet/ID', 'Books', 'Keys', 'Clothing', 'Other'];

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
    fetchListings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab, searchKeyword, categoryFilter]);

  const handleSubmit = async (e) => {
    e.preventDefault();
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
        setShowModal(false);
        setFormData({ itemName: '', category: '', description: '', location: '', date: '' });
        fetchListings();
      } else {
        alert("Server Error: Could not submit the listing. Please check the backend connection.");
      }
    } catch (err) {
      console.error(err);
      alert("Network Error: Backend is unreachable.");
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
    <div className="lost-found-container">
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
        <select 
          value={categoryFilter} 
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="filter-select"
        >
          <option value="">All Categories</option>
          {categories.map(c => <option key={c} value={c}>{c}</option>)}
        </select>
        
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
              <div key={listing.id} className="glass-card listing-card">
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
              <div key={listing.id} className="glass-card listing-card resolved">
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
                <select required value={formData.category} onChange={e => setFormData({...formData, category: e.target.value})}>
                  <option value="">Select Category</option>
                  {categories.map(c => <option key={c} value={c}>{c}</option>)}
                </select>
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
