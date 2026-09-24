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

    const [d,studentsData,subjectsData,teachersData,classes]=await Promise.all([
      api('/api/v1/management/absences'),
      api('/api/v1/management/students'),
      api('/api/v1/management/subjects'),
      api('/api/v1/management/teachers'),
      api('/api/v1/management/classes')
    ]);

    const selected=String(window.adminAbsencesClassId||'');
    const activeClasses=(classes||[]).filter(x=>x.active!==false)
      .sort((a,b)=>Number(a.gradeLevel)-Number(b.gradeLevel)||String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));

    // PA KLASË: shfaq kategorinë e klasave direkt, pa u varur nga adminClassCards.
    if(!selected){
      $('moduleContent').innerHTML=`
        <div class="admin-class-module">
          <div class="admin-class-module-head">
            <div><strong>Zgjidh klasën</strong><small>Zgjidh klasën për të parë dhe menaxhuar mungesat.</small></div>
          </div>
          <div class="admin-class-grid">
            ${activeClasses.length?activeClasses.map(x=>`
              <button type="button" class="admin-class-card admin-absence-class-card" data-class-id="${esc(x.id)}">
                <strong>${esc(x.name)}</strong><span>Klasa ${esc(x.gradeLevel)}</span>
              </button>`).join(''):'<div class="empty-state">Nuk ka klasa aktive.</div>'}
          </div>
        </div>`;
      $('resultCount').textContent=`${activeClasses.length} klasa`;
      if(!document.getElementById('adminClassModuleStyles')){
        const st=document.createElement('style');
        st.id='adminClassModuleStyles';
        st.textContent=`
          .admin-class-module{display:grid;gap:16px}
          .admin-class-module-head strong{display:block;font-size:18px}
          .admin-class-module-head small{display:block;margin-top:4px;color:#667085}
          .admin-class-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}
          .admin-class-card{min-height:92px;padding:14px;text-align:left;border:1px solid #dbe3ef;border-radius:12px;background:#fff;cursor:pointer;display:flex;flex-direction:column;justify-content:center;gap:6px}
          .admin-class-card:hover,.admin-class-card.selected{border-color:#1f6feb;box-shadow:0 3px 12px rgba(16,24,40,.08)}
          .admin-class-card strong{font-size:18px}
          .admin-class-card span{font-size:13px;color:#667085}
          @media(max-width:900px){.admin-class-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}
          @media(max-width:650px){.admin-class-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
        `;
        document.head.appendChild(st);
      }
      document.querySelectorAll('.admin-absence-class-card').forEach(b=>{
        b.onclick=()=>{window.adminAbsencesClassId=String(b.dataset.classId);window.absences();};
      });
      return;
    }

    const cls=activeClasses.find(x=>String(x.id)===selected);
    if(!cls){
      window.adminAbsencesClassId='';
      return window.absences();
    }

    const classStudents=(studentsData||[]).filter(s=>String(s.classId)===selected&&s.active!==false);
    const rows=classStudents.map(s=>{
      const as=(d||[]).filter(a=>String(a.studentId)===String(s.id));
      return [
        `<td><strong>${esc(s.fullName)}</strong></td>`,
        `<td>${as.length?as.map(a=>`${esc(subjectsData.find(x=>String(x.id)===String(a.subjectId))?.name||a.subjectId)}: ${esc(a.date)} — ${esc(a.status)}`).join('<br>'):'—'}</td>`,
        `<td>${as.length?as.map(a=>esc(a.note||'—')).join('<br>'):'—'}</td>`,
        `<td><button type="button" class="secondary admin-add-absence" data-student="${esc(s.id)}">+ Shto mungesë</button></td>`
      ];
    });

    $('moduleContent').innerHTML=`
      <div class="section-actions" style="margin-bottom:14px">
        <button type="button" class="secondary" id="adminAbsencesBack">← Klasat</button>
        <strong style="margin-left:8px">${esc(cls.name)}</strong>
      </div>
      <div class="table-wrap"><table><thead><tr>
        <th>Emri dhe mbiemri</th><th>Mungesat</th><th>Shënimi</th><th>Veprime</th>
      </tr></thead><tbody>
        ${rows.length?rows.map(r=>`<tr>${r.join('')}</tr>`).join(''):'<tr><td colspan="4" class="empty-state">Nuk ka nxënës në këtë klasë.</td></tr>'}
      </tbody></table></div>`;

    $('resultCount').textContent=`${rows.length} nxënës · ${esc(cls.name)}`;
    $('adminAbsencesBack').onclick=()=>{
      window.adminAbsencesClassId='';
      window.absences();
    };
    function adminAbsenceForm(student){
      const studentSubjects=(subjectsData||[]).filter(x=>x.active!==false);
      const studentTeachers=(teachersData||[]).filter(x=>x.active!==false);
      const today=new Date().toISOString().slice(0,10);
      const subjectOptions=studentSubjects.map(x=>'<option value="'+esc(x.id)+'">'+esc(x.name)+(x.code?' ('+esc(x.code)+')':'')+'</option>').join('');
      const teacherOptions=studentTeachers.map(x=>'<option value="'+esc(x.id)+'">'+esc(x.fullName)+'</option>').join('');
      if(!student)throw new Error('Nxënësi nuk u gjet.');
      if(!subjectOptions)throw new Error('Nuk ka lëndë aktive.');
      if(!teacherOptions)throw new Error('Nuk ka mësimdhënës aktivë.');
      const body=
        '<label>Nxënësi<input name="studentName" value="'+esc(student.fullName)+'" disabled></label>'+
        '<input type="hidden" name="studentId" value="'+esc(student.id)+'">'+
        '<label>Lënda<select name="subjectId" required><option value="">Zgjidh lëndën</option>'+subjectOptions+'</select></label>'+
        '<label>Mësimdhënësi<select name="teacherId" required><option value="">Zgjidh mësimdhënësin</option>'+teacherOptions+'</select></label>'+
        '<label>Data<input name="date" type="date" value="'+today+'" required></label>'+
        '<label>Statusi<select name="status" required><option value="E_ARSYESHME">E arsyeshme</option><option value="E_PAAFTESUAR">E paarsyeshme</option></select></label>'+
        '<label>Shënimi<textarea name="note" rows="3" placeholder="Shënim (opsional)"></textarea></label>';
      modal('Shto mungesë',body,async form=>{
        const payload={
          studentId:String(form.get('studentId')||''),
          subjectId:String(form.get('subjectId')||''),
          teacherId:String(form.get('teacherId')||''),
          date:String(form.get('date')||''),
          status:String(form.get('status')||''),
          note:String(form.get('note')||'').trim()||null
        };
        if(!payload.subjectId||!payload.teacherId||!payload.date||!payload.status)throw new Error('Plotëso fushat e detyrueshme.');
        await api('/api/v1/management/absences',{method:'POST',body:JSON.stringify(payload)});
        window.adminAbsencesClassId=selected;
        await window.absences();
      });
    }
    document.querySelectorAll('.admin-add-absence').forEach(b=>{
      b.onclick=()=>{
        try{
          const st=studentsData.find(s=>String(s.id)===String(b.dataset.student));
          adminAbsenceForm(st);
        }catch(e){
          console.error('ADMIN MUNGESAT - Shto mungesë:',e);
          alert('Nuk u hap formulari i mungesës: '+e.message);
        }
      };
    });
  };

  // ADMIN: Nxënësit ndjek të njëjtin navigim standard si Mungesat: Klasat/paralelet -> nxënësit -> Klasat.
  const _adminStudentsBase=window.students;
  window.students=async function adminStudentsClassFirst(admin=false){
    if(currentUser?.role!=='ADMINISTRATOR')return _adminStudentsBase(admin);
    const [studentsData,classes]=await Promise.all([
      api('/api/v1/management/students'),
      api('/api/v1/management/classes')
    ]);
    const selected=String(window.adminStudentsClassId||'');
    const activeClasses=(classes||[]).filter(x=>x.active!==false)
      .sort((a,b)=>Number(a.gradeLevel)-Number(b.gradeLevel)||String(a.name).localeCompare(String(b.name),'sq',{numeric:true}));
    if(!selected){
      $('moduleContent').innerHTML=`
        <div class="admin-class-module">
          <div class="admin-class-module-head">
            <div><strong>Zgjidh klasën / paralelen</strong><small>Zgjidh klasën dhe paralelen për të parë dhe menaxhuar nxënësit.</small></div>
          </div>
          <div class="admin-class-grid">
            ${activeClasses.length?activeClasses.map(x=>`
              <button type="button" class="admin-class-card admin-student-class-card" data-class-id="${esc(x.id)}">
                <strong>${esc(x.name)}</strong><span>Klasa ${esc(x.gradeLevel)}</span>
              </button>`).join(''):'<div class="empty-state">Nuk ka klasa/paralele aktive.</div>'}
          </div>
        </div>`;
      $('resultCount').textContent=`${activeClasses.length} klasa/paralele`;
      document.querySelectorAll('.admin-student-class-card').forEach(b=>{
        b.onclick=()=>{window.adminStudentsClassId=String(b.dataset.classId);window.students(true);};
      });
      return;
    }
    const cls=activeClasses.find(x=>String(x.id)===selected);
    if(!cls){
      window.adminStudentsClassId='';
      return window.students(true);
    }
    const classStudents=(studentsData||[]).filter(s=>String(s.classId)===selected&&s.active!==false);
    const rows=classStudents.map(x=>[
      `<td>${esc(x.id)}</td>`,
      `<td><strong>${esc(x.fullName)}</strong></td>`,
      `<td>${esc(x.birthDate||'—')}</td>`,
      `<td>${x.active?'Aktiv':'Joaktiv'}</td>`,
      `<td>${adminStandardButtons('student',x.id,x.fullName)}</td>`
    ]);
    $('moduleContent').innerHTML=`
      <div class="section-actions" style="margin-bottom:14px">
        <button type="button" class="secondary" id="adminStudentsBack">← Klasat</button>
        <strong style="margin-left:8px">${esc(cls.name)}</strong>
        <button type="button" class="primary" id="dynamicAdd">+ Shto nxënës</button>
      </div>
      <div class="table-wrap"><table><thead><tr>
        <th>ID</th><th>Emri dhe mbiemri</th><th>Datëlindja</th><th>Statusi</th><th>Veprime</th>
      </tr></thead><tbody>
        ${rows.length?rows.map(r=>`<tr>${r.join('')}</tr>`).join(''):'<tr><td colspan="5" class="empty-state">Nuk ka nxënës në këtë klasë/paralele.</td></tr>'}
      </tbody></table></div>`;
    $('resultCount').textContent=`${rows.length} nxënës · ${esc(cls.name)}`;
    $('adminStudentsBack').onclick=()=>{window.adminStudentsClassId='';window.students(true);};
    $('dynamicAdd').onclick=()=>studentForm(null);
    bindAdminStandardActions();
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