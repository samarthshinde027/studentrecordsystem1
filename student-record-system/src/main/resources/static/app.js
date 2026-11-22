'use strict';

/* ===================== helpers ===================== */
const $ = (id) => document.getElementById(id);
const enc = encodeURIComponent;
const esc = (v) => String(v == null ? '' : v).replace(/[&<>"']/g,
  (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

let token = localStorage.getItem('srms_token') || '';
let currentTab = 'dashboard';

function toast(message, type = 'ok') {
  const el = $('toast');
  el.textContent = message;
  el.className = 'toast ' + type;
  el.hidden = false;
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => { el.hidden = true; }, 2800);
}

async function api(url, options = {}) {
  const headers = {};
  if (options.body) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(url, { ...options, headers });
  if (res.status === 401 && !url.includes('/auth/login')) {
    logout();
    throw new Error('Session expired. Please log in again.');
  }
  if (!res.ok) {
    let message = 'Request failed';
    try { message = (await res.json()).message || message; } catch (e) { /* ignore */ }
    throw new Error(message);
  }
  if (res.status === 204) return null;
  const type = res.headers.get('content-type') || '';
  return type.includes('json') ? res.json() : res.text();
}

function saveBlob(blob, filename) {
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 1500);
}

async function download(url, filename) {
  const res = await fetch(url, { headers: { Authorization: 'Bearer ' + token } });
  if (!res.ok) throw new Error('Download failed');
  saveBlob(await res.blob(), filename);
}

function confirmBox(message) {
  return new Promise((resolve) => {
    const dlg = $('confirmDlg');
    $('confirmText').textContent = message;
    const finish = (value) => { dlg.close(); resolve(value); };
    $('confirmYes').onclick = () => finish(true);
    $('confirmNo').onclick = () => finish(false);
    dlg.oncancel = () => resolve(false);
    dlg.showModal();
  });
}

function initials(name) {
  return String(name || '?').trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
}

function avatarHtml(photo, name, large) {
  const cls = 'avatar' + (large ? ' lg' : '');
  return photo
    ? `<img class="${cls}" src="${esc(photo)}" alt="">`
    : `<span class="${cls}">${esc(initials(name))}</span>`;
}

/* ===================== theme ===================== */
function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem('srms_theme', theme);
  drawCharts();
}
$('themeBtn').addEventListener('click', () => {
  applyTheme(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
});

/* ===================== auth ===================== */
function showLogin() {
  $('appView').hidden = true;
  $('loginView').hidden = false;
  $('loginPass').value = '';
}

function showApp() {
  $('loginView').hidden = true;
  $('appView').hidden = false;
  switchTab(currentTab);
}

function logout() {
  token = '';
  localStorage.removeItem('srms_token');
  showLogin();
}

$('logoutBtn').addEventListener('click', logout);

$('loginForm').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  try {
    const res = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: $('loginUser').value.trim(), password: $('loginPass').value }),
    });
    token = res.token;
    localStorage.setItem('srms_token', token);
    $('loginError').hidden = true;
    showApp();
  } catch (e) {
    $('loginError').textContent = e.message;
    $('loginError').hidden = false;
  }
});

