// Rregullimi i rrjedhës ADMIN: përdoruesi krijohet sipas rolit;
// mësimdhënësi lidhet me lëndë/klasë/paralele vetëm te Caktimet.
(function(){
  window.userForm=function userForm(){
    let students=[];
    modal('Krijo përdorues dhe cakto rol',
      field('fullName','Emri dhe mbiemri','text','required')+
      field('username','Përdoruesi','text','required')+
      field('password','Fjalëkalimi','password','minlength="10" required')+
      `<label>Roli<select name="role" id="newUserRole" required>
        <option value="ADMINISTRATOR">Administrator</option>
        <option value="DREJTOR">Drejtor</option>
        <option value="MESIMDHENES">Mësimdhënës</option>
        <option value="NXENES">Nxënës</option>
        <option value="PRIND">Prind</option>
      </select></label>
      <div id="newUserStudentWrap" class="hidden"></div>`,
      async f=>{
        const role=f.get('role');
        const studentId=f.get('studentId');
        if(role==='PRIND'&&!studentId) throw Error('Për rolin Prind duhet të zgjidhet nxënësi i lidhur.');
        const payload={username:f.get('username'),fullName:f.get('fullName'),password:f.get('password'),role};
        if(role==='PRIND')payload.studentId=studentId;
        return api('/api/v1/management/users',{method:'POST',body:JSON.stringify(payload)});
      }
    );
    const roleSelect=document.getElementById('newUserRole');
    const wrap=document.getElementById('newUserStudentWrap');
    const loadStudents=async()=>{
      if(students.length)return students;
      students=await api('/api/v1/management/students');
      return students;
    };
    const updateStudentField=async()=>{
      if(!roleSelect||!wrap)return;
      if(roleSelect.value!=='PRIND'){
        wrap.innerHTML='';
        wrap.classList.add('hidden');
        return;
      }
      try{
        const data=await loadStudents();
        wrap.innerHTML=`<label>Nxënësi i lidhur<select name="studentId" required><option value="">Zgjidh nxënësin...</option>${data.filter(x=>x.active!==false).map(x=>`<option value="${esc(x.id)}">${esc(x.fullName)} — ${esc(x.className||x.classId||'')}</option>`).join('')}</select></label>`;
        wrap.classList.remove('hidden');
      }catch(e){wrap.innerHTML=`<p class="error">${esc(e.message)}</p>`;wrap.classList.remove('hidden');}
    };
    roleSelect?.addEventListener('change',updateStudentField);
    updateStudentField();
  };

  window.assignmentForm=function assignmentForm(teachers,subjects,classes){
    const grouped=[...new Map(classes.map(x=>[String(x.gradeLevel),x])).values()].sort((a,b)=>a.gradeLevel-b.gradeLevel);
    const body=`
      <label>Mësimdhënësi<select name="teacherId" required><option value="">Zgjidh mësimdhënësin...</option>${teachers.filter(x=>x.active!==false).map(x=>`<option value="${esc(x.id)}">${esc(x.fullName)} — ${esc(x.username)}</option>`).join('')}</select></label>
      <label>Lënda<select name="subjectId" required><option value="">Zgjidh lëndën...</option>${subjects.filter(x=>x.active!==false).map(x=>`<option value="${esc(x.id)}">${esc(x.name)}${x.code?' — '+esc(x.code):''}</option>`).join('')}</select></label>
      <label>Klasa<select name="gradeLevel" id="assignmentGrade" required><option value="">Zgjidh klasën...</option>${grouped.map(x=>`<option value="${esc(x.gradeLevel)}">Klasa ${esc(x.gradeLevel)}</option>`).join('')}</select></label>
      <label>Paralelja<select name="classId" id="assignmentParallel" required disabled><option value="">Zgjidh fillimisht klasën...</option></select></label>`;
    modal('Cakto: Mësimdhënësi → Lënda → Klasa → Paralelja',body,async f=>{
      return api('/api/v1/management/teacher-subject-assignments',{method:'POST',body:JSON.stringify({teacherId:f.get('teacherId'),subjectId:f.get('subjectId'),classId:f.get('classId')})});
    });
    const grade=document.getElementById('assignmentGrade');
    const parallel=document.getElementById('assignmentParallel');
    const refresh=()=>{
      const level=grade?.value;
      const matches=classes.filter(x=>String(x.gradeLevel)===String(level));
      parallel.innerHTML=level?`<option value="">Zgjidh paralelen...</option>${matches.map(x=>`<option value="${esc(x.id)}">${esc(x.name)}</option>`).join('')}`:'<option value="">Zgjidh fillimisht klasën...</option>';
      parallel.disabled=!level;
    };
    grade?.addEventListener('change',refresh);
  };
})();
