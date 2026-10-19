/* =====================================================================
   DevTracker V2 — dashboard.js
   Keeps your existing GET /api/dashboard loader, and adds what the V1 JSP
   had: inline edit form, delete, safe DOM rendering, initials fallback,
   progress animation, loading UI and error state.

   Edit / delete have no confirmed V2 endpoint yet, so they're isolated in
   ENDPOINTS below (null = not connected, user gets a clear message).
   ===================================================================== */
(function () {
  'use strict';

  /* ── 1. CONFIG ───────────────────────────────────────────────────── */

  const ROUTES = {
    login: '/login.html',
    syllabus: '/roadmap-view.html',   // ?id=<roadmapId>
    schedule: '/schedule.html'        // ?roadmapId=<roadmapId>
  };

  const ENDPOINTS = {
    // Return { url, init } for the fetch() that saves a roadmap edit, or null.
    // payload = { title, description }
    update: (id, payload) => null,

    // Return { url, init } for the fetch() that deletes a roadmap, or null.
    remove: (id) => null
  };

  /* ── 2. HELPERS ──────────────────────────────────────────────────── */

  const $ = (id) => document.getElementById(id);
  const tpl = (id) => $(id).content.cloneNode(true);

  function progressTier(p) { return p >= 100 ? 'high' : p >= 40 ? 'medium' : 'low'; }

  function clampPercent(v) {
    const n = Number(v);
    return Number.isFinite(n) ? Math.max(0, Math.min(100, Math.round(n))) : 0;
  }

  // Backend sends ISO strings like 2026-10-15T00:00:00.000Z — show the date part.
  function formatDate(date) {
    return date ? String(date).slice(0, 10) : '';
  }

  function makeInitials(name) {
    if (!name || !name.trim()) return '';
    const parts = name.trim().split(/\s+/);
    let out = parts[0].charAt(0).toUpperCase();
    if (parts.length > 1) out += parts[parts.length - 1].charAt(0).toUpperCase();
    return out;
  }

  function notConnected(what) {
    alert(what + " isn't available in V2 yet — the roadmap API for it hasn't been connected.");
  }

  /* ── 3. LOADING UI FRAGMENT ──────────────────────────────────────── */

  async function loadLoadingUi() {
    try {
      const res = await fetch('/includes/loading.html');
      if (res.ok) $('loadingContainer').innerHTML = await res.text(); // static local fragment
    } catch (_) { /* loading UI is optional */ }
  }

  /* ── 4. LOAD DASHBOARD ───────────────────────────────────────────── */

  async function loadDashboard() {
    try {
      const response = await fetch('/api/dashboard', { method: 'GET', credentials: 'include' });

      // Not authenticated
      if (response.status === 401) {
        window.location.href = ROUTES.login;
        return;
      }
      if (!response.ok) throw new Error('Failed to load dashboard');

      const data = await response.json();
      renderUser(data);
      renderStats(data);
      renderRoadmaps(data.roadmaps || []);
    } catch (error) {
      console.error('Dashboard loading error:', error);
      showGridMessage('Could not load your dashboard. Please refresh the page.');
    }
  }

  /* ── 5. RENDER: USER & STATS ─────────────────────────────────────── */

  function renderUser(data) {
    const name = data.userName || '';
    $('initials').textContent = data.initials || makeInitials(name) || '--';
    $('sidebarUserName').textContent = name;
    $('welcomeName').textContent = name ? name.trim().split(/\s+/)[0] : '';
  }

  function renderStats(data) {
    $('totalRoadmaps').textContent = data.total ?? 0;
    $('avgProgress').textContent = (data.avgProgress ?? 0) + '%';
    $('completedRoadmaps').textContent = data.completed ?? 0;
    $('aiCount').textContent = data.aiCount ?? 0;
    $('roadmapCount').textContent = (data.total ?? 0) + ' active';
  }

  /* ── 6. RENDER: ROADMAP CARDS ────────────────────────────────────── */

  function showGridMessage(text) {
    const grid = $('roadmapGrid');
    grid.replaceChildren();
    const frag = tpl('gridMessageTemplate');
    frag.querySelector('[data-message]').textContent = text;
    grid.append(frag);
  }

  function renderRoadmaps(roadmaps) {
    const grid = $('roadmapGrid');
    grid.replaceChildren();

    if (roadmaps.length === 0) {
      showGridMessage('No roadmaps yet. Create one to get started.');
    }
    roadmaps.forEach((r) => grid.append(createRoadmapCard(r)));
    grid.append(tpl('newRoadmapCardTemplate'));
  }

  function createRoadmapCard(roadmap) {
    const frag = tpl('roadmapCardTemplate');
    const progress = clampPercent(roadmap.progress);
    const isDone = progress >= 100;

    // Badges
    const badges = frag.querySelector('[data-badges]');
    if (roadmap.ai) badges.append(tpl('badgeAiTemplate'));
    if (isDone) badges.append(tpl('badgeDoneTemplate'));

    // Text
    const titleEl = frag.querySelector('[data-title]');
    const descEl = frag.querySelector('[data-desc]');
    titleEl.textContent = roadmap.title || '';
    descEl.textContent = roadmap.description || '';

    // Progress (animate from 0 so the CSS transition runs)
    frag.querySelector('[data-progress-text]').textContent = progress + '%';
    const fill = frag.querySelector('[data-progress-fill]');
    fill.classList.add(progressTier(progress));
    fill.style.width = '0%';
    requestAnimationFrame(() => setTimeout(() => { fill.style.width = progress + '%'; }, 60));

    // Dates
    frag.querySelector('[data-dates]').textContent =
      formatDate(roadmap.startDate) + ' – ' + formatDate(roadmap.targetDate);

    // Links
    frag.querySelector('[data-syllabus-link]').href =
      ROUTES.syllabus + '?id=' + encodeURIComponent(roadmap.id);
    frag.querySelector('[data-schedule-link]').href =
      ROUTES.schedule + '?roadmapId=' + encodeURIComponent(roadmap.id);

    // Inline edit / delete
    const editForm = frag.querySelector('[data-edit-form]');
    const deleteForm = frag.querySelector('[data-delete-form]');
    editForm.elements.title.value = roadmap.title || '';
    editForm.elements.description.value = roadmap.description || '';

    frag.querySelector('[data-edit-btn]').addEventListener('click', () => {
      editForm.style.display = 'flex';
      deleteForm.style.display = 'block';
      titleEl.style.display = 'none';
      descEl.style.display = 'none';
    });
    frag.querySelector('[data-cancel-btn]').addEventListener('click', () => {
      editForm.style.display = 'none';
      deleteForm.style.display = 'none';
      titleEl.style.display = '';
      descEl.style.display = '';
    });

    editForm.addEventListener('submit', (e) => {
      e.preventDefault();
      saveRoadmap(roadmap.id, {
        title: editForm.elements.title.value.trim(),
        description: editForm.elements.description.value.trim()
      });
    });
    deleteForm.addEventListener('submit', (e) => {
      e.preventDefault();
      deleteRoadmap(roadmap.id);
    });

    return frag;
  }

  /* ── 7. EDIT / DELETE (isolated until V2 endpoints exist) ────────── */

  async function saveRoadmap(id, payload) {
    const req = ENDPOINTS.update(id, payload);
    if (!req) { notConnected('Editing a roadmap'); return; }
    try {
      const res = await fetch(req.url, req.init);
      if (res.status === 401) { window.location.href = ROUTES.login; return; }
      if (!res.ok) throw new Error('HTTP ' + res.status);
      await loadDashboard();
    } catch (err) {
      alert('Could not save the roadmap. Please try again.');
    }
  }

  async function deleteRoadmap(id) {
    const req = ENDPOINTS.remove(id);
    if (!req) { notConnected('Deleting a roadmap'); return; }
    if (!confirm('Delete this roadmap?')) return;
    try {
      const res = await fetch(req.url, req.init);
      if (res.status === 401) { window.location.href = ROUTES.login; return; }
      if (!res.ok) throw new Error('HTTP ' + res.status);
      await loadDashboard();
    } catch (err) {
      alert('Could not delete the roadmap. Please try again.');
    }
  }

  /* ── 8. INIT ─────────────────────────────────────────────────────── */

  document.addEventListener('DOMContentLoaded', () => {
    loadLoadingUi();
    loadDashboard();
  });
})();