const demoUsers = {
  admin: { fullName: "Administrator", role: "ADMINISTRATOR" },
  drejtor: { fullName: "Drejtor i shkollës", role: "DREJTOR" },
  mesimdhenes: { fullName: "Mësimdhënës Demo", role: "MESIMDHENES" },
  nxenes: { fullName: "Nxënës Demo", role: "NXENES" },
  prind: { fullName: "Prind Demo", role: "PRIND" }
};

const $ = (id) => document.getElementById(id);

function showDashboard(user) {
  $("login").classList.add("hidden");
  $("dashboard").classList.remove("hidden");
  $("status").textContent = "I kyçur";
  $("status").className = "status success";
  $("welcome").textContent = `Mirë se vini, ${user.fullName}.`;
  $("profile").textContent = `Roli: ${user.role}`;
}

function showLogin() {
  $("dashboard").classList.add("hidden");
  $("login").classList.remove("hidden");
  $("status").textContent = "Demo";
  $("status").className = "status";
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

$("logoutButton").addEventListener("click", () => {
  sessionStorage.removeItem("eshkollaUser");
  showLogin();
});

const savedUser = sessionStorage.getItem("eshkollaUser");
if (savedUser) showDashboard(JSON.parse(savedUser));
