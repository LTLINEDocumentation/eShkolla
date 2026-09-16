function timetableCodeLabel(code){return `${code.code} — ${code.displayName||'Pa mësimdhënës'}${code.subjectName?` — ${code.subjectName}`:''}`}

async function renderTimetableMatrix(data,classes){
  const codes=await api('/api/v1/management/timetable-codes');
  const periods=data.periods||[]; const schedule=data.schedule||[];
  const codeByTeacher=new Map(codes.filter(x=>x.teacherId).map(x=>[String(x.teacherId),x.code]));
  const orderedNames=['VI1','VI2','VII1','VII2','VIII1','VIII2','IX1','IX2'];
  const orderedClasses=orderedNames.map(name=>classes.find(x=>String(x.name).trim().toUpperCase()===name)).filter(Boolean);
  const fallbackClasses=classes.filter(x=>!orderedClasses.some(y=>y.id===x.id)).sort((a,b)=>String(a.name).localeCompare(String(b.name)));
  const visibleClasses=[...orderedClasses,...fallbackClasses];
  const bySlot=new Map(schedule.map(x=>[`${x.classId}|${x.weekday}|${x.startTime}|${x.endTime}`,x]));
  const dayGroups=Array.from({length:5},(_,i)=>i+1).map(day=>{
    const cols=periods.map(p=>`<th>${p.lessonNumber}</th>`).join('');
    const body=visibleClasses.map(c=>`<tr><th>${esc(c.name)}</th>${periods.map(p=>{
      const x=bySlot.get(`${c.id}|${day}|${p.startTime}|${p.endTime}`); const code=x?codeByTeacher.get(String(x.teacherId)):null;
      const label=x?`${code??'—'}${x.subjectId?'':' — pa lëndë'}`:'·';
      return `<td class="timetable-cell" data-class="${esc(c.id)}" data-day="${day}" data-lesson="${p.lessonNumber}" title="${x?esc('Ndrysho orën'):esc('Cakto mësimdhënësin')}" style="cursor:pointer">${label}</td>`;
    }).join('')}</tr>`).join('');
    return `<div class="table-wrap" style="margin-top:12px"><table class="timetable-matrix"><thead><tr><th>${esc(weekdayNames[day])}</th>${cols}</tr></thead><tbody>${body||`<tr><td colspan="${periods.length+1}" class="empty-state">Nuk ka klasa të regjistruara.</td></tr>`}</tbody></table></div>`;
  }).join('');
  const legend=codes.filter(x=>x.displayName||x.teacherId).sort((a,b)=>a.code-b.code).map(x=>`<span style="display:inline-flex;gap:5px;align-items:center;margin:3px 10px 3px 0"><strong>${x.code}</strong> — ${esc(x.displayName||x.teacherId||'')}${x.subjectName?` — ${esc(x.subjectName)}`:''}</span>`).join('');
  const wrapper=document.createElement('div'); wrapper.style.marginTop='22px';
  wrapper.innerHTML=`<div class="section-title"><div><p class="eyebrow">Orari operativ</p><h3>Klasa → dita → ora → numri i mësimdhënësit</h3><p class="muted">Kliko një qelizë për të caktuar ose ndryshuar mësimdhënësin. Lënda merret automatikisht nga kodi i mësimdhënësit.</p></div></div>${dayGroups}<div class="card" style="margin-top:12px;padding:14px"><p class="eyebrow">Legjenda e kodeve</p><div>${legend||'<span class="muted">Nuk ka kode të caktuara.</span>'}</div></div>`;
  $('moduleContent').appendChild(wrapper);
  wrapper.querySelectorAll('.timetable-cell').forEach(cell=>cell.onclick=()=>{
    const existing=schedule.find(x=>x.classId===cell.dataset.class&&x.weekday===Number(cell.dataset.day)&&x.startTime===periods.find(p=>p.lessonNumber===Number(cell.dataset.lesson))?.startTime);
    scheduleCodeEntryForm({classId:cell.dataset.class,weekday:Number(cell.dataset.day),lessonNumber:Number(cell.dataset.lesson),existing,codes,classes});
  });
}

async function scheduleEntryForm(existing=null){
  const [classes,codes,data]=await Promise.all([api('/api/v1/management/classes'),api('/api/v1/management/timetable-codes'),api('/api/v1/timetable')]);
  const periods=data.periods||[];
  scheduleCodeEntryForm({classId:existing?.classId||classes[0]?.id||'',weekday:Number(existing?.weekday||1),lessonNumber:existing?Math.max(1,periods.find(p=>p.startTime===existing.startTime)?.lessonNumber||1):1,existing,codes,classes});
}

function scheduleCodeEntryForm({classId,weekday,lessonNumber,existing,codes,classes}){
  const classOptions=classes.map(x=>`<option value="${esc(x.id)}" ${String(x.id)===String(classId)?'selected':''}>${esc(x.name)}</option>`).join('');
  const dayOptions=Object.entries(weekdayNames).slice(0,5).map(([n,l])=>`<option value="${n}" ${Number(n)===weekday?'selected':''}>${l}</option>`).join('');
  const lessonOptions=Array.from({length:6},(_,i)=>i+1).map(n=>`<option value="${n}" ${n===lessonNumber?'selected':''}>Ora ${n}</option>`).join('');
  const codeOptions=codes.filter(x=>x.active!==false).map(x=>`<option value="${x.code}">${esc(timetableCodeLabel(x))}</option>`).join('');
  const existingCode=existing?codes.find(x=>String(x.teacherId)===String(existing.teacherId))?.code:'';
  const body=`<p class="muted">Zgjedhja bëhet sipas <strong>klasës → ditës → orës → kodit të mësimdhënësit</strong>. Lënda plotësohet automatikisht.</p><label>Klasa / paralelja<select name="classId" required>${classOptions}</select></label><label>Dita<select name="weekday" required>${dayOptions}</select></label><label>Ora<select name="lessonNumber" required>${lessonOptions}</select></label><label>Mësimdhënësi / kodi<select name="code" required><option value="">Zgjidh kodin...</option>${codeOptions}</select></label><label>Salla<input name="room" value="${esc(existing?.room||'')}" placeholder="p.sh. A-12"></label>`;
  modal(existing?'Ndrysho orën':'Cakto orë në orar',body,async f=>{
    if(existing){await api('/api/v1/schedule/'+encodeURIComponent(existing.id),{method:'DELETE'});}
    await api('/api/v1/management/timetable-assignment',{method:'POST',body:JSON.stringify({classId:f.get('classId'),weekday:Number(f.get('weekday')),lessonNumber:Number(f.get('lessonNumber')),code:Number(f.get('code')),room:f.get('room')||null,active:true})});
  });
  const modalRoot=document.querySelector('.modal');
  if(modalRoot){const select=modalRoot.querySelector('[name="code"]'); if(select&&existingCode) select.value=String(existingCode);}
}
