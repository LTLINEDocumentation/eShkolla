const _timetableBase=window.timetable;
async function timetable(){
  await _timetableBase();
  const data=await api('/api/v1/timetable');
  const canManage=['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role);
  const [classes,teachers,subjects]=canManage?await Promise.all([api('/api/v1/management/classes'),api('/api/v1/management/teachers'),api('/api/v1/management/subjects')]):[[],[],[]];
  const className=id=>classes.find(x=>x.id===id)?.name||id;
  const teacherName=id=>teachers.find(x=>x.id===id)?.fullName||id;
  const subjectName=id=>subjects.find(x=>x.id===id)?.name||id;
  const rows=(data.schedule||[]).map(x=>`<tr><td>${esc(weekdayNames[x.weekday]||x.weekday)}</td><td>${esc(x.startTime)} – ${esc(x.endTime)}</td><td>${esc(className(x.classId))}</td><td>${esc(subjectName(x.subjectId))}</td><td>${esc(teacherName(x.teacherId))}</td><td>${esc(x.room||'—')}</td>${canManage?`<td><div class="section-actions"><button type="button" class="secondary timetable-edit" data-id="${esc(x.id)}">Rregullo</button><button type="button" class="secondary timetable-delete" data-id="${esc(x.id)}">Fshij</button></div></td>`:''}</tr>`).join('');
  const section=document.createElement('div');section.style.marginTop='18px';section.innerHTML=`<div class="section-title"><div><p class="eyebrow">Orari javor</p><h3>Oraret e klasave</h3></div>${canManage?'<button class="primary" id="timetableAdd">+ Shto orë</button>':''}</div><div class="table-wrap"><table><thead><tr><th>Dita</th><th>Koha</th><th>Klasa</th><th>Lënda</th><th>Mësimdhënësi</th><th>Salla</th>${canManage?'<th>Veprime</th>':''}</tr></thead><tbody>${rows||`<tr><td colspan="${canManage?7:6}" class="empty-state">Nuk ka orë të regjistruara.</td></tr>`}</tbody></table></div>`;
  $('moduleContent').appendChild(section);
  if(canManage){$('timetableAdd').onclick=()=>scheduleEntryForm();document.querySelectorAll('.timetable-edit').forEach(b=>b.onclick=()=>{const x=data.schedule.find(v=>v.id===b.dataset.id);if(x)scheduleEntryForm(x)});document.querySelectorAll('.timetable-delete').forEach(b=>b.onclick=()=>deleteScheduleEntry(b.dataset.id));}
}
function scheduleEntryForm(existing=null){
  Promise.all([api('/api/v1/management/classes'),api('/api/v1/management/teachers'),api('/api/v1/management/subjects')]).then(([classes,teachers,subjects])=>{
    const opt=(items,value,label)=>items.map(x=>`<option value="${esc(x.id)}" ${String(value)===String(x.id)?'selected':''}>${esc(label(x))}</option>`).join('');
    const body=`<label>Dita<select name="weekday" required>${Object.entries(weekdayNames).slice(0,5).map(([n,l])=>`<option value="${n}" ${String(existing?.weekday||1)===n?'selected':''}>${l}</option>`).join('')}</select></label><label>Klasa / paralelja<select name="classId" required>${opt(classes,existing?.classId,x=>x.name)}</select></label><label>Lënda<select name="subjectId" required>${opt(subjects,existing?.subjectId,x=>x.name)}</select></label><label>Mësimdhënësi<select name="teacherId" required>${opt(teachers,existing?.teacherId,x=>x.fullName)}</select></label>`+field('startTime','Fillimi','time',`required value="${esc(existing?.startTime||'')}"`)+field('endTime','Mbarimi','time',`required value="${esc(existing?.endTime||'')}"`)+field('room','Salla','text',`value="${esc(existing?.room||'')}" placeholder="p.sh. A-12"`);
    modal(existing?'Rregullo orën':'Shto orë në orar',body,async f=>api(existing?`/api/v1/schedule/${encodeURIComponent(existing.id)}`:'/api/v1/schedule',{method:existing?'PUT':'POST',body:JSON.stringify({classId:f.get('classId'),teacherId:f.get('teacherId'),subjectId:f.get('subjectId'),weekday:Number(f.get('weekday')),startTime:f.get('startTime'),endTime:f.get('endTime'),room:f.get('room')||null,active:true})}));
  }).catch(e=>alert(e.message));
}
async function deleteScheduleEntry(id){if(!confirm('A jeni i sigurt që doni ta fshini këtë orë nga orari?\n\nKy veprim nuk mund të zhbëhet.'))return;try{await api('/api/v1/schedule/'+encodeURIComponent(id),{method:'DELETE'});await timetable()}catch(e){alert(e.message)}}
