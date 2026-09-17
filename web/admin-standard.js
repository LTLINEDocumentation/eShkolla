// Standardi i Administratorit (SHEFI): çdo modul menaxhimi ka Shto, Rregullo, Fshij + konfirmim.
// Të dhënat administrative lexohen nga endpointet reale të management.

function adminStandardButtons(kind,id,name){
  return `<div class="section-actions"><button type="button" class="secondary admin-standard-edit" data-kind="${esc(kind)}" data-id="${esc(id)}">Rregullo</button><button type="button" class="secondary admin-standard-delete" data-kind="${esc(kind)}" data-id="${esc(id)}" data-name="${esc(name)}">Fshij</button></div>`;
}

function bindAdminStandardActions(){
  document.querySelectorAll('.admin-standard-edit').forEach(b=>b.onclick=()=>adminEdit(b.dataset.kind,b.dataset.id));
  document.querySelectorAll('.admin-standard-delete').forEach(b=>b.onclick=()=>adminDelete(b.dataset.kind,b.dataset.id,b.dataset.name));
}

function adminStandardTable(headers,rows,addLabel,onAdd){
  $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button type="button" class="primary" id="dynamicAdd">+ ${esc(addLabel)}</button></div><div class="table-wrap"><table><thead><tr>${headers.map(x=>`<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>${rows.length?rows.map(r=>`<tr>${r.join('')}</tr>`).join(''):`<tr><td colspan="${headers.length}" class="empty-state">Nuk ka të dhëna.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${rows.length} rezultate reale`;
  $('dynamicAdd').onclick=onAdd;
  bindAdminStandardActions();
  if(typeof window.bindAdminFilters==='function') window.bindAdminFilters();
}

async function schools(){
  const d=await api('/api/v1/management/schools');
  adminStandardTable(['ID','Shkolla','Adresa','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.name)}</td>`,`<td>${esc(x.address||'')}</td>`,`<td>${x.active?'Aktive':'Joaktive'}</td>`,`<td>${adminStandardButtons('school',x.id,x.name)}</td>`]),'Shto shkollë',()=>schoolForm());
}

