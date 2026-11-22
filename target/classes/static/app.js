const API = '/api/students';
const $ = (id) => document.getElementById(id);

let editingPrn = null;
let searchTimer = null;
let totalCount = 0;

async function request(url, options = {}) {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let message = 'Request failed';
    try { message = (await res.json()).message || message; } catch (e) { /* ignore */ }
    throw new Error(message);
  }
  return res.status === 204 ? null : res.json();
}

function esc(value) {
  const div = document.createElement('div');
  div.textContent = value == null ? '' : String(value);
  return div.innerHTML;
}

function toast(message, type = 'ok') {
  const el = $('toast');
  el.textContent = message;
  el.className = 'toast ' + type;
  el.hidden = false;
  clearTimeout(toast.t);
  toast.t = setTimeout(() => { el.hidden = true; }, 2600);
}

async function load() {
  const q = $('search').value.trim();
  try {
    const students = await request(API + (q ? '?q=' + encodeURIComponent(q) : ''));
    render(students, q);
  } catch (e) {
    toast(e.message, 'err');
  }
}

function render(students, q) {
  $('rows').innerHTML = students.map((s) => `
    <tr>
      <td>${esc(s.prn)}</td>
      <td>${esc(s.name)}</td>
      <td>${esc(s.course)}</td>
      <td><span class="pill">Year ${esc(s.year)}</span></td>
      <td>${esc(s.phone)}</td>
      <td class="actions-cell">
        <button class="btn sm" data-edit="${esc(s.prn)}">Edit</button>
        <button class="btn sm del" data-del="${esc(s.prn)}">Delete</button>
      </td>
    </tr>`).join('');

  $('empty').hidden = students.length > 0;
  $('statShown').textContent = students.length;
  if (!q) {
    totalCount = students.length;
    $('statTotal').textContent = totalCount;
    $('statCourses').textContent = new Set(students.map((s) => s.course.toLowerCase())).size;
  }
}

function openModal(student) {
  editingPrn = student ? student.prn : null;
  $('modalTitle').textContent = student ? 'Update Student' : 'Add Student';
  $('prn').value = student ? student.prn : '';
  $('prn').disabled = !!student;
  $('name').value = student ? student.name : '';
  $('course').value = student ? student.course : '';
  $('year').value = student ? student.year : '';
  $('phone').value = student ? student.phone || '' : '';
  $('formError').hidden = true;
  $('modal').showModal();
  (student ? $('name') : $('prn')).focus();
}

function closeModal() { $('modal').close(); }

$('addBtn').addEventListener('click', () => openModal(null));
$('cancelBtn').addEventListener('click', closeModal);

$('form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const body = {
    prn: $('prn').value.trim(),
    name: $('name').value.trim(),
    course: $('course').value.trim(),
    year: $('year').value === '' ? null : Number($('year').value),
    phone: $('phone').value.trim(),
  };
  if (!body.prn || !body.name || !body.course || !body.year) {
    $('formError').textContent = 'Please enter all required details.';
    $('formError').hidden = false;
    return;
  }
  try {
    if (editingPrn) {
      await request(API + '/' + encodeURIComponent(editingPrn), { method: 'PUT', body: JSON.stringify(body) });
      toast('Student updated successfully.');
    } else {
      await request(API, { method: 'POST', body: JSON.stringify(body) });
      toast('Student added successfully.');
    }
    closeModal();
    load();
  } catch (e) {
    $('formError').textContent = e.message;
    $('formError').hidden = false;
  }
});

$('rows').addEventListener('click', async (ev) => {
  const editPrn = ev.target.getAttribute('data-edit');
  const delPrn = ev.target.getAttribute('data-del');
  if (editPrn) {
    try {
      openModal(await request(API + '/' + encodeURIComponent(editPrn)));
    } catch (e) { toast(e.message, 'err'); }
  }
  if (delPrn && confirm('Delete student ' + delPrn + '?')) {
    try {
      await request(API + '/' + encodeURIComponent(delPrn), { method: 'DELETE' });
      toast('Student deleted successfully.');
      load();
    } catch (e) { toast(e.message, 'err'); }
  }
});

$('search').addEventListener('input', () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(load, 250);
});

load();
