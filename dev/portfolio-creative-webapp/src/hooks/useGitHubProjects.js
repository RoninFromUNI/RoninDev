import { useState, useEffect } from 'react';

/**
 * Hook to fetch GitHub repos + metadata files
 * Queries GitHub API for user repos, then enriches with project.json from each repo
 */
export const useGitHubProjects = (username) => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        // Fetch repos from GitHub API
        const reposRes = await fetch(
          `https://api.github.com/users/${username}/repos?sort=updated&per_page=100`
        );
        if (!reposRes.ok) throw new Error('GitHub API failed');

        const repos = await reposRes.json();

        // Enrich each repo with metadata from project.json
        const enrichedProjects = await Promise.all(
          repos.map(async (repo) => {
            let metadata = {};
            try {
              // Attempt to fetch project.json from repo root
              const metaRes = await fetch(
                `https://raw.githubusercontent.com/${username}/${repo.name}/main/project.json`
              );
              if (metaRes.ok) {
                metadata = await metaRes.json();
              }
            } catch (e) {
              // Fallback: use GitHub API data if project.json doesn't exist
              metadata = {
                name: repo.name,
                description: repo.description,
                status: 'active'
              };
            }

            return {
              id: repo.id,
              repoName: repo.name,
              url: repo.html_url,
              language: repo.language,
              updated: repo.updated_at,
              stars: repo.stargazers_count,
              ...metadata
            };
          })
        );

        setProjects(enrichedProjects);
        setLoading(false);
      } catch (err) {
        setError(err.message);
        setLoading(false);
      }
    };

    fetchProjects();
  }, [username]);

  return { projects, loading, error };
};
