// Standardi i Administratorit (SHEFI): çdo modul menaxhimi ka Shto, Rregullo, Fshij + konfirmim.
// Ky skedar ngarkohet i fundit që standardi të jetë uniform edhe kur module të tjera kanë implementime të veçanta.

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
  const d=await api('/api/v1/management/teachers');
  adminStandardTable(['ID','Mësimdhënësi','Lënda kryesore','Përdoruesi','Klasa','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.fullName)}</td>`,`<td>${esc(x.subjectId)}</td>`,`<td>${esc(x.username)}</td>`,`<td>${esc((x.classIds||[]).join(', ')||'—')}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td>${adminStandardButtons('teacher',x.id,x.fullName)}</td>`]),'Shto mësimdhënës',()=>teacherForm());
}

async function students(admin=false){
  if(!admin){
    const q=$('tableSearch').value.trim();
    const d=await api('/api/v1/students?page=1&pageSize=100'+(q?'&search='+encodeURIComponent(q):''));
    table(['ID','Emri','Klasa','Datëlindja','Statusi'],(d.items||[]).map(x=>[x.id,x.fullName,x.classId,x.birthDate,x.isActive?'Aktiv':'Joaktiv']));
    return;
  }
  const d=await api('/api/v1/management/students');
  adminStandardTable(['ID','Emri','Klasa','Datëlindja','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.fullName)}</td>`,`<td>${esc(x.className)}</td>`,`<td>${esc(x.birthDate)}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td>${adminStandardButtons('student',x.id,x.fullName)}</td>`]),'Shto nxënës',()=>studentForm());
}

async function users(){
  const d=await api('/api/v1/management/users');
  adminStandardTable(['ID','Përdoruesi','Emri','Roli','Statusi','Veprime'],d.map(x=>[`<td>${esc(x.id)}</td>`,`<td>${esc(x.username)}</td>`,`<td>${esc(x.fullName)}</td>`,`<td>${esc(roles[x.role]||x.role)}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td>${adminStandardButtons('user',x.id,x.fullName)}</td>`]),'Krijo përdorues dhe cakto rol',()=>userForm());
}

async function grades(){
  const [d,studentsData,subjectsData,teachersData]=await Promise.all([api('/api/v1/grades'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
  const studentName=id=>studentsData.find(x=>x.id===id)?.fullName||id;
  const subjectName=id=>subjectsData.find(x=>x.id===id)?.name||id;
  const teacherName=id=>teachersData.find(x=>x.id===id)?.fullName||id;
  adminStandardTable(['Nxënësi','Lënda','Nota','Periudha','Viti','Mësimdhënësi','Veprime'],d.map(x=>[`<td>${esc(studentName(x.studentId))}</td>`,`<td>${esc(subjectName(x.subjectId))}</td>`,`<td>${esc(x.value)}</td>`,`<td>${esc(x.period)}</td>`,`<td>${esc(x.academicYear)}</td>`,`<td>${esc(teacherName(x.teacherId))}</td>`,`<td>${adminStandardButtons('grade',x.id,`${studentName(x.studentId)} — ${subjectName(x.subjectId)}`)}</td>`]),'Shto notë',()=>gradeForm(null,studentsData,subjectsData,teachersData));
}

async function absences(){
  const [d,studentsData,subjectsData,teachersData]=await Promise.all([api('/api/v1/absences'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
  const studentName=id=>studentsData.find(x=>x.id===id)?.fullName||id;
  const subjectName=id=>subjectsData.find(x=>x.id===id)?.name||id;
  const teacherName=id=>teachersData.find(x=>x.id===id)?.fullName||id;
  adminStandardTable(['Nxënësi','Lënda','Data','Statusi','Shënim','Mësimdhënësi','Veprime'],d.map(x=>[`<td>${esc(studentName(x.studentId))}</td>`,`<td>${esc(subjectName(x.subjectId))}</td>`,`<td>${esc(x.date)}</td>`,`<td>${esc(x.status)}</td>`,`<td>${esc(x.note||'')}</td>`,`<td>${esc(teacherName(x.teacherId))}</td>`,`<td>${adminStandardButtons('absence',x.id,`${studentName(x.studentId)} — ${x.date}`)}</td>`]),'Shto mungesë',()=>absenceForm(null,studentsData,subjectsData,teachersData));
}

function selectOptions(items,selected,mode){
  return items.map(x=>{
    const label=mode==='student'?`${x.fullName} — ${x.className}`:mode==='teacher'?`${x.fullName} — ${x.username}`:x.name;
    return `<option value="${esc(x.id)}" ${x.id===selected?'selected':''}>${esc(label)}</option>`;
  }).join('');
}

function gradeForm(item,studentsData,subjectsData,teachersData){
  const body=`<label>Nxënësi<select name="studentId" required>${selectOptions(studentsData,item?.studentId,'student')}</select></label><label>Lënda<select name="subjectId" required>${selectOptions(subjectsData,item?.subjectId,'subject')}</select></label><label>Nota<input name="value" type="number" min="1" max="5" step="1" required value="${esc(item?.value??'')}"></label><label>Periudha<input name="period" required value="${esc(item?.period??'P1')}"></label><label>Viti shkollor<input name="academicYear" required value="${esc(item?.academicYear??new Date().getFullYear()+'-'+(new Date().getFullYear()+1))}"></label><label>Mësimdhënësi<select name="teacherId" required>${selectOptions(teachersData,item?.teacherId,'teacher')}</select></label><label>Shënim<input name="note" value="${esc(item?.note||'')}"></label>`;
  modal(item?'Rregullo notën':'Shto notë',body,async f=>{
    const payload={studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:f.get('teacherId'),value:Number(f.get('value')),period:f.get('period'),academicYear:f.get('academicYear'),note:f.get('note')||null};
    return api(item?`/api/v1/grades/${encodeURIComponent(item.id)}`:'/api/v1/grades',{method:item?'PUT':'POST',body:JSON.stringify(payload)});
  });
}

function absenceForm(item,studentsData,subjectsData,teachersData){
  const body=`<label>Nxënësi<select name="studentId" required>${selectOptions(studentsData,item?.studentId,'student')}</select></label><label>Lënda<select name="subjectId" required>${selectOptions(subjectsData,item?.subjectId,'subject')}</select></label><label>Data<input name="date" type="date" required value="${esc(item?.date||new Date().toISOString().slice(0,10))}"></label><label>Statusi<select name="status"><option value="E_PAAFTESUAR" ${item?.status==='E_PAAFTESUAR'?'selected':''}>E Paaftësuar</option><option value="E_ARSYESHME" ${item?.status==='E_ARSYESHME'?'selected':''}>E arsyeshme</option></select></label><label>Mësimdhënësi<select name="teacherId" required>${selectOptions(teachersData,item?.teacherId,'teacher')}</select></label><label>Shënim<input name="note" value="${esc(item?.note||'')}"></label>`;
  modal(item?'Rregullo mungesën':'Shto mungesë',body,async f=>{
    const payload={studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:f.get('teacherId'),date:f.get('date'),status:f.get('status'),note:f.get('note')||null};
    return api(item?`/api/v1/absences/${encodeURIComponent(item.id)}`:'/api/v1/absences',{method:item?'PUT':'POST',body:JSON.stringify(payload)});
  });
}

async function adminEdit(kind,id){
  if(kind==='school') return editSchool(id);
  if(kind==='subject') return editSubject(id);
  if(kind==='teacher') return editTeacher(id);
  if(kind==='student') return editStudent(id);
  if(kind==='user') return editUser(id);
  if(kind==='grade'){
    const [d,s,sub,t]=await Promise.all([api('/api/v1/grades'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
    const item=d.find(x=>x.id===id); if(item) gradeForm(item,s,sub,t); return;
  }
  if(kind==='absence'){
    const [d,s,sub,t]=await Promise.all([api('/api/v1/absences'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers')]);
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
