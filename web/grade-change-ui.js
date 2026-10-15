(function(){
 const escText=v=>String(v??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
 const originalGrades=window.grades;
 const currentYear=()=>{const n=new Date();const y=n.getMonth()>=8?n.getFullYear():n.getFullYear()-1;return y+'/'+(y+1)};
 const optionList=(items,selected,label)=>items.map(x=>{const v=label(x);return '<option value="'+escText(x.id)+'" '+(String(x.id)===String(selected)?'selected':'')+'>'+escText(v)+'</option>'}).join('');
 async function directorGrades(){
  const [grades,students,subjects,teachers,requests]=await Promise.all([
   api('/api/v1/management/grades'),api('/api/v1/management/students'),api('/api/v1/management/subjects'),api('/api/v1/management/teachers'),api('/api/v1/grade-change-requests')
  ]);
  const pending=requests.filter(r=>r.status==='PENDING');
  $('moduleContent').innerHTML='<div class="grade-change-panel"><div class="section-actions"><strong>Propozimet për ndryshim note</strong><button type="button" class="primary" id="directorNewGrade">+ Propozo notë të re</button></div>'+
   '<div class="table-wrap"><table><thead><tr><th>Nxënësi</th><th>Lënda</th><th>Nota aktuale</th><th>Nota e propozuar</th><th>Statusi</th><th>Veprim</th></tr></thead><tbody>'+
   (requests.length?requests.map(r=>'<tr><td>'+escText(r.studentName)+'</td><td>'+escText(r.subjectName)+'</td><td>'+(r.currentValue??'—')+'</td><td><strong>'+r.proposedValue+'</strong></td><td><span class="grade-change-status '+escText(r.status.toLowerCase())+'">'+escText(r.status)+'</span></td><td>'+escText(r.reviewNote||'')+'</td></tr>').join(''):'<tr><td colspan="6" class="empty-state">Nuk ka propozime të regjistruara.</td></tr>')+
   '</tbody></table></div><h4>Notat ekzistuese</h4><div class="table-wrap"><table><thead><tr><th>Nxënësi</th><th>Lënda</th><th>Mësimdhënësi</th><th>Nota</th><th>Periudha / viti</th><th>Veprimi</th></tr></thead><tbody>'+
   (grades.length?grades.map(g=>'<tr><td>'+escText(g.studentName)+'</td><td>'+escText(g.subjectName)+'</td><td>'+escText(g.teacherName)+'</td><td>'+g.value+'</td><td>'+escText(g.period)+' · '+escText(g.academicYear)+'</td><td><button type="button" class="secondary director-propose-edit" data-grade="'+escText(g.id)+'">Propozo ndryshim</button></td></tr>').join(''):'<tr><td colspan="6" class="empty-state">Nuk ka nota të regjistruara.</td></tr>')+
   '</tbody></table></div></div>';
  $('resultCount').textContent=pending.length+' propozime në pritje · '+grades.length+' nota';
  $('directorNewGrade').onclick=()=>openDirectorProposal(null,students,subjects,teachers);
  document.querySelectorAll('.director-propose-edit').forEach(b=>b.onclick=()=>{const g=grades.find(x=>String(x.id)===String(b.dataset.grade));if(g)openDirectorProposal(g,students,subjects,teachers)});
 }
 function openDirectorProposal(item,students,subjects,teachers){
  const wrap=document.createElement('div');wrap.className='modal';
  const selectedStudent=item?.studentId||students[0]?.id||'';
  const selectedSubject=item?.subjectId||subjects[0]?.id||'';
  const selectedTeacher=item?.teacherId||teachers[0]?.id||'';
  const studentField=item?'<input type="hidden" name="studentId" value="'+escText(item.studentId)+'"><p><strong>Nxënësi:</strong> '+escText(item.studentName)+'</p>':'<label>Nxënësi<select name="studentId" required>'+optionList(students,selectedStudent,x=>x.fullName+' — '+(x.className||x.classId))+'</select></label>';
  const subjectField=item?'<input type="hidden" name="subjectId" value="'+escText(item.subjectId)+'"><p><strong>Lënda:</strong> '+escText(item.subjectName)+'</p>':'<label>Lënda<select name="subjectId" required>'+optionList(subjects,selectedSubject,x=>x.name)+'</select></label>';
  const teacherField=item?'<input type="hidden" name="teacherId" value="'+escText(item.teacherId)+'"><p><strong>Mësimdhënësi:</strong> '+escText(item.teacherName)+'</p>':'<label>Mësimdhënësi përgjegjës<select name="teacherId" required>'+optionList(teachers,selectedTeacher,x=>x.fullName)+'</select></label>';
  wrap.innerHTML='<div class="modal-card"><div class="section-title"><div><h3>'+(item?'Propozo ndryshimin e notës':'Propozo notë të re')+'</h3><p class="muted">Ndryshimi mbetet PENDING deri në vendimin e mësimdhënësit të lëndës.</p></div><button type="button" class="secondary" data-close>Mbyll</button></div><form id="directorGradeProposalForm">'+
   (item?'<input type="hidden" name="gradeId" value="'+escText(item.id)+'">':'')+studentField+subjectField+teacherField+
   '<label>Nota e propozuar<select name="value" required>'+[1,2,3,4,5].map(n=>'<option value="'+n+'" '+(Number(item?.value)===n?'selected':'')+'>'+n+'</option>').join('')+'</select></label>'+
   '<label>Periudha<input name="period" required value="'+escText(item?.period||'Periudha I')+'"></label><label>Viti shkollor<input name="academicYear" required value="'+escText(item?.academicYear||currentYear())+'"></label><label>Arsyetimi / shënimi<textarea name="note" rows="3">'+escText(item?.note||'')+'</textarea></label><p class="error" id="directorProposalError"></p><div class="form-actions"><button type="button" class="secondary" data-close>Anulo</button><button type="submit" class="primary">Dërgo për aprovim</button></div></form></div>';
  document.body.appendChild(wrap);const close=()=>wrap.remove();wrap.querySelectorAll('[data-close]').forEach(b=>b.onclick=close);
  wrap.querySelector('form').onsubmit=async e=>{e.preventDefault();const f=new FormData(e.currentTarget);const payload={gradeId:item?.id||null,studentId:f.get('studentId'),subjectId:f.get('subjectId'),teacherId:f.get('teacherId')||null,value:Number(f.get('value')),period:f.get('period'),academicYear:f.get('academicYear'),note:f.get('note')||null};const btn=e.currentTarget.querySelector('[type=submit]');btn.disabled=true;try{await api('/api/v1/grade-change-requests',{method:'POST',body:JSON.stringify(payload)});close();await directorGrades()}catch(err){wrap.querySelector('#directorProposalError').textContent=err.message;btn.disabled=false}};
 }
 async function addTeacherRequests(){
  const requests=await api('/api/v1/grade-change-requests');
  if(!requests.length)return;
  const box=document.createElement('section');box.className='grade-change-panel grade-change-review';
  box.innerHTML='<div><strong>Kërkesa për ndryshim note nga Drejtori</strong><p class="muted">Nota aktuale nuk ndryshon pa aprovimin tuaj.</p></div><div class="table-wrap"><table><thead><tr><th>Nxënësi</th><th>Lënda</th><th>Aktuale</th><th>E propozuar</th><th>Arsyetimi</th><th>Vendimi</th></tr></thead><tbody>'+
   requests.map(r=>'<tr><td>'+escText(r.studentName)+'</td><td>'+escText(r.subjectName)+'</td><td>'+(r.currentValue??'—')+'</td><td><strong>'+r.proposedValue+'</strong></td><td>'+escText(r.note||'—')+'</td><td><div class="section-actions"><button type="button" class="primary grade-change-approve" data-id="'+escText(r.id)+'">Aprovo</button><button type="button" class="secondary grade-change-reject" data-id="'+escText(r.id)+'">Refuzo</button></div></td></tr>').join('')+
   '</tbody></table></div>';
  $('moduleContent').prepend(box);
  box.querySelectorAll('.grade-change-approve,.grade-change-reject').forEach(b=>b.onclick=async()=>{const approve=b.classList.contains('grade-change-approve');const note=approve?'':(prompt('Shënoni arsyen e refuzimit (opsionale):')||'');b.disabled=true;try{await api('/api/v1/grade-change-requests/'+encodeURIComponent(b.dataset.id)+'/decision',{method:'POST',body:JSON.stringify({decision:approve?'APPROVE':'REJECT',note})});await window.grades()}catch(e){alert(e.message);b.disabled=false}});
 }
 window.grades=async function gradeChangeRoleRouter(){
  if(currentUser?.role==='DREJTOR')return directorGrades();
  await originalGrades();
  if(currentUser?.role==='MESIMDHENES')await addTeacherRequests();
 };
 const css=document.createElement('style');css.textContent='.grade-change-panel{display:grid;gap:14px;margin-bottom:20px}.grade-change-panel h4{margin:8px 0 0}.grade-change-status{display:inline-block;padding:4px 8px;border-radius:6px;background:#f2f4f7;font-weight:700;font-size:12px}.grade-change-status.pending{background:#fff4ce;color:#854d0e}.grade-change-status.approved{background:#dcfae6;color:#067647}.grade-change-status.rejected{background:#fee4e2;color:#b42318}.grade-change-review{border:1px solid #f0c36d;border-radius:12px;padding:14px;background:#fffdf5}.admin-grade-entry{display:flex;align-items:center;justify-content:space-between;gap:8px;margin:4px 0}.admin-grade-entry button{padding:4px 8px;font-size:12px}';document.head.appendChild(css);
})();