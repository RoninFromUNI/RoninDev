import { Link } from 'react-router-dom';
import './Footer.css';

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="footer-container">
        {/* Footer Sections */}
        <div className="footer-content">
          {/* About Section */}
          <div className="footer-section">
            <h3>RoninDev</h3>
            <p>
              Software engineer & creative developer exploring the intersection of code and design.
            </p>
          </div>

          {/* Links Section */}
          <div className="footer-section">
            <h4>Navigation</h4>
            <ul className="footer-links">
              <li><Link to="/">Home</Link></li>
              <li><Link to="/portfolio">Portfolio</Link></li>
              <li><Link to="/about">About</Link></li>
              <li><Link to="/contact">Contact</Link></li>
            </ul>
          </div>

          {/* Social Section */}
          <div className="footer-section">
            <h4>Connect</h4>
            <ul className="footer-social">
              <li>
                <a 
                  href="https://github.com/YOUR_USERNAME" 
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  GitHub
                </a>
              </li>
              <li>
                <a 
                  href="https://twitter.com/yourhandle" 
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Twitter
                </a>
              </li>
              <li>
                <a href="mailto:your.email@example.com">
                  Email
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Footer Bottom */}
        <div className="footer-bottom">
          <p className="footer-credit">
            © {currentYear} RoninDev. Built with React, Vite & deployed on Vercel.
          </p>
          <p className="footer-note">
            All projects are open-source on{' '}
            <a 
              href="https://github.com/YOUR_USERNAME" 
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </a>
          </p>
        </div>
      </div>
    </footer>
  );
}