/* ===================== tabs ===================== */
function switchTab(name) {
  currentTab = name;
  document.querySelectorAll('.tab-btn').forEach((b) => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab').forEach((s) => { s.hidden = s.id !== 'tab-' + name; });
  if (name === 'dashboard') loadDashboard();
  if (name === 'students') { loadCourses(); loadStudents(); }
  if (name === 'attendance') loadAttendance();
}
document.querySelectorAll('.tab-btn').forEach((b) => b.addEventListener('click', () => switchTab(b.dataset.tab)));

/* ===================== dashboard ===================== */
let lastStats = null;
const charts = {};
const PALETTE = ['#ff8a1f', '#f5b942', '#fb7185', '#38bdf8', '#a78bfa', '#34d399', '#f472b6', '#94a3b8'];

async function loadDashboard() {
  try {
    lastStats = await api('/api/stats');
    $('statTotal').textContent = lastStats.total;
    $('statCourses').textContent = Object.keys(lastStats.byCourse).length;
    $('statAtt').textContent = lastStats.attendanceRecords ? lastStats.averageAttendance + '%' : '-';
    drawCharts();
  } catch (e) { toast(e.message, 'err'); }
}

function drawCharts() {
  if (!lastStats || typeof Chart === 'undefined' || $('appView').hidden) return;
  const light = document.documentElement.getAttribute('data-theme') === 'light';
  const textColor = light ? '#44403c' : '#d4d4d8';
  const gridColor = light ? 'rgba(0,0,0,.08)' : 'rgba(255,255,255,.08)';
  Chart.defaults.color = textColor;
  Chart.defaults.font.family = 'Poppins, system-ui, sans-serif';

  const courseLabels = Object.keys(lastStats.byCourse);
  const yearLabels = Object.keys(lastStats.byYear);

  if (charts.course) charts.course.destroy();
  if (charts.year) charts.year.destroy();

  charts.course = new Chart($('courseChart'), {
    type: 'doughnut',
    data: {
      labels: courseLabels,
      datasets: [{ data: courseLabels.map((k) => lastStats.byCourse[k]), backgroundColor: PALETTE, borderWidth: 0 }],
    },
    options: { maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } },
  });

  charts.year = new Chart($('yearChart'), {
    type: 'bar',
    data: {
      labels: yearLabels.map((y) => 'Year ' + y),
      datasets: [{ label: 'Students', data: yearLabels.map((k) => lastStats.byYear[k]), backgroundColor: '#ff8a1f', borderRadius: 8 }],
    },
    options: {
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: gridColor } },
        x: { grid: { display: false } },
      },
    },
  });
}

/* ===================== students ===================== */
const st = { page: 0, size: 10, sort: 'name', dir: 'asc' };
let searchTimer = null;

async function loadCourses() {
  try {
    const courses = await api('/api/students/courses');
    const select = $('courseFilter');
    const current = select.value;
    select.innerHTML = '<option value="">All courses</option>'
      + courses.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
    select.value = courses.includes(current) ? current : '';
  } catch (e) { /* ignore */ }
}

async function loadStudents() {
  $('rows').innerHTML = Array.from({ length: 5 }, () =>
    '<tr><td colspan="6"><div class="shimmer"></div></td></tr>').join('');
  $('empty').hidden = true;
  const params = new URLSearchParams({
    q: $('search').value.trim(), course: $('courseFilter').value, year: $('yearFilter').value,
    sort: st.sort, dir: st.dir, page: st.page, size: st.size,
  });
  try {
    const data = await api('/api/students?' + params.toString());
    if (data.items.length === 0 && st.page > 0) {
      st.page = Math.max(0, Math.ceil(data.total / st.size) - 1);
      return loadStudents();
    }
    $('rows').innerHTML = data.items.map((s) => `
      <tr>
        <td data-label="PRN">${esc(s.prn)}</td>
        <td data-label="Name"><span>${avatarHtml(null, s.name, false)}${esc(s.name)}</span></td>
        <td data-label="Course">${esc(s.course)}</td>
        <td data-label="Year"><span class="pill">Year ${esc(s.year)}</span></td>
        <td data-label="Phone">${esc(s.phone) || '-'}</td>
        <td class="actions-cell">
          <button class="btn sm" data-report="${esc(s.prn)}">Report</button>
          <button class="btn sm" data-edit="${esc(s.prn)}">Edit</button>
          <button class="btn sm del" data-del="${esc(s.prn)}">Delete</button>
        </td>
      </tr>`).join('');
    $('empty').hidden = data.items.length > 0;
    const pages = Math.max(1, Math.ceil(data.total / st.size));
    $('pageInfo').textContent = `Page ${st.page + 1} of ${pages} \u00B7 ${data.total} students`;
    $('prevBtn').disabled = st.page <= 0;
    $('nextBtn').disabled = st.page + 1 >= pages;
  } catch (e) {
    $('rows').innerHTML = '';
    toast(e.message, 'err');
  }
}

