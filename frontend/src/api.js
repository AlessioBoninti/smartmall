import { getXsrfToken } from "./auth.js";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export class ApiError extends Error {
  constructor(message, status = 0, data = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

export async function ensureCsrfToken(forceRefresh = false) {
  if (!forceRefresh && getXsrfToken()) {
    return getXsrfToken();
  }

  const response = await fetch(`${API_URL}/api/auth/csrf`, {
    method: "GET",
    credentials: "include",
  });
  const data = await readResponseData(response);

  if (!response.ok) {
    throw new ApiError(resolveErrorMessage(response.status, data), response.status, data);
  }

  const token = getXsrfToken();
  if (!token) {
    throw new ApiError("Token CSRF non disponibile.", response.status, data);
  }

  return token;
}

export async function apiFetch(path, { method = "GET", body } = {}) {
  const upperMethod = method.toUpperCase();
  const headers = {};

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (MUTATING_METHODS.has(upperMethod)) {
    headers["X-XSRF-TOKEN"] = await ensureCsrfToken();
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: upperMethod,
    headers,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const data = await readResponseData(response);

  if (!response.ok) {
    throw new ApiError(resolveErrorMessage(response.status, data), response.status, data);
  }

  return data;
}

async function readResponseData(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    try {
      return await response.json();
    } catch {
      return null;
    }
  }

  try {
    const text = await response.text();
    if (!text) {
      return null;
    }

    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  } catch {
    return null;
  }
}

function resolveErrorMessage(status, data) {
  const backendMessage = extractBackendMessage(data);
  if (backendMessage) {
    return backendMessage;
  }

  if (status === 401) {
    return "Sessione scaduta. Effettua di nuovo il login.";
  }

  if (status === 403) {
    return "Operazione non autorizzata o token CSRF non valido.";
  }

  if (status >= 500) {
    return "Errore del server. Riprova piu tardi.";
  }

  return "Operazione non riuscita.";
}

function extractBackendMessage(data) {
  if (!data) {
    return "";
  }

  if (typeof data === "string") {
    return data;
  }

  return data.message || data.error || "";
}
