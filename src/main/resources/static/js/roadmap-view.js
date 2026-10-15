/* =====================================================================
   DevTracker V2 — roadmap-view.js
   Migrated from V1 ai-roadmap-view.jsp.
   Only GET /api/roadmaps/{id} is connected. Every other V1 action is
   isolated behind notifyPending() until its V2 endpoint exists.
   ===================================================================== */
(function () {
  'use strict';

  /* ── 1. CONFIG ───────────────────────────────────────────────────── */

  // Fill these in once the V2 pages exist. null = not migrated yet
  // (link stays inert; legacy .jsp routes are never called).
  const ROUTES = {
    dashboard: null,
    createRoadmap: null,
    aiRoadmaps: null,
    schedule: null,   // will receive ?roadmapId=<id>
    trash: null
  };

  // Features whose V2 endpoints do not exist yet.
  const PENDING = {
    updateSubTopic: 'Marking lessons complete',
    editSubject: 'Editing subjects',
    deleteSubject: 'Deleting subjects',
    editTopic: 'Editing topics',
    deleteTopic: 'Deleting topics',
    editSubTopic: 'Editing lessons',
    deleteSubTopic: 'Deleting lessons',
    addSubject: 'Adding subjects',
    addTopic: 'Adding topics',
    addSubTopic: 'Adding lessons',
    generateLearning: 'Opening a lesson',
    generateLearningJob: 'AI lesson generation',
    downloadRoadmapPDF: 'Roadmap PDF download',
    logout: 'Logout'
  };

  /* ── 2. SMALL HELPERS ────────────────────────────────────────────── */

  const $ = (id) => document.getElementById(id);

  function h(tag, opts, ...children) {
    const e = document.createElement(tag);
    opts = opts || {};
    if (opts.class) e.className = opts.class;
    if (opts.id) e.id = opts.id;
    if (opts.text != null) e.textContent = opts.text;
    if (opts.style) e.style.cssText = opts.style;
    if (opts.attrs) for (const k in opts.attrs) e.setAttribute(k, opts.attrs[k]);
    children.flat().forEach((c) => { if (c) e.append(c); });
    return e;
  }
  const icon = (name, style) => h('i', { class: 'ti ' + name, style: style || '' });

  function setButtonContent(btn, iconName, label) {
    btn.replaceChildren(icon(iconName), document.createTextNode(' ' + label));
  }

  function notifyPending(key) {
    alert((PENDING[key] || 'This feature') +
      " isn't available in V2 yet — the backend endpoint hasn't been migrated.");
  }

  function progressTier(pct) { return pct >= 100 ? 'high' : pct >= 40 ? 'medium' : 'low'; }
  function pctOf(done, total) { return total > 0 ? Math.round(100 * done / total) : 0; }
  function formatMinutes(m) { return Math.floor(m / 60) + 'h ' + (m % 60) + 'm'; }
  const arr = (v) => (Array.isArray(v) ? v : []);

  /* ── 3. JSON NORMALISATION (single place to fix property names) ──────
     Names inferred from the V1 getters via Jackson conventions:
       getSubtopics()->subtopics, isCompleted()->completed,
       getEstimatedMinutes()->estimatedMinutes, getAiLearning()->aiLearning.
     CONFIRM against the V2 model classes; adjust only here.            */

  function normalizeSubTopic(s) {
    return {
      id: s.id,
      name: s.name || '',
      difficulty: s.difficulty || 'medium',
      minutes: Number(s.estimatedMinutes) || 0,
      completed: s.completed === true,
      aiLearning: s.aiLearning || ''
    };
  }
  function normalizeTopic(t) {
    return {
      id: t.id,
      name: t.name || '',
      subtopics: arr(t.subtopics !== undefined ? t.subtopics : t.subTopics).map(normalizeSubTopic)
    };
  }
  function normalizeSubject(s) {
    return { id: s.id, name: s.name || '', topics: arr(s.topics).map(normalizeTopic) };
  }
  function normalizeRoadmap(r) {
    return {
      id: r.id,
      title: r.title || '',
      description: r.description || '',
      ai: r.ai === true,
      subjects: arr(r.subjects).map(normalizeSubject)
    };
  }

  /* ── 4. PROGRESS & STATS ─────────────────────────────────────────── */

  function computeStats(rm) {
    const s = { subjects: rm.subjects.length, topics: 0, lessons: 0, minutes: 0, done: 0, generated: 0 };
    rm.subjects.forEach((sub) => {
      s.topics += sub.topics.length;
      sub.topics.forEach((t) => t.subtopics.forEach((st) => {
        s.lessons++;
        s.minutes += st.minutes;
        if (st.completed) s.done++;
        if (st.aiLearning.trim() !== '') s.generated++;
      }));
    });
    s.pct = pctOf(s.done, s.lessons);
    s.allGenerated = s.lessons > 0 && s.generated === s.lessons;
    return s;
  }

  const topicDone = (t) => t.subtopics.filter((x) => x.completed).length;
  const topicPct = (t) => pctOf(topicDone(t), t.subtopics.length);
  function subjectPct(sub) {
    let done = 0, total = 0;
    sub.topics.forEach((t) => { done += topicDone(t); total += t.subtopics.length; });
    return pctOf(done, total);
  }

  /* ── 5. PAGE STATE ───────────────────────────────────────────────── */

  const CONTENT_IDS = ['syllabusHero', 'learningHint', 'toolbarActions',
    'addSubjectPanel', 'customizeHint', 'expandToggleBtn'];

  function showState(msg) {
    const el = $('pageState');
    el.textContent = msg;
    el.style.display = '';
    CONTENT_IDS.forEach((id) => { $(id).style.display = 'none'; });
    $('syllabusTree').replaceChildren();
  }
  function showContent() {
    $('pageState').style.display = 'none';
    CONTENT_IDS.forEach((id) => { $(id).style.display = ''; });
  }

  /* ── 6. RENDER: HERO / TOOLBAR ───────────────────────────────────── */

  function renderHero(rm, stats) {
    document.title = rm.title + ' — Syllabus | DevTracker';
    $('breadcrumbTitle').textContent = rm.title;
    $('heroTitle').textContent = rm.title;
    $('heroDesc').textContent = rm.description;
    $('heroAiPill').style.display = rm.ai ? '' : 'none';

    $('statSubjects').textContent = stats.subjects;
    $('statTopics').textContent = stats.topics;
    $('statLessons').textContent = stats.lessons;
    $('statHours').textContent = Math.floor(stats.minutes / 60);

    $('heroProgressFill').style.width = stats.pct + '%';
    $('heroProgressText').textContent = stats.pct + '% complete';
    $('heroPercentVal').textContent = stats.pct + '%';
  }

  function renderToolbar(stats) {
    $('generateLearningBtn').style.display = stats.allGenerated ? 'none' : '';
    const pdf = $('downloadPdfBtn');
    setButtonContent(pdf, 'ti-file-download',
      'Download Roadmap PDF (' + stats.generated + '/' + stats.lessons + ')');
    pdf.dataset.allGenerated = String(stats.allGenerated);
    pdf.dataset.generated = stats.generated;
    pdf.dataset.total = stats.lessons;
  }

  /* ── 7. RENDER: SUBTOPICS ────────────────────────────────────────── */

  function pendingForm(form, key) {
    form.addEventListener('submit', (e) => { e.preventDefault(); notifyPending(key); });
  }

  function renderSubtopic(st, isFirstEver) {
    const diff = st.difficulty.toLowerCase().trim();
    const diffClass = diff === 'easy' ? 'diff-easy' : diff === 'hard' ? 'diff-hard' : 'diff-medium';
    const nodes = [];

    // Read row
    const editBtn = () => {
      const b = h('button', { class: 'inline-edit-btn', attrs: { type: 'button', title: 'Edit lesson' } }, icon('ti-pencil'));
      b.addEventListener('click', () => toggleSubtopicEdit(st.id, true));
      return b;
    };
    const checkTitle = st.completed ? 'Mark as not complete' : 'Mark as complete';
    const check = h('button', {
      class: 'lesson-check-btn',
      attrs: { type: 'button', title: checkTitle, 'aria-label': checkTitle }
    }, icon(st.completed ? 'ti-circle-check-filled' : 'ti-circle'));
    check.addEventListener('click', () => notifyPending('updateSubTopic'));

    const link = h('a', { class: 'lesson-link', text: st.name, attrs: { role: 'button', tabindex: '0', 'aria-disabled': 'true' } });
    const learn = h('a', { class: 'task-learn-btn', attrs: { role: 'button', tabindex: '0', 'aria-disabled': 'true' } },
      icon('ti-book-2'), h('span', { text: 'Learn' }));
    [link, learn].forEach((a) => a.addEventListener('click', () => notifyPending('generateLearning')));

    const row = h('div', { class: 'subtopic-row' + (isFirstEver ? ' tutorial-target' : ''), id: 'str-' + st.id },
      editBtn(),
      h('div', { class: 'lesson-title' }, check, link, learn, editBtn()),
      h('div', { class: 'subtopic-right' },
        h('span', { class: 'hours-pill ' + diffClass },
          icon('ti-clock', 'font-size: 11px'), document.createTextNode(' ' + formatMinutes(st.minutes)))));
    nodes.push(row);

    // Edit row (hidden)
    const nameIn = h('input', { attrs: { type: 'text', name: 'name', placeholder: 'Subtopic name' } });
    nameIn.value = st.name;
    const sel = h('select', { attrs: { name: 'difficulty' } });
    ['Easy', 'Medium', 'Hard'].forEach((d) => {
      const o = h('option', { text: d });
      o.selected = d.toLowerCase() === diff;
      sel.append(o);
    });
    const mins = h('input', { attrs: { type: 'number', name: 'minutes', min: '1', step: '1' } });
    mins.value = st.minutes;
    const cancel = h('button', { class: 'btn btn-xs', text: 'Cancel', attrs: { type: 'button' } });
    cancel.addEventListener('click', () => toggleSubtopicEdit(st.id, false));
    const editForm = h('form', { class: 'subtopic-edit-row', id: 'ste-' + st.id }, nameIn, sel, mins,
      h('div', { class: 'subtopic-edit-btns' },
        h('button', { class: 'btn btn-primary btn-xs', attrs: { type: 'submit' } }, icon('ti-check'), document.createTextNode(' Save')),
        cancel));
    pendingForm(editForm, 'editSubTopic');
    nodes.push(editForm);

    const delForm = h('form', { class: 'subtopic-delete-form', id: 'stdel-' + st.id, style: 'display: none' },
      h('button', { class: 'row-delete-btn', attrs: { type: 'submit' } }, icon('ti-trash')));
    pendingForm(delForm, 'deleteSubTopic');
    nodes.push(delForm);

    return nodes;
  }

  function renderAddSubtopic(topic) {
    const btn = h('div', { class: 'add-plus', id: 'addSubBtn-' + topic.id }, icon('ti-plus'), h('span', { text: 'Add Lesson' }));
    const nameIn = h('input', { attrs: { type: 'text', name: 'name', placeholder: 'New subtopic name…', required: '' } });
    const sel = h('select', { attrs: { name: 'difficulty' } });
    ['Easy', 'Medium', 'Hard'].forEach((d) => { const o = h('option', { text: d }); o.selected = d === 'Medium'; sel.append(o); });
    const mins = h('input', { attrs: { type: 'number', name: 'minutes', min: '15', step: '1' } });
    mins.value = 60;
    const cancel = h('button', { class: 'btn btn-xs', text: 'Cancel', attrs: { type: 'button' } });
    const form = h('form', { class: 'subtopic-edit-row', id: 'addSub-' + topic.id, style: 'display: none; background: var(--surface-2);' },
      nameIn, sel, mins,
      h('div', { class: 'subtopic-edit-btns' },
        h('button', { class: 'btn btn-xs btn-primary', attrs: { type: 'submit' } }, icon('ti-plus'), document.createTextNode(' Add')),
        cancel));
    btn.addEventListener('click', () => { btn.style.display = 'none'; form.style.display = 'flex'; });
    cancel.addEventListener('click', () => { form.style.display = 'none'; btn.style.display = 'flex'; });
    pendingForm(form, 'addSubTopic');
    return [btn, form];
  }

  /* ── 8. RENDER: TOPICS ───────────────────────────────────────────── */

  function renderTopic(topic, si, ti) {
    const key = si + '-' + ti;
    const sectionId = 'ts-' + key;
    const pct = topicPct(topic);

    const dot = h('div', { class: 'topic-dot', style: 'cursor: pointer; width: 8px; height: 8px; flex-shrink: 0' });
    const editBtn = h('button', { class: 'inline-edit-btn', attrs: { type: 'button', title: 'Edit topic' } }, icon('ti-pencil'));
    const name = h('span', { class: 'topic-name', id: 'tName-' + key, text: topic.name, style: 'cursor: pointer' });

    const nameIn = h('input', { attrs: { type: 'text', name: 'name', placeholder: 'Topic name' } });
    nameIn.value = topic.name;
    const cancel = h('button', { class: 'btn btn-xs', text: 'Cancel', attrs: { type: 'button' } });
    const editForm = h('form', { class: 'topic-edit-form', id: 'tEF-' + key }, nameIn,
      h('div', { class: 'topic-edit-form-btns' },
        h('button', { class: 'btn btn-primary btn-xs', attrs: { type: 'submit' } }, icon('ti-check'), document.createTextNode(' Save')),
        cancel));
    pendingForm(editForm, 'editTopic');

    const delForm = h('form', { class: 'topic-delete-form', id: 'tdel-' + key, style: 'display: none' },
      h('button', { class: 'row-delete-btn', attrs: { type: 'submit' } }, icon('ti-trash')));
    pendingForm(delForm, 'deleteTopic');

    const progressBox = h('div', { class: 'topic-progress-box' },
      h('span', { class: 'count-pill', id: 'topicCnt-' + topic.id, text: topicDone(topic) + '/' + topic.subtopics.length + ' lessons completed' }),
      h('div', { class: 'mini-progress' },
        h('div', { id: 'topicBar-' + topic.id, class: 'progress-fill ' + progressTier(pct), style: 'width:' + pct + '%' })),
      h('span', { id: 'topicProgress-' + topic.id, text: pct + '%' }));
    const chevron = h('i', { class: 'ti ti-chevron-down topic-chevron' });

    const header = h('div', { class: 'topic-header' }, dot, editBtn, name, editForm, delForm, progressBox, chevron);

    const table = h('div', { class: 'subtopic-table', id: 'subtable-' + topic.id });
    topic.subtopics.forEach((st, i) => {
      renderSubtopic(st, si === 1 && ti === 1 && i === 0).forEach((n) => table.append(n));
    });
    renderAddSubtopic(topic).forEach((n) => table.append(n));

    const section = h('div', { class: 'topic-section', id: sectionId }, header, table);

    [dot, name, chevron].forEach((el) => el.addEventListener('click', () => section.classList.toggle('open')));
    editBtn.addEventListener('click', () => {
      name.style.display = 'none'; editForm.style.display = 'flex'; delForm.style.display = 'flex';
    });
    cancel.addEventListener('click', () => {
      name.style.display = ''; editForm.style.display = 'none'; delForm.style.display = 'none';
    });
    return section;
  }

  function renderAddTopic(subject) {
    const btn = h('div', { class: 'add-plus', id: 'addTopicBtn-' + subject.id }, icon('ti-plus'), h('span', { text: 'Add Topic' }));
    const input = h('input', {
      attrs: { type: 'text', name: 'name', placeholder: 'Add a topic…', required: '' },
      style: 'flex: 1; padding: 7px 11px; font-size: 13px; border-radius: 8px; border: 1px solid var(--border); font-family: var(--font-display); outline: none; background: var(--surface); color: var(--text);'
    });
    const cancel = h('button', { class: 'btn btn-xs', text: 'Cancel', attrs: { type: 'button' } });
    const form = h('form', { class: 'topic-header', id: 'addTopic-' + subject.id, style: 'display: none; background: var(--surface-2);' },
      icon('ti-plus', 'color: var(--accent); font-size: 14px; flex-shrink: 0'), input,
      h('button', { class: 'btn btn-xs btn-primary', attrs: { type: 'submit' } }, icon('ti-plus'), document.createTextNode(' Add topic')),
      cancel);
    btn.addEventListener('click', () => { btn.style.display = 'none'; form.style.display = 'flex'; });
    cancel.addEventListener('click', () => { form.style.display = 'none'; btn.style.display = 'flex'; });
    pendingForm(form, 'addTopic');
    return [btn, form];
  }

  /* ── 9. RENDER: SUBJECTS ─────────────────────────────────────────── */

  function renderSubject(subject, si) {
    const blockId = 'sb-' + si;
    const pct = subjectPct(subject);
    const lessonCount = subject.topics.reduce((n, t) => n + t.subtopics.length, 0);

    const index = h('div', { class: 'subject-index', text: String(si), style: 'cursor: pointer' });
    const editBtn = h('button', { class: 'inline-edit-btn', attrs: { type: 'button', title: 'Edit subject' } }, icon('ti-pencil'));
    const name = h('span', { class: 'subject-name', id: 'sName-' + si, text: subject.name });
    const left = h('div', { class: 'subject-header-left', style: 'cursor: pointer' }, editBtn, name);

    const nameIn = h('input', { attrs: { type: 'text', name: 'name', placeholder: 'Subject name' } });
    nameIn.value = subject.name;
    const cancel = h('button', { class: 'btn btn-xs', text: 'Cancel', attrs: { type: 'button' } });
    const editForm = h('form', { class: 'subject-edit-form', id: 'sEF-' + si }, nameIn,
      h('div', { class: 'subject-edit-form-btns' },
        h('button', { class: 'btn btn-primary btn-xs', attrs: { type: 'submit' } }, icon('ti-check'), document.createTextNode(' Save')),
        cancel));
    pendingForm(editForm, 'editSubject');

    const delForm = h('form', { class: 'subject-delete-form', id: 'sdel-' + si, style: 'display: none' },
      h('button', { class: 'row-delete-btn', attrs: { type: 'submit' } }, icon('ti-trash')));
    pendingForm(delForm, 'deleteSubject');

    const countPill = h('span', { class: 'count-pill', id: 'sCnt-' + si, text: subject.topics.length + ' topics · ' + lessonCount + ' Lessons' });
    const chevron = h('i', { class: 'ti ti-chevron-down chevron-icon' });
    const meta = h('div', { class: 'subject-meta' },
      h('div', { class: 'subject-progress-box' },
        countPill,
        h('span', { id: 'subjectProgress-' + subject.id, class: 'progress-text', text: pct + '%' }),
        h('div', { class: 'mini-progress' },
          h('div', { id: 'subjectBar-' + subject.id, class: 'progress-fill ' + progressTier(pct), style: 'width:' + pct + '%' }))),
      chevron);

    const header = h('div', { class: 'subject-header' }, index, left, editForm, delForm, meta);

    const body = h('div', { class: 'subject-body' });
    subject.topics.forEach((t, i) => body.append(renderTopic(t, si, i + 1)));
    renderAddTopic(subject).forEach((n) => body.append(n));

    const block = h('div', { class: 'subject-block', id: blockId }, header, body);

    [index, name, chevron].forEach((el) => el.addEventListener('click', () => block.classList.toggle('open')));
    editBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      name.style.display = 'none'; countPill.style.display = 'none';
      editForm.style.display = 'flex'; delForm.style.display = 'flex';
    });
    cancel.addEventListener('click', () => {
      name.style.display = ''; countPill.style.display = '';
      editForm.style.display = 'none'; delForm.style.display = 'none';
    });
    return block;
  }

  function renderTree(rm) {
    const tree = $('syllabusTree');
    tree.replaceChildren();
    if (rm.subjects.length === 0) {
      tree.append(h('div', { class: 'empty-state', text: 'This roadmap has no subjects yet. Use Customize to add some.' }));
      return;
    }
    rm.subjects.forEach((s, i) => tree.append(renderSubject(s, i + 1)));
  }

  /* ── 10. INTERACTIONS ────────────────────────────────────────────── */

  // Accordion / expand-collapse all
  let allExpanded = true;
  function setAllOpen(open) {
    document.querySelectorAll('.subject-block, .topic-section')
      .forEach((el) => el.classList.toggle('open', open));
  }
  function toggleExpandAll() {
    const btn = $('expandToggleBtn');
    if (allExpanded) {
      setAllOpen(false);
      btn.querySelector('span').textContent = 'Expand all';
      btn.classList.add('collapsed');
    } else {
      setAllOpen(true);
      btn.querySelector('span').textContent = 'Collapse all';
      btn.classList.remove('collapsed');
    }
    allExpanded = !allExpanded;
  }

  // Subtopic inline edit
  function toggleSubtopicEdit(id, editing) {
    $('str-' + id).style.display = editing ? 'none' : '';
    $('ste-' + id).style.display = editing ? 'flex' : 'none';
    $('stdel-' + id).style.display = editing ? 'flex' : 'none';
  }

  // Customize mode
  let customizeMode = false;
  function toggleCustomize() {
    customizeMode = !customizeMode;
    document.body.classList.toggle('customize-mode', customizeMode);
    const btn = $('customizeBtn');
    btn.classList.toggle('active', customizeMode);
    if (customizeMode) setButtonContent(btn, 'ti-check', 'Done');
    else setButtonContent(btn, 'ti-pencil', 'Customize');
  }

  // PDF readiness modal
  function handleDownloadPdfClick() {
    const btn = $('downloadPdfBtn');
    if (btn.dataset.allGenerated === 'true') { notifyPending('downloadRoadmapPDF'); return; }
    $('pdfReadyGenerated').textContent = btn.dataset.generated;
    $('pdfReadyTotal').textContent = btn.dataset.total;
    $('pdfReadyModal').style.display = 'flex';
  }
  function closePdfReadyModal() { $('pdfReadyModal').style.display = 'none'; }

  /* ── 11. AI GENERATION MODAL (UI only — not wired to any endpoint) ──
     V1 used generateLearningJob + aiJobProgress. Neither exists in V2, so
     nothing opens this modal. renderAIGenProgress() is ready for when a
     V2 progress endpoint exists; adapt field names to its real response. */

  function taskStatusClass(status) {
    switch (status) {
      case 'RUNNING': return 'active';
      case 'COMPLETED': return 'done';
      case 'FAILED': return 'error';
      default: return 'pending';
    }
  }
  function renderAIGenProgress(data) {
    const C = 264; // 2πr, r=42
    const total = data.total || 0, completed = data.completed || 0, failed = data.failed || 0;
    const percent = total > 0 ? Math.round(100 * (completed + failed) / total) : 0;
    const tasks = arr(data.tasks);
    const running = tasks.find((t) => t.status === 'RUNNING');
    $('aiGenPercent').textContent = percent + '%';
    $('aiGenSubject').textContent = running ? running.subjectName : '—';
    $('aiGenTopic').textContent = running ? running.subtopicName : '—';
    $('aiGenCount').textContent = completed + ' / ' + total;
    $('aiGenRing').style.strokeDashoffset = C - (C * percent / 100);
    $('aiGenTaskList').replaceChildren(...tasks.map((t) =>
      h('div', { class: 'ai-gen-task ' + taskStatusClass(t.status) },
        h('span', { class: 'dot' }), h('span', { text: t.subtopicName }))));
  }
  function closeAIGenModal() {
    $('aiGenModal').style.display = 'none';
    location.reload();
  }

  /* ── 12. LOAD & INIT ─────────────────────────────────────────────── */

  function applyRoutes(roadmapId) {
    document.querySelectorAll('[data-route]').forEach((a) => {
      let url = ROUTES[a.dataset.route];
      if (!url) { a.setAttribute('aria-disabled', 'true'); a.title = 'Not migrated to V2 yet'; a.addEventListener('click', (e) => e.preventDefault()); return; }
      if (a.dataset.route === 'schedule' && roadmapId) url += (url.includes('?') ? '&' : '?') + 'roadmapId=' + encodeURIComponent(roadmapId);
      a.setAttribute('href', url);
    });
  }

  function bindStaticControls(roadmapId) {
    $('heroStatsToggle').addEventListener('click', () => $('syllabusHero').classList.toggle('stats-open'));
    $('hintToggle').addEventListener('click', () => $('learningHint').classList.toggle('open'));
    $('expandToggleBtn').addEventListener('click', toggleExpandAll);
    $('customizeBtn').addEventListener('click', toggleCustomize);
    $('downloadPdfBtn').addEventListener('click', handleDownloadPdfClick);
    $('generateLearningBtn').addEventListener('click', () => notifyPending('generateLearningJob'));
    $('pdfReadyCancelBtn').addEventListener('click', closePdfReadyModal);
    $('pdfReadyGenerateBtn').addEventListener('click', () => { closePdfReadyModal(); notifyPending('generateLearningJob'); });
    $('aiGenCloseBtn').addEventListener('click', closeAIGenModal);
    $('logoutBtn').addEventListener('click', (e) => { e.preventDefault(); notifyPending('logout'); });
    $('dailyTasksBtn').addEventListener('click', () => {
      const url = ROUTES.schedule;
      if (!url) { alert("Daily Tasks isn't available in V2 yet."); return; }
      location.href = url + (url.includes('?') ? '&' : '?') + 'roadmapId=' + encodeURIComponent(roadmapId);
    });

	// Add subject without refreshing the page
	const addBtn = $('addSubjectBtn');
	const addBox = $('addSubject');
	const addForm = $('addSubjectForm');

	addBtn.addEventListener('click', () => {
	    addBtn.style.display = 'none';
	    addBox.style.display = 'block';
	});

	$('addSubjectCancel').addEventListener('click', () => {
	    addBox.style.display = 'none';
	    addBtn.style.display = 'flex';
	    addForm.reset();
	});

	addForm.addEventListener('submit', async (e) => {
	    e.preventDefault();

	    const input = addForm.querySelector('[name="name"]');
	    const name = input.value.trim();

	    if (!name) {
	        input.focus();
	        return;
	    }

	    const submitBtn = addForm.querySelector('[type="submit"]');
	    submitBtn.disabled = true;

	    try {
	        const response = await fetch(
	            `/api/roadmaps/${encodeURIComponent(roadmapId)}/subjects`,
	            {
	                method: 'POST',
	                credentials: 'include',
	                headers: {
	                    'Content-Type': 'application/json',
	                    'Accept': 'application/json'
	                },
	                body: JSON.stringify({ name })
	            }
	        );

	        if (!response.ok) {
	            const message = await response.text();
	            throw new Error(
	                message || `Failed to add subject (${response.status})`
	            );
	        }

	        // Fetch updated data and redraw the relevant page sections.
	        const rm = normalizeRoadmap(await fetchRoadmap(roadmapId));
	        const stats = computeStats(rm);

	        renderHero(rm, stats);
	        renderToolbar(stats);
	        renderTree(rm);
	        setAllOpen(true);

	        addForm.reset();
	        addBox.style.display = 'none';
	        addBtn.style.display = 'flex';

	    } catch (error) {
	        console.error('Add subject error:', error);
	        alert(error.message || 'Could not add the subject. Please try again.');
	    } finally {
	        submitBtn.disabled = false;
	    }
	});
  }

  async function loadLoadingUi() {
    try {
      const res = await fetch('includes/loading.html');
      if (res.ok) $('loadingContainer').innerHTML = await res.text(); // static local fragment, not API data
    } catch (_) { /* loading UI is optional */ }
  }

  async function fetchRoadmap(id) {
    const res = await fetch('/api/roadmaps/' + encodeURIComponent(id), { headers: { Accept: 'application/json' } });
    if (res.status === 404) throw new Error('Roadmap not found.');
    if (!res.ok) throw new Error('Server error (' + res.status + ') while loading the roadmap.');
    return res.json();
  }

  async function init() {
    loadLoadingUi();
    const id = new URLSearchParams(location.search).get('id');
    applyRoutes(id);

    if (!id || !/^\d+$/.test(id)) {
      showState('No valid roadmap ID in the URL. Open this page as roadmap-view.html?id=1');
      return;
    }
    bindStaticControls(id);

    try {
      const rm = normalizeRoadmap(await fetchRoadmap(id));
      const stats = computeStats(rm);
      showContent();
      renderHero(rm, stats);
      renderToolbar(stats);
      renderTree(rm);
      setAllOpen(true); // V1 expands everything by default
    } catch (err) {
      showState(err.message || 'Could not load the roadmap.');
    }
  }

  document.addEventListener('DOMContentLoaded', init);
})();