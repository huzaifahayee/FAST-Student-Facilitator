import { Link } from 'react-router-dom';
import './FeatureCarousel.css';

/**
 * FeatureCarousel Component
 * 
 * Update logic:
 * We now use 'Link' to navigate to the feature pages.
 * Each feature in the array now has a 'path' mapping.
 */
const FeatureCarousel = () => {
  const features = [
    { name: 'Carpool', desc: 'Secure shared rides with other students.', path: '/carpool' },
    { name: 'Lost & Found', desc: 'Report or recover items on campus.', path: '/lost-found' },
    { name: 'Past Papers', desc: 'Prepare for exams with previous papers.', path: '/past-papers' },
    { name: 'Events', desc: 'Latest campus activities & semester plan.', path: '/events' },
    { name: 'Reminders', desc: 'Never miss a quiz or assignment alert.', path: '/reminders' },
    { name: 'Map Guide', desc: 'Interactive campus map & room finder.', path: '/map' },
    { name: 'Timetable', desc: 'View your weekly class schedule.', path: '/timetable' },
    { name: 'Book Exchange', desc: 'Buy/Sell used books with students.', path: '/marketplace' },
    { name: 'FastNotes', desc: 'Student-curated PDF study notes.', path: '/notes' }
  ];

  return (
    <div className="carousel-view">
      <div className="carousel-track">
        {features.map((feature, index) => (
          <Link to={feature.path} key={index} className="feature-card-wrapper">
            <div className="feature-card glass-card">
              <span className="card-number">0{index + 1}</span>
              <h4>{feature.name}</h4>
              <p>{feature.desc}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
};

export default FeatureCarousel;
