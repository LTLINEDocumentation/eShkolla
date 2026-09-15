function classLevelOptions(selected=''){return Array.from({length:13},(_,i)=>i+1).map(n=>`<option value="${n}" ${String(selected)===String(n)?'selected':''}>${n}</option>`).join('')}

async function classes(admin=false){
  const d=await api('/api/v1/management/classes');
  $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto klasë / paralele</button></div><div class="table-wrap"><table><thead><tr><th>ID</th><th>Klasa/paralelja</th><th>Klasa / niveli</th><th>Nxënës</th><th>Mësimdhënës</th><th>Statusi</th><th>Veprime</th></tr></thead><tbody>${d.length?d.map(x=>`<tr><td>${esc(x.id)}</td><td>${esc(x.name)}</td><td>${esc(x.gradeLevel)}</td><td>${esc(x.studentCount)}</td><td>${esc((x.teacherIds||[]).join(', ')||'—')}</td><td>${x.active?'Aktive':'Joaktive'}</td><td><button class="secondary" onclick="editClass(${JSON.stringify(x.id)},${JSON.stringify(x.name)},${Number(x.gradeLevel)})">Rregullo</button> <button class="secondary" onclick="deleteClass(${JSON.stringify(x.id)},${JSON.stringify(x.name)})">Fshij</button></td></tr>`).join(''):`<tr><td colspan="7" class="empty-state">Nuk ka të dhëna.</td></tr>`}</tbody></table></div>`;
  $('resultCount').textContent=`${d.length} rezultate reale`;
  $('dynamicAdd').onclick=()=>classForm();
}

async function classForm(){
  const schools=await api('/api/v1/management/schools');
  modal('Shto klasë / paralele',
    field('name','Klasa / paralelja, p.sh. 8-A','text','required')+
    `<label>Klasa / niveli<select name="gradeLevel" required>${classLevelOptions()}</select><small style="display:block;margin-top:4px">Zgjidh klasën sipas vitit shkollor; p.sh. 8 = klasa e 8-të. Paralelja vendoset te emri, p.sh. 8-A.</small></label>`+
    `<label>Shkolla<select name="schoolId"><option value="">Pa shkollë</option>${schools.map(x=>`<option value="${esc(x.id)}">${esc(x.name)}</option>`).join('')}</select></label>`,
    async f=>api('/api/v1/management/classes',{method:'POST',body:JSON.stringify({name:f.get('name'),gradeLevel:Number(f.get('gradeLevel')),schoolId:f.get('schoolId')||null})})
  );
}

function editClass(id,name,gradeLevel){
  modal('Rregullo klasën / paralelen',
    field('name','Klasa / paralelja','text',`required value="${esc(name)}"`)+
    `<label>Klasa / niveli<select name="gradeLevel" required>${classLevelOptions(gradeLevel)}</select><small style="display:block;margin-top:4px">Ky është viti i klasës, jo shkronja e paraleles.</small></label>`,
    async f=>api(`/api/v1/management/classes/${encodeURIComponent(id)}`,{method:'PUT',body:JSON.stringify({name:f.get('name'),gradeLevel:Number(f.get('gradeLevel'))})})
  );
}

async function deleteClass(id,name){
  if(!confirm(`A dëshironi ta fshini klasën/paralelen "${name}"?\n\nFshirja lejohet vetëm nëse nuk ka nxënës, mësimdhënës, caktime ose orar të lidhur me të.`)) return;
  try{
    await api(`/api/v1/management/classes/${encodeURIComponent(id)}`,{method:'DELETE'});
    await classes(true);
  }catch(e){alert(e.message)}
}
