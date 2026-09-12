// Demo profili i Leonard Tahiraj — Mësimdhënës i Matematikës.
// Shtresa është e ndarë nga app.js që profili mund të zëvendësohet lehtë kur API real të jetë aktiv.

demoUsers.mesimdhenes.fullName = "Leonard Tahiraj";
data.users[2] = ["3", "Leonard Tahiraj", "leonard.tahiraj", "Mësimdhënës", "Aktiv"];
data.teachers[0] = ["M001", "Leonard Tahiraj", "Matematikë", "T-1001", "Aktiv"];

data.teacherGrades = [
  ["Ardit Krasniqi", "VIII/1", "Matematikë", "Test", "5", "Periudha I"],
  ["Era Gashi", "VIII/1", "Matematikë", "Detyrë shtëpie", "4", "Periudha I"],
  ["Diar Berisha", "VIII/2", "Matematikë", "Aktivitet në klasë", "5", "Periudha I"],
  ["Suela Hoxha", "VIII/2", "Matematikë", "Test", "5", "Periudha I"],
  ["Majlinda Berisha", "IX/1", "Matematikë", "Detyrë shtëpie", "4", "Periudha I"],
  ["Aron Krasniqi", "IX/1", "Matematikë", "Aktivitet në klasë", "5", "Periudha I"]
];
data.absences = [
  ["Ardit Krasniqi", "VIII/1", "14.09.2026", "Matematikë", "E pajustifikuar"],
  ["Era Gashi", "VIII/1", "16.09.2026", "Matematikë", "E arsyetuar"],
  ["Diar Berisha", "VIII/2", "17.09.2026", "Matematikë", "E pajustifikuar"],
  ["Suela Hoxha", "VIII/2", "18.09.2026", "Matematikë", "E arsyetuar"],
  ["Aron Krasniqi", "IX/1", "21.09.2026", "Matematikë", "E pajustifikuar"]
];

defs.grades = ["Vlerësimi dhe notat", ["Nxënësi", "Klasa", "Lënda", "Lloji i vlerësimit", "Nota", "Periudha"], data.teacherGrades];
defs.absences = ["Mungesat", ["Nxënësi", "Klasa", "Data", "Lënda", "Statusi"], data.absences];
icons.absences = "📝";

configs.MESIMDHENES[1] = [["Klasat", "3"], ["Nxënës", "74"], ["Lënda", "Matematikë"], ["Vlerësime për t'u regjistruar", "16"]];
configs.MESIMDHENES[2] = [
  ["classes", "Klasat e mia", "VIII/1, VIII/2 dhe IX/1."],
  ["students", "Nxënësit", "Nxënësit e klasave që mëson."],
  ["grades", "Vlerësimi dhe notat", "Regjistro dhe kontrollo notat."],
  ["absences", "Mungesat", "Regjistro dhe arsyeto mungesat."],
  ["schedule", "Orari", "Orari yt i Matematikës."],
  ["announcements", "Njoftimet", "Njoftimet e shkollës."],
  ["profile", "Profili", "Të dhënat personale."]
];

// E bëjmë qartë në panel se ky është profili personal i mësimdhënësit.
const originalDashboard = dashboard;
dashboard = function(user) {
  if (user && user.id === "3") {
    user = { ...user, fullName: "Leonard Tahiraj", roleLabel: "Mësimdhënës i Matematikës" };
  }
  originalDashboard(user);
};
