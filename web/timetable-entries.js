async function timetableEntriesView(timetableData=null){
  const data=timetableData||await api('/api/v1/timetable');
  const canManage=['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role);
  const [classes,teachers,subjects]=canManage?await Promise.all([api('/api/v1/management/classes'),api('/api/v1/management/teachers'),api('/api/v1/management/subjects')]):[[],[],[]];
  const className=id=>classes.find(x=>x.id===id)?.name||data.schedule.find(x=>String(x.classId)===String(id))?.className||id;
  const teacherName=id=>teachers.find(x=>x.id===id)?.fullName||data.schedule.find(x=>String(x.teacherId)===String(id))?.teacherName||id;
  const subjectName=id=>subjects.find(x=>x.id===id)?.name||data.schedule.find(x=>String(x.subjectId)===String(id))?.subjectName||id;
  const rows=(data.schedule||[]).map(x=>`<tr><td>${esc(weekdayNames[x.weekday]||x.weekday)}</td><td>${esc(x.startTime)} – ${esc(x.endTime)}</td><td>${esc(className(x.classId))}</td><td>${esc(subjectName(x.subjectId))}</td><td>${esc(teacherName(x.teacherId))}</td><td>${esc(x.room||'—')}</td>${canManage?`<td><div class="section-actions"><button type="button" class="secondary timetable-edit" data-id="${esc(x.id)}">Rregullo</button><button type="button" class="secondary timetable-delete" data-id="${esc(x.id)}">Fshij</button></div></td>`:''}</tr>`).join('');
  const section=document.createElement('div');section.style.marginTop='18px';section.innerHTML=`<div class="section-title"><div><p class="eyebrow">Orari javor</p><h3>Oraret e klasave</h3></div>${canManage?'<button class="primary" id="timetableAdd">+ Shto orë</button>':''}</div><div class="table-wrap"><table><thead><tr><th>Dita</th><th>Koha</th><th>Klasa</th><th>Lënda</th><th>Mësimdhënësi</th><th>Salla</th>${canManage?'<th>Veprime</th>':''}</tr></thead><tbody>${rows||`<tr><td colspan="${canManage?7:6}" class="empty-state">Nuk ka orë të regjistruara.</td></tr>`}</tbody></table></div>`;
  $('moduleContent').appendChild(section);
  if(canManage){
    await renderTimetableMatrix(data,classes);
    $('timetableAdd').onclick=()=>scheduleEntryForm();
    document.querySelectorAll('.timetable-edit').forEach(b=>b.onclick=()=>{const x=data.schedule.find(v=>v.id===b.dataset.id);if(x)scheduleEntryForm(x)});
    document.querySelectorAll('.timetable-delete').forEach(b=>b.onclick=()=>deleteScheduleEntry(b.dataset.id));
  }
}

async function deleteScheduleEntry(id){if(!confirm('A jeni i sigurt që doni ta fshini këtë orë nga orari?\n\nKy veprim nuk mund të zhbëhet.'))return;try{await api('/api/v1/schedule/'+encodeURIComponent(id),{method:'DELETE'});await timetableView()}catch(e){alert(e.message)}}

async function timetableView(){const data=await api('/api/v1/timetable');if(!['ADMINISTRATOR','DREJTOR'].includes(currentUser?.role))await timetableSettingsView(data);await timetableEntriesView(data)}