document.querySelectorAll('th[data-sort]').forEach((th) => th.addEventListener('click', () => {
  const col = th.dataset.sort;
  st.dir = st.sort === col && st.dir === 'asc' ? 'desc' : 'asc';
  st.sort = col;
  st.page = 0;
  document.querySelectorAll('th[data-sort]').forEach((x) => x.classList.remove('asc', 'desc'));
  th.classList.add(st.dir);
  loadStudents();
}));

$('search').addEventListener('input', () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => { st.page = 0; loadStudents(); }, 250);
});
$('courseFilter').addEventListener('change', () => { st.page = 0; loadStudents(); });
$('yearFilter').addEventListener('change', () => { st.page = 0; loadStudents(); });
$('prevBtn').addEventListener('click', () => { st.page = Math.max(0, st.page - 1); loadStudents(); });
$('nextBtn').addEventListener('click', () => { st.page += 1; loadStudents(); });

$('rows').addEventListener('click', async (ev) => {
  const btn = ev.target.closest('button');
  if (!btn) return;
  try {
    if (btn.dataset.report) {
      await openReport(btn.dataset.report);
      if (!$('reportDlg').open) $('reportDlg').showModal();
    } else if (btn.dataset.edit) {
      openForm(await api('/api/students/' + enc(btn.dataset.edit)));
    } else if (btn.dataset.del) {
      if (await confirmBox('Delete student ' + btn.dataset.del + ' and all their attendance and marks?')) {
        await api('/api/students/' + enc(btn.dataset.del), { method: 'DELETE' });
        toast('Student deleted successfully.');
        loadStudents();
        loadCourses();
      }
    }
  } catch (e) { toast(e.message, 'err'); }
});

$('exportBtn').addEventListener('click', async () => {
  try { await download('/api/export/students.csv', 'students.csv'); }
  catch (e) { toast(e.message, 'err'); }
});

/* ---------- add / edit form ---------- */
let editingPrn = null;
const photoState = { data: null, remove: false };

async function openForm(student) {
  editingPrn = student ? student.prn : null;
  photoState.data = null;
  photoState.remove = false;
  $('formTitle').textContent = student ? 'Update Student' : 'Add Student';
  $('prn').value = student ? student.prn : '';
  $('prn').disabled = !!student;
  $('name').value = student ? student.name : '';
  $('course').value = student ? student.course : '';
  $('year').value = student ? student.year : '';
  $('phone').value = student ? student.phone || '' : '';
  $('formError').hidden = true;
  $('formAvatar').innerHTML = avatarHtml(null, student ? student.name : '+', true);
  $('formDlg').showModal();
  if (student) {
    try {
      const p = await api('/api/students/' + enc(student.prn) + '/photo');
      if (p && p.photo && !photoState.data && !photoState.remove) {
        $('formAvatar').innerHTML = avatarHtml(p.photo, student.name, true);
      }
    } catch (e) { /* ignore */ }
  }
}

$('addBtn').addEventListener('click', () => openForm(null));
$('formCancel').addEventListener('click', () => $('formDlg').close());

$('photoPick').addEventListener('click', () => $('photoFile').click());
$('photoFile').addEventListener('change', async () => {
  const file = $('photoFile').files[0];
  if (!file) return;
  try {
    photoState.data = await resizeImage(file, 256);
    photoState.remove = false;
    $('formAvatar').innerHTML = avatarHtml(photoState.data, $('name').value, true);
  } catch (e) { toast(e.message, 'err'); }
  $('photoFile').value = '';
});
$('photoRemove').addEventListener('click', () => {
  photoState.data = null;
  photoState.remove = true;
  $('formAvatar').innerHTML = avatarHtml(null, $('name').value || '+', true);
});

