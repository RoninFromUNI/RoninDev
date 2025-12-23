// src/utils/constants.js
export const PORTFOLIO_CONFIG = {
  author: 'RoninDev',
  title: import.meta.env.VITE_PORTFOLIO_TITLE || 'RoninDev - Portfolio',
  description: import.meta.env.VITE_PORTFOLIO_DESCRIPTION || 'Software Engineer & Creative Developer',
  github: 'https://github.com/YOUR_USERNAME',
  twitter: 'https://twitter.com/yourhandle',
  email: 'your.email@example.com',
};

export const THEME = {
  colors: {
    primary: '#0a0a0a',
    secondary: '#1a1a1a',
    textPrimary: '#ffffff',
    textSecondary: '#a0a0a0',
    accent: '#0099ff',
    accentHover: '#00ccff',
    border: '#222222',
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '2rem',
    xl: '4rem',
  },
};

export const BREAKPOINTS = {
  mobile: 480,
  tablet: 768,
  desktop: 1024,
  wide: 1440,
};

export const NAVIGATION = [
  { label: 'Home', path: '/' },
  { label: 'Portfolio', path: '/portfolio' },
  { label: 'About', path: '/about' },
  { label: 'Contact', path: '/contact' },
];

export const SOCIAL_LINKS = [
  { label: 'GitHub', url: 'https://github.com/YOUR_USERNAME', icon: 'github' },
  { label: 'Twitter', url: 'https://twitter.com/yourhandle', icon: 'twitter' },
  { label: 'Email', url: 'mailto:your.email@example.com', icon: 'email' },
];
