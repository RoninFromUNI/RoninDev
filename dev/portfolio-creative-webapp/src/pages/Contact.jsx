import { useState } from 'react';
import './Contact.css';

export default function Contact() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    message: ''
  });
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Replace with your actual contact endpoint
      // For now, just simulate success
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      console.log('Form data:', formData);
      setSubmitted(true);
      
      // Reset form after 3 seconds
      setTimeout(() => {
        setFormData({ name: '', email: '', subject: '', message: '' });
        setSubmitted(false);
      }, 3000);
    } catch (error) {
      console.error('Error submitting form:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="contact">
      <div className="contact-container">
        <section className="contact-hero">
          <h1>Get in Touch</h1>
          <p>
            Have a project in mind? Want to collaborate? Or just want to chat about tech and design? 
            I'd love to hear from you.
          </p>
        </section>

        <div className="contact-content">
          {/* Contact Form */}
          <section className="contact-form-section">
            <h2>Send Me a Message</h2>
            
            {submitted ? (
              <div className="success-message">
                <h3>✓ Message Sent!</h3>
                <p>
                  Thanks for reaching out. I'll get back to you as soon as possible.
                </p>
              </div>
            ) : (
              <form className="contact-form" onSubmit={handleSubmit}>
                <div className="form-group">
                  <label htmlFor="name">Name *</label>
                  <input
                    type="text"
                    id="name"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                    placeholder="Your name"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="email">Email *</label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    placeholder="your.email@example.com"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="subject">Subject *</label>
                  <input
                    type="text"
                    id="subject"
                    name="subject"
                    value={formData.subject}
                    onChange={handleChange}
                    required
                    placeholder="What's this about?"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="message">Message *</label>
                  <textarea
                    id="message"
                    name="message"
                    value={formData.message}
                    onChange={handleChange}
                    required
                    rows="6"
                    placeholder="Your message here..."
                  />
                </div>

                <button 
                  type="submit" 
                  className="btn btn-primary btn-large"
                  disabled={loading}
                >
                  {loading ? 'Sending...' : 'Send Message'}
                </button>
              </form>
            )}
          </section>

          {/* Contact Info */}
          <section className="contact-info-section">
            <h2>Other Ways to Connect</h2>
            
            <div className="contact-info-grid">
              <div className="contact-method">
                <h3>Email</h3>
                <a href="mailto:your.email@example.com">
                  your.email@example.com
                </a>
              </div>

              <div className="contact-method">
                <h3>GitHub</h3>
                <a 
                  href="https://github.com/YOUR_USERNAME"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  github.com/YOUR_USERNAME
                </a>
              </div>

              <div className="contact-method">
                <h3>Twitter</h3>
                <a 
                  href="https://twitter.com/yourhandle"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  @yourhandle
                </a>
              </div>

              <div className="contact-method">
                <h3>Location</h3>
                <p>London, UK</p>
              </div>
            </div>

            <div className="contact-faq">
              <h3>FAQ</h3>
              <div className="faq-item">
                <h4>How quickly do you respond?</h4>
                <p>I try to respond to messages within 24-48 hours. If it's urgent, feel free to reach out on social media.</p>
              </div>
              <div className="faq-item">
                <h4>What services do you offer?</h4>
                <p>I offer full-stack web development, game development consulting, and creative coding solutions. Let's discuss your specific needs.</p>
              </div>
              <div className="faq-item">
                <h4>What's your availability?</h4>
                <p>Currently focused on my Final Year Project, but open to interesting collaboration opportunities. Let's talk!</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