function resizeImage(file, max) {
  return new Promise((resolve, reject) => {
    if (!file.type.startsWith('image/')) return reject(new Error('Please choose an image file'));
    const img = new Image();
    const url = URL.createObjectURL(file);
    img.onload = () => {
      const ratio = Math.min(1, max / Math.max(img.width, img.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.round(img.width * ratio);
      canvas.height = Math.round(img.height * ratio);
      const ctx = canvas.getContext('2d');
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(url);
      resolve(canvas.toDataURL('image/jpeg', 0.8));
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('Could not read that image')); };
    img.src = url;
  });
}

function formProblem(b) {
  if (!/^[A-Za-z0-9]{3,20}$/.test(b.prn)) return 'PRN must be 3-20 letters or digits.';
  if (!b.name) return 'Name is required.';
  if (!b.course) return 'Course is required.';
  if (!Number.isInteger(b.year) || b.year < 1 || b.year > 6) return 'Year must be between 1 and 6.';
  if (b.phone && !/^[0-9]{10}$/.test(b.phone)) return 'Phone must be exactly 10 digits.';
  return null;
}

$('studentForm').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const body = {
    prn: editingPrn || $('prn').value.trim(),
    name: $('name').value.trim(),
    course: $('course').value.trim(),
    year: $('year').value === '' ? NaN : Number($('year').value),
    phone: $('phone').value.trim(),
  };
  const problem = formProblem(body);
  if (problem) {
    $('formError').textContent = problem;
    $('formError').hidden = false;
    return;
  }
  try {
    if (editingPrn) {
      await api('/api/students/' + enc(editingPrn), { method: 'PUT', body: JSON.stringify(body) });
    } else {
      await api('/api/students', { method: 'POST', body: JSON.stringify(body) });
    }
    if (photoState.data) {
      await api('/api/students/' + enc(body.prn) + '/photo', { method: 'PUT', body: JSON.stringify({ photo: photoState.data }) });
    } else if (photoState.remove && editingPrn) {
      await api('/api/students/' + enc(body.prn) + '/photo', { method: 'DELETE' });
    }
    $('formDlg').close();
    toast(editingPrn ? 'Student updated successfully.' : 'Student added successfully.');
    loadStudents();
    loadCourses();
  } catch (e) {
    $('formError').textContent = e.message;
    $('formError').hidden = false;
  }
});

/* ---------- report card (marks + photo + print) ---------- */
let reportPrn = null;

