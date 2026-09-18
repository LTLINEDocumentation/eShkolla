let teacherRoleData=null;
async function loadTeacherRoleData(force=false){if(teacherRoleData&&!force)return teacherRoleData;teacherRoleData=await api('/api/v1/me/data');return teacherRoleData;}
function baseModules(){
  if(currentUser?.role==='MESIMDHENES') return [['classes','Klasat e mia','Klasat e caktuara'],['subjects','Lëndët e mia','Lëndët e caktuara'],['students','Nxënësit','Nxënësit e klasave të mia'],['grades','Notat','Notat e nxënësve të mi'],['absences','Mungesat','Evidenca e klasave të mia'],['profile','Profili','Llogaria']];
  if(currentUser?.role==='DREJTOR') return [['classes','Klasat & paralelet','Menaxhimi i klasave'],['subjects','Lëndët mësimore','Shikim i lëndëve'],['teachers','Mësimdhënësit','Menaxhimi i mësimdhënësve'],['students','Nxënësit','Regjistri i nxënësve'],['assignments','Caktimet','Mësimdhënës + lëndë + klasë'],['grades','Notat','Shikim dhe veprim i kontrolluar'],['absences','Mungesat','Shikim dhe veprim i kontrolluar'],['profile','Profili','Llogaria']];
  return [['classes','Klasat','Të dhënat reale'],['students','Nxënësit','Regjistri real'],['grades','Notat','Të dhënat reale'],['absences','Mungesat','Evidenca reale'],['profile','Profili','Llogaria']];
}
const _originalSubjects=window.subjects;
window.subjects=async function roleSubjects(){if(currentUser?.role==='MESIMDHENES'){const d=await loadTeacherRoleData();table(['ID','Lënda','Kodi','Klasat'],d.subjects.map(x=>[x.id,x.name,x.code||'—',x.classIds.map(id=>d.classes.find(c=>c.id===id)?.name||id).join(', ')||'—']));return;}return _originalSubjects();}
const _originalClasses=window.classes;
window.classes=async function roleClasses(admin=false){if(currentUser?.role==='MESIMDHENES'){const d=await loadTeacherRoleData();table(['ID','Klasa / paralelja','Niveli','Nxënës'],d.classes.map(x=>[x.id,x.name,x.gradeLevel,x.studentCount]));return;}return _originalClasses(admin);}
const _originalStudents=window.students;
window.students=async function roleStudents(admin=false){if(currentUser?.role==='MESIMDHENES'){const d=await loadTeacherRoleData();const q=($('tableSearch')?.value||'').trim().toLowerCase();const rows=d.students.filter(x=>!q||x.fullName.toLowerCase().includes(q)||x.id.toLowerCase().includes(q)).map(x=>[x.id,x.fullName,x.className,x.birthDate,x.active?'Aktiv':'Joaktiv']);table(['ID','Emri','Klasa','Datëlindja','Statusi'],rows);return;}return _originalStudents(admin);}
const _originalGrades=window.grades;
const webAssessmentColumns=[
  {key:'test1',label:'Testi 1',note:'Testi 1',period:'Periudha I',editable:true},
  {key:'test2',label:'Testi 2',note:'Testi 2',period:'Periudha I',editable:true},
  {key:'mid1',label:'Nota 1 e gjysmëvitit',note:'Nota 1 e gjysmëvitit',period:'Periudha I',editable:false,automatic:true},
  {key:'test3',label:'Testi 3',note:'Testi 3',period:'Periudha II',editable:true},
  {key:'test4',label:'Testi 4',note:'Testi 4',period:'Periudha II',editable:true},
  {key:'mid2',label:'Nota 2 e gjysmëvitit',note:'Nota 2 e gjysmëvitit',period:'Periudha II',editable:false,automatic:true},
  {key:'final',label:'Nota Përfundimtare',note:'Nota Përfundimtare',period:'Periudha II',editable:false,automatic:true}
];
function webAcademicYear(){const now=new Date();const start=now.getMonth()>=8?now.getFullYear():now.getFullYear()-1;return `${start}/${start+1}`;}
function webGradeFor(grades,studentId,subjectId,column){return grades.find(g=>g.studentId===studentId&&g.subjectId===subjectId&&g.note===column.note)||null;}
function webManualGrade(grades,studentId,subjectId,note){return grades.find(g=>g.studentId===studentId&&g.subjectId===subjectId&&g.note===note)||null;}
function webRoundedAverage(values){if(values.length!==2||values.some(v=>v==null||Number.isNaN(Number(v))))return null;return Math.min(5,Math.max(1,Math.floor((Number(values[0])+Number(values[1]))/2+0.5)));}
function webAutomaticGrade(grades,studentId,subjectId,column){
  if(!column.automatic)return webGradeFor(grades,studentId,subjectId,column)?.value??null;
  if(column.key==='mid1'){
    const a=webManualGrade(grades,studentId,subjectId,'Testi 1');
    const b=webManualGrade(grades,studentId,subjectId,'Testi 2');
    return webRoundedAverage([a?.value,b?.value]);
  }
  if(column.key==='mid2'){
    const a=webManualGrade(grades,studentId,subjectId,'Testi 3');
    const b=webManualGrade(grades,studentId,subjectId,'Testi 4');
    return webRoundedAverage([a?.value,b?.value]);
  }
  if(column.key==='final'){
    const mid1=webAutomaticGrade(grades,studentId,subjectId,webAssessmentColumns.find(x=>x.key==='mid1'));
    const mid2=webAutomaticGrade(grades,studentId,subjectId,webAssessmentColumns.find(x=>x.key==='mid2'));
    return webRoundedAverage([mid1,mid2]);
  }
  return null;
}
function webGradeCellValue(g){return g==null?'—':String(g);}
function renderWebGrades(state){
  const {data,classId,subjectId,students}=state;
  const subject=data.subjects.find(s=>s.id===subjectId);
  const activeStudents=students.filter(s=>s.active).sort((a,b)=>a.fullName.localeCompare(b.fullName,'sq'));
  const className=data.classes.find(c=>c.id===classId)?.name||classId;
  const gradeList=data.grades||[];
  $('moduleContent').innerHTML=`
    <div class="web-grade-panel">
      <div class="web-grade-toolbar">
        <div><strong>Vlerësimi i nxënësve</strong><small>${esc(className)} · ${esc(subject?.name||'Lënda')} · ${esc(webAcademicYear())}</small></div>
        <button type="button" class="secondary" id="webGradeRefresh">Rifresko</button>
      </div>
      <div class="web-grade-hint">Kliko në qelizën e nxënësit dhe zgjidh notën 1–5. Pas ruajtjes kalon automatikisht te nxënësi tjetër; në fund të kolonës vazhdon te vlerësimi tjetër.</div>
      <div class="table-wrap web-grade-table-wrap"><table class="web-grade-table"><thead><tr>${['Emri dhe Mbiemri',...webAssessmentColumns.map(c=>c.label)].map(x=>`<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>
        ${activeStudents.length?activeStudents.map((student,rowIndex)=>`<tr><td class="web-grade-name"><strong>${esc(student.fullName)}</strong></td>${webAssessmentColumns.map((column,colIndex)=>{const value=webAutomaticGrade(gradeList,student.id,subjectId,column);const editable=column.editable!==false;return editable?`<td><button type="button" class="web-grade-cell ${value!=null?'has-grade':''}" data-student="${esc(student.id)}" data-row="${rowIndex}" data-col="${colIndex}">${esc(webGradeCellValue(value))}</button></td>`:`<td class="web-grade-auto-cell ${value!=null?'has-grade':''}" title="Llogaritet automatikisht nga sistemi"><strong>${esc(webGradeCellValue(value))}</strong></td>`}).join('')}</tr>`).join(''):`<tr><td colspan="8" class="empty-state">Nuk ka nxënës aktivë në këtë klasë.</td></tr>`}
      </tbody></table></div>
      <p id="webGradeStatus" class="result-count"></p>
    </div>`;
  $('resultCount').textContent=`${activeStudents.length} nxënës · ${subject?.name||'Lëndë'}`;
  $('webGradeRefresh').onclick=()=>grades();
  document.querySelectorAll('.web-grade-cell').forEach(btn=>btn.onclick=()=>openWebGradePicker(state,Number(btn.dataset.row),Number(btn.dataset.col)));
  injectWebGradeStyles();
}
function nextWebCell(state,row,col){
  const rows=state.students.filter(s=>s.active).sort((a,b)=>a.fullName.localeCompare(b.fullName,'sq'));
  const editableColumns=webAssessmentColumns.map((x,i)=>x.editable!==false?i:-1).filter(i=>i>=0);
  if(!rows.length)return null;
  if(row+1<rows.length)return {row:row+1,col};
  const nextColumn=editableColumns.find(i=>i>col);
  return nextColumn==null?null:{row:0,col:nextColumn};
}
async function saveWebGrade(state,row,col,value,existing){
  const column=webAssessmentColumns[col];
  if(!column||column.editable===false)throw Error('Kjo notë llogaritet automatikisht nga sistemi.');
  const d=state.data,student=state.students.filter(s=>s.active).sort((a,b)=>a.fullName.localeCompare(b.fullName,'sq'))[row];
  if(!student||!column)return;
  const payload={studentId:student.id,subjectId:state.subjectId,teacherId:d.teacherId,value:Number(value),period:column.period,academicYear:webAcademicYear(),note:column.note};
  const saved=await api(existing?`/api/v1/grades/${encodeURIComponent(existing.id)}`:'/api/v1/grades',{method:existing?'PUT':'POST',body:JSON.stringify(payload)});
  if(existing)Object.assign(existing,saved);else d.grades.push(saved);
}
async function deleteWebGrade(state,row,col,existing){if(!existing)return;await api(`/api/v1/grades/${encodeURIComponent(existing.id)}`,{method:'DELETE'});state.data.grades=state.data.grades.filter(g=>g.id!==existing.id);renderWebGrades(state);}
function openWebGradePicker(state,row,col){
  const students=state.students.filter(s=>s.active).sort((a,b)=>a.fullName.localeCompare(b.fullName,'sq'));
  const student=students[row],column=webAssessmentColumns[col];
  if(!column||column.editable===false)return;
  const existing=webGradeFor(state.data.grades||[],student.id,state.subjectId,column);
  const wrap=document.createElement('div');wrap.className='modal';wrap.innerHTML=`<div class="modal-card web-grade-picker"><div class="section-title"><div><h3>Vlerëso nxënësin</h3><p class="muted">${esc(student.fullName)} · ${esc(column.label)}</p></div><button type="button" class="secondary" id="closeWebGrade">Mbyll</button></div><div class="web-grade-buttons">${[1,2,3,4,5].map(n=>`<button type="button" class="web-grade-choice ${String(existing?.value)===String(n)?'selected':''}" data-value="${n}">${n}</button>`).join('')}</div>${existing?'<button type="button" class="secondary web-grade-delete">Fshij notën</button>':''}<p id="webGradePickerError" class="error"></p></div>`;
  document.body.appendChild(wrap);
  const close=()=>wrap.remove();$('closeWebGrade').onclick=close;
  wrap.querySelectorAll('.web-grade-choice').forEach(button=>button.onclick=async()=>{const value=button.dataset.value;button.disabled=true;try{await saveWebGrade(state,row,col,value,existing);close();const next=nextWebCell(state,row,col);renderWebGrades(state);if(next){const target=document.querySelector(`.web-grade-cell[data-row="${next.row}"][data-col="${next.col}"]`);target?.focus();openWebGradePicker(state,next.row,next.col);}}catch(e){wrap.querySelector('#webGradePickerError').textContent=e.message;button.disabled=false;}});
  wrap.querySelector('.web-grade-delete')?.addEventListener('click',async()=>{try{await deleteWebGrade(state,row,col,existing);close();}catch(e){wrap.querySelector('#webGradePickerError').textContent=e.message;}});
}
window.grades=async function roleGrades(){
  if(currentUser?.role!=='MESIMDHENES')return _originalGrades();
  const d=await loadTeacherRoleData(true);
  const classes=(d.classes||[]).filter(c=>c.active!==false).sort((a,b)=>String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));
  const selectedClass=window.webGradesClassId||'';
  const selectedClassObj=classes.find(c=>c.id===selectedClass);
  if(!selectedClassObj){
    window.webGradesSubjectId='';
    $('moduleContent').innerHTML=`<div class="web-grade-panel">
      <div class="web-grade-toolbar"><div><strong>Zgjidh klasën</strong><small>Zgjidh klasën/paralelen për të filluar vlerësimin.</small></div></div>
      <div class="web-grade-class-grid">${classes.length?classes.map(c=>`<button type="button" class="web-grade-class-card" data-class="${esc(c.id)}"><strong>${esc(c.name)}</strong><span>${Number(c.studentCount||0)} nxënës</span></button>`).join(''):`<div class="empty-state">Nuk ka klasa të caktuara.</div>`}</div>
    </div>`;
    $('resultCount').textContent=`${classes.length} klasa`;
    document.querySelectorAll('.web-grade-class-card').forEach(btn=>btn.onclick=()=>{window.webGradesClassId=btn.dataset.class;window.webGradesSubjectId='';grades();});
    injectWebGradeStyles();
    return;
  }

  const subjectsForClass=(d.subjects||[]).filter(s=>s.classIds?.includes(selectedClass)).sort((a,b)=>String(a.name).localeCompare(String(b.name),'sq'));
  const selectedSubject=window.webGradesSubjectId&&subjectsForClass.some(s=>s.id===window.webGradesSubjectId)?window.webGradesSubjectId:(subjectsForClass[0]?.id||'');
  window.webGradesSubjectId=selectedSubject;
  const students=(d.students||[]).filter(s=>s.classId===selectedClass);

  $('moduleContent').innerHTML=`<div class="web-grade-panel">
    <div class="web-grade-breadcrumb">
      <button type="button" class="secondary" id="webGradeBackClasses">← Klasat</button>
      <div><strong>${esc(selectedClassObj.name)}</strong><small>Lista e nxënësve për vlerësim</small></div>
    </div>
    <div class="web-grade-selectors">
      <label>Lënda / kompetenca<select id="webGradeSubject">${subjectsForClass.map(s=>`<option value="${esc(s.id)}" ${s.id===selectedSubject?'selected':''}>${esc(s.name)}${s.code?' — '+esc(s.code):''}</option>`).join('')}</select></label>
    </div>
    ${selectedSubject?'<div id="webGradeTableHost"></div>':'<div class="empty-state">Kjo klasë nuk ka ende lëndë të caktuara.</div>'}
  </div>`;
  $('resultCount').textContent=`${students.length} nxënës · ${esc(selectedClassObj.name)}`;
  $('webGradeBackClasses').onclick=()=>{window.webGradesClassId='';window.webGradesSubjectId='';grades();};
  $('webGradeSubject').onchange=()=>{window.webGradesSubjectId=$('webGradeSubject').value;grades();};
  if(selectedClass&&selectedSubject)renderWebGrades({data:d,classId:selectedClass,subjectId:selectedSubject,students});
}
function injectWebGradeStyles(){if(document.getElementById('webGradeStyles'))return;const style=document.createElement('style');style.id='webGradeStyles';style.textContent=`
.web-grade-panel{display:grid;gap:14px}
.web-grade-class-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}
.web-grade-class-card{min-height:92px;padding:14px;text-align:left;border:1px solid #dbe3ef;border-radius:12px;background:#fff;cursor:pointer;display:flex;flex-direction:column;justify-content:center;gap:6px}
.web-grade-class-card:hover{border-color:#1f6feb;box-shadow:0 4px 14px rgba(16,24,40,.08)}
.web-grade-class-card strong{font-size:18px}.web-grade-class-card span{font-size:13px;color:#667085}
.web-grade-breadcrumb{display:flex;align-items:center;gap:14px;padding:12px 0}.web-grade-breadcrumb>div{display:grid;gap:3px}.web-grade-breadcrumb small{color:#667085}
.web-grade-selectors{display:grid;grid-template-columns:minmax(260px,420px);gap:14px;padding:14px;border:1px solid #dbe3ef;border-radius:12px;background:#f8fafc}.web-grade-selectors label{display:grid;gap:6px;font-weight:600}.web-grade-selectors select{width:100%}
.web-grade-toolbar{display:flex;justify-content:space-between;align-items:center;gap:12px}.web-grade-toolbar strong{display:block;font-size:18px}.web-grade-toolbar small{display:block;margin-top:4px;color:#667085}
.web-grade-hint{padding:10px 12px;border:1px dashed #cbd5e1;border-radius:10px;background:#f8fafc;color:#475467}
.web-grade-table-wrap{overflow:hidden}.web-grade-table{width:100%;table-layout:fixed}.web-grade-table th:first-child,.web-grade-table td:first-child{width:22%;text-align:left}.web-grade-table th:not(:first-child),.web-grade-table td:not(:first-child){width:11.14%;text-align:center}.web-grade-table th{white-space:normal;font-size:12px;padding:9px 5px}.web-grade-name{padding:10px 8px!important;white-space:normal}.web-grade-cell,.web-grade-auto-cell{width:100%;min-width:0;min-height:38px;height:40px;padding:4px;border-radius:7px;font-size:16px}.web-grade-cell{border:1px solid #d0d5dd;background:#fff;font-weight:700;cursor:pointer}.web-grade-cell:hover{border-color:#1f6feb}.web-grade-cell.has-grade{background:#f0fdf4}.web-grade-auto-cell{border:1px dashed #cbd5e1;background:#f8fafc;font-weight:700;display:flex;align-items:center;justify-content:center}.web-grade-auto-cell.has-grade{background:#eef6ff;border-style:solid}.web-grade-buttons{display:grid;grid-template-columns:repeat(5,1fr);gap:10px;margin:18px 0}.web-grade-choice{height:58px;border:1px solid #d0d5dd;border-radius:10px;background:#fff;font-size:22px;font-weight:800;cursor:pointer}.web-grade-choice.selected{border-width:2px}.web-grade-picker{max-width:520px}.web-grade-delete{width:100%;margin-top:8px}.web-grade-picker .section-title{align-items:flex-start}
@media(max-width:900px){.web-grade-class-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.web-grade-table{font-size:12px}.web-grade-table th:first-child,.web-grade-table td:first-child{width:25%}.web-grade-table th:not(:first-child),.web-grade-table td:not(:first-child){width:10.71%}}
@media(max-width:700px){.web-grade-class-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.web-grade-selectors{grid-template-columns:1fr}.web-grade-toolbar{align-items:stretch;flex-direction:column}.web-grade-table th:first-child,.web-grade-table td:first-child{width:30%}.web-grade-table th:not(:first-child),.web-grade-table td:not(:first-child){width:10%}}
`;document.head.appendChild(style)}
const _originalAbsences=window.absences;
window.absences=async function roleAbsences(){if(currentUser?.role!=='MESIMDHENES')return _originalAbsences();const d=await loadTeacherRoleData(true);$('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto mungesë</button></div><div class="table-wrap"><table><thead><tr>${['Nxënësi','Lënda','Data','Statusi','Shënim','Veprime'].map(x=>`<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>${d.absences.length?d.absences.map(x=>`<tr><td>${esc(x.studentName)}</td><td>${esc(x.subjectName)}</td><td>${esc(x.date)}</td><td>${esc(x.status)}</td><td>${esc(x.note||'—')}</td><td><div class="section-actions"><button type="button" class="secondary teacher-absence-edit" data-id="${esc(x.id)}">Rregullo</button><button type="button" class="secondary teacher-absence-delete" data-id="${esc(x.id)}">Fshij</button></div></td></tr>`).join(''):`<tr><td colspan="6" class="empty-state">Nuk ka mungesa të regjistruara.</td></tr>`}</tbody></table></div>`;$('resultCount').textContent=`${d.absences.length} rezultate reale`;$('dynamicAdd').onclick=()=>teacherAbsenceForm();document.querySelectorAll('.teacher-absence-edit').forEach(b=>b.onclick=()=>{const x=d.absences.find(v=>v.id===b.dataset.id);if(x)teacherAbsenceForm(x)});document.querySelectorAll('.teacher-absence-delete').forEach(b=>b.onclick=()=>teacherDelete('/api/v1/absences/'+encodeURIComponent(b.dataset.id),'mungesën'));}
async function teacherAbsenceForm(existing=null){const d=await loadTeacherRoleData();const body=`<label>Nxënësi<select name="studentId" required>${d.students.filter(x=>x.active).map(x=>`<option value="${esc(x.id)}" ${existing?.studentId===x.id?'selected':''}>${esc(x.fullName)} — ${esc(x.className)}</option>`).join('')}</select></label><label>Lënda<select name="subjectId" required>${d.subjects.map(x=>`<option value="${esc(x.id)}" ${existing?.subjectId===x.id?'selected':''}>${esc(x.name)} — ${esc(x.classIds?.length||0)} klasa</option>`).join('')}</select></label>`+field('date','Data','date',`required value="${esc(existing?.date||new Date().toISOString().slice(0,10))}"`)+`<label>Statusi<select name="status" required>${['Mungon','Me arsye','Pa arsye','E arsyetuar'].map(x=>`<option ${existing?.status===x?'selected':''}>${x}</option>`).join('')}</select></label>`+field('note','Shënim','text',`value="${esc(existing?.note||'')}"`);modal(existing?'Rregullo mungesën':'Shto mungesë',body,async f=>api(existing?`/api/v1/absences/${encodeURIComponent(existing.id)}`:'/api/v1/absences',{method:existing?'PUT':'POST',body:JSON.stringify({studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:d.teacherId,date:f.get('date'),status:f.get('status'),note:f.get('note')||null})}));}
async function teacherDelete(url,label){if(!confirm(`A jeni i sigurt që doni ta fshini ${label}?\n\nKy veprim nuk mund të zhbëhet.`))return;try{await api(url,{method:'DELETE'});teacherRoleData=null;await openModule(currentModule)}catch(e){alert(e.message)}}