async function subjects(){
  const d=await api('/api/v1/management/subjects');
  adminStandardTable(['ID','Lënda mësimore','Kodi','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.name)}</td>`,`<td>${esc(x.code||'')}</td>`,`<td>${x.active?'Aktive':'Joaktive'}</td>`,`<td>${adminStandardButtons('subject',x.id,x.name)}</td>`]),'Shto lëndë',()=>subjectForm());
}

async function teachers(){
  const [d,subjectsData,classesData,codes]=await Promise.all([
    api('/api/v1/management/teachers'),api('/api/v1/management/subjects'),api('/api/v1/management/classes'),api('/api/v1/management/timetable-codes')
  ]);
  const subjectMap=new Map(subjectsData.map(x=>[String(x.id),x.name]));
  const classMap=new Map(classesData.map(x=>[String(x.id),x.name]));
  const codeMap=new Map(codes.filter(x=>x.teacherId).map(x=>[String(x.teacherId),x.code]));
  const rows=d.map(x=>{
    const classNames=(x.classIds||[]).map(id=>classMap.get(String(id))||id);
    const code=x.scheduleCode??codeMap.get(String(x.id));
    const status=x.relationStatus||((x.username&&classNames.length&&code)?'OK':'Kontrollo lidhjet');
    const statusHtml=status==='OK'?'<span class="status">Në rregull</span>':`<span class="status">${esc(status)}</span>`;
    return [`<td>${esc(x.id)}</td>`,`<td><strong>${esc(x.fullName)}</strong></td>`,`<td>${esc(subjectMap.get(String(x.subjectId))||x.subjectName||x.subjectId||'—')}</td>`,`<td>${esc(x.username||'—')}</td>`,`<td>${classNames.length?classNames.map(esc).join(', '):'—'}</td>`,`<td>${code??'—'}</td>`,`<td>${statusHtml}</td>`,`<td>${adminStandardButtons('teacher',x.id,x.fullName)}</td>`];
  });
  adminStandardTable(['ID','Mësimdhënësi','Lënda','Përdoruesi','Klasat / paralelet','Kodi i orarit','Lidhjet','Veprime'],rows,'Shto mësimdhënës',()=>teacherForm());
}

async function students(admin=false){
  if(!admin){
    const q=$('tableSearch').value.trim();
    const d=await api('/api/v1/students?page=1&pageSize=100'+(q?'&search='+encodeURIComponent(q):''));
    table(['ID','Emri','Klasa','Datëlindja','Statusi'],(d.items||[]).map(x=>[x.id,x.fullName,x.classId,x.birthDate,x.isActive?'Aktiv':'Joaktiv']));
    return;
  }
  const d=await api('/api/v1/management/students');
  adminStandardTable(['ID','Emri','Klasa / paralelja','Datëlindja','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.fullName)}</td>`,`<td>${esc(x.className)}</td>`,`<td>${esc(x.birthDate)}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td>${adminStandardButtons('student',x.id,x.fullName)}</td>`]),'Shto nxënës',()=>studentForm());
}

async function users(){
  const d=await api('/api/v1/management/users');
  adminStandardTable(['ID','Përdoruesi','Emri','Roli','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.username)}</td>`,`<td>${esc(x.fullName)}</td>`,`<td>${esc(roles[x.role]||x.role)}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td>${adminStandardButtons('user',x.id,x.fullName)}</td>`]),'Krijo përdorues dhe cakto rol',()=>userForm());
}

async function grades(){
  const [d,studentsData,subjectsData,teachersData]=await Promise.all([api('/api/v1/management/grades'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
  const studentName=id=>studentsData.find(x=>x.id===id)?.fullName||id;
  const subjectName=id=>subjectsData.find(x=>x.id===id)?.name||id;
  const teacherName=id=>teachersData.find(x=>x.id===id)?.fullName||id;
  adminStandardTable(['Nxënësi','Lënda','Nota','Periudha','Viti','Mësimdhënësi','Veprime'],d.map(x=>[`<td>${esc(x.studentName||studentName(x.studentId))}</td>`,`<td>${esc(x.subjectName||subjectName(x.subjectId))}</td>`,`<td>${esc(x.value)}</td>`,`<td>${esc(x.period)}</td>`,`<td>${esc(x.academicYear)}</td>`,`<td>${esc(x.teacherName||teacherName(x.teacherId))}</td>`,`<td>${adminStandardButtons('grade',x.id,`${x.studentName||studentName(x.studentId)} — ${x.subjectName||subjectName(x.subjectId)}`)}</td>`]),'Shto notë',()=>gradeForm(null,studentsData,subjectsData,teachersData));
}

async function absences(){
  const [d,studentsData,subjectsData,teachersData]=await Promise.all([api('/api/v1/management/absences'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
  const studentName=id=>studentsData.find(x=>x.id===id)?.fullName||id;
  const subjectName=id=>subjectsData.find(x=>x.id===id)?.name||id;
  const teacherName=id=>teachersData.find(x=>x.id===id)?.fullName||id;
  adminStandardTable(['Nxënësi','Lënda','Data','Statusi','Shënim','Mësimdhënësi','Veprime'],d.map(x=>[`<td>${esc(x.studentName||studentName(x.studentId))}</td>`,`<td>${esc(x.subjectName||subjectName(x.subjectId))}</td>`,`<td>${esc(x.date)}</td>`,`<td>${esc(x.status)}</td>`,`<td>${esc(x.note||'')}</td>`,`<td>${esc(x.teacherName||teacherName(x.teacherId))}</td>`,`<td>${adminStandardButtons('absence',x.id,`${x.studentName||studentName(x.studentId)} — ${x.date}`)}</td>`]),'Shto mungesë',()=>absenceForm(null,studentsData,subjectsData,teachersData));
}

function selectOptions(items,selected,mode){
  return items.map(x=>{
    const label=mode==='student'?`${x.fullName} — ${x.className}`:mode==='teacher'?`${x.fullName} — ${x.username}`:x.name;
    return `<option value="${esc(x.id)}" ${x.id===selected?'selected':''}>${esc(label)}</option>`;
  }).join('');
}

async function studentForm(item=null){
  const classes=await api('/api/v1/management/classes');
  const activeClasses=classes.filter(x=>x.active!==false);
  const body=`<label>Emri dhe mbiemri<input name="fullName" type="text" required value="${esc(item?.fullName||'')}"></label><label>Datëlindja<input name="birthDate" type="date" required value="${esc(item?.birthDate||'')}"></label><label>Klasa / paralelja<select name="classId" required><option value="">Zgjidh klasën / paralelen...</option>${activeClasses.map(x=>`<option value="${esc(x.id)}" ${String(x.id)===String(item?.classId)?'selected':''}>${esc(x.name)} — Klasa ${esc(x.gradeLevel)}</option>`).join('')}</select></label><p class="form-help">Prindi: opsional. Nxënësi mund të regjistrohet edhe pa prind të lidhur; lidhja me prindin mund të bëhet më vonë.</p>`;
  modal(item?'Rregullo nxënësin':'Shto nxënës',body,async f=>{
    const payload={fullName:f.get('fullName'),birthDate:f.get('birthDate'),classId:f.get('classId')};
    return api(item?`/api/v1/management/students/${encodeURIComponent(item.id)}`:'/api/v1/management/students',{method:item?'PUT':'POST',body:JSON.stringify(payload)});
  });
}

async function adminEdit(kind,id){
  if(kind==='school') return editSchool(id);
  if(kind==='subject') return editSubject(id);
  if(kind==='teacher') return editTeacher(id);
  if(kind==='student') return editStudent(id);
  if(kind==='user') return editUser(id);
  if(kind==='grade'){
    const [d,s,sub,t]=await Promise.all([api('/api/v1/management/grades'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
    const item=d.find(x=>x.id===id); if(item) gradeForm(item,s,sub,t); return;
  }
  if(kind==='absence'){
    const [d,s,sub,t]=await Promise.all([api('/api/v1/management/absences'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
    const item=d.find(x=>x.id===id); if(item) absenceForm(item,s,sub,t); return;
  }
}

async function adminDelete(kind,id,name){
  if(!confirm(`A jeni i sigurt që doni ta fshini ${kindLabel(kind)} "${name}"?\n\nKy veprim do të çaktivizojë/fshijë të dhënën sipas rregullave të modulit. Historiku i lidhur ruhet kur është e nevojshme.`)) return;
  try{
    const path={school:'/api/v1/management/schools/',subject:'/api/v1/management/subjects/',teacher:'/api/v1/management/teachers/',student:'/api/v1/management/students/',user:'/api/v1/management/users/',grade:'/api/v1/grades/',absence:'/api/v1/absences/'}[kind];
    await api(path+encodeURIComponent(id),{method:'DELETE'});
    await openModule(currentModule);
  }catch(e){alert(e.message)}
}