async function openReport(prn) {
  reportPrn = prn;
  const [r, p] = await Promise.all([
    api('/api/students/' + enc(prn) + '/report'),
    api('/api/students/' + enc(prn) + '/photo'),
  ]);
  const s = r.student;
  const attClass = r.attendanceTotal === 0 ? '' : (r.attendancePercent < 75 ? 'low' : 'good');
  $('reportBody').innerHTML = `
    <div class="rc-head">
      ${avatarHtml(p && p.photo, s.name, true)}
      <div>
        <h3>${esc(s.name)}</h3>
        <p>PRN ${esc(s.prn)} &middot; ${esc(s.course)} &middot; Year ${esc(s.year)}</p>
        <p>Phone: ${esc(s.phone) || '-'}</p>
      </div>
    </div>
    <div class="rc-grid">
      <div class="rc-box"><p>Total</p><b>${r.totalMarks} / ${r.totalMax}</b></div>
      <div class="rc-box"><p>Percentage</p><b>${r.percentage}%</b></div>
      <div class="rc-box"><p>Grade</p><b>${esc(r.grade)}</b></div>
    </div>
    <p class="hint">Attendance: <span class="pill ${attClass}">${r.attendanceTotal ? r.attendancePercent + '%' : 'not marked'}</span>
      (${r.attendancePresent} of ${r.attendanceTotal} days)</p>
    <table>
      <thead><tr><th>Subject</th><th>Marks</th><th>Out of</th><th class="no-print"></th></tr></thead>
      <tbody>
        ${r.marks.length ? r.marks.map((m) => `
          <tr><td>${esc(m.subject)}</td><td>${m.marks}</td><td>${m.maxMarks}</td>
          <td class="actions-cell no-print"><button class="btn sm del" data-delmark="${esc(m.subject)}">Remove</button></td></tr>`).join('')
          : '<tr><td colspan="4" class="hint">No marks added yet.</td></tr>'}
      </tbody>
    </table>
    <form id="markForm" class="mark-form no-print" novalidate>
      <input id="mSubject" placeholder="Subject" maxlength="60">
      <input id="mMarks" type="number" min="0" placeholder="Marks">
      <input id="mMax" type="number" min="1" value="100" placeholder="Out of">
      <button class="btn primary sm" type="submit">Add</button>
    </form>
    <p id="markError" class="error" hidden></p>`;

  $('markForm').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const body = {
      subject: $('mSubject').value.trim(),
      marks: $('mMarks').value === '' ? null : Number($('mMarks').value),
      maxMarks: $('mMax').value === '' ? null : Number($('mMax').value),
    };
    try {
      if (!body.subject || body.marks === null || body.maxMarks === null) throw new Error('Enter subject, marks and maximum marks.');
      await api('/api/students/' + enc(reportPrn) + '/marks', { method: 'POST', body: JSON.stringify(body) });
      await openReport(reportPrn);
    } catch (e) {
      $('markError').textContent = e.message;
      $('markError').hidden = false;
    }
  });
}

$('reportBody').addEventListener('click', async (ev) => {
  const btn = ev.target.closest('button[data-delmark]');
  if (!btn) return;
  try {
    await api('/api/students/' + enc(reportPrn) + '/marks?subject=' + enc(btn.dataset.delmark), { method: 'DELETE' });
    await openReport(reportPrn);
  } catch (e) { toast(e.message, 'err'); }
});
$('reportClose').addEventListener('click', () => $('reportDlg').close());
$('reportPrint').addEventListener('click', () => window.print());

/* ---------- CSV import ---------- */
function parseCsv(text) {
  const rows = [];
  let row = [];
  let cur = '';
  let quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"') {
        if (text[i + 1] === '"') { cur += '"'; i++; } else { quoted = false; }
      } else { cur += c; }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ',') {
      row.push(cur); cur = '';
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i++;
      row.push(cur); cur = '';
      rows.push(row); row = [];
    } else {
      cur += c;
    }
  }
  if (cur !== '' || row.length) { row.push(cur); rows.push(row); }
  return rows.filter((r) => r.some((v) => v.trim() !== ''));
}

$('importBtn').addEventListener('click', () => {
  $('importFile').value = '';
  $('importResult').hidden = true;
  $('importDlg').showModal();
});
$('importClose').addEventListener('click', () => $('importDlg').close());
$('templateBtn').addEventListener('click', () => {
  const csv = 'prn,name,course,year,phone\r\n202508021214,Samarth Shinde,B.Tech CSE,3,9876543210\r\n';
  saveBlob(new Blob([csv], { type: 'text/csv' }), 'students-template.csv');
});

