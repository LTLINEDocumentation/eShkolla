// eShkolla - Moduli NOTAT (rrjedha standarde sipas ditarit)
(function () {
  const columns = [
    { key: 'nota1', label: 'Nota 1', editable: true, period: 'Gjysmëvjetori I', note: 'Nota 1' },
    { key: 'nota2', label: 'Nota 2', editable: true, period: 'Gjysmëvjetori I', note: 'Nota 2' },
    { key: 'mes1', label: 'Mesatarja GJ.V. I', auto: true },
    { key: 'nota3', label: 'Nota 3', editable: true, period: 'Gjysmëvjetori II', note: 'Nota 3' },
    { key: 'nota4', label: 'Nota 4', editable: true, period: 'Gjysmëvjetori II', note: 'Nota 4' },
    { key: 'mes2', label: 'Mesatarja GJ.V. II', auto: true },
    { key: 'final', label: 'Nota Finale', auto: true }
  ];

  function year() {
    const d = new Date();
    const start = d.getMonth() >= 8 ? d.getFullYear() : d.getFullYear() - 1;
    return `${start}/${start + 1}`;
  }

  function studentsFor(data, classId) {
    return (data.students || [])
      .filter(s => s.classId === classId && s.active)
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'sq'));
  }

  function gradeFor(grades, studentId, subjectId, note) {
    return (grades || []).find(g =>
      g.studentId === studentId && g.subjectId === subjectId && g.note === note
    ) || null;
  }

  function avg(values) {
    const nums = values.filter(v => Number.isFinite(v));
    if (!nums.length) return null;
    return nums.reduce((a, b) => a + b, 0) / nums.length;
  }

  function displayAverage(value) {
    return value == null ? '—' : Number(value).toFixed(2).replace(/\.00$/, '');
  }

  function calculatedValue(data, student, subjectId, key) {
    const g1 = gradeFor(data.grades, student.id, subjectId, 'Nota 1');
    const g2 = gradeFor(data.grades, student.id, subjectId, 'Nota 2');
    const g3 = gradeFor(data.grades, student.id, subjectId, 'Nota 3');
    const g4 = gradeFor(data.grades, student.id, subjectId, 'Nota 4');
    const sem1 = avg([g1?.value, g2?.value].map(Number).filter(Number.isFinite));
    const sem2 = avg([g3?.value, g4?.value].map(Number).filter(Number.isFinite));
    if (key === 'mes1') return sem1;
    if (key === 'mes2') return sem2;
    if (key === 'final') {
      const finalAverage = avg([sem1, sem2].filter(Number.isFinite));
      return finalAverage == null ? null : Math.round(finalAverage);
    }
    return null;
  }

  async function save(state, student, column, value, existing) {
    const payload = {
      studentId: student.id,
      subjectId: state.subjectId,
      teacherId: state.data.teacherId,
      value: Number(value),
      period: column.period,
      academicYear: year(),
      note: column.note
    };
    const saved = await api(
      existing ? `/api/v1/grades/${encodeURIComponent(existing.id)}` : '/api/v1/grades',
      { method: existing ? 'PUT' : 'POST', body: JSON.stringify(payload) }
    );
    if (existing) Object.assign(existing, saved);
    else state.data.grades.push(saved);
  }

  function nextEditableCell(state, row, col) {
    const rows = studentsFor(state.data, state.classId);
    for (let c = col + 1; c < columns.length; c++) if (columns[c].editable) return { row, col: c };
    for (let r = row + 1; r < rows.length; r++) for (let c = 0; c < columns.length; c++) if (columns[c].editable) return { row: r, col: c };
    return null;
  }

  function openPicker(state, row, col) {
    const rows = studentsFor(state.data, state.classId);
    const student = rows[row];
    const column = columns[col];
    if (!student || !column?.editable) return;
    const existing = gradeFor(state.data.grades, student.id, state.subjectId, column.note);
    const wrap = document.createElement('div');
    wrap.className = 'modal';
    wrap.innerHTML = `<div class="modal-card web-grade-picker">
      <div class="section-title"><div><h3>Vendos ${column.label}</h3><p class="muted">${esc(student.fullName)} · ${esc(column.label)}</p></div><button type="button" class="secondary" id="closeGradeV2">Mbyll</button></div>
      <div class="web-grade-buttons">${[1,2,3,4,5].map(n => `<button type="button" class="web-grade-choice ${String(existing?.value) === String(n) ? 'selected' : ''}" data-value="${n}">${n}</button>`).join('')}</div>
      ${existing ? '<button type="button" class="secondary web-grade-delete">Fshij notën</button>' : ''}
      <p id="gradeV2Error" class="error"></p>
    </div>`;
    document.body.appendChild(wrap);
    const close = () => wrap.remove();
    $('closeGradeV2').onclick = close;
    wrap.querySelectorAll('.web-grade-choice').forEach(btn => btn.onclick = async () => {
      btn.disabled = true;
      try {
        await save(state, student, column, btn.dataset.value, existing);
        close();
        render(state);
        const next = nextEditableCell(state, row, col);
        if (next) openPicker(state, next.row, next.col);
      } catch (e) {
        $('gradeV2Error').textContent = e.message;
        btn.disabled = false;
      }
    });
    wrap.querySelector('.web-grade-delete')?.addEventListener('click', async () => {
      try {
        await api(`/api/v1/grades/${encodeURIComponent(existing.id)}`, { method: 'DELETE' });
        state.data.grades = state.data.grades.filter(g => g.id !== existing.id);
        close();
        render(state);
      } catch (e) { $('gradeV2Error').textContent = e.message; }
    });
  }

  function render(state) {
    const rows = studentsFor(state.data, state.classId);
    const subject = (state.data.subjects || []).find(s => s.id === state.subjectId);
    const yearLabel = year();
    $('moduleContent').innerHTML = `<div class="web-grade-panel">
      <div class="web-grade-toolbar"><div><strong>Vlerësimi i nxënësve</strong><small>${esc(state.className)} · ${esc(subject?.name || '')} · ${yearLabel}</small></div><button type="button" class="secondary" id="gradeV2Refresh">Rifresko</button></div>
      <div class="web-grade-hint">Notat 1–2 i takojnë Gjysmëvjetorit I, ndërsa Notat 3–4 Gjysmëvjetorit II. Mesataret dhe Nota Finale llogariten automatikisht. Nota Finale rrumbullakoset në numrin e plotë më të afërt; 4.5 bëhet 5.</div>
      <div class="table-wrap web-grade-table-wrap"><table class="web-grade-table grade-standard-table"><thead><tr><th>Nr.</th><th>Emri dhe Mbiemri</th>${columns.map(c => `<th class="${c.auto ? 'grade-auto-head' : ''}">${esc(c.label)}</th>`).join('')}</tr></thead><tbody>
      ${rows.length ? rows.map((student, rowIndex) => `<tr><td class="grade-number">${rowIndex + 1}</td><td class="web-grade-name"><strong>${esc(student.fullName)}</strong></td>${columns.map((c, colIndex) => {
        if (c.auto) {
          const value = calculatedValue(state.data, student, state.subjectId, c.key);
          return `<td class="grade-auto-cell" title="Llogaritet automatikisht">${displayAverage(value)}</td>`;
        }
        const g = gradeFor(state.data.grades, student.id, state.subjectId, c.note);
        return `<td><button type="button" class="web-grade-cell ${g ? 'has-grade' : ''}" data-row="${rowIndex}" data-col="${colIndex}">${g ? g.value : '—'}</button></td>`;
      }).join('')}</tr>`).join('') : `<tr><td colspan="9" class="empty-state">Nuk ka nxënës aktivë në këtë klasë.</td></tr>`}
      </tbody></table></div>
      <p class="result-count">${rows.length} nxënës · 4 nota plotësohen nga mësimdhënësi · 3 fusha llogariten automatikisht</p>
    </div>`;
    $('resultCount').textContent = `${rows.length} nxënës · ${subject?.name || 'Lëndë'}`;
    $('gradeV2Refresh').onclick = () => grades();
    document.querySelectorAll('.grade-standard-table .web-grade-cell').forEach(btn => btn.onclick = () => openPicker(state, Number(btn.dataset.row), Number(btn.dataset.col)));
    injectStyles();
  }

  function injectStyles() {
    if (document.getElementById('gradeV2Styles')) return;
    const style = document.createElement('style');
    style.id = 'gradeV2Styles';
    style.textContent = `
      .grade-standard-table{min-width:1120px}.grade-standard-table th:first-child,.grade-standard-table td:first-child{width:48px;text-align:center}.grade-number{font-weight:700;color:#667085}.grade-standard-table th:nth-child(2),.grade-standard-table td:nth-child(2){position:sticky;left:0;z-index:2;background:#fff}.grade-standard-table th{white-space:nowrap}.grade-standard-table th:not(:first-child){min-width:125px}.grade-standard-table .web-grade-cell{min-height:44px}.grade-auto-head{background:#f2f4f7}.grade-auto-cell{background:#f8fafc;text-align:center;font-weight:800;font-size:16px;border-left:1px solid #e4e7ec}
    `;
    document.head.appendChild(style);
  }

  window.grades = async function () {
    if (currentUser?.role !== 'MESIMDHENES') {
      return typeof _originalGrades === 'function' ? _originalGrades() : undefined;
    }
    const data = await loadTeacherRoleData(true);
    const classes = data.classes || [];
    let classId = window.webGradesClassId || '';
    if (!classes.some(c => c.id === classId)) classId = '';
    const subjectsForClass = classId ? (data.subjects || []).filter(s => s.classIds?.includes(classId)) : [];
    let subjectId = window.webGradesSubjectId || '';
    if (!subjectsForClass.some(s => s.id === subjectId)) subjectId = subjectsForClass.length === 1 ? subjectsForClass[0].id : '';
    window.webGradesClassId = classId;
    window.webGradesSubjectId = subjectId;
    const selectedClass = classes.find(c => c.id === classId);
    const selectedSubject = subjectsForClass.find(s => s.id === subjectId);
    $('moduleContent').innerHTML = `<div class="web-grade-panel">
      <div class="web-grade-selectors"><label>1. Cakto klasën<select id="webGradeClass"><option value="">-- Zgjidh klasën --</option>${classes.map(c => `<option value="${esc(c.id)}" ${c.id === classId ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}</select></label>
      <label>2. Lënda<select id="webGradeSubject" ${subjectsForClass.length <= 1 ? 'disabled' : ''}><option value="">-- Zgjidh lëndën --</option>${subjectsForClass.map(s => `<option value="${esc(s.id)}" ${s.id === subjectId ? 'selected' : ''}>${esc(s.name)}${s.code ? ' — ' + esc(s.code) : ''}</option>`).join('')}</select>${subjectsForClass.length === 1 ? '<small class="muted">Lënda përcaktohet automatikisht.</small>' : ''}</label></div>
      ${classId && !subjectsForClass.length ? '<div class="empty-state">Kjo klasë nuk ka lëndë të caktuar për këtë mësimdhënës.</div>' : ''}
      ${classId && subjectId ? '<div id="webGradeTableHost"></div>' : classId ? '<div class="empty-state">Zgjidh lëndën për të shfaqur listën e nxënësve.</div>' : '<div class="empty-state">Fillimisht zgjidh klasën. Pastaj do të shfaqet lista e nxënësve të asaj klase.</div>'}
    </div>`;
    $('webGradeClass').onchange = () => { window.webGradesClassId = $('webGradeClass').value; window.webGradesSubjectId = ''; grades(); };
    $('webGradeSubject').onchange = () => { window.webGradesSubjectId = $('webGradeSubject').value; grades(); };
    if (classId && subjectId) render({ data, classId, subjectId, className: selectedClass?.name || classId });
  };
})();
