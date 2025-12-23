import './About.css';

export default function About() {
  return (
    <div className="about">
      <div className="about-container">
        <section className="about-hero">
          <h1>About Me</h1>
          <p className="about-lead">
            Software engineer and creative developer passionate about building elegant solutions 
            at the intersection of code and design.
          </p>
        </section>

        <section className="about-content">
          <div className="about-section">
            <h2>Background</h2>
            <p>
              I'm a final-year Software Engineering student exploring how technology can enhance 
              human wellbeing. My work spans full-stack web development, game development with Unity, 
              and 3D animation.
            </p>
            <p>
              Currently developing a therapeutic IntelliJ plugin for programmer wellness that 
              detects flow states and provides context-aware break suggestions.
            </p>
          </div>

          <div className="about-section">
            <h2>Experience & Projects</h2>
            <div className="project-list">
              <div className="project-item">
                <h3>Therapeutic IDE Plugin (FYP)</h3>
                <p className="project-meta">Java • IntelliJ SDK • Kotlin • Android</p>
                <p>
                  Designing and developing an IntelliJ plugin that monitors programmer stress levels 
                  and detects flow states, providing intelligent break suggestions without interrupting workflow.
                </p>
              </div>

              <div className="project-item">
                <h3>Blade Watcher</h3>
                <p className="project-meta">React • Three.js • MySQL • Data Visualization</p>
                <p>
                  Interactive dashboard visualizing London knife crime statistics with noir-inspired 
                  aesthetics and 3D data representation.
                </p>
              </div>

              <div className="project-item">
                <h3>Portfolio & Game Projects</h3>
                <p className="project-meta">React • Vite • Unity • C#</p>
                <p>
                  Full-stack portfolio webapp, isometric Unity game with advanced character animation 
                  systems and FSM-based enemy AI.
                </p>
              </div>
            </div>
          </div>

          <div className="about-section">
            <h2>Technical Intersection</h2>
            <p>
              I'm particularly interested in the intersection of software engineering and creative 
              development—where code meets design, animation, and human-centered experience.
            </p>
            <p>
              My toolkit spans programming (Java, JavaScript, Python, Kotlin) and creative tools 
              (Blender, Source Filmmaker, Procreate), allowing me to approach problems from 
              multiple disciplines.
            </p>
          </div>

          <div className="about-section">
            <h2>When I'm Not Coding</h2>
            <p>
              You'll find me exploring 3D animation in Blender or Source Filmmaker, playing 
              Cyberpunk 2077, or working on game development projects. I'm also passionate about 
              exploring new tech stacks and pushing the boundaries of what's possible in web and 
              game development.
            </p>
          </div>
        </section>

        <section className="about-cta">
          <h2>Let's Connect</h2>
          <p>
            Interested in collaborating or want to chat about tech, design, or games? 
            Reach out—I'd love to hear from you.
          </p>
          <a href="mailto:your.email@example.com" className="btn btn-primary">
            Get in Touch
          </a>
        </section>
      </div>
    </div>
  );
}
