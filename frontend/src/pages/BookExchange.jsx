import React, { useState, useEffect } from 'react';
import './BookExchange.css';

const BookExchange = ({ user }) => {
  const [activeTab, setActiveTab] = useState('SELL');
  const [books, setBooks] = useState([]);
  const [isPosting, setIsPosting] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  
  const [newBook, setNewBook] = useState({
    bookTitle: '', author: '', courseCode: '',
    bookCondition: 'Good', price: '', frontCoverImage: '', backCoverImage: '', listingType: 'SELL'
  });
  
   const [validationError, setValidationError] = useState('');
   const [editBookId, setEditBookId] = useState(null);
   const [selectedImage, setSelectedImage] = useState(null);

  useEffect(() => {
    fetchBooks();
  }, [activeTab]);

  const fetchBooks = async () => {
    try {
      let url = `http://localhost:8080/api/books?type=${activeTab === 'BUY' ? 'SELL' : activeTab}`;
      if (searchQuery) {
        url = `http://localhost:8080/api/books/search?query=${encodeURIComponent(searchQuery)}&type=${activeTab === 'BUY' ? 'SELL' : activeTab}`;
      }
      const res = await fetch(url);
      const data = await res.json();
      setBooks(data);
    } catch (err) {
      console.error("Failed to fetch books", err);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    fetchBooks();
  };

  const handleFlag = async (id) => {
    const confirmed = window.confirm("Are you sure you want to report this listing? Misuse may lead to a ban.");
    if (!confirmed) return;

    try {
      await fetch(`http://localhost:8080/api/books/${id}/flag`, { method: 'PUT' });
      alert("This listing has been reported for review.");
      fetchBooks();
    } catch (err) {
      console.error("Failed to flag listing:", err);
    }
  };

  const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => resolve(reader.result);
      reader.onerror = error => reject(error);
    });
  };

  const handleFileChange = async (e, type) => {
    const file = e.target.files[0];
    if (file) {
      if (file.type !== 'image/png') {
        alert("Please upload a PNG image.");
        e.target.value = null;
        return;
      }
      try {
        const base64 = await convertToBase64(file);
        setNewBook({ ...newBook, [type]: base64 });
      } catch (err) {
        console.error("Image conversion failed", err);
      }
    }
  };

  const handlePostSubmit = async (e) => {
    e.preventDefault();
    if (!user?.email || !user?.name) {
      setValidationError("Session identity missing. Please log in.");
      return;
    }

    if (activeTab !== 'BUY' && (!newBook.frontCoverImage || !newBook.backCoverImage)) {
      setValidationError("Please upload both front and back images of the book.");
      return;
    }

    setValidationError('');

    try {
      const payload = {
        ...newBook,
        listingType: activeTab, // Post under current tab
        ownerName: user.name,
        ownerEmail: user.email,
        price: newBook.price ? parseFloat(newBook.price) : 0.0
      };

      const url = editBookId 
        ? `http://localhost:8080/api/books/${editBookId}` 
        : 'http://localhost:8080/api/books';
      
      const method = editBookId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        setIsPosting(false);
        setEditBookId(null);
        alert(`Listing ${editBookId ? 'updated' : 'submitted'}! It will appear once an Admin approves it.`);
        fetchBooks();
        setNewBook({
          bookTitle: '', author: '', courseCode: '',
          bookCondition: 'Good', price: '', frontCoverImage: '', backCoverImage: '', listingType: 'SELL'
        });
      } else {
        setValidationError(`Failed to save listing (${res.status}).`);
      }
    } catch (err) {
      setValidationError("Network error while saving.");
    }
  };

  const handleClose = async (id) => {
    if (!window.confirm("Mark this listing as closed/fulfilled?")) return;
    try {
      await fetch(`http://localhost:8080/api/books/${id}/close`, { method: 'PUT' });
      fetchBooks();
    } catch (err) {
      console.error("Failed to close listing:", err);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this listing?")) return;
    try {
      await fetch(`http://localhost:8080/api/books/${id}?reason=User+deleted+own+listing`, { method: 'DELETE' });
      fetchBooks();
    } catch (err) {
      console.error("Failed to delete listing:", err);
    }
  };

  const handleEdit = (book) => {
    setNewBook({
      bookTitle: book.bookTitle,
      author: book.author,
      courseCode: book.courseCode,
      bookCondition: book.bookCondition,
      price: book.price || '',
      frontCoverImage: book.frontCoverImage || '',
      backCoverImage: book.backCoverImage || '',
      listingType: book.listingType
    });
    setEditBookId(book.id);
    setIsPosting(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="book-exchange-page">
      <header className="book-exchange-header">
        <h2>Book Exchange / Marketplace</h2>
        <p>Buy, sell, or exchange academic books and materials.</p>
        
        <div className="tab-navigation">
          {['SELL', 'BUY', 'EXCHANGE'].map(tab => (
            <button 
              key={tab}
              className={`tab-btn ${activeTab === tab ? 'active' : ''}`}
              onClick={() => { setActiveTab(tab); setIsPosting(false); setEditBookId(null); setSearchQuery(''); }}
            >
              {tab === 'SELL' ? 'Sell Books' : tab === 'BUY' ? 'Buy Requests' : 'Exchange'}
            </button>
          ))}
        </div>
      </header>

      <div className="marketplace-actions">
        <form className="search-form" onSubmit={handleSearch}>
          <input 
            type="text" 
            placeholder="Search by Title or Course Code..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <button type="submit" className="secondary-btn">Search</button>
        </form>
        <button className="primary-btn" onClick={() => { setIsPosting(true); setEditBookId(null); setNewBook({ bookTitle: '', author: '', courseCode: '', bookCondition: 'Good', price: '', frontCoverImage: '', backCoverImage: '', listingType: 'SELL' }); }}>
          Post {activeTab === 'SELL' ? 'a Book to Sell' : activeTab === 'BUY' ? 'a Buy Request' : 'an Exchange'}
        </button>
      </div>

      {isPosting && (
        <div className="post-form-container glass-card">
          <div className="form-header">
            <h3>{editBookId ? 'Edit' : 'Post a New'} {activeTab} Listing</h3>
            {validationError && <p className="error-text">{validationError}</p>}
          </div>
          <form onSubmit={handlePostSubmit}>
            <div className="form-grid">
              <input type="text" placeholder="Book Title" required 
                value={newBook.bookTitle} onChange={e => setNewBook({...newBook, bookTitle: e.target.value})} />
              
              <input type="text" placeholder="Author" required 
                value={newBook.author} onChange={e => setNewBook({...newBook, author: e.target.value})} />
              
              <input type="text" placeholder="Course Code (e.g., CS201)" required 
                value={newBook.courseCode} onChange={e => setNewBook({...newBook, courseCode: e.target.value})} />
              
              <select value={newBook.bookCondition} onChange={e => setNewBook({...newBook, bookCondition: e.target.value})}>
                <option value="New">New</option>
                <option value="Like New">Like New</option>
                <option value="Good">Good</option>
                <option value="Fair">Fair</option>
              </select>

              {activeTab !== 'EXCHANGE' && (
                <input type="number" placeholder="Price (Rs.)" required min="0"
                  value={newBook.price} onChange={e => setNewBook({...newBook, price: e.target.value})} />
              )}

              {activeTab !== 'BUY' && (
                <>
                  <div className="file-upload-group">
                    <label>Front Cover (PNG)</label>
                    <input type="file" accept="image/png" onChange={e => handleFileChange(e, 'frontCoverImage')} />
                    {newBook.frontCoverImage && <span className="upload-success">✓ Loaded</span>}
                  </div>
                  
                  <div className="file-upload-group">
                    <label>Back Cover (PNG)</label>
                    <input type="file" accept="image/png" onChange={e => handleFileChange(e, 'backCoverImage')} />
                    {newBook.backCoverImage && <span className="upload-success">✓ Loaded</span>}
                  </div>
                </>
              )}
            </div>

            <div className="form-btns">
              <button type="submit" className="post-btn">{editBookId ? 'Save Changes' : 'Submit Listing'}</button>
              <button type="button" className="cancel-btn" onClick={() => { setIsPosting(false); setEditBookId(null); }}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <section className="listings-explorer">
        <div className="listings-grid">
          {books.length > 0 ? (
            books.map((book) => (
              <div key={book.id} className={`book-card glass-card ${book.status === 'CLOSED' ? 'closed' : ''}`}>
                {book.frontCoverImage && book.backCoverImage ? (
                  <div className="book-images-container">
                    <div className="book-image" 
                      style={{ backgroundImage: `url(${book.frontCoverImage})` }}
                      onClick={() => setSelectedImage(book.frontCoverImage)}
                    >
                      <span className="image-label">FRONT</span>
                    </div>
                    <div className="book-image" 
                      style={{ backgroundImage: `url(${book.backCoverImage})` }}
                      onClick={() => setSelectedImage(book.backCoverImage)}
                    >
                      <span className="image-label">BACK</span>
                    </div>
                  </div>
                ) : (
                  <div className="no-image-placeholder">
                    <span>No Images Provided (Buy Request)</span>
                  </div>
                )}
                <div className="book-content">
                  <div className="book-header">
                    <h4>{book.bookTitle}</h4>
                    <span className={`status-badge ${book.status.toLowerCase()}`}>
                      {book.status === 'CLOSED' ? 'Sold/Fulfilled' : 'Active'}
                    </span>
                  </div>
                  <p className="author">by {book.author}</p>
                  
                  <div className="book-meta">
                    <span className="tag course">{book.courseCode}</span>
                    <span className="tag condition">{book.bookCondition}</span>
                    {(book.listingType === 'SELL' || book.listingType === 'BUY') && (
                      <span className="tag price">Rs. {book.price}</span>
                    )}
                  </div>
                  
                  <div className="book-footer">
                    <div className="poster-info">
                      <span className="label">Posted by:</span>
                      <span className="email">{book.ownerEmail}</span>
                    </div>
                    <div className="actions">
                      {activeTab === 'BUY' ? (
                        <button className="primary-btn" style={{padding: '0.4rem 1rem'}} onClick={() => alert(`Interested in buying? Contact the seller at: ${book.ownerEmail}`)}>🛒 BUY</button>
                      ) : (
                        book.ownerEmail === user?.email ? (
                          <>
                            <button className="close-btn" style={{marginRight: '0.5rem', color: 'var(--text-secondary)', borderColor: 'var(--text-secondary)'}} onClick={() => handleEdit(book)}>✏️ Edit</button>
                            <button className="report-btn" style={{marginRight: '0.5rem'}} onClick={() => handleDelete(book.id)}>🗑️ Delete</button>
                            {book.status === 'ACTIVE' && (
                              <button className="close-btn" onClick={() => handleClose(book.id)}>Mark Closed</button>
                            )}
                          </>
                        ) : (
                          <button className="report-btn" onClick={() => handleFlag(book.id)}>🚩 Report</button>
                        )
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="empty-state glass-card">
              <p>{activeTab === 'BUY' ? 'No available books found in the marketplace.' : `No ${activeTab.toLowerCase()} listings found.`}</p>
              <p className="subtext">Check back later or {activeTab === 'BUY' ? 'post a buy request' : 'post your own'}!</p>
            </div>
          )}
        </div>
      </section>

      {selectedImage && (
        <div className="image-modal-overlay" onClick={() => setSelectedImage(null)}>
          <div className="image-modal-content">
            <button className="close-modal" onClick={() => setSelectedImage(null)}>&times;</button>
            <img src={selectedImage} alt="Full Screen Book" />
          </div>
        </div>
      )}
    </div>
  );
};

export default BookExchange;
