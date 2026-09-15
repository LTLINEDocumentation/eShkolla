let teacherRoleData=null;

async function loadTeacherRoleData(force=false){
  if(teacherRoleData&&!force)return teacherRoleData;
  teacherRoleData=await api('/api/v1/me/data');
  return teacherRoleData;
}

function baseModules(){
  if(currentUser?.role==='MESIMDHENES') return [
    ['classes','Klasat e mia','Klasat e caktuara'],
    ['subjects','Lëndët e mia','Lëndët e caktuara'],
    ['students','Nxënësit','Nxënësit e klasave të mia'],
    ['grades','Notat','Notat e nxënësve të mi'],
    ['absences','Mungesat','Evidenca e klasave të mia'],
    ['profile','Profili','Llogaria']
  ];
  return [['classes','Klasat','Të dhënat reale'],['students','Nxënësit','Regjistri real'],['grades','Notat','Të dhënat reale'],['absences','Mungesat','Evidenca reale'],['profile','Profili','Llogaria']];
}

const _originalSubjects=window.subjects;
async function subjects(){
  if(currentUser?.role!=='MESIMDHENES') return _originalSubjects();
  const d=await loadTeacherRoleData();
  const rows=d.subjects.map(x=>[x.id,x.name,x.code||'—',x.classIds.map(id=>d.classes.find(c=>c.id===id)?.name||id).join(', ')||'—']);
  table(['ID','Lënda','Kodi','Klasat'],rows);
}

const _originalClasses=window.classes;
async function classes(admin=false){
  if(currentUser?.role!=='MESIMDHENES') return _originalClasses(admin);
  const d=await loadTeacherRoleData();
  table(['ID','Klasa / paralelja','Niveli','Nxënës'],d.classes.map(x=>[x.id,x.name,x.gradeLevel,x.studentCount]));
}

const _originalStudents=window.students;
async function students(admin=false){
  if(currentUser?.role!=='MESIMDHENES') return _originalStudents(admin);
  const d=await loadTeacherRoleData();
  const q=($('tableSearch')?.value||'').trim().toLowerCase();
  const rows=d.students.filter(x=>!q||x.fullName.toLowerCase().includes(q)||x.id.toLowerCase().includes(q)).map(x=>[x.id,x.fullName,x.className,x.birthDate,x.active?'Aktiv':'Joaktiv']);
  table(['ID','Emri','Klasa','Datëlindja','Statusi'],rows);
}

