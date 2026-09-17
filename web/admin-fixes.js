// Rregullime të përbashkëta për ADMINISTRATORIN.
// 1) Heq ciklin rekursiv të wrappers të role-modules për klasat.
// 2) Aktivizon Kërko / Filtro / Rendit për listat e ADMIN-it.
(function(){
  const roleClasses=window.classes;
  const originalOpenModule=window.openModule;

  async function adminClasses(){
    const d=await api('/api/v1/management/classes');
    $('moduleContent').innerHTML=`<div class="section-actions" style="margin-bottom:14px"><button class="primary" id="dynamicAdd">+ Shto klasë / paralele</button></div><div class="table-wrap"><table><thead><tr><th>ID</th><th>Klasa/paralelja</th><th>Niveli</th><th>Nxënës</th><th>Mësimdhënës</th><th>Statusi</th><th>Veprime</th></tr></thead><tbody>${d.length?d.map(x=>`<tr><td>${esc(x.id)}</td><td>${esc(x.name)}</td><td>${esc(x.gradeLevel)}</td><td>${esc(x.studentCount)}</td><td>${esc((x.teacherIds||[]).join(', ')||'—')}</td><td>${x.active?'Aktive':'Joaktive'}</td><td><div class="section-actions"><button type="button" class="secondary" data-class-edit="${esc(x.id)}">Rregullo</button><button type="button" class="secondary" data-class-delete="${esc(x.id)}" data-class-name="${esc(x.name)}">Fshij</button></div></td></tr>`).join(''):`<tr><td colspan="7" class="empty-state">Nuk ka të dhëna.</td></tr>`}</tbody></table></div>`;
    $('resultCount').textContent=`${d.length} rezultate reale`;
    $('dynamicAdd').onclick=()=>classForm();
    document.querySelectorAll('[data-class-edit]').forEach(b=>b.onclick=()=>editClass(b.dataset.classEdit,d.find(x=>String(x.id)===String(b.dataset.classEdit))?.name||'',d.find(x=>String(x.id)===String(b.dataset.classEdit))?.gradeLevel||''));
    document.querySelectorAll('[data-class-delete]').forEach(b=>b.onclick=()=>deleteClass(b.dataset.classDelete,b.dataset.className));
  }

  window.classes=async function(admin=false){
    if(currentUser?.role==='ADMINISTRATOR' || admin)return adminClasses();
    return roleClasses(admin);
  };

  function bindAdminFilters(){
    const controls=$('moduleControls'), search=$('tableSearch'), filter=$('tableFilter'), sort=$('tableSort');
    if(!controls||controls.classList.contains('hidden')||!search||!filter||!sort)return;
    const tableEl=$('moduleContent')?.querySelector('table');
    if(!tableEl)return;
    const headers=[...tableEl.querySelectorAll('thead th')].map(x=>x.textContent.trim());
    const rows=[...tableEl.querySelectorAll('tbody tr')].filter(r=>!r.querySelector('.empty-state'));
    const oldSearch=search.dataset.adminFilterBound;
    if(oldSearch==='1'){
      applyFilters();
      return;
    }
    search.dataset.adminFilterBound='1';
    search.oninput=applyFilters;
    filter.onchange=applyFilters;
    sort.onchange=applyFilters;
    $('clearFilters').onclick=()=>{search.value='';filter.value='';sort.value='';applyFilters();};

    filter.innerHTML='<option value="">Të gjitha</option>';
    const statusIndex=headers.findIndex(h=>h.toLowerCase()==='statusi');
    const levelIndex=headers.findIndex(h=>h.toLowerCase()==='niveli');
    const filterIndex=statusIndex>=0?statusIndex:levelIndex;
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
      const si=sort.value;
      if(si!==''){
        const idx=Number(si);
        visible.sort((a,b)=>a.children[idx]?.textContent.trim().localeCompare(b.children[idx]?.textContent.trim(),'sq',{numeric:true,sensitivity:'base'})||0);
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
