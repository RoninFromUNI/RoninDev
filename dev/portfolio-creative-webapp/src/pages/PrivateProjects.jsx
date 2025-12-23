import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ProjectGrid } from '../components/ProjectGrid';
import './PrivateProjects.css';

const PRIVATE_ACCESS_KEYS = {
  'manager-key-12345': 'Manager Access',
  'employee-key-67890': 'Employee Access',
};

export default function PrivateProjects() {
  const [searchParams] = useSearchParams();
  const [inputKey, setInputKey] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const GITHUB_USERNAME = import.meta.env.VITE_GITHUB_USERNAME || 'YOUR_USERNAME';

  // Check if valid key in URL
  const urlKey = searchParams.get('key');
  const hasValidUrlKey = urlKey && PRIVATE_ACCESS_KEYS[urlKey];

  const handleSubmit = (e) => {
    e.preventDefault();
    if (PRIVATE_ACCESS_KEYS[inputKey]) {
      setIsAuthenticated(true);
    } else {
      alert('Invalid access key');
      setInputKey('');
    }
  };

  // If URL has valid key, authenticate
  if (hasValidUrlKey && !isAuthenticated) {
    return (
        <div className="private-projects">
          <section className="private-header">
            <h1>🔓 Private Projects</h1>
            <p className="auth-badge">
              {PRIVATE_ACCESS_KEYS[urlKey]}
            </p>
            <p className="private-subtitle">
              These projects are confidential and under active development. This view is restricted to authorized personnel only.
            </p>
          </section>

          <section className="private-grid">
            <ProjectGrid
                username={GITHUB_USERNAME}
                fetchPrivate={true}
            />
          </section>

          <section className="private-footer">
            <p>All code is proprietary. Unauthorized access is prohibited.</p>
          </section>
        </div>
    );
  }

  if (!isAuthenticated) {
    return (
        <div className="private-projects">
          <div className="auth-container">
            <h1>🔒 Private Projects</h1>
            <p>This section requires authentication to view work in progress and confidential projects.</p>

            <form className="auth-form" onSubmit={handleSubmit}>
              <input
                  type="password"
                  placeholder="Enter access key"
                  value={inputKey}
                  onChange={(e) => setInputKey(e.target.value)}
                  className="auth-input"
              />
              <button type="submit" className="auth-button">
                Unlock
              </button>
            </form>

            <div className="auth-note">
              <p>Contact RoninDev for access credentials</p>
            </div>
          </div>
        </div>
    );
  }

  // Authenticated view - show private projects
  return (
      <div className="private-projects">
        <section className="private-header">
          <h1>🔓 Private Projects</h1>
          <p className="auth-badge">
            Authenticated Access
          </p>
          <p className="private-subtitle">
            These projects are confidential and under active development. This view is restricted to authorized personnel only.
          </p>
        </section>

        <section className="private-grid">
          <ProjectGrid
              username={GITHUB_USERNAME}
              fetchPrivate={true}
          />
        </section>

        <section className="private-footer">
          <p>All code is proprietary. Unauthorized access is prohibited.</p>
        </section>
      </div>
  );
}