$('importRun').addEventListener('click', async () => {
  const file = $('importFile').files[0];
  if (!file) { toast('Choose a CSV file first.', 'err'); return; }
  try {
    const text = (await file.text()).replace(/^\uFEFF/, '');
    let rows = parseCsv(text);
    let idx = { prn: 0, name: 1, course: 2, year: 3, phone: 4 };
    if (rows.length && rows[0][0].trim().toLowerCase() === 'prn') {
      const header = rows[0].map((h) => h.trim().toLowerCase());
      const find = (key, fallback) => (header.indexOf(key) >= 0 ? header.indexOf(key) : fallback);
      idx = { prn: find('prn', 0), name: find('name', 1), course: find('course', 2), year: find('year', 3), phone: find('phone', 4) };
      rows = rows.slice(1);
    }
    const students = rows.map((r) => {
      const y = (r[idx.year] || '').trim();
      return {
        prn: (r[idx.prn] || '').trim(),
        name: (r[idx.name] || '').trim(),
        course: (r[idx.course] || '').trim(),
        year: /^\d+$/.test(y) ? Number(y) : null,
        phone: (r[idx.phone] || '').trim(),
      };
    });
    if (!students.length) throw new Error('No rows found in the file.');
    const res = await api('/api/students/import', { method: 'POST', body: JSON.stringify(students) });
    $('importResult').innerHTML = `<b>${res.added}</b> added, <b>${res.skipped}</b> skipped.`
      + (res.errors.length ? '<ul>' + res.errors.map((e) => `<li>${esc(e)}</li>`).join('') + '</ul>' : '');
    $('importResult').hidden = false;
    loadStudents();
    loadCourses();
  } catch (e) { toast(e.message, 'err'); }
});

/* ===================== attendance ===================== */
const att = { rows: [], map: {} };

function today() {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 10);
}

async function loadAttendance() {
  if (!$('attDate').value) $('attDate').value = today();
  try {
    att.rows = await api('/api/attendance?date=' + enc($('attDate').value));
    att.map = {};
    att.rows.forEach((r) => { if (r.status) att.map[r.prn] = r.status; });
    renderAttendance();
  } catch (e) { toast(e.message, 'err'); }
}

function renderAttendance() {
  $('attEmpty').hidden = att.rows.length > 0;
  $('attRows').innerHTML = att.rows.map((r) => {
    const s = att.map[r.prn];
    const pct = r.total ? Math.round((r.present * 100) / r.total) : null;
    const cls = pct === null ? '' : (pct < 75 ? 'low' : 'good');
    return `<tr>
      <td data-label="PRN">${esc(r.prn)}</td>
      <td data-label="Name">${esc(r.name)}</td>
      <td data-label="Status"><div class="seg">
        <button class="p ${s === 'PRESENT' ? 'on' : ''}" data-prn="${esc(r.prn)}" data-status="PRESENT">Present</button>
        <button class="a ${s === 'ABSENT' ? 'on' : ''}" data-prn="${esc(r.prn)}" data-status="ABSENT">Absent</button>
      </div></td>
      <td data-label="Overall"><span class="pill ${cls}">${pct === null ? '-' : pct + '%'}</span></td>
    </tr>`;
  }).join('');
}

$('attRows').addEventListener('click', (ev) => {
  const btn = ev.target.closest('button[data-status]');
  if (!btn) return;
  att.map[btn.dataset.prn] = btn.dataset.status;
  renderAttendance();
});
$('attDate').addEventListener('change', loadAttendance);
$('markAllBtn').addEventListener('click', () => {
  att.rows.forEach((r) => { att.map[r.prn] = 'PRESENT'; });
  renderAttendance();
});
$('saveAttBtn').addEventListener('click', async () => {
  const records = Object.entries(att.map).map(([prn, status]) => ({ prn, status }));
  if (!records.length) { toast('Mark at least one student first.', 'err'); return; }
  try {
    await api('/api/attendance', { method: 'POST', body: JSON.stringify({ date: $('attDate').value, records }) });
    toast('Attendance saved.');
    loadAttendance();
  } catch (e) { toast(e.message, 'err'); }
});

/* ===================== start ===================== */
(async function init() {
  const savedTheme = localStorage.getItem('srms_theme');
  if (savedTheme) document.documentElement.setAttribute('data-theme', savedTheme);
  if (!token) { showLogin(); return; }
  try {
    await api('/api/auth/me');
    showApp();
  } catch (e) {
    showLogin();
  }
})();
