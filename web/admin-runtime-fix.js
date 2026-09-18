// Rregullim runtime për modulet ADMIN: shmang mbështjellëset globale që mund të kapin veten dhe shkaktojnë "Maximum call stack size exceeded".
(function(){
  const originalAdminSubjects=window.subjects;
  window.subjects=async function adminSubjectsSafe(){
    if(currentUser?.role!=='ADMINISTRATOR'){
      return originalAdminSubjects();
    }
    const d=await api('/api/v1/management/subjects');
    adminTable(
      ['ID','Lënda mësimore','Kodi','Statusi','Veprime'],
      (d||[]).map(x=>[
        `<td>${esc(x.id)}</td>`,
        `<td>${esc(x.name)}</td>`,
        `<td>${esc(x.code||'')}</td>`,
        `<td>${x.active?'Aktive':'Joaktive'}</td>`,
        `<td>${adminActionButtons('subject',x.id,x.name)}</td>`
      ]),
      'Shto lëndë',
      ()=>subjectForm()
    );
    if(typeof window.bindAdminFilters==='function')window.bindAdminFilters();
  };

  const originalAdminGrades=window.grades;
  window.grades=async function adminGradesSafe(){
    if(currentUser?.role!=='ADMINISTRATOR') return originalAdminGrades();
    await originalAdminGrades();
    const selected=window.adminGradesClassId||'';
    if(!selected) return;
    const host=$('moduleContent');
    if(!host) return;
    if($('adminGradesBackSafe')) return;
    const toolbar=host.querySelector('.section-actions');
    if(!toolbar) return;
    const back=document.createElement('button');
    back.type='button';
    back.className='secondary';
    back.id='adminGradesBackSafe';
    back.textContent='← Klasat';
    back.style.marginRight='8px';
    back.onclick=async()=>{
      window.adminGradesClassId='';
      await originalAdminGrades();
    };
    toolbar.insertBefore(back,toolbar.firstChild);
  };
})();
