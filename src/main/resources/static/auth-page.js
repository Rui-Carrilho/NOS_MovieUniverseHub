import { api, getSession, message } from "./ui-api.js";
const register = location.pathname === "/register";
const form = document.querySelector("#auth-form");
const button = document.querySelector("#submit");
const password = document.querySelector("#password");
const error = document.querySelector("#auth-error");
if (register) {
  document.title = "Criar conta · MovieUniverse";
  document.querySelector("#auth-title").textContent =
    "O teu universo começa aqui.";
  document.querySelector("#auth-subtitle").textContent =
    "Cria uma conta e dá lugar aos teus filmes.";
  password.autocomplete = "new-password";
  password.minLength = 10;
  document.querySelector("#password-help").hidden = false;
  button.replaceChildren(document.createTextNode("Criar conta ↗"));
  const line = document.querySelector("#auth-switch");
  const link = document.createElement("a");
  link.href = "/login";
  link.textContent = "Entrar";
  line.replaceChildren(document.createTextNode("Já tens conta? "), link);
}
document.querySelector("#show-password").addEventListener("click", (event) => {
  const show = password.type === "password";
  password.type = show ? "text" : "password";
  event.currentTarget.textContent = show ? "Ocultar" : "Mostrar";
  event.currentTarget.setAttribute("aria-pressed", String(show));
});
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  error.hidden = true;
  button.disabled = true;
  button.textContent = register ? "A criar a tua conta…" : "A entrar…";
  try {
    await api(register ? "/api/auth/register" : "/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: document.querySelector("#name").value.trim(),
        password: password.value,
      }),
    });
    password.value = "";
    location.replace("/");
  } catch (cause) {
    error.textContent = message(cause);
    error.hidden = false;
    button.disabled = false;
    button.textContent = register ? "Criar conta ↗" : "Entrar ↗";
  }
});
getSession()
  .then((value) => {
    if (value.user) location.replace("/");
  })
  .catch((cause) => {
    error.textContent = message(cause);
    error.hidden = false;
  });
