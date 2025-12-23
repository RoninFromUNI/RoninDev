import React, { useState, useMemo } from 'react';
import { useGitHubProjects } from '../hooks';

/**
 * ProjectGrid component
 * Renders projects fetched from GitHub with filtering by tech stack and status
 */
export const ProjectGrid = ({
                              username = 'YOUR_GITHUB_USERNAME',
                              techFilter = null,
                              statusFilter = null
                            }) => {
  const { projects, loading, error } = useGitHubProjects(username);
  const [sortBy, setSortBy] = useState('updated'); // 'updated', 'stars', 'name'

  // Filter + sort logic
  const filteredProjects = useMemo(() => {
    let filtered = projects;

    if (techFilter) {
      filtered = filtered.filter(p =>
          p.tech?.includes(techFilter) || p.language?.toLowerCase() === techFilter.toLowerCase()
      );
    }

    if (statusFilter) {
      filtered = filtered.filter(p => p.status === statusFilter);
    }

    return filtered.sort((a, b) => {
      switch (sortBy) {
        case 'updated':
          return new Date(b.updated) - new Date(a.updated);
        case 'stars':
          return b.stars - a.stars;
        case 'name':
          return a.repoName.localeCompare(b.repoName);
        default:
          return 0;
      }
    });
  }, [projects, techFilter, statusFilter, sortBy]);

  if (loading) return <div className="project-loader">Loading projects...</div>;
  if (error) return <div className="project-error">Error: {error}</div>;

  return (
      <div className="project-grid-container">
        <div className="project-controls">
          <label>
            Sort by:
            <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
              <option value="updated">Last Updated</option>
              <option value="stars">Stars</option>
              <option value="name">Name</option>
            </select>
          </label>
        </div>

        <div className="projects-grid">
          {filteredProjects.map(project => (
              <ProjectCard key={project.id} project={project} />
          ))}
        </div>

        {filteredProjects.length === 0 && (
            <div className="no-projects">No projects match filters</div>
        )}
      </div>
  );
};

/**
 * Individual project card component
 */
const ProjectCard = ({ project }) => {
  const lastUpdated = new Date(project.updated).toLocaleDateString();

  return (
      <a
          href={project.url}
          target="_blank"
          rel="noopener noreferrer"
          className="project-card"
      >
        <div className="project-header">
          <h3>{project.name || project.repoName}</h3>
          {project.status && (
              <span className={`status-badge status-${project.status}`}>
            {project.status}
          </span>
          )}
        </div>

        <p className="project-description">
          {project.description || 'No description'}
        </p>

        <div className="project-meta">
          {project.language && (
              <span className="tech-tag">{project.language}</span>
          )}
          {project.tech && project.tech.map(t => (
              <span key={t} className="tech-tag">{t}</span>
          ))}
        </div>

        <div className="project-footer">
          <span className="last-updated">Updated: {lastUpdated}</span>
          {project.stars > 0 && (
              <span className="stars">⭐ {project.stars}</span>
          )}
        </div>
      </a>
  );
};