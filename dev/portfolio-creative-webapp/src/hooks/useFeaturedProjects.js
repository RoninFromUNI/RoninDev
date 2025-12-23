import { useState, useEffect } from 'react';
import { useGitHubProjects } from './useGitHubProjects';

/**
 * Hook to get featured projects
 * Returns projects marked as featured in project.json
 * Falls back to top 3 most recent if no featured projects exist
 */
export const useFeaturedProjects = (username) => {
  const { projects, loading, error } = useGitHubProjects(username);
  const [featured, setFeatured] = useState([]);

  useEffect(() => {
    if (!projects.length) return;

    // Filter for projects marked as featured
    let featuredProjects = projects.filter(p => p.featured === true);

    // Fallback: if no featured projects, use top 3 most recent
    if (featuredProjects.length === 0) {
      featuredProjects = projects.slice(0, 3);
    }

    setFeatured(featuredProjects);
  }, [projects]);

  return featured;
};
