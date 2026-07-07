export const ROLE_LABELS = {
  CUSTOMER: "Customer",
  MERCHANT: "Merchant",
  SUPER_ADMIN: "Super admin",
};

export const ROLE_OPTIONS = ["CUSTOMER", "MERCHANT"];

export const WEEK_DAYS = [
  [1, "Lunedì"],
  [2, "Martedì"],
  [3, "Mercoledì"],
  [4, "Giovedì"],
  [5, "Venerdì"],
  [6, "Sabato"],
  [7, "Domenica"],
];

function pad(value) {
  return String(value).padStart(2, "0");
}

export function toDateInputValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function toDateTimeLocalValue(date) {
  return `${toDateInputValue(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function formatDateTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("it-IT", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("it-IT", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function isAtLeastHoursAway(value, hours) {
  return new Date(value).getTime() - Date.now() >= hours * 60 * 60 * 1000;
}
