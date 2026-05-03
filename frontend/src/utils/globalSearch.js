/** Backend base URL — matches other feature pages */
const API = 'http://localhost:8080';

async function fetchArray(url, signal) {
  try {
    const res = await fetch(url, { signal });
    if (!res.ok) return [];
    const data = await res.json();
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function slice(arr, limit) {
  return arr.slice(0, limit);
}

/**
 * Parallel search across approved/public listings for the top-bar omnibox.
 * @param {string} query raw user input
 * @param {AbortSignal} signal
 * @param {number} limit max rows per bucket
 */
export async function fetchGlobalSearch(query, signal, limit = 8) {
  const q = query.trim();
  if (q.length < 2) {
    return {
      papers: [],
      rides: [],
      books: [],
      locations: [],
      notes: [],
      lost: [],
      events: [],
    };
  }

  const enc = encodeURIComponent(q);

  const ql = q.toLowerCase();

  const [
    papers,
    ridesDestOnly,
    booksSell,
    booksBuy,
    locations,
    notes,
    lostLost,
    lostFound,
    eventsRaw,
  ] = await Promise.all([
    fetchArray(`${API}/api/past-papers/search?query=${enc}`, signal),
    fetchArray(`${API}/api/rides/search?destination=${enc}`, signal),
    fetchArray(`${API}/api/books/search?query=${enc}&type=SELL`, signal),
    fetchArray(`${API}/api/books/search?query=${enc}&type=BUY`, signal),
    fetchArray(`${API}/api/campus-map/locations/search?query=${enc}`, signal),
    fetchArray(`${API}/api/notes?keyword=${enc}`, signal),
    fetchArray(`${API}/api/lost-found?type=Lost&keyword=${enc}`, signal),
    fetchArray(`${API}/api/lost-found?type=Found&keyword=${enc}`, signal),
    fetchArray(`${API}/api/events`, signal),
  ]);

  let rides = ridesDestOnly;
  if (rides.length === 0) {
    const allRides = await fetchArray(`${API}/api/rides`, signal);
    rides = allRides.filter((r) => {
      const parts = [
        r.origin,
        r.destination,
        r.departureTime,
        r.driverName,
        r.contactInfo,
        r.vehicleType,
        ...(Array.isArray(r.checkpoints) ? r.checkpoints : []),
      ];
      const hay = parts.map((x) => String(x ?? '').toLowerCase()).join(' ');
      return hay.includes(ql);
    });
  }

  const events = eventsRaw.filter((e) =>
    [e.title, e.description, e.venue, e.category, e.organizer].some((v) =>
      String(v || '')
        .toLowerCase()
        .includes(ql)
    )
  );

  const bookMap = new Map();
  for (const b of [...booksSell, ...booksBuy]) {
    if (b?.id != null) bookMap.set(b.id, b);
  }

  const lost = [...lostLost, ...lostFound];

  return {
    papers: slice(papers, limit),
    rides: slice(rides, limit),
    books: slice([...bookMap.values()], limit),
    locations: slice(locations, limit),
    notes: slice(notes, limit),
    lost: slice(lost, limit),
    events: slice(events, limit),
  };
}
