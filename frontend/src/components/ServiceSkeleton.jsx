import React from 'react';
import './ServiceSkeleton.css';

/**
 * ServiceSkeleton Component
 * 
 * What is this?
 * This is a placeholder page for features that haven't been built yet.
 * It ensures the site feels 'complete' even while we are in active 
 * development of the core features.
 */
const ServiceSkeleton = ({ featureName }) => {
  return (
    <div className="skeleton-container glass-card">
      <div className="skeleton-content">
        <div className="skeleton-badge">Coming Soon</div>
        <h2>{featureName}</h2>
        <p>
          We are currently building this service to centralize your campus life.
          Please check back soon for the full {featureName} experience.
        </p>
        <div className="skeleton-loader"></div>
      </div>
    </div>
  );
};

export default ServiceSkeleton;
