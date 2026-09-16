async function timetableCodes(){
  const canManage=['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role);
  if(!canManage){$('moduleContent').innerHTML='<p class="error">Nuk keni të drejtë për këtë modul.</p>';return}
  const [codes,teachers,subjects,classes]=await Promise.all([api('/api/v1/management/timetable-codes'),api('/api/v1/management/teachers'),api('/api/v1/management/subjects'),api('/api/v1/management/classes')]);
  const rows=codes.map(x=>[`<td><strong>${esc(x.code)}</strong></td>`,`<td>${esc(x.displayName||'—')}</td>`,`<td>${esc(x.subjectName||'—')}</td>`,`<td>${esc(x.classNames?.join(', ')||'—')}</td>`,`<td>${x.active?'Aktiv':'Joaktiv'}</td>`,`<td><div class="section-actions"><button type="button" class="secondary code-edit" data-code="${x.code}">Rregullo</button><button type="button" class="secondary code-delete" data-code="${x.code}" data-name="${esc(x.displayName||`Kodi ${x.code}`)}">Hiq</button></div></td>`]);
  $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button type="button" class="primary" id="codeAdd">+ Shto / cakto kod</button></div><div class="table-wrap"><table><thead><tr><th>Kodi</th><th>Mësimdhënësi</th><th>Lënda</th><th>Klasa / paralele</th><th>Statusi</th><th>Veprime</th></tr></thead><tbody>${rows.length?rows.map(r=>`<tr>${r.join('')}</tr>`).join(''):`<tr><td colspan="6" class="empty-state">Nuk ka kode të regjistruara.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${codes.length} kode të orarit`;
  $('codeAdd').onclick=()=>timetableCodeForm(null,codes,teachers,subjects,classes);
  document.querySelectorAll('.code-edit').forEach(b=>b.onclick=()=>timetableCodeForm(codes.find(x=>x.code===Number(b.dataset.code)),codes,teachers,subjects,classes));
  document.querySelectorAll('.code-delete').forEach(b=>b.onclick=async()=>{if(!confirm(`A jeni i sigurt që doni ta hiqni lidhjen e ${b.dataset.name}?\n\nKodi ${b.dataset.code} do të mbetet i rezervuar, por mësimdhënësi, lënda dhe klasat do të hiqen nga ky kod.`))return;try{await api('/api/v1/management/timetable-codes/'+encodeURIComponent(b.dataset.code),{method:'DELETE'});await timetableCodes()}catch(e){alert(e.message)}});
}
function timetableCodeForm(item,codes,teachers,subjects,classes){
  const selectedCode=item?.code??codes.find(x=>!x.displayName)?.code??1;
  const codeOptions=codes.map(x=>`<option value="${x.code}" ${x.code===selectedCode?'selected':''}>${x.code}${x.displayName?' — '+esc(x.displayName):' — i lirë'}</option>`).join('');
  const teacherOptions=`<option value="">Pa mësimdhënës / rezervë</option>`+teachers.map(x=>`<option value="${esc(x.id)}" ${x.id===item?.teacherId?'selected':''}>${esc(x.fullName)}${x.username?' — '+esc(x.username):''}</option>`).join('');
  const subjectOptions=`<option value="">Pa lëndë</option>`+subjects.map(x=>`<option value="${esc(x.id)}" ${x.id===item?.subjectId?'selected':''}>${esc(x.name)}</option>`).join('');
  const classOptions=classes.map(x=>`<label style="display:flex;gap:8px;align-items:center"><input type="checkbox" name="classIds" value="${esc(x.id)}" ${(item?.classIds||[]).includes(x.id)?'checked':''}>${esc(x.name)} — ${esc(x.gradeLevel)}</label>`).join('');
  const body=`<label>Kodi 1–17<select name="code" required>${codeOptions}</select></label>`+field('displayName','Emri i shfaqur','text',`value="${esc(item?.displayName||'')}" placeholder="p.sh. Xhavit Tahiri"`)+`<label>Mësimdhënësi<select name="teacherId">${teacherOptions}</select></label><label>Lënda<select name="subjectId">${subjectOptions}</select></label><div><strong>Klasa / paralele</strong><div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:8px;margin-top:8px">${classOptions||'<span class="muted">Nuk ka klasa të regjistruara.</span>'}</div></div>`;
  modal(item?`Rregullo kodin ${item.code}`:'Shto / cakto kodin',body,async f=>{
    const classIds=[...f.getAll('classIds')].map(String);
    const payload={code:Number(f.get('code')),displayName:f.get('displayName')||null,teacherId:f.get('teacherId')||null,subjectId:f.get('subjectId')||null,classIds,active:true};
    return api(item?`/api/v1/management/timetable-codes/${encodeURIComponent(item.code)}`:'/api/v1/management/timetable-codes',{method:item?'PUT':'POST',body:JSON.stringify(payload)});
  });
}

const _eshkollaBaseModules=window.baseModules;
window.baseModules=function(){const modules=_eshkollaBaseModules();if(['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role)&&!modules.some(x=>x[0]==='timetableCodes'))modules.splice(Math.min(6,modules.length),0,['timetableCodes','Kodet e orarit','Mësimdhënës + lëndë + klasa/paralele']);return modules};
const _eshkollaOpenModule=window.openModule;
window.openModule=async function(m){if(m!=='timetableCodes')return _eshkollaOpenModule(m);currentModule=m;$('modules').classList.add('hidden');$('moduleView').classList.remove('hidden');$('moduleTitle').textContent='Kodet e orarit';$('moduleControls').classList.add('hidden');$('addStudentButton').classList.add('hidden');try{await timetableCodes()}catch(e){$('moduleContent').innerHTML=`<p class="error">${esc(e.message)}</p>`}};
const _eshkollaIcons=window.icons||{};_eshkollaIcons.timetableCodes='🔢';
