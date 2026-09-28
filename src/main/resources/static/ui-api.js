let session;
export async function getSession() {
  const response = await fetch("/api/auth/session", {
    signal: AbortSignal.timeout(15000),
    cache: "no-store",
  });
  if (!response.ok)
    throw new Error("Não foi possível verificar a sessão. Tenta novamente.");
  session = await response.json();
  return session;
}
export async function api(path, options = {}) {
  const { timeoutMs = 20000, ...settings } = options;
  const write = !["GET", "HEAD"].includes(settings.method || "GET");
  if (write && !session) await getSession();
  const response = await fetch(path, {
    ...settings,
    signal: AbortSignal.timeout(timeoutMs),
    headers: {
      Accept: "application/json",
      ...(settings.body ? { "Content-Type": "application/json" } : {}),
      ...(write ? { [session.csrfHeader]: session.csrfToken } : {}),
    },
  });
  if (!response.ok) {
    let data;
    try {
      data = await response.json();
    } catch {}
    if (response.status === 401) {
      session = null;
      if (!["/login", "/register", "/auth.html"].includes(location.pathname))
        location.replace("/login");
    }
    if (response.status === 403) session = null;
    const error = new Error(
      data?.message || data?.detail || "Não foi possível concluir o pedido. Tenta novamente.",
    );
    error.status = response.status;
    throw error;
  }
  if (path.startsWith("/api/auth/") && write) session = null;
  const value = await response.text();
  return value ? JSON.parse(value) : null;
}
export function message(error) {
  return error.name === "TimeoutError"
    ? "O pedido demorou demasiado. Tenta novamente."
    : error instanceof TypeError
      ? "Não foi possível contactar a aplicação. Verifica a ligação."
      : error.message;
}
