// Të dhënat e plota të Administratorit: pa filtrin e mësimdhënësit.
(function(){
  window.grades=async function(){
    const d=await api('/api/v1/management/grades');
    const rows=(d||[]).map(x=>[x.id,x.studentName,x.subjectName,x.teacherName,x.value,x.period,x.academicYear,x.note||'']);
    table(['ID','Nxënësi','Lënda','Mësimdhënësi','Nota','Periudha','Viti','Shënim'],rows);
  };
  window.absences=async function(){
    const d=await api('/api/v1/management/absences');
    const rows=(d||[]).map(x=>[x.id,x.studentName,x.subjectName,x.teacherName,x.date,x.status,x.note||'']);
    table(['ID','Nxënësi','Lënda','Mësimdhënësi','Data','Statusi','Shënim'],rows);
  };
})();
