// Administrator: menaxhim i plotë i caktimeve (shto, ndrysho, hiq)
async function assignments(){
  const [d,t,s,c]=await Promise.all([
    api('/api/v1/management/teacher-subject-assignments'),
    api('/api/v1/management/teachers'),
    api('/api/v1/management/subjects'),
    api('/api/v1/management/classes')
  ]);
  $('moduleContent').innerHTML=`<div class="table-wrap"><table><thead><tr><th>Mësimdhënësi</th><th>Lënda</th><th>Klasa/paralelja</th><th>Veprime</th></tr></thead><tbody>${d.length?d.map(x=>`<tr><td>${esc(x.teacherName)}</td><td>${esc(x.subjectName)}</td><td>${esc(x.className)}</td><td><div class="section-actions"><button class="secondary" data-edit="${esc(x.teacherId)}|${esc(x.subjectId)}|${esc(x.classId)}">Rregullo</button><button class="secondary" data-delete="${esc(x.teacherId)}|${esc(x.subjectId)}|${esc(x.classId)}">Hiq</button></div></td></tr>`).join(''):`<tr><td colspan="4" class="empty-state">Nuk ka caktime.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${d.length} caktime reale`;
  addButton('Cakto mësimdhënës + lëndë + klasë',()=>assignmentForm(t,s,c));
  document.querySelectorAll('[data-edit]').forEach(b=>b.onclick=()=>{
    const [teacherId,subjectId,classId]=b.dataset.edit.split('|');
    assignmentEditForm(d.find(x=>x.teacherId===teacherId&&x.subjectId===subjectId&&x.classId===classId),t,s,c);
  });
  document.querySelectorAll('[data-delete]').forEach(b=>b.onclick=()=>removeAssignment(b.dataset.delete.split('|'),d));
}

function assignmentForm(teachers,subjects,classes){
  assignmentModal('Cakto mësimdhënës + lëndë + klasë',null,teachers,subjects,classes);
}

function assignmentEditForm(item,teachers,subjects,classes){
  assignmentModal('Rregullo caktimin',item,teachers,subjects,classes);
}

function assignmentModal(title,item,teachers,subjects,classes){
  const selected=(id,items)=>items.map(x=>`<option value="${esc(x.id)}" ${id===x.id?'selected':''}>${esc(x.fullName||x.name)}${x.username?' — '+esc(x.username):''}${x.gradeLevel?' — klasa '+esc(x.gradeLevel):''}</option>`).join('');
  const body=`<label>Mësimdhënësi<select name="teacherId" required>${selected(item?.teacherId,teachers)}</select></label><label>Lënda<select name="subjectId" required>${selected(item?.subjectId,subjects)}</select></label><label>Klasa / paralelja<select name="classId" required>${selected(item?.classId,classes)}</select></label>`;
  modal(title,body,async f=>{
    const payload={teacherId:f.get('teacherId'),subjectId:f.get('subjectId'),classId:f.get('classId')};
    if(!item) return api('/api/v1/management/teacher-subject-assignments',{method:'POST',body:JSON.stringify(payload)});
    return api('/api/v1/management/teacher-subject-assignments',{method:'PUT',body:JSON.stringify({oldTeacherId:item.teacherId,oldSubjectId:item.subjectId,oldClassId:item.classId,...payload})});
  });
}

async function removeAssignment(parts){
  const [teacherId,subjectId,classId]=parts;
  if(!confirm('A dëshironi ta hiqni këtë caktim? Ky veprim e heq lidhjen mësimdhënës + lëndë + klasë.')) return;
  try{
    await api(`/api/v1/management/teacher-subject-assignments?teacherId=${encodeURIComponent(teacherId)}&subjectId=${encodeURIComponent(subjectId)}&classId=${encodeURIComponent(classId)}`,{method:'DELETE'});
    await assignments();
  }catch(e){alert(e.message)}
}
