export function readCookie(name) {
  return document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

export function getXsrfToken() {
  const token = readCookie("XSRF-TOKEN");
  return token ? decodeURIComponent(token) : "";
}
