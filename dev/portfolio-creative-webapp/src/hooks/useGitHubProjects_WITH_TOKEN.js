import { useState, useEffect } from 'react';

/**
 * Hook to fetch GitHub repos + metadata files
 * Supports both public and private repos with authentication token
 */
export const useGitHubProjects = (username, includePrivate = false) => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        // Build fetch headers with optional token for private repo access
        const headers = {};
        const token = import.meta.env.VITE_GITHUB_TOKEN;
        if (token) {
          headers['Authorization'] = `token ${token}`;
        }

        // Fetch repos from GitHub API
        const repoType = includePrivate && token ? 'private' : 'public';
        const reposRes = await fetch(
          `https://api.github.com/user/repos?type=${repoType}&sort=updated&per_page=100`,
          { headers }
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
                `https://raw.githubusercontent.com/${username}/${repo.name}/main/project.json`,
                { headers }
              );
              if (metaRes.ok) {
                metadata = await metaRes.json();
              }
            } catch (e) {
              // Fallback: use GitHub API data if project.json doesn't exist
              metadata = {
                name: repo.name,
                description: repo.description,
                status: repo.private ? 'private' : 'active'
              };
            }

            return {
              id: repo.id,
              repoName: repo.name,
              url: repo.html_url,
              language: repo.language,
              updated: repo.updated_at,
              stars: repo.stargazers_count,
              private: repo.private,
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
  }, [username, includePrivate]);

  return { projects, loading, error };
};