const _originalGrades=window.grades;
async function grades(){
  if(currentUser?.role!=='MESIMDHENES') return _originalGrades();
  const d=await loadTeacherRoleData(true);
  const rows=d.grades.map(x=>[x.id,x.studentName,x.subjectName,x.value,x.period,x.academicYear,x.note||'—']);
  $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto notë</button></div><div class="table-wrap"><table><thead><tr>${['Nxënësi','Lënda','Nota','Periudha','Viti','Shënim','Veprime'].map(x=>`<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>${rows.length?d.grades.map(x=>`<tr><td>${esc(x.studentName)}</td><td>${esc(x.subjectName)}</td><td>${esc(x.value)}</td><td>${esc(x.period)}</td><td>${esc(x.academicYear)}</td><td>${esc(x.note||'—')}</td><td><div class="section-actions"><button type="button" class="secondary teacher-grade-edit" data-id="${esc(x.id)}">Rregullo</button><button type="button" class="secondary teacher-grade-delete" data-id="${esc(x.id)}">Fshij</button></div></td></tr>`).join(''):`<tr><td colspan="7" class="empty-state">Nuk ka nota të regjistruara.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${d.grades.length} rezultate reale`;
  $('dynamicAdd').onclick=()=>teacherGradeForm();
  document.querySelectorAll('.teacher-grade-edit').forEach(b=>b.onclick=()=>{const x=d.grades.find(v=>v.id===b.dataset.id);if(x)teacherGradeForm(x)});
  document.querySelectorAll('.teacher-grade-delete').forEach(b=>b.onclick=()=>teacherDelete('/api/v1/grades/'+encodeURIComponent(b.dataset.id),'notën'));
}

async function teacherGradeForm(existing=null){
  const d=await loadTeacherRoleData();
  const body=`<label>Nxënësi<select name="studentId" required>${d.students.filter(x=>x.active).map(x=>`<option value="${esc(x.id)}" ${existing?.studentId===x.id?'selected':''}>${esc(x.fullName)} — ${esc(x.className)}</option>`).join('')}</select></label>`+
    `<label>Lënda<select name="subjectId" required>${d.subjects.map(x=>`<option value="${esc(x.id)}" ${existing?.subjectId===x.id?'selected':''}>${esc(x.name)}</option>`).join('')}</select></label>`+
    `<label>Nota<select name="value" required>${[1,2,3,4,5].map(n=>`<option value="${n}" ${String(existing?.value)===String(n)?'selected':''}>${n}</option>`).join('')}</select></label>`+
    field('period','Periudha','text',`required value="${esc(existing?.period||'') }" placeholder="p.sh. Periudha I"`)+
    field('academicYear','Viti shkollor','text',`required value="${esc(existing?.academicYear||'') }" placeholder="p.sh. 2026/2027"`)+
    field('note','Shënim','text',`value="${esc(existing?.note||'') }"`);
  modal(existing?'Rregullo notën':'Shto notë',body,async f=>api(existing?`/api/v1/grades/${encodeURIComponent(existing.id)}`:'/api/v1/grades',{method:existing?'PUT':'POST',body:JSON.stringify({studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:d.teacherId,value:Number(f.get('value')),period:f.get('period'),academicYear:f.get('academicYear'),note:f.get('note')||null})}));
}

const _originalAbsences=window.absences;
async function absences(){
  if(currentUser?.role!=='MESIMDHENES') return _originalAbsences();
  const d=await loadTeacherRoleData(true);
  $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto mungesë</button></div><div class="table-wrap"><table><thead><tr>${['Nxënësi','Lënda','Data','Statusi','Shënim','Veprime'].map(x=>`<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>${d.absences.length?d.absences.map(x=>`<tr><td>${esc(x.studentName)}</td><td>${esc(x.subjectName)}</td><td>${esc(x.date)}</td><td>${esc(x.status)}</td><td>${esc(x.note||'—')}</td><td><div class="section-actions"><button type="button" class="secondary teacher-absence-edit" data-id="${esc(x.id)}">Rregullo</button><button type="button" class="secondary teacher-absence-delete" data-id="${esc(x.id)}">Fshij</button></div></td></tr>`).join(''):`<tr><td colspan="6" class="empty-state">Nuk ka mungesa të regjistruara.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${d.absences.length} rezultate reale`;
  $('dynamicAdd').onclick=()=>teacherAbsenceForm();
  document.querySelectorAll('.teacher-absence-edit').forEach(b=>b.onclick=()=>{const x=d.absences.find(v=>v.id===b.dataset.id);if(x)teacherAbsenceForm(x)});
  document.querySelectorAll('.teacher-absence-delete').forEach(b=>b.onclick=()=>teacherDelete('/api/v1/absences/'+encodeURIComponent(b.dataset.id),'mungesën'));
}

async function teacherAbsenceForm(existing=null){
  const d=await loadTeacherRoleData();
  const body=`<label>Nxënësi<select name="studentId" required>${d.students.filter(x=>x.active).map(x=>`<option value="${esc(x.id)}" ${existing?.studentId===x.id?'selected':''}>${esc(x.fullName)} — ${esc(x.className)}</option>`).join('')}</select></label>`+
    `<label>Lënda<select name="subjectId" required>${d.subjects.map(x=>`<option value="${esc(x.id)}" ${existing?.subjectId===x.id?'selected':''}>${esc(x.name)}</option>`).join('')}</select></label>`+
    field('date','Data','date',`required value="${esc(existing?.date||new Date().toISOString().slice(0,10))}"`)+
    `<label>Statusi<select name="status" required>${['Mungon','Me arsye','Pa arsye','E arsyetuar'].map(x=>`<option ${existing?.status===x?'selected':''}>${x}</option>`).join('')}</select></label>`+
    field('note','Shënim','text',`value="${esc(existing?.note||'') }"`);
  modal(existing?'Rregullo mungesën':'Shto mungesë',body,async f=>api(existing?`/api/v1/absences/${encodeURIComponent(existing.id)}`:'/api/v1/absences',{method:existing?'PUT':'POST',body:JSON.stringify({studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:d.teacherId,date:f.get('date'),status:f.get('status'),note:f.get('note')||null})}));
}

async function teacherDelete(url,label){
  if(!confirm(`A jeni i sigurt që doni ta fshini ${label}?\n\nKy veprim nuk mund të zhbëhet.`))return;
  try{await api(url,{method:'DELETE'});teacherRoleData=null;await openModule(currentModule)}catch(e){alert(e.message)}
}
