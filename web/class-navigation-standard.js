// Navigimi standard i klasave/paraleleve për të gjitha modulet që punojnë me klasë.
// Ky skedar ngarkohet i fundit dhe unifikon vetëm hyrjen në klasë; logjika e të dhënave mbetet ajo e moduleve ekzistuese.
(function(){
  const CLASS_MODULES=new Set(['students','grades','absences']);

  function activeClasses(data){
    return (data||[]).filter(c=>c.active!==false)
      .sort((a,b)=>Number(a.gradeLevel)-Number(b.gradeLevel)||String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));
  }

  function standardClassPicker(classes,onSelect){
    $('moduleControls')?.classList.add('hidden');
    const list=activeClasses(classes);
    $('moduleContent').innerHTML=`
      <div class="class-nav-standard">
        <div class="class-nav-standard-head">
          <div>
            <strong>Zgjidh klasën</strong>
            <small>Zgjidh klasën/paralelen për të vazhduar.</small>
          </div>
        </div>
        <div class="class-nav-standard-grid">
          ${list.length?list.map(c=>`
            <button type="button" class="class-nav-standard-card" data-class-id="${esc(c.id)}">
              <strong>${esc(c.name)}</strong>
              <span>Klasa ${esc(c.gradeLevel)}</span>
            </button>`).join(''):'<div class="empty-state">Nuk ka klasa aktive.</div>'}
        </div>
      </div>`;
    $('resultCount').textContent=`${list.length} klasa/paralele`;
    const grid=document.querySelector('.class-nav-standard-grid');
    if(grid){
      grid.onclick=async e=>{
        const btn=e.target.closest('.class-nav-standard-card');
        if(!btn||!grid.contains(btn))return;
        e.preventDefault();
        e.stopPropagation();
        const classId=String(btn.dataset.classId||'');
        if(!classId||btn.disabled)return;
        btn.disabled=true;
        try{await onSelect(classId);}catch(err){console.error(err);alert(err?.message||'Nuk u hap klasa.');}
        finally{btn.disabled=false;}
      };
    }
  }

  function injectStyles(){
    if(document.getElementById('classNavigationStandardStyles'))return;
    const s=document.createElement('style');
    s.id='classNavigationStandardStyles';
    s.textContent=`
      .class-nav-standard{display:grid;gap:16px}
      .class-nav-standard-head strong{display:block;font-size:18px}
      .class-nav-standard-head small{display:block;margin-top:4px;color:#667085}
      .class-nav-standard-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}
      .class-nav-standard-card{min-height:92px;padding:14px;text-align:left;border:1px solid #dbe3ef;border-radius:12px;background:#fff;cursor:pointer;display:flex;flex-direction:column;justify-content:center;gap:6px}
      .class-nav-standard-card:hover{border-color:#1f6feb;box-shadow:0 3px 12px rgba(16,24,40,.08)}
      .class-nav-standard-card strong{font-size:18px}
      .class-nav-standard-card span{font-size:13px;color:#667085}
      .class-nav-standard-back{display:inline-flex!important;align-items:center;gap:6px}
      @media(max-width:900px){.class-nav-standard-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
      @media(max-width:650px){.class-nav-standard-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
    `;
    document.head.appendChild(s);
  }

  function resetSelection(){
    if(currentUser?.role==='ADMINISTRATOR'){
      window.adminStudentsClassId='';
      window.adminGradesClassId='';
      window.adminAbsencesClassId='';
    }else if(currentUser?.role==='MESIMDHENES'){
      window.teacherStudentsClassId='';
      window.webGradesClassId='';
      window.webGradesSubjectId='';
      window.teacherAbsencesClassId='';
    }
  }

  function setupAdmin(){
    const oldStudents=window.students;
    const oldGrades=window.grades;
    const oldAbsences=window.absences;

    window.students=async function standardAdminStudents(admin=false){
      if(currentUser?.role!=='ADMINISTRATOR')return oldStudents(admin);
      const selected=String(window.adminStudentsClassId||'');
      const classes=await api('/api/v1/management/classes');
      injectStyles();
      if(!selected){
        return standardClassPicker(classes,id=>{
          window.adminStudentsClassId=id;
          return window.students(true);
        });
      }
      return oldStudents(true);
    };

    window.grades=async function standardAdminGrades(){
      if(currentUser?.role!=='ADMINISTRATOR')return oldGrades();
      const selected=String(window.adminGradesClassId||'');
      const [classes,allGrades,allStudents]=await Promise.all([
        api('/api/v1/management/classes'),
        selected?api('/api/v1/management/grades'):Promise.resolve([]),
        selected?api('/api/v1/management/students'):Promise.resolve([])
      ]);
      injectStyles();
      if(!selected){
        return standardClassPicker(classes,id=>{
          window.adminGradesClassId=id;
          return window.grades();
        });
      }
      const cls=(classes||[]).find(x=>String(x.id)===selected&&x.active!==false);
      if(!cls){
        window.adminGradesClassId='';
        return window.grades();
      }
      const studentIds=new Set((allStudents||[]).filter(s=>String(s.classId)===selected).map(s=>String(s.id)));
      const rows=(allGrades||[]).filter(g=>studentIds.has(String(g.studentId))).map(g=>[
        `<td>${esc(g.studentName||g.studentId||'—')}</td>`,
        `<td>${esc(g.subjectName||g.subjectId||'—')}</td>`,
        `<td>${esc(g.teacherName||'—')}</td>`,
        `<td>${esc(g.value??'—')}</td>`,
        `<td>${esc(g.period||'—')}</td>`,
        `<td>${esc(g.academicYear||'—')}</td>`,
        `<td>${esc(g.note||'—')}</td>`,
        `<td><button type="button" class="secondary admin-grade-edit" data-grade="${esc(g.id)}">Rregullo notën</button></td>`
      ]);
      $('moduleContent').innerHTML=`
        <div class="section-actions" style="margin-bottom:14px">
          <button type="button" class="secondary class-nav-standard-back" id="adminGradesBack">← Klasat</button>
          <strong>${esc(cls.name)}</strong>
        </div>
        <div class="table-wrap"><table><thead><tr>
          <th>Nxënësi</th><th>Lënda</th><th>Mësimdhënësi</th><th>Nota</th><th>Periudha</th><th>Viti shkollor</th><th>Vlerësimi</th><th>Veprime</th>
        </tr></thead><tbody>
          ${rows.length?rows.map(row=>`<tr>${row.join('')}</tr>`).join(''):'<tr><td colspan="8" class="empty-state">Nuk ka nota të regjistruara për këtë klasë.</td></tr>'}
        </tbody></table></div>`;
      $('resultCount').textContent=`${rows.length} nota · ${esc(cls.name)}`;
      $('adminGradesBack').onclick=()=>{window.adminGradesClassId='';window.grades();};
      document.querySelectorAll('.admin-grade-edit').forEach(b=>b.onclick=()=>window.adminEdit?.('grade',b.dataset.grade));
    };

    window.absences=async function standardAdminAbsences(){
      if(currentUser?.role!=='ADMINISTRATOR')return oldAbsences();
      const selected=String(window.adminAbsencesClassId||'');
      const classes=await api('/api/v1/management/classes');
      injectStyles();
      if(!selected){
        return standardClassPicker(classes,id=>{
          window.adminAbsencesClassId=id;
          return window.absences();
        });
      }
      return oldAbsences();
    };
  }

  function setupTeacher(){
    const oldStudents=window.students;
    const oldGrades=window.grades;
    const oldAbsences=window.absences;

    window.students=async function standardTeacherStudents(admin=false){
      if(currentUser?.role!=='MESIMDHENES')return oldStudents(admin);
      const selected=String(window.teacherStudentsClassId||'');
      const d=await loadTeacherRoleData(true);
      injectStyles();
      if(!selected){
        return standardClassPicker(d.classes,id=>{
          window.teacherStudentsClassId=id;
          return window.students();
        });
      }
      return oldStudents(admin);
    };

    window.grades=async function standardTeacherGrades(){
      if(currentUser?.role!=='MESIMDHENES')return oldGrades();
      const selected=String(window.webGradesClassId||'');
      const d=await loadTeacherRoleData(true);
      injectStyles();
      if(!selected){
        return standardClassPicker(d.classes,id=>{
          window.webGradesClassId=id;
          window.webGradesSubjectId='';
          return window.grades();
        });
      }
      return oldGrades();
    };

    window.absences=async function standardTeacherAbsences(){
      if(currentUser?.role!=='MESIMDHENES')return oldAbsences();
      const selected=String(window.teacherAbsencesClassId||'');
      const d=await loadTeacherRoleData(true);
      injectStyles();
      if(!selected){
        return standardClassPicker(d.classes,id=>{
          window.teacherAbsencesClassId=id;
          return window.absences();
        });
      }
      return oldAbsences();
    };
  }

  // Inicializohet pas autentikimit, jo gjatë ngarkimit të skriptit kur currentUser është ende null.
  let initializedRole='';
  window.initializeClassNavigationStandard=function(){
    if(!currentUser?.role)return;
    injectStyles();
    if(initializedRole===currentUser.role)return;
    if(currentUser.role==='ADMINISTRATOR')setupAdmin();
    else if(currentUser.role==='MESIMDHENES')setupTeacher();
    initializedRole=currentUser.role;
  };
  window.standardClassNavigationReset=resetSelection;
})();
