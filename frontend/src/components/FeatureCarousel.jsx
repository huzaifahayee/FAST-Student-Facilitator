import { useRef, useCallback, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import './FeatureCarousel.css';

/**
 * FeatureCarousel — horizontal snap scroll of feature cards.
 * A single floating arrow stays fixed on the viewport (right edge of the carousel);
 * cards slide underneath until there is nothing left to scroll.
 */
const FeatureCarousel = () => {
  const trackRef = useRef(null);
  const [canScrollForward, setCanScrollForward] = useState(false);

  const updateScrollHint = useCallback(() => {
    const track = trackRef.current;
    if (!track) return;
    const { scrollLeft, scrollWidth, clientWidth } = track;
    const hasOverflow = scrollWidth > clientWidth + 1;
    const nearEnd = scrollLeft + clientWidth >= scrollWidth - 3;
    setCanScrollForward(hasOverflow && !nearEnd);
  }, []);

  useEffect(() => {
    const track = trackRef.current;
    if (!track) return;

    updateScrollHint();
    track.addEventListener('scroll', updateScrollHint, { passive: true });
    window.addEventListener('resize', updateScrollHint);

    const ro = new ResizeObserver(() => updateScrollHint());
    ro.observe(track);

    return () => {
      track.removeEventListener('scroll', updateScrollHint);
      window.removeEventListener('resize', updateScrollHint);
      ro.disconnect();
    };
  }, [updateScrollHint]);

  const scrollMoreIntoView = useCallback(() => {
    const track = trackRef.current;
    if (!track) return;
    const step = Math.max(track.clientWidth * 0.55, 280);
    track.scrollBy({ left: step, behavior: 'smooth' });
    window.setTimeout(updateScrollHint, 400);
  }, [updateScrollHint]);

  const features = [
    { name: 'Carpool', desc: 'Secure shared rides with other students.', path: '/carpool' },
    { name: 'Lost & Found', desc: 'Report or recover items on campus.', path: '/lost-found' },
    { name: 'Past Papers', desc: 'Prepare for exams with previous papers.', path: '/past-papers' },
    { name: 'Events', desc: 'Latest campus activities & semester plan.', path: '/events' },
    { name: 'Reminders', desc: 'Never miss a quiz or assignment alert.', path: '/reminders' },
    { name: 'Map Guide', desc: 'Interactive campus map & room finder.', path: '/campus-map' },
    { name: 'Timetable', desc: 'View your weekly class schedule.', path: '/timetable' },
    { name: 'Book Exchange', desc: 'Buy/Sell used books with students.', path: '/marketplace' },
    { name: 'FastNotes', desc: 'Student-curated PDF study notes.', path: '/notes' }
  ];

  return (
    <div className="carousel-view">
      <div className="carousel-track" ref={trackRef}>
        {features.map((feature, i) => (
          <Link to={feature.path} key={feature.path} className="feature-card-wrapper">
            <div className="feature-card glass-card">
              <span className="card-number">0{i + 1}</span>
              <h4>{feature.name}</h4>
              <p>{feature.desc}</p>
            </div>
          </Link>
        ))}
      </div>
      {canScrollForward && (
        <button
          type="button"
          className="carousel-scroll-arrow"
          onClick={scrollMoreIntoView}
          aria-label="Slide sideways to see more features"
          title="Slide to see more features"
        >
          <ChevronRight className="carousel-scroll-arrow-icon" strokeWidth={2.5} />
        </button>
      )}
    </div>
  );
};

export default FeatureCarousel;
