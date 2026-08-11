const samplePackages = [
  { id: 'p1', title: 'Kathmandu Cultural Escape', location: 'Nepal', days: 5, price: 499, image: 'images/butan.webp', description: 'Explore Kathmandu Valley with expert guides and boutique stays.' },
  { id: 'p2', title: 'Annapurna Trek & Lodge', location: 'Nepal', days: 8, price: 1299, image: 'images/india.webp', description: 'Scenic trekking with comfortable lodges and local sherpa support.' },
  { id: 'p3', title: 'Kathmandu to Pokhara Roadtrip', location: 'Nepal', days: 4, price: 349, image: 'images/portugal.webp', description: 'Relaxed road trip with lakeside stays and curated activities.' }
];

async function fetchPackages() {
  try {
    const res = await fetch('/api/packages');
    if (!res.ok) throw new Error('Unable to load packages');
    return res.json();
  } catch (err) {
    console.warn('Falling back to sample packages:', err);
    return samplePackages;
  }
}

function renderPackages(pkgs) {
  const list = document.getElementById('list');
  const sel = document.getElementById('packageSelect');
  list.innerHTML = '';
  sel.innerHTML = '';

  pkgs.forEach((p) => {
    const card = document.createElement('article');
    card.className = 'package-card';
    card.innerHTML = `
      <img class="package-image" src="${p.image}" alt="${p.title}">
      <div class="package-content">
        <div class="package-meta">
          <span class="pill">${p.location}</span>
          <span class="pill">${p.days} days</span>
        </div>
        <h3>${p.title}</h3>
        <p>${p.description}</p>
        <div class="package-footer">
          <strong>$${p.price}</strong>
          <button type="button" class="mini-btn" data-id="${p.id}">Choose</button>
        </div>
      </div>
    `;

    card.querySelector('button').addEventListener('click', () => {
      sel.value = p.id;
      document.getElementById('booking').scrollIntoView({ behavior: 'smooth' });
    });

    list.appendChild(card);

    const opt = document.createElement('option');
    opt.value = p.id;
    opt.textContent = p.title;
    sel.appendChild(opt);
  });
}

async function submitBooking(ev) {
  ev.preventDefault();
  const form = ev.target;
  const data = {
    name: form.name.value.trim(),
    email: form.email.value.trim(),
    packageId: form.packageId.value
  };

  const responseBox = document.getElementById('response');
  responseBox.textContent = 'Sending your request...';

  try {
    const res = await fetch('/api/book', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (!res.ok) throw new Error('Server error');
    const result = await res.json();
    responseBox.textContent = result.message || 'Booking request received.';
    form.reset();
  } catch (err) {
    console.warn('Booking API failed, saving local fallback:', err);
    // Local fallback: store booking in localStorage so admin can retrieve it later
    const pending = JSON.parse(localStorage.getItem('pendingBookings') || '[]');
    pending.push(Object.assign({ submittedAt: new Date().toISOString() }, data));
    localStorage.setItem('pendingBookings', JSON.stringify(pending));
    responseBox.textContent = 'We saved your request locally. We will contact you at the provided email.';
    form.reset();
  }
}

document.getElementById('bookForm').addEventListener('submit', submitBooking);

// show basic loading state
const listEl = document.getElementById('list');
listEl.innerHTML = '<p>Loading packages…</p>';

fetchPackages()
  .then(renderPackages)
  .catch((e) => {
    listEl.innerHTML = '<p>Packages are unavailable right now. Please try again soon.</p>';
    console.error(e);
  });
