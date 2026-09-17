// Rregullime të përbashkëta për ADMINISTRATORIN.
// - Mbron klasat nga rekursioni i role-modules.
// - Aktivizon Kërko / Filtro / Rendit në listat administrative.
(function(){
  const roleClasses=window.classes;
  const originalOpenModule=window.openModule;

  async function adminClasses(){
    const d=await api('/api/v1/management/classes');
    $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto klasë / paralele</button></div><div class="table-wrap"><table><thead><tr><th>ID</th><th>Klasa/paralelja</th><th>Niveli</th><th>Nxënës</th><th>Mësimdhënës</th><th>Statusi</th><th>Veprime</th></tr></thead><tbody>${d.length?d.map(x=>`<tr><td>${esc(x.id)}</td><td>${esc(x.name)}</td><td>${esc(x.gradeLevel)}</td><td>${esc(x.studentCount)}</td><td>${esc((x.teacherIds||[]).join(', ')||'—')}</td><td>${x.active?'Aktive':'Joaktive'}</td><td><div class="section-actions"><button type="button" class="secondary" data-class-edit="${esc(x.id)}">Rregullo</button><button type="button" class="secondary" data-class-delete="${esc(x.id)}" data-class-name="${esc(x.name)}">Fshij</button></div></td></tr>`).join(''):`<tr><td colspan="7" class="empty-state">Nuk ka të dhëna.</td></tr>`}</tbody></table></div>`;
    $('resultCount').textContent=`${d.length} rezultate reale`;
    $('dynamicAdd').onclick=()=>classForm();
    document.querySelectorAll('[data-class-edit]').forEach(b=>b.onclick=()=>{const x=d.find(v=>String(v.id)===String(b.dataset.classEdit));if(x)editClass(x.id,x.name,x.gradeLevel);});
    document.querySelectorAll('[data-class-delete]').forEach(b=>b.onclick=()=>deleteClass(b.dataset.classDelete,b.dataset.className));
  }

  window.classes=async function(admin=false){
    if(currentUser?.role==='ADMINISTRATOR' || admin)return adminClasses();
    return roleClasses(admin);
  };

  function bindAdminFilters(){
    const controls=$('moduleControls'), search=$('tableSearch'), filter=$('tableFilter'), sort=$('tableSort'), clear=$('clearFilters');
    if(!controls||!search||!filter||!sort||!clear)return;
    if(currentUser?.role!=='ADMINISTRATOR')return;
    if(currentModule==='profile')return;
    controls.classList.remove('hidden');
    const tableEl=$('moduleContent')?.querySelector('table');
    if(!tableEl)return;
    const headers=[...tableEl.querySelectorAll('thead th')].map(x=>x.textContent.trim());
    const rows=[...tableEl.querySelectorAll('tbody tr')].filter(r=>!r.querySelector('.empty-state'));
    const statusIndex=headers.findIndex(h=>h.toLocaleLowerCase('sq')==='statusi');
    const levelIndex=headers.findIndex(h=>h.toLocaleLowerCase('sq')==='niveli');
    const filterIndex=statusIndex>=0?statusIndex:levelIndex;

    search.oninput=applyFilters;
    filter.onchange=applyFilters;
    sort.onchange=applyFilters;
    clear.onclick=()=>{search.value='';filter.value='';sort.value='';applyFilters();};

    filter.innerHTML='<option value="">Të gjitha</option>';
    if(filterIndex>=0){
      const vals=[...new Set(rows.map(r=>r.children[filterIndex]?.textContent.trim()).filter(Boolean))].sort((a,b)=>a.localeCompare(b,'sq'));
      vals.forEach(v=>{const o=document.createElement('option');o.value=v;o.textContent=statusIndex>=0?v:`Niveli: ${v}`;filter.appendChild(o);});
    }

    sort.innerHTML='<option value="">Rendit sipas...</option>';
    headers.forEach((h,i)=>{
      if(!h||h==='Veprime')return;
      const o=document.createElement('option');o.value=String(i);o.textContent=h+' ↑';sort.appendChild(o);
    });

    function applyFilters(){
      const q=search.value.trim().toLocaleLowerCase('sq');
      const fv=filter.value;
      const visible=[];
      rows.forEach(r=>{
        const text=r.textContent.toLocaleLowerCase('sq');
        const filterMatch=!fv||r.children[filterIndex]?.textContent.trim()===fv;
        const searchMatch=!q||text.includes(q);
        const show=filterMatch&&searchMatch;
        r.style.display=show?'':'none';
        if(show)visible.push(r);
      });
      if(sort.value!==''){
        const idx=Number(sort.value);
        visible.sort((a,b)=>(a.children[idx]?.textContent.trim()||'').localeCompare(b.children[idx]?.textContent.trim()||'','sq',{numeric:true,sensitivity:'base'}));
        const tbody=tableEl.querySelector('tbody');
        visible.forEach(r=>tbody.appendChild(r));
      }
      $('resultCount').textContent=`${visible.length} rezultate reale`;
    }
    applyFilters();
  }

  window.openModule=async function(m){
    await originalOpenModule(m);
    bindAdminFilters();
  };
})();
