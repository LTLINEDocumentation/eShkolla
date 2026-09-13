// Rrjedhat kryesore të mësimdhënësit: fillimisht klasa, pastaj nxënësi, pastaj veprimi.
(function () {
  const baseModule = module;
  let selectedClass = "";
  let activeFlow = "grades";

  const teacherClasses = [...new Set((teacherStudents || []).map(r => r[1]))];

  function escFlow(v) {
    return String(v).replace(/[&<>\"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;"}[c]));
  }

  function studentsForClass(className) {
    return (teacherStudents || []).filter(s => s[1] === className);
  }

  function gradeCount(studentName, className) {
    return (data.teacherGrades || []).filter(r => r[0] === studentName && r[1] === className).length;
  }

  function absenceCount(studentName, className) {
    return (data.absences || []).filter(r => r[0] === studentName && r[1] === className).length;
  }

  function renderFlow() {
    const content = document.getElementById("moduleContent");
    const controls = document.getElementById("moduleControls");
    const actions = document.getElementById("teacherModuleActions");
    if (!content) return;
    controls?.classList.add("hidden");
    actions?.remove();

    const isGrades = activeFlow === "grades";
    const students = selectedClass ? studentsForClass(selectedClass) : [];
    const title = isGrades ? "Vlerësimi i nxënësve" : "Evidenca e mungesave";
    const actionLabel = isGrades ? "Vlerëso nxënësin" : "Regjistro mungesë";
    const countFn = isGrades ? gradeCount : absenceCount;

    content.innerHTML = `
      <div class="grade-flow">
        <div class="grade-step ${selectedClass ? "done" : "active"}">
          <div class="grade-step-number">1</div>
          <div class="grade-step-body">
            <strong>Zgjidh klasën</strong>
            <small>Klasa caktohet para se të shfaqen nxënësit.</small>
            <select id="flowClassSelect" class="grade-class-select">
              <option value="">-- Zgjidh klasën --</option>
              ${teacherClasses.map(c => `<option value="${escFlow(c)}" ${c === selectedClass ? "selected" : ""}>${escFlow(c)}</option>`).join("")}
            </select>
          </div>
        </div>

        ${selectedClass ? `
          <div class="grade-step active">
            <div class="grade-step-number">2</div>
            <div class="grade-step-body">
              <strong>${title}: ${escFlow(selectedClass)}</strong>
              <small>Zgjidh nxënësin për të vazhduar.</small>
              <div class="grade-student-list">
                ${students.map(s => `
                  <div class="grade-student-row">
                    <div><strong>${escFlow(s[0])}</strong><small>${escFlow(s[1])} · ${countFn(s[0], s[1])} ${isGrades ? "vlerësim(e)" : "mungesë(a)"}</small></div>
                    <button class="primary-button flow-student-action" data-student="${escFlow(s[0])}" data-class="${escFlow(s[1])}">${actionLabel}</button>
                  </div>`).join("") || `<div class="empty-state">Nuk ka nxënës në këtë klasë.</div>`}
              </div>
            </div>
          </div>
        ` : `
          <div class="grade-empty-hint">Pas zgjedhjes së klasës do të shfaqen vetëm nxënësit e asaj klase.</div>
        `}
      </div>`;

    document.getElementById("flowClassSelect")?.addEventListener("change", e => {
      selectedClass = e.target.value;
      renderFlow();
    });

    document.querySelectorAll(".flow-student-action").forEach(btn => {
      btn.onclick = () => isGrades
        ? openGradeForm(btn.dataset.student, btn.dataset.class)
        : openAbsenceForm(btn.dataset.student, btn.dataset.class);
    });
  }

  function openGradeForm(studentName, className) {
    teacherModal("Regjistro vlerësim", [
      field("Nxënësi", "student", `<input name="student" value="${escFlow(studentName)}" readonly>`),
      field("Klasa", "class", `<input name="class" value="${escFlow(className)}" readonly>`),
      field("Lënda", "subject", `<input name="subject" value="Matematikë" readonly>`),
      field("Lloji i vlerësimit", "type", selectHtml("type", ["Test", "Detyrë shtëpie", "Aktivitet në klasë", "Pjesëmarrje", "Projekt"])),
      field("Nota", "grade", selectHtml("grade", ["1", "2", "3", "4", "5"])),
      field("Periudha", "period", selectHtml("period", ["Periudha I", "Periudha II", "Periudha III"])),
      field("Shënim", "note", `<textarea name="note" rows="3" placeholder="Shënim opsional"></textarea>`)
    ].join(""), fd => {
      data.teacherGrades.unshift([
        fd.get("student"), fd.get("class"), "Matematikë", fd.get("type"),
        fd.get("grade"), fd.get("period"), fd.get("note") || ""
      ]);
      defs.grades[2] = data.teacherGrades;
      document.getElementById("teacherModal")?.remove();
      renderFlow();
    });
  }

  function openAbsenceForm(studentName, className) {
    teacherModal("Regjistro mungesë", [
      field("Nxënësi", "student", `<input name="student" value="${escFlow(studentName)}" readonly>`),
      field("Klasa", "class", `<input name="class" value="${escFlow(className)}" readonly>`),
      field("Data", "date", `<input name="date" type="date" value="2026-09-13" required>`),
      field("Lënda", "subject", `<input name="subject" value="Matematikë" readonly>`),
      field("Statusi", "status", selectHtml("status", ["E pajustifikuar", "E arsyetuar"])),
      field("Arsyeja / shënimi", "note", `<textarea name="note" rows="3" placeholder="Shkruaj arsyen e mungesës"></textarea>`)
    ].join(""), fd => {
      const date = String(fd.get("date")).split("-").reverse().join(".");
      data.absences.unshift([
        fd.get("student"), fd.get("class"), date, "Matematikë", fd.get("status"), fd.get("note") || ""
      ]);
      defs.absences[2] = data.absences;
      document.getElementById("teacherModal")?.remove();
      renderFlow();
    });
  }

  module = function (k) {
    if ((k !== "grades" && k !== "absences") || !sessionStorage.getItem("eshkollaUser")) {
      baseModule(k);
      return;
    }
    baseModule(k);
    activeFlow = k;
    selectedClass = "";
    renderFlow();
  };

  const style = document.createElement("style");
  style.textContent = `
    .grade-flow{display:grid;gap:16px}
    .grade-step{border:1px solid #dbe3ef;border-radius:14px;padding:18px;background:#fff;display:flex;gap:14px}
    .grade-step.active{border-color:#9db9e8;box-shadow:0 3px 14px rgba(31,111,235,.08)}
    .grade-step.done{border-color:#b7dec8}
    .grade-step-number{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;background:#eef4ff;color:#1f6feb;font-weight:800;flex:0 0 34px}
    .grade-step-body{flex:1}.grade-step-body>strong{display:block;font-size:17px;margin-bottom:4px}.grade-step-body>small{display:block;margin-bottom:14px}
    .grade-class-select{max-width:420px;margin-top:0}
    .grade-student-list{display:grid;gap:8px}
    .grade-student-row{display:flex;justify-content:space-between;align-items:center;gap:14px;padding:13px 14px;border:1px solid #e4e7ec;border-radius:10px;background:#f8fafc}
    .grade-student-row strong{display:block}.grade-student-row small{display:block;margin-top:4px}
    .grade-student-row .primary-button{width:auto;white-space:nowrap}
    .grade-empty-hint{padding:18px;border:1px dashed #cbd5e1;border-radius:12px;color:#667085;background:#f8fafc}
    @media(max-width:650px){.grade-student-row{align-items:stretch;flex-direction:column}.grade-student-row .primary-button{width:100%}.grade-step{padding:14px}}
  `;
  document.head.appendChild(style);
})();
