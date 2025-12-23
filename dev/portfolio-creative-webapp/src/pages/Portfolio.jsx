import React from 'react';
import { ProjectGrid } from '../components/ProjectGrid';
import '../styles/ProjectGrid.css';
import './Portfolio.css'; // Optional: portfolio-specific styling

export default function Portfolio() {
  const GITHUB_USERNAME = import.meta.env.VITE_GITHUB_USERNAME || 'YOUR_GITHUB_USERNAME';

  return (
    <section className="portfolio-section">
      <div className="portfolio-container">
        <header className="portfolio-header">
          <h1>Projects</h1>
          <p className="portfolio-subtitle">
            Live sync from GitHub. Explore my work across code, design, and creative development.
          </p>
        </header>

        <div className="portfolio-content">
          <ProjectGrid username={GITHUB_USERNAME} />
        </div>

        <footer className="portfolio-footer">
          <p>
            All projects are open-source on{' '}
            <a 
              href={`https://github.com/${GITHUB_USERNAME}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </a>
            . Each repo includes detailed documentation and commit history.
          </p>
        </footer>
      </div>
    </section>
  );
}
