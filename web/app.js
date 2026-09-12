const demoUsers = {
  admin: { id: "1", fullName: "Administrator", role: "ADMINISTRATOR", roleLabel: "Administrator" },
  drejtor: { id: "2", fullName: "Drejtor i shkollës", role: "DREJTOR", roleLabel: "Drejtor" },
  mesimdhenes: { id: "3", fullName: "Mësimdhënës Demo", role: "MESIMDHENES", roleLabel: "Mësimdhënës" },
  nxenes: { id: "4", fullName: "Nxënës Demo", role: "NXENES", roleLabel: "Nxënës" },
  prind: { id: "5", fullName: "Prind Demo", role: "PRIND", roleLabel: "Prind" }
};

const roleConfig = {
  ADMINISTRATOR: {
    description: "Menaxhim i plotë i platformës dhe parametrave të shkollës.",
    stats: [["Përdorues", "124"], ["Nxënës", "286"], ["Mësimdhënës", "32"], ["Klasat", "18"]],
    modules: [
      ["users", "Përdoruesit", "Menaxho llogaritë, rolet dhe statusin e përdoruesve."],
      ["school", "Shkolla", "Të dhënat bazë dhe parametrat e institucionit."],
      ["classes", "Klasat", "Klasat, paralelet dhe organizimi i nxënësve."],
      ["teachers", "Mësimdhënësit", "Regjistri dhe të dhënat e stafit mësimor."],
      ["students", "Nxënësit", "Regjistri qendror i nxënësve."],
      ["subjects", "Lëndët", "Lëndët mësimore dhe lidhja me klasat."]
    ]
  },
  DREJTOR: {
    description: "Mbikëqyrje e punës së shkollës, stafit, nxënësve dhe raporteve.",
    stats: [["Nxënës", "286"], ["Mësimdhënës", "32"], ["Klasat", "18"], ["Njoftime", "4"]],
    modules: [
      ["classes", "Klasat", "Organizimi dhe pasqyra e klasave."],
      ["teachers", "Mësimdhënësit", "Stafi dhe ngarkesa mësimore."],
      ["students", "Nxënësit", "Lista dhe të dhënat e nxënësve."],
      ["schedule", "Orari", "Orari mësimor i shkollës."],
      ["announcements", "Njoftimet", "Njoftime për komunitetin e shkollës."],
      ["reports", "Raportet", "Raporte dhe pasqyra të performancës."]
    ]
  },
  MESIMDHENES: {
    description: "Mjetet e përditshme për mësimdhënie, nxënësit dhe vlerësimin.",
    stats: [["Klasat", "4"], ["Nxënës", "96"], ["Lëndët", "2"], ["Nota për t'u regjistruar", "12"]],
    modules: [
      ["classes", "Klasat e mia", "Shiko klasat dhe nxënësit që i mëson."],
      ["students", "Nxënësit", "Lista e nxënësve sipas klasës."],
      ["grades", "Notat", "Regjistro dhe menaxho vlerësimet."],
      ["schedule", "Orari", "Orari yt mësimor."],
      ["announcements", "Njoftimet", "Njoftimet e shkollës."],
      ["profile", "Profili", "Të dhënat personale dhe profesionale."]
    ]
  },
  NXENES: {
    description: "Hapësira personale për orarin, notat dhe njoftimet.",
    stats: [["Klasa", "VIII/2"], ["Lëndë", "12"], ["Nota", "28"], ["Njoftime", "3"]],
    modules: [
      ["schedule", "Orari", "Orari yt mësimor."],
      ["grades", "Notat", "Shiko notat dhe suksesin."],
      ["announcements", "Njoftimet", "Njoftime nga shkolla dhe mësimdhënësit."],
      ["profile", "Profili", "Të dhënat e profilit tënd."]
    ]
  },
  PRIND: {
    description: "Pasqyrë e fëmijëve, suksesit, orarit dhe komunikimeve të shkollës.",
    stats: [["Fëmijë", "1"], ["Nota", "28"], ["Njoftime", "3"], ["Mungesa", "2"]],
    modules: [
      ["children", "Fëmijët", "Shiko fëmijët e lidhur me llogarinë."],
      ["grades", "Notat", "Shiko suksesin dhe vlerësimet."],
      ["schedule", "Orari", "Orari mësimor i fëmijës."],
      ["announcements", "Njoftimet", "Komunikime nga shkolla."],
      ["profile", "Profili", "Të dhënat e llogarisë."]
    ]
  }
};

