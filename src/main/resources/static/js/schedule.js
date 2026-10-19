/* =====================================================================
   DevTracker V2 — schedule.js  (standard study timetable only)
   Works with schedule.html?roadmapId=N

   The only backend endpoint I could confirm is
   POST /api/schedule/generate?roadmapId={id}, and this page doesn't call it.
   "Fetch tasks" and "update task status" endpoints are NOT confirmed,
   so they're isolated in ENDPOINTS below. Fill them in once you've checked
   ScheduleController. Until then the page shows a clear message instead
   of calling anything made up.
   ===================================================================== */
(function () {
  'use strict';

  /* ── 1. CONFIG ───────────────────────────────────────────────────── */

  // null = no V2 page yet (link stays inert).
  const ROUTES = {
    dashboard: null,
    createRoadmap: null,
    trash: null,
    lesson: null,                 // Learn button
    syllabus: 'roadmap-view.html' // gets ?id=<roadmapId> appended
  };


  const ENDPOINTS = {
      // Load the standard timetable for a roadmap.
      list: (roadmapId) =>
          `/api/schedule/tasks?roadmapId=${encodeURIComponent(roadmapId)}`,

      // Save a task's completion status.
      update: (task, newStatus, roadmapId) => ({
          url: `/api/schedule/tasks/${encodeURIComponent(task.id)}?roadmapId=${encodeURIComponent(roadmapId)}&completed=${newStatus === 'COMPLETED'}`,
          init: {
              method: 'PATCH',
              headers: {
                  'Accept': 'application/json'
              },
              credentials: 'same-origin'
          }
      })
  };


  /* ── 2. HELPERS ──────────────────────────────────────────────────── */

  const $ = (id) => document.getElementById(id);
  const STATUS_ICON = {
    COMPLETED: 'ti-circle-check-filled',
    LEARNING: 'ti-circle-half-2',
    REVISION: 'ti-refresh-dot',
    NOT_STARTED: 'ti-circle'
  };
  const pctOf = (done, total) => (total > 0 ? Math.round(100 * done / total) : 0);

  // Property names come from the DailyTask getters used in schedule.jsp
  // (Jackson default naming). Confirm against DailyTask.java and adjust here.
  function normalizeTask(t) {
    return {
      id: t.id,
      subtopicId: t.subtopicId,
      subjectName: t.subjectName || '',
      topicName: t.topicName || '',
      subtopicName: t.subtopicName || '',
      plannedMinutes: Number(t.plannedMinutes) || 0,
      date: t.scheduleDate ? String(t.scheduleDate).slice(0, 10) : '',
      status: t.status || 'NOT_STARTED'
    };
  }

  const entries = [];   // { task, card, checkBtn, icon, dayRef }
  const days = [];      // { block, header, countEl, items: [entry] }
  let roadmapId = null;

  /* ── 3. PAGE STATE ───────────────────────────────────────────────── */

  function showOnly(which, msg) {
    $('scheduleLoading').hidden = which !== 'loading';
    $('scheduleError').hidden = which !== 'error';
    $('scheduleEmpty').hidden = which !== 'empty';
    $('plannerContainer').hidden = which !== 'planner';
    $('expandToggleBtnDays').hidden = which !== 'planner';
    if (which === 'error') $('scheduleError').textContent = msg;
  }

  /* ── 4. PROGRESS ─────────────────────────────────────────────────── */

  function recalc() {
    const total = entries.length;
    const done = entries.filter((e) => e.task.status === 'COMPLETED').length;
    const pct = pctOf(done, total);

    $('heroProgressFill').style.width = pct + '%';
    $('heroProgressTrack').setAttribute('aria-valuenow', pct);
    $('heroProgressText').textContent = pct + '% complete';
    $('totalTasksVal').textContent = total;
    $('completedVal').textContent = done;
    $('remainingVal').textContent = total - done;
    $('progressVal').textContent = pct + '%';

    days.forEach((d) => {
      const dDone = d.items.filter((e) => e.task.status === 'COMPLETED').length;
      d.countEl.textContent = dDone + '/' + d.items.length + ' tasks done';
    });
  }

  /* ── 5. RENDERING ────────────────────────────────────────────────── */

  function setIcon(entry, status) {
    entry.icon.className = 'ti ' + (STATUS_ICON[status] || STATUS_ICON.NOT_STARTED);
    const done = status === 'COMPLETED';
    const label = done ? 'Mark as not complete' : 'Mark as complete';
    entry.checkBtn.title = label;
    entry.checkBtn.setAttribute('aria-label', label);
  }

  function renderTask(task) {
    const frag = $('taskCardTemplate').content.cloneNode(true);
    const card = frag.querySelector('[data-task-card]');
    card.id = 'taskRow-' + task.id;
    frag.querySelector('[data-task-minutes]').textContent = task.plannedMinutes;
    frag.querySelector('[data-task-subject]').textContent = task.subjectName;
    frag.querySelector('[data-task-topic]').textContent = task.topicName;
    frag.querySelector('[data-task-subtopic]').textContent = task.subtopicName;

    const entry = {
      task,
      card,
      checkBtn: frag.querySelector('[data-task-check]'),
      icon: frag.querySelector('[data-task-check-icon]')
    };
    setIcon(entry, task.status);
    entry.checkBtn.addEventListener('click', () => toggleTask(entry));

    const learn = frag.querySelector('[data-task-learn]');
    const lessonRoute = ROUTES.lesson;
    if (lessonRoute) {
      learn.href = lessonRoute + (lessonRoute.includes('?') ? '&' : '?') +
        'subtopicId=' + encodeURIComponent(task.subtopicId) + '&roadmapId=' + encodeURIComponent(roadmapId);
    } else {
      learn.setAttribute('aria-disabled', 'true');
      learn.title = 'Lesson page not migrated to V2 yet';
      learn.addEventListener('click', (e) => {
        e.preventDefault();
        alert("Opening a lesson isn't available in V2 yet.");
      });
    }
    entries.push(entry);
    return { node: frag, entry };
  }

  function renderDays(tasks) {
    const container = $('plannerContainer');
    container.replaceChildren();
    entries.length = 0;
    days.length = 0;

    // Group by date, keeping the date order the API returned
    const order = [];
    const byDate = new Map();
    tasks.forEach((t) => {
      if (!byDate.has(t.date)) { byDate.set(t.date, []); order.push(t.date); }
      byDate.get(t.date).push(t);
    });

    order.forEach((date, idx) => {
      const frag = $('dayGroupTemplate').content.cloneNode(true);
      const block = frag.querySelector('[data-day-block]');
      const header = frag.querySelector('[data-day-header]');
      const body = frag.querySelector('[data-day-body]');
      const day = { block, header, countEl: frag.querySelector('[data-day-count]'), items: [] };

      block.id = 'day-' + date.replace(/-/g, '');
      frag.querySelector('[data-day-date]').textContent = date;

      byDate.get(date).forEach((t) => {
        const r = renderTask(t);
        r.entry.day = day;
        day.items.push(r.entry);
        body.append(r.node);
      });

      header.addEventListener('click', () => toggleDay(day));
      header.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggleDay(day); }
      });

      setDayOpen(day, idx === 0); // V1 opens the first day
      days.push(day);
      container.append(frag);
    });
    syncExpandButton();
  }

  /* ── 6. ACCORDION ────────────────────────────────────────────────── */

  function setDayOpen(day, open) {
    day.block.classList.toggle('open', open);
    day.header.setAttribute('aria-expanded', String(open));
  }
  function toggleDay(day) {
    setDayOpen(day, !day.block.classList.contains('open'));
    syncExpandButton();
  }
  function syncExpandButton() {
    const btn = $('expandToggleBtnDays');
    const allOpen = days.length > 0 && days.every((d) => d.block.classList.contains('open'));
    btn.querySelector('span').textContent = allOpen ? 'Collapse all' : 'Expand all';
    btn.classList.toggle('collapsed', !allOpen);
  }
  function toggleExpandAll() {
    const allOpen = days.every((d) => d.block.classList.contains('open'));
    days.forEach((d) => setDayOpen(d, !allOpen));
    syncExpandButton();
  }

  /* ── 7. TASK COMPLETION ──────────────────────────────────────────── */

  async function toggleTask(entry) {
    const task = entry.task;
    const newStatus = task.status === 'COMPLETED' ? 'NOT_STARTED' : 'COMPLETED';
    const req = ENDPOINTS.update(task, newStatus, roadmapId);
    if (!req) {
      alert("Saving task progress isn't available in V2 yet — the update endpoint isn't connected.");
      return;
    }

    entry.checkBtn.disabled = true;
    entry.icon.className = 'ti ti-loader-2';
    try {
      const res = await fetch(req.url, req.init);
      if (!res.ok) throw new Error('HTTP ' + res.status);
      task.status = newStatus;
      setIcon(entry, newStatus);
      recalc();
    } catch (err) {
      setIcon(entry, task.status); // roll back, nothing was saved
      alert('Could not update the task. Please try again.');
    } finally {
      entry.checkBtn.disabled = false;
    }
  }

  /* ── 8. SIDEBAR (MOBILE) ─────────────────────────────────────────── */

  function setSidebar(open) {
    $('sidebar').classList.toggle('open', open);
    $('overlay').classList.toggle('open', open);
    $('openSidebarBtn').setAttribute('aria-expanded', String(open));
    document.body.classList.toggle('sidebar-open', open);
  }

  /* ── 9. INIT ─────────────────────────────────────────────────────── */

  function applyRoutes() {
    document.querySelectorAll('[data-route]').forEach((a) => {
      const key = a.dataset.route;
      if (key === 'lesson') return; // handled per task
      let url = ROUTES[key];
      if (!url) {
        a.setAttribute('aria-disabled', 'true');
        a.title = 'Not migrated to V2 yet';
        a.addEventListener('click', (e) => e.preventDefault());
        return;
      }
      if (key === 'syllabus') url += (url.includes('?') ? '&' : '?') + 'id=' + encodeURIComponent(roadmapId);
      a.setAttribute('href', url);
    });
  }

  function bindStaticControls() {
    $('openSidebarBtn').addEventListener('click', () => setSidebar(true));
    $('overlay').addEventListener('click', () => setSidebar(false));
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') setSidebar(false); });
    $('expandToggleBtnDays').addEventListener('click', toggleExpandAll);
    $('logoutBtn').addEventListener('click', (e) => {
      e.preventDefault();
      alert("Logout isn't available in V2 yet.");
    });
  }


  async function init() {
      bindStaticControls();

      const id = new URLSearchParams(location.search).get('roadmapId');

      if (!id || !/^\d+$/.test(id)) {
          applyRoutes();
          showOnly('error',
              'No valid roadmap ID in the URL. Open this page as schedule.html?roadmapId=1');
          return;
      }

      roadmapId = id;
      applyRoutes();
      showOnly('loading');

      try {
          // 1. Load the existing timetable first.
          let res = await fetch(
              `/api/schedule/tasks?roadmapId=${encodeURIComponent(id)}`,
              {
                  headers: { Accept: 'application/json' },
                  credentials: 'same-origin'
              }
          );

          if (!res.ok) {
              if (res.status === 401) {
                  window.location.href = '/login.html';
                  return;
              }
              throw new Error(`Could not load timetable (HTTP ${res.status}).`);
          }

          let tasks = await res.json();

          if (!Array.isArray(tasks)) {
              throw new Error('Unexpected response while loading timetable.');
          }

          // 2. Generate only when no standard timetable exists.
          if (tasks.length === 0) {
              res = await fetch(
                  `/api/schedule/generate?roadmapId=${encodeURIComponent(id)}`,
                  {
                      method: 'POST',
                      headers: { Accept: 'application/json' },
                      credentials: 'same-origin'
                  }
              );

              const result = await res.json();

              if (!res.ok || !result.success) {
                  throw new Error(
                      result.message || 'Could not generate the timetable.'
                  );
              }

              // 3. Load the newly generated timetable.
              res = await fetch(
                  `/api/schedule/tasks?roadmapId=${encodeURIComponent(id)}`,
                  {
                      headers: { Accept: 'application/json' },
                      credentials: 'same-origin'
                  }
              );

              if (!res.ok) {
                  throw new Error('Timetable was generated but could not be loaded.');
              }

              tasks = await res.json();

              if (!Array.isArray(tasks)) {
                  throw new Error('Unexpected response after timetable generation.');
              }
          }

          // 4. Display the timetable.
          if (tasks.length === 0) {
              showOnly('empty');
              recalc();
              return;
          }

          renderDays(tasks.map(normalizeTask));
          showOnly('planner');
          recalc();

      } catch (err) {
          console.error('Schedule loading error:', err);
          showOnly('error', err.message || 'Could not load the timetable.');
      }
  }


  document.addEventListener('DOMContentLoaded', init);
})();