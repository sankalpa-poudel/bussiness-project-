const express = require('express');
const path = require('path');
const fs = require('fs');
const bodyParser = require('body-parser');
const morgan = require('morgan');

const app = express();
app.use(morgan('dev'));
app.use(bodyParser.json());

const frontendRoot = path.join(__dirname, '..', 'frontend');
app.use(express.static(frontendRoot));

// Simple packages endpoint (mirrors frontend sample data if not present)
app.get('/api/packages', (req, res) => {
  const pkgs = [
    { id: 'p1', title: 'Kathmandu Cultural Escape', location: 'Nepal', days: 5, price: 499, image: '/images/butan.webp', description: 'Explore Kathmandu Valley with expert guides and boutique stays.' },
    { id: 'p2', title: 'Annapurna Trek & Lodge', location: 'Nepal', days: 8, price: 1299, image: '/images/india.webp', description: 'Scenic trekking with comfortable lodges and local sherpa support.' },
    { id: 'p3', title: 'Kathmandu to Pokhara Roadtrip', location: 'Nepal', days: 4, price: 349, image: '/images/portugal.webp', description: 'Relaxed road trip with lakeside stays and curated activities.' }
  ];
  res.json(pkgs);
});

// Booking endpoint: append to ndjson and respond
const bookingsFile = path.join(__dirname, 'data', 'bookings.ndjson');
app.post('/api/book', (req, res) => {
  const { name, email, packageId } = req.body || {};
  if (!name || !email || !packageId) {
    return res.status(400).json({ message: 'Missing required fields' });
  }

  const record = { id: Date.now(), name, email, packageId, createdAt: new Date().toISOString() };
  const line = JSON.stringify(record) + '\n';

  fs.appendFile(bookingsFile, line, (err) => {
    if (err) {
      console.error('Failed to write booking:', err);
      return res.status(500).json({ message: 'Failed to save booking' });
    }
    res.json({ message: 'Booking received. We will contact you shortly.' });
  });
});

// Fallback to index.html for SPA routes
app.get('*', (req, res) => {
  res.sendFile(path.join(frontendRoot, 'index.html'));
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`Server listening on http://localhost:${port}`));
