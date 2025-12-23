import { useState, useEffect } from 'react';

/**
 * Hook to fetch GitHub repos + metadata files
 * Supports both public and private repos with authentication token
 */
export const useGitHubProjects = (username, fetchPrivate = false) => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        const token = import.meta.env.VITE_GITHUB_TOKEN;

        // Build headers with token if available
        const headers = {};
        if (token) {
          headers['Authorization'] = `token ${token}`;
        }

        // If we have a token and want private repos, use authenticated endpoint
        let reposUrl;
        if (token && fetchPrivate) {
          // Authenticated endpoint returns private + public repos
          reposUrl = `https://api.github.com/user/repos?sort=updated&per_page=100&affiliation=owner`;
        } else {
          // Public repos only
          reposUrl = `https://api.github.com/users/${username}/repos?sort=updated&per_page=100`;
        }

        const reposRes = await fetch(reposUrl, { headers });

        if (!reposRes.ok) {
          throw new Error(`GitHub API failed: ${reposRes.status}`);
        }

        const repos = await reposRes.json();

        // Filter for private if needed
        let filteredRepos = repos;
        if (fetchPrivate && token) {
          filteredRepos = repos.filter(r => r.private);
        } else if (!fetchPrivate) {
          filteredRepos = repos.filter(r => !r.private);
        }

        // Enrich each repo with metadata from project.json
        const enrichedProjects = await Promise.all(
            filteredRepos.map(async (repo) => {
              let metadata = {};
              try {
                // Try to fetch project.json from repo root
                const metaRes = await fetch(
                    `https://raw.githubusercontent.com/${repo.owner.login}/${repo.name}/main/project.json`,
                    { headers }
                );
                if (metaRes.ok) {
                  metadata = await metaRes.json();
                }
              } catch (e) {
                // Fallback to basic info
                metadata = {
                  name: repo.name,
                  description: repo.description,
                  status: repo.private ? 'private' : 'active'
                };
              }

              return {
                id: repo.id,
                repoName: repo.name,
                name: metadata.name || repo.name,
                url: repo.html_url,
                language: repo.language,
                updated: repo.updated_at,
                stars: repo.stargazers_count,
                private: repo.private,
                description: metadata.description || repo.description,
                tech: metadata.tech || [],
                status: metadata.status || (repo.private ? 'private' : 'active'),
                featured: metadata.featured || false,
                ...metadata
              };
            })
        );

        setProjects(enrichedProjects);
        setLoading(false);
      } catch (err) {
        console.error('GitHub fetch error:', err);
        setError(err.message);
        setLoading(false);
      }
    };

    fetchProjects();
  }, [username, fetchPrivate]);

  return { projects, loading, error };
};