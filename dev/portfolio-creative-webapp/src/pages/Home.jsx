import { Link } from 'react-router-dom';
import { useFeaturedProjects } from '../hooks/useFeaturedProjects';
import './Home.css';

export default function Home() {
  const GITHUB_USERNAME = import.meta.env.VITE_GITHUB_USERNAME || 'YOUR_USERNAME';
  const featuredProjects = useFeaturedProjects(GITHUB_USERNAME);

  return (
    <div className="home">
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-content">
          <h1 className="hero-title">
            Hi, I'm <span className="hero-highlight">Ronin</span>
          </h1>
          <p className="hero-subtitle">
            Software Engineer & Creative Developer
          </p>
          <p className="hero-description">
            I build interactive experiences at the intersection of code, design, and 3D animation. 
            Currently working on a therapeutic IntelliJ plugin and exploring the web with React & Three.js.
          </p>
          <div className="hero-cta">
            <Link to="/portfolio" className="btn btn-primary">
              View My Work
            </Link>
            <Link to="/contact" className="btn btn-secondary">
              Get in Touch
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Projects Section */}
      <section className="featured-section">
        <div className="section-container">
          <h2 className="section-title">Featured Projects</h2>
          <p className="section-subtitle">
            A selection of recent work. View all projects on my <Link to="/portfolio">portfolio</Link>.
          </p>

          {featuredProjects.length > 0 ? (
            <div className="featured-grid">
              {featuredProjects.map(project => (
                <a
                  key={project.id}
                  href={project.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="featured-card"
                >
                  <div className="featured-card-content">
                    <h3>{project.name || project.repoName}</h3>
                    <p>{project.description || 'No description available'}</p>
                    <div className="featured-tags">
                      {project.tech && project.tech.slice(0, 3).map(t => (
                        <span key={t} className="tech-tag">{t}</span>
                      ))}
                    </div>
                  </div>
                  <div className="featured-card-arrow">
                    →
                  </div>
                </a>
              ))}
            </div>
          ) : (
            <div className="loading">Loading featured projects...</div>
          )}

          <div className="featured-cta">
            <Link to="/portfolio" className="btn btn-outline">
              Explore All Projects
            </Link>
          </div>
        </div>
      </section>

      {/* Skills Section */}
      <section className="skills-section">
        <div className="section-container">
          <h2 className="section-title">Technical Skills</h2>
          
          <div className="skills-grid">
            <div className="skill-category">
              <h3>Languages</h3>
              <ul>
                <li>Java</li>
                <li>JavaScript</li>
                <li>Python</li>
                <li>Kotlin</li>
              </ul>
            </div>

            <div className="skill-category">
              <h3>Frontend</h3>
              <ul>
                <li>React</li>
                <li>Three.js</li>
                <li>CSS Grid & Flexbox</li>
                <li>Responsive Design</li>
              </ul>
            </div>

            <div className="skill-category">
              <h3>Tools & Platforms</h3>
              <ul>
                <li>IntelliJ IDEA</li>
                <li>Git & GitHub</li>
                <li>Vite</li>
                <li>Vercel</li>
              </ul>
            </div>

            <div className="skill-category">
              <h3>Creative</h3>
              <ul>
                <li>Blender</li>
                <li>Source Filmmaker</li>
                <li>Procreate</li>
                <li>Motion Design</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta-section">
        <div className="section-container cta-container">
          <h2>Let's Create Something Awesome</h2>
          <p>Whether you're looking for a developer, designer, or both, I'd love to hear from you.</p>
          <Link to="/contact" className="btn btn-primary btn-large">
            Start a Conversation
          </Link>
        </div>
      </section>
    </div>
  );
}