const moduleIcons = {
  users: "👥", school: "🏫", classes: "📚", teachers: "🧑‍🏫", students: "🎓",
  subjects: "📖", schedule: "🗓️", announcements: "📢", reports: "📊", grades: "⭐",
  profile: "👤", children: "👨‍👩‍👧"
};

const $ = (id) => document.getElementById(id);

function showDashboard(user) {
  const config = roleConfig[user.role];
  $("login").classList.add("hidden");
  $("dashboard").classList.remove("hidden");
  $("status").textContent = "I kyçur";
  $("status").className = "status success";
  $("welcome").textContent = `Mirë se vini, ${user.fullName}`;
  $("roleDescription").textContent = config.description;

  $("stats").innerHTML = config.stats.map(([label, value]) => `
    <div class="stat-card"><span>${label}</span><strong>${value}</strong></div>
  `).join("");

  $("modules").innerHTML = config.modules.map(([key, title, description]) => `
    <button class="module-card" data-module="${key}">
      <span class="module-icon">${moduleIcons[key] || "•"}</span>
      <span><strong>${title}</strong><small>${description}</small></span>
      <span class="arrow">→</span>
    </button>
  `).join("");

  $("activityList").innerHTML = [
    "Autentikimi u krye me sukses.",
    "Paneli u ngarkua sipas rolit të përdoruesit.",
    "Modulet janë gati për lidhjen me API-në reale."
  ].map((text, index) => `<div class="activity"><span class="dot"></span><span>${text}</span><small>${index === 0 ? "Tani" : "Sot"}</small></div>`).join("");

  $("profile").innerHTML = `
    <div><span>Përdoruesi</span><strong>${getUsername(user)}</strong></div>
    <div><span>Emri</span><strong>${user.fullName}</strong></div>
    <div><span>Roli</span><strong>${user.roleLabel}</strong></div>
    <div><span>Statusi</span><strong>Aktiv</strong></div>
  `;

  document.querySelectorAll(".module-card").forEach((button) => {
    button.addEventListener("click", () => {
      const moduleName = button.querySelector("strong").textContent;
      alert(`${moduleName}: moduli është në fazën e parë të ndërtimit. Tani po përgatitet lidhja me të dhënat reale.`);
    });
  });
}

function getUsername(user) {
  return Object.keys(demoUsers).find((key) => demoUsers[key].id === user.id) || "demo";
}

function showLogin() {
  $("dashboard").classList.add("hidden");
  $("login").classList.remove("hidden");
  $("status").textContent = "Demo";
  $("status").className = "status";
  $("error").textContent = "";
}

$("loginButton").addEventListener("click", () => {
  const username = $("username").value.trim().toLowerCase();
  const password = $("password").value;
  const user = demoUsers[username];

  if (!username || !password) {
    $("error").textContent = "Plotësoni përdoruesin dhe fjalëkalimin.";
    return;
  }
  if (!user || password !== "123456") {
    $("error").textContent = "Përdoruesi ose fjalëkalimi është i pasaktë.";
    return;
  }

  $("error").textContent = "";
  sessionStorage.setItem("eshkollaUser", JSON.stringify(user));
  showDashboard(user);
});

$("password").addEventListener("keydown", (event) => {
  if (event.key === "Enter") $("loginButton").click();
});

$("logoutButton").addEventListener("click", () => {
  sessionStorage.removeItem("eshkollaUser");
  showLogin();
});

const savedUser = sessionStorage.getItem("eshkollaUser");
if (savedUser) {
  try {
    showDashboard(JSON.parse(savedUser));
  } catch {
    sessionStorage.removeItem("eshkollaUser");
  }
}
