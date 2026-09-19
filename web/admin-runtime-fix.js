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


  // Detyro navigimin standard sipas klasës edhe kur një modul tjetër e ka mbështjellë funksionin global.
  const _gradesFinal=window.grades;
  window.grades=async function finalGrades(){
    if(currentUser?.role!=='ADMINISTRATOR')return _gradesFinal();
    const classes=await api('/api/v1/management/classes');
    const selected=window.adminGradesClassId||'';
    if(!selected){
      await adminClassCards('grade',classes,'',id=>{window.adminGradesClassId=String(id);grades();});
      return;
    }
    return _gradesFinal();
  };
  const _absencesFinal=window.absences;
  window.absences=async function finalAbsences(){
    if(currentUser?.role!=='ADMINISTRATOR')return _absencesFinal();
    const classes=await api('/api/v1/management/classes');
    const selected=window.adminAbsencesClassId||'';
    if(!selected){
      await adminClassCards('absence',classes,'',id=>{window.adminAbsencesClassId=String(id);absences();});
      return;
    }
    return _absencesFinal();
  };

  const _teacherStudentsFinal=window.students;
  window.students=async function finalStudents(admin=false){
    if(currentUser?.role!=='MESIMDHENES')return _teacherStudentsFinal(admin);
    const d=await loadTeacherRoleData(true);
    const classes=(d.classes||[]).filter(c=>c.active!==false).sort((a,b)=>Number(a.gradeLevel)-Number(b.gradeLevel)||String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));
    const selected=window.teacherStudentsClassId||'';
    if(!selected){
      $('moduleContent').innerHTML=`<div class="web-grade-panel"><div class="web-grade-toolbar"><div><strong>Zgjidh klasën</strong><small>Vetëm klasat që i janë caktuar këtij mësimdhënësi.</small></div></div><div class="web-grade-class-grid">${classes.length?classes.map(c=>`<button type="button" class="web-grade-class-card teacher-student-final-class" data-class="${esc(c.id)}"><strong>${esc(c.name)}</strong><span>${Number(c.studentCount||0)} nxënës</span></button>`).join(''):`<div class="empty-state">Nuk ka klasa të caktuara.</div>`}</div></div>`;
      $('resultCount').textContent=`${classes.length} klasa`;
      document.querySelectorAll('.teacher-student-final-class').forEach(b=>b.onclick=()=>{window.teacherStudentsClassId=b.dataset.class;students();});
      injectWebGradeStyles();
      return;
    }
    return _teacherStudentsFinal(admin);
  };
  const _teacherAbsencesFinal=window.absences;
  window.absences=async function finalAbsencesAll(){
    if(currentUser?.role!=='MESIMDHENES')return _teacherAbsencesFinal();
    const d=await loadTeacherRoleData(true);
    const classes=(d.classes||[]).filter(c=>c.active!==false).sort((a,b)=>Number(a.gradeLevel)-Number(b.gradeLevel)||String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));
    const selected=window.teacherAbsencesClassId||'';
    if(!selected){
      $('moduleContent').innerHTML=`<div class="web-grade-panel"><div class="web-grade-toolbar"><div><strong>Zgjidh klasën</strong><small>Vetëm klasat që i janë caktuar këtij mësimdhënësi.</small></div></div><div class="web-grade-class-grid">${classes.length?classes.map(c=>`<button type="button" class="web-grade-class-card teacher-absence-final-class" data-class="${esc(c.id)}"><strong>${esc(c.name)}</strong><span>${Number(c.studentCount||0)} nxënës</span></button>`).join(''):`<div class="empty-state">Nuk ka klasa të caktuara.</div>`}</div></div>`;
      $('resultCount').textContent=`${classes.length} klasa`;
      document.querySelectorAll('.teacher-absence-final-class').forEach(b=>b.onclick=()=>{window.teacherAbsencesClassId=b.dataset.class;absences();});
      injectWebGradeStyles();
      return;
    }
    return _teacherAbsencesFinal();
  };

})();