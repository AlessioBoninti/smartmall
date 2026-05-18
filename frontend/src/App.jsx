import { useEffect, useMemo, useState } from "react";
import {
  Ban,
  Building2,
  CalendarCheck,
  CalendarDays,
  CheckCircle2,
  Clock,
  Edit3,
  KeyRound,
  LoaderCircle,
  LockKeyhole,
  LogIn,
  LogOut,
  Mail,
  PauseCircle,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  Store,
  Ticket,
  Trash2,
  UserPlus,
  XCircle,
} from "lucide-react";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const TOKEN_KEY = "smartmall_token";

const ROLE_LABELS = {
  CUSTOMER: "Customer",
  MERCHANT: "Merchant",
  SUPER_ADMIN: "Super admin",
};

const ROLE_OPTIONS = ["CUSTOMER", "MERCHANT", "SUPER_ADMIN"];

const WEEK_DAYS = [
  [1, "Lunedì"],
  [2, "Martedì"],
  [3, "Mercoledì"],
  [4, "Giovedì"],
  [5, "Venerdì"],
  [6, "Sabato"],
  [7, "Domenica"],
];

async function apiFetch(path, { method = "GET", token, body } = {}) {
  const headers = {};

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    throw new Error(data?.message || data?.error || data || "Operazione non riuscita");
  }

  return data;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function toDateInputValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function toDateTimeLocalValue(date) {
  return `${toDateInputValue(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDateTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("it-IT", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("it-IT", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function isAtLeastHoursAway(value, hours) {
  return new Date(value).getTime() - Date.now() >= hours * 60 * 60 * 1000;
}

function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || "");
  const [currentUser, setCurrentUser] = useState(null);
  const [booting, setBooting] = useState(Boolean(token));
  const [authError, setAuthError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!token) {
      setBooting(false);
      return;
    }

    apiFetch("/api/me", { token })
      .then(setCurrentUser)
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        setToken("");
        setCurrentUser(null);
      })
      .finally(() => setBooting(false));
  }, []);

  function saveToken(nextToken) {
    setToken(nextToken);

    if (nextToken) {
      localStorage.setItem(TOKEN_KEY, nextToken);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  async function handleLogin(credentials) {
    setAuthError("");
    setNotice("");

    try {
      const response = await apiFetch("/api/auth/login", {
        method: "POST",
        body: credentials,
      });
      saveToken(response.token);
      const user = await apiFetch("/api/me", { token: response.token });
      setCurrentUser(user);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function handleRegister(credentials) {
    setAuthError("");
    setNotice("");

    try {
      await apiFetch("/api/auth/register", {
        method: "POST",
        body: credentials,
      });
      await handleLogin(credentials);
      setNotice("Account creato con ruolo customer.");
    } catch (error) {
      setAuthError(error.message);
    }
  }

  function handleLogout() {
    saveToken("");
    setCurrentUser(null);
    setNotice("");
    setAuthError("");
  }

  return (
    <div className="app-shell">
      <TopBar user={currentUser} onLogout={handleLogout} />

      <main className="app-main">
        {booting ? (
          <LoadingScreen />
        ) : currentUser ? (
          <AuthenticatedApp user={currentUser} token={token} />
        ) : (
          <PublicApp
            onLogin={handleLogin}
            onRegister={handleRegister}
            authError={authError}
            notice={notice}
          />
        )}
      </main>
    </div>
  );
}

function AuthenticatedApp({ user, token }) {
  if (user.role === "SUPER_ADMIN") {
    return <AdminDashboard user={user} token={token} />;
  }

  if (user.role === "MERCHANT") {
    return <MerchantDashboard token={token} />;
  }

  return <CustomerDashboard token={token} />;
}

function TopBar({ user, onLogout }) {
  return (
    <header className="topbar">
      <div className="brand">
        <span className="brand-mark">
          <Building2 size={22} />
        </span>
        <div>
          <strong>SmartMall</strong>
          <span>Prenotazioni negozi</span>
        </div>
      </div>

      <div className="topbar-actions">
        {user ? (
          <>
            <div className="user-pill">
              <span>{user.email}</span>
              <RoleBadge role={user.role} />
            </div>
            <button className="button ghost" type="button" onClick={onLogout}>
              <LogOut size={18} />
              Esci
            </button>
          </>
        ) : (
          <div className="guest-pill">
            <LockKeyhole size={17} />
            Visitatore
          </div>
        )}
      </div>
    </header>
  );
}

function PublicApp({ onLogin, onRegister, authError, notice }) {
  return (
    <div className="dashboard visitor-dashboard">
      <section className="panel auth-panel">
        <SectionHeader
          icon={<LogIn size={20} />}
          title="Accesso"
          subtitle="Consulta gli slot anche senza account."
        />
        <AuthPanel
          onLogin={onLogin}
          onRegister={onRegister}
          error={authError}
          notice={notice}
        />
      </section>

      <MarketplacePanel role="GUEST" />
    </div>
  );
}

function AuthPanel({ onLogin, onRegister, error, notice }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("customer@test.com");
  const [password, setPassword] = useState("password123");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);

    const payload = { email, password };
    try {
      if (mode === "login") {
        await onLogin(payload);
      } else {
        await onRegister(payload);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="segmented">
        <button
          type="button"
          className={mode === "login" ? "active" : ""}
          onClick={() => setMode("login")}
        >
          <LogIn size={16} />
          Login
        </button>
        <button
          type="button"
          className={mode === "register" ? "active" : ""}
          onClick={() => setMode("register")}
        >
          <UserPlus size={16} />
          Registrati
        </button>
      </div>

      {error && <Message type="error" text={error} />}
      {notice && <Message type="success" text={notice} />}

      <label className="field">
        <span>Email</span>
        <div className="input-with-icon">
          <Mail size={17} />
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="nome@email.it"
            required
          />
        </div>
      </label>

      <label className="field">
        <span>Password</span>
        <div className="input-with-icon">
          <KeyRound size={17} />
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password"
            minLength={mode === "register" ? 6 : undefined}
            required
          />
        </div>
      </label>

      <button className="button primary wide" type="submit" disabled={submitting}>
        {submitting ? <LoaderCircle className="spin" size={18} /> : mode === "login" ? <LogIn size={18} /> : <UserPlus size={18} />}
        {mode === "login" ? "Entra" : "Crea account"}
      </button>
    </form>
  );
}

function CustomerDashboard({ token }) {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="dashboard customer-dashboard">
      <MarketplacePanel
        role="CUSTOMER"
        token={token}
        onBookingCreated={() => setRefreshKey((value) => value + 1)}
      />
      <CustomerBookings
        token={token}
        refreshKey={refreshKey}
        onChanged={() => setRefreshKey((value) => value + 1)}
      />
      <CustomerRoleRequest token={token} />
    </div>
  );
}

function CustomerRoleRequest({ token }) {
  const [request, setRequest] = useState(null);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    loadRequest();
  }, []);

  async function loadRequest() {
    setLoading(true);
    setMessage("");

    try {
      const response = await fetch(`${API_URL}/api/me/merchant-request`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 204) {
        setRequest(null);
        return;
      }

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data?.message || "Impossibile caricare la richiesta merchant");
      }

      setRequest(data);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function submitRequest(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");

    try {
      const data = await apiFetch("/api/me/merchant-request", {
        method: "POST",
        token,
        body: {
          reason: reason.trim() || null,
        },
      });

      setRequest(data);
      setReason("");
      setMessage("Richiesta inviata all'admin.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  const hasPendingRequest = request?.status === "PENDING";

  return (
    <section className="panel">
      <SectionHeader
        icon={<ShieldCheck size={20} />}
        title="Diventa merchant"
        subtitle="Invia una richiesta: il super admin deciderà se cambiare il tuo ruolo."
      />

      {message && <Message type={message.includes("inviata") ? "success" : "info"} text={message} />}

      {loading ? (
        <InlineLoader text="Caricamento richiesta" />
      ) : (
        <div className="role-request-box">
          {request && (
            <div className="request-summary">
              <div>
                <strong>Ultima richiesta</strong>
                <span>{formatDateTime(request.createdAt)}</span>
              </div>
              <StatusBadge status={request.status} />
            </div>
          )}

          {hasPendingRequest ? (
            <Message type="info" text="La tua richiesta è in attesa di valutazione." />
          ) : (
            <form className="role-request-form" onSubmit={submitRequest}>
              <label className="field">
                <span>Motivo</span>
                <textarea
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  maxLength={500}
                  placeholder="Scrivi perché vuoi diventare merchant"
                />
              </label>
              <button className="button primary wide" type="submit" disabled={submitting}>
                {submitting ? <LoaderCircle className="spin" size={18} /> : <ShieldCheck size={18} />}
                Invia richiesta
              </button>
            </form>
          )}
        </div>
      )}
    </section>
  );
}

function MarketplacePanel({ role, token, onBookingCreated }) {
  const [stores, setStores] = useState([]);
  const [selectedStoreId, setSelectedStoreId] = useState("");
  const [date, setDate] = useState(toDateInputValue(new Date()));
  const [slots, setSlots] = useState([]);
  const [loadingStores, setLoadingStores] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [message, setMessage] = useState("");

  const selectedStore = useMemo(
    () => stores.find((store) => String(store.id) === String(selectedStoreId)),
    [stores, selectedStoreId]
  );

  useEffect(() => {
    let ignore = false;
    setLoadingStores(true);

    apiFetch("/api/stores")
      .then((data) => {
        if (ignore) return;
        setStores(data);
        setSelectedStoreId((current) => current || data[0]?.id || "");
      })
      .catch((error) => setMessage(error.message))
      .finally(() => {
        if (!ignore) setLoadingStores(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!selectedStoreId || !date) {
      setSlots([]);
      return;
    }

    let ignore = false;
    setLoadingSlots(true);
    setMessage("");

    apiFetch(`/api/slots?storeId=${selectedStoreId}&date=${date}`)
      .then((data) => {
        if (!ignore) setSlots(data);
      })
      .catch((error) => {
        if (!ignore) setMessage(error.message);
      })
      .finally(() => {
        if (!ignore) setLoadingSlots(false);
      });

    return () => {
      ignore = true;
    };
  }, [selectedStoreId, date]);

  async function handleBooking(slot) {
    if (role !== "CUSTOMER") {
      setMessage("Per prenotare devi accedere con un profilo customer.");
      return;
    }

    if (!isAtLeastHoursAway(slot.startDateTime, 5)) {
      setMessage("Le prenotazioni richiedono almeno 5 ore di anticipo.");
      return;
    }

    try {
      await apiFetch("/api/bookings", {
        method: "POST",
        token,
        body: {
          storeId: Number(selectedStoreId),
          startDateTime: slot.startDateTime,
        },
      });

      setMessage("Prenotazione confermata.");
      onBookingCreated?.();
      const updatedSlots = await apiFetch(`/api/slots?storeId=${selectedStoreId}&date=${date}`);
      setSlots(updatedSlots);
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <section className="panel marketplace-panel">
      <SectionHeader
        icon={<Store size={20} />}
        title={role === "CUSTOMER" ? "Prenota uno slot" : "Store e slot"}
        subtitle={role === "CUSTOMER" ? "Scegli negozio, data e orario." : "Gli slot sono consultabili liberamente."}
      />

      {message && <Message type={message.includes("confermata") ? "success" : "info"} text={message} />}

      <div className="toolbar">
        <label className="field grow">
          <span>Negozio</span>
          <select
            value={selectedStoreId}
            onChange={(event) => setSelectedStoreId(event.target.value)}
            disabled={loadingStores || stores.length === 0}
          >
            {stores.map((store) => (
              <option key={store.id} value={store.id}>
                {store.name}
              </option>
            ))}
          </select>
        </label>

        <label className="field date-field">
          <span>Data</span>
          <input type="date" value={date} onChange={(event) => setDate(event.target.value)} />
        </label>

        <button
          className="button secondary"
          type="button"
          onClick={() => setDate(toDateInputValue(new Date()))}
        >
          <CalendarDays size={18} />
          Oggi
        </button>
      </div>

      <div className="store-strip">
        {loadingStores ? (
          <InlineLoader text="Caricamento store" />
        ) : stores.length === 0 ? (
          <EmptyState icon={<Store size={24} />} title="Nessuno store attivo" />
        ) : (
          stores.map((store) => (
            <button
              type="button"
              key={store.id}
              className={`store-chip ${String(store.id) === String(selectedStoreId) ? "active" : ""}`}
              onClick={() => setSelectedStoreId(store.id)}
            >
              <Store size={16} />
              <span>{store.name}</span>
            </button>
          ))
        )}
      </div>

      <div className="slot-list">
        <div className="list-heading">
          <span>{selectedStore?.name || "Slot"}</span>
          <small>{slots.length} disponibili</small>
        </div>

        {loadingSlots ? (
          <InlineLoader text="Caricamento slot" />
        ) : slots.length === 0 ? (
          <EmptyState icon={<Clock size={24} />} title="Nessuno slot disponibile per questa data" />
        ) : (
          slots.map((slot) => {
            const canBook = role === "CUSTOMER" && isAtLeastHoursAway(slot.startDateTime, 5);

            return (
              <div className="slot-row" key={`${slot.startDateTime}-${slot.endDateTime}`}>
                <div>
                  <strong>
                    {formatTime(slot.startDateTime)} - {formatTime(slot.endDateTime)}
                  </strong>
                  <span>{slot.availableCapacity} posti liberi</span>
                </div>
                <button
                  className={canBook ? "button primary" : "button muted"}
                  type="button"
                  onClick={() => handleBooking(slot)}
                  disabled={role === "CUSTOMER" && !canBook}
                >
                  <Ticket size={18} />
                  {role === "CUSTOMER" ? (canBook ? "Prenota" : "Meno di 5h") : "Login"}
                </button>
              </div>
            );
          })
        )}
      </div>
    </section>
  );
}

function CustomerBookings({ token, refreshKey, onChanged }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    loadBookings();
  }, [refreshKey]);

  async function loadBookings() {
    setLoading(true);
    setMessage("");

    try {
      const data = await apiFetch("/api/bookings/my", { token });
      setBookings(data);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function cancelBooking(id) {
    try {
      await apiFetch(`/api/bookings/${id}/cancel`, {
        method: "PATCH",
        token,
      });
      setMessage("Prenotazione cancellata.");
      onChanged?.();
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <section className="panel">
      <SectionHeader
        icon={<CalendarCheck size={20} />}
        title="Le mie prenotazioni"
        subtitle="Puoi cancellare solo con almeno 12 ore di preavviso."
      />

      {message && <Message type={message.includes("cancellata") ? "success" : "info"} text={message} />}

      {loading ? (
        <InlineLoader text="Caricamento prenotazioni" />
      ) : bookings.length === 0 ? (
        <EmptyState icon={<CalendarCheck size={24} />} title="Non hai prenotazioni" />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Store</th>
                <th>Inizio</th>
                <th>Fine</th>
                <th>Stato</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((booking) => {
                const canCancel =
                  booking.status === "CONFIRMED" && isAtLeastHoursAway(booking.startDateTime, 12);

                return (
                  <tr key={booking.id}>
                    <td>{booking.storeName}</td>
                    <td>{formatDateTime(booking.startDateTime)}</td>
                    <td>{formatDateTime(booking.endDateTime)}</td>
                    <td>
                      <StatusBadge status={booking.status} />
                    </td>
                    <td className="align-right">
                      <button
                        className="button compact danger"
                        type="button"
                        disabled={!canCancel}
                        onClick={() => cancelBooking(booking.id)}
                      >
                        <XCircle size={16} />
                        Cancella
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function MerchantDashboard({ token }) {
  const [stores, setStores] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  useEffect(() => {
    loadMerchantData();
  }, []);

  async function loadMerchantData() {
    setLoading(true);
    setMessage("");

    try {
      const [storeData, bookingData] = await Promise.all([
        apiFetch("/api/merchant/stores", { token }),
        apiFetch("/api/merchant/bookings", { token }),
      ]);
      setStores(storeData);
      setBookings(bookingData);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function cancelBooking(id) {
    try {
      await apiFetch(`/api/bookings/${id}/cancel`, {
        method: "PATCH",
        token,
      });
      setMessage("Prenotazione cancellata.");
      await loadMerchantData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  const activeStores = stores.filter((store) => store.status === "ACTIVE").length;
  const confirmedBookings = bookings.filter((booking) => booking.status === "CONFIRMED").length;

  return (
    <div className="dashboard merchant-dashboard">
      <section className="panel">
        <SectionHeader
          icon={<Building2 size={20} />}
          title="Area merchant"
          subtitle="Store assegnati e prenotazioni ricevute."
          action={
            <button className="button secondary" type="button" onClick={loadMerchantData}>
              <RefreshCw size={18} />
              Aggiorna
            </button>
          }
        />

        {message && <Message type={message.includes("cancellata") ? "success" : "info"} text={message} />}

        <div className="metric-grid">
          <Metric icon={<Store size={20} />} label="Store" value={stores.length} />
          <Metric icon={<CheckCircle2 size={20} />} label="Attivi" value={activeStores} />
          <Metric icon={<Ticket size={20} />} label="Prenotazioni confermate" value={confirmedBookings} />
        </div>
      </section>

      <section className="panel">
        <SectionHeader icon={<Store size={20} />} title="I miei store" />
        {loading ? (
          <InlineLoader text="Caricamento store" />
        ) : stores.length === 0 ? (
          <EmptyState icon={<Store size={24} />} title="Nessuno store assegnato" />
        ) : (
          <div className="store-cards">
            {stores.map((store) => (
              <article className="store-card" key={store.id}>
                <div>
                  <strong>{store.name}</strong>
                  {store.suspendedReason && <span>{store.suspendedReason}</span>}
                </div>
                <StatusBadge status={store.status} />
              </article>
            ))}
          </div>
        )}
      </section>

      <MerchantAvailabilityRules stores={stores} loadingStores={loading} token={token} />

      <section className="panel wide-panel">
        <SectionHeader icon={<CalendarCheck size={20} />} title="Prenotazioni ricevute" />

        {loading ? (
          <InlineLoader text="Caricamento prenotazioni" />
        ) : bookings.length === 0 ? (
          <EmptyState icon={<CalendarCheck size={24} />} title="Nessuna prenotazione ricevuta" />
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Store</th>
                  <th>Cliente</th>
                  <th>Inizio</th>
                  <th>Fine</th>
                  <th>Stato</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => {
                  const canCancel =
                    booking.status === "CONFIRMED" && isAtLeastHoursAway(booking.startDateTime, 12);

                  return (
                    <tr key={booking.id}>
                      <td>{booking.storeName}</td>
                      <td>{booking.customerEmail}</td>
                      <td>{formatDateTime(booking.startDateTime)}</td>
                      <td>{formatDateTime(booking.endDateTime)}</td>
                      <td>
                        <StatusBadge status={booking.status} />
                      </td>
                      <td className="align-right">
                        <button
                          className="button compact danger"
                          type="button"
                          disabled={!canCancel}
                          onClick={() => cancelBooking(booking.id)}
                        >
                          <XCircle size={16} />
                          Cancella
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function MerchantAvailabilityRules({ stores, loadingStores, token }) {
  const [selectedStoreId, setSelectedStoreId] = useState("");
  const [rules, setRules] = useState([]);
  const [rulesByStore, setRulesByStore] = useState({});
  const [loadingRules, setLoadingRules] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState(null);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({
    dayOfWeek: "1",
    morningStartTime: "08:00",
    morningEndTime: "13:00",
    afternoonStartTime: "14:00",
    afternoonEndTime: "19:00",
    closed: false,
    slotMinutes: 30,
    capacityPerSlot: 2,
    active: true,
  });

  useEffect(() => {
    if (!selectedStoreId && stores.length > 0) {
      setSelectedStoreId(String(stores[0].id));
    }
  }, [stores, selectedStoreId]);

  useEffect(() => {
    if (selectedStoreId) {
      loadRules(selectedStoreId);
    } else {
      setRules([]);
    }
  }, [selectedStoreId]);

  useEffect(() => {
    if (stores.length > 0) {
      loadAllRulesCoverage();
    } else {
      setRulesByStore({});
    }
  }, [stores]);

  const configuredDays = useMemo(
    () => new Set(rules.map((rule) => rule.dayOfWeek)),
    [rules]
  );

  const currentRule = useMemo(
    () => rules.find((rule) => rule.id === editingRuleId),
    [rules, editingRuleId]
  );

  const dayOptions = useMemo(() => {
    if (editingRuleId) {
      return WEEK_DAYS.filter(([value]) => value === currentRule?.dayOfWeek || !configuredDays.has(value));
    }

    return WEEK_DAYS.filter(([value]) => !configuredDays.has(value));
  }, [configuredDays, currentRule, editingRuleId]);

  const missingSetup = useMemo(() => {
    return stores
      .map((store) => {
        const storeRules = rulesByStore[store.id] || [];
        const configured = new Set(storeRules.map((rule) => rule.dayOfWeek));
        const missingDays = WEEK_DAYS
          .filter(([value]) => !configured.has(value))
          .map(([, label]) => label);

        return {
          store,
          missingDays,
        };
      })
      .filter((item) => item.missingDays.length > 0);
  }, [rulesByStore, stores]);

  const selectedStoreMissingDays = useMemo(() => {
    const configured = new Set(rules.map((rule) => rule.dayOfWeek));
    return WEEK_DAYS.filter(([value]) => !configured.has(value)).map(([, label]) => label);
  }, [rules]);

  const isClosedDay = Boolean(form.closed);

  useEffect(() => {
    if (!editingRuleId && dayOptions.length > 0 && !dayOptions.some(([value]) => String(value) === form.dayOfWeek)) {
      setForm((current) => ({ ...current, dayOfWeek: String(dayOptions[0][0]) }));
    }
  }, [dayOptions, editingRuleId, form.dayOfWeek]);

  async function loadRules(storeId = selectedStoreId) {
    if (!storeId) return;

    setLoadingRules(true);
    setMessage("");

    try {
      const data = await apiFetch(`/api/merchant/stores/${storeId}/availability-rules`, { token });
      setRules(data);
      setRulesByStore((current) => ({
        ...current,
        [storeId]: data,
      }));
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoadingRules(false);
    }
  }

  async function loadAllRulesCoverage() {
    try {
      const entries = await Promise.all(
        stores.map(async (store) => {
          const data = await apiFetch(`/api/merchant/stores/${store.id}/availability-rules`, { token });
          return [store.id, data];
        })
      );

      setRulesByStore(Object.fromEntries(entries));
    } catch (error) {
      setMessage(error.message);
    }
  }

  function resetForm() {
    setEditingRuleId(null);
    setForm({
      dayOfWeek: "1",
      morningStartTime: "08:00",
      morningEndTime: "13:00",
      afternoonStartTime: "14:00",
      afternoonEndTime: "19:00",
      closed: false,
      slotMinutes: 30,
      capacityPerSlot: 2,
      active: true,
    });
  }

  function editRule(rule) {
    setEditingRuleId(rule.id);
    setForm({
      dayOfWeek: String(rule.dayOfWeek),
      morningStartTime: rule.morningStartTime?.slice(0, 5) || "08:00",
      morningEndTime: rule.morningEndTime?.slice(0, 5) || "13:00",
      afternoonStartTime: rule.afternoonStartTime?.slice(0, 5) || "14:00",
      afternoonEndTime: rule.afternoonEndTime?.slice(0, 5) || "19:00",
      closed: Boolean(rule.closed),
      slotMinutes: rule.slotMinutes,
      capacityPerSlot: rule.capacityPerSlot,
      active: !rule.closed && Boolean(rule.active),
    });
  }

  async function saveRule(event) {
    event.preventDefault();

    const payload = {
      dayOfWeek: Number(form.dayOfWeek),
      morningStartTime: form.morningStartTime,
      morningEndTime: form.morningEndTime,
      afternoonStartTime: form.afternoonStartTime,
      afternoonEndTime: form.afternoonEndTime,
      closed: form.closed,
      slotMinutes: Number(form.slotMinutes),
      capacityPerSlot: Number(form.capacityPerSlot),
      active: !form.closed && form.active,
    };

    try {
      if (editingRuleId) {
        await apiFetch(`/api/merchant/availability-rules/${editingRuleId}`, {
          method: "PATCH",
          token,
          body: payload,
        });
        setMessage("Regola aggiornata.");
      } else {
        await apiFetch(`/api/merchant/stores/${selectedStoreId}/availability-rules`, {
          method: "POST",
          token,
          body: payload,
        });
        setMessage("Regola creata.");
      }

      resetForm();
      await loadRules();
      await loadAllRulesCoverage();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function deleteRule(ruleId) {
    try {
      await apiFetch(`/api/merchant/availability-rules/${ruleId}`, {
        method: "DELETE",
        token,
      });
      setMessage("Regola eliminata.");
      if (editingRuleId === ruleId) {
        resetForm();
      }
      await loadRules();
      await loadAllRulesCoverage();
    } catch (error) {
      setMessage(error.message);
    }
  }

  return (
    <section className="panel wide-panel">
      <SectionHeader
        icon={<Clock size={20} />}
        title="Regole disponibilità"
        subtitle="Definisci giorni, orari, durata slot e capacità dei tuoi store."
      />

      {message && (
        <Message
          type={message.includes("creata") || message.includes("aggiornata") || message.includes("eliminata") ? "success" : "info"}
          text={message}
        />
      )}

      {missingSetup.length > 0 && (
        <div className="setup-warning">
          <strong>Setup disponibilità richiesto</strong>
          <span>
            Completa una regola per ogni giorno della settimana prima di considerare configurato il negozio.
          </span>
          <ul>
            {missingSetup.map(({ store, missingDays }) => (
              <li key={store.id}>
                {store.name}: mancano {missingDays.join(", ")}
              </li>
            ))}
          </ul>
        </div>
      )}

      {loadingStores ? (
        <InlineLoader text="Caricamento store" />
      ) : stores.length === 0 ? (
        <EmptyState icon={<Store size={24} />} title="Nessuno store assegnato" />
      ) : (
        <div className="availability-layout">
          <form className="availability-form" onSubmit={saveRule}>
            <label className="field">
              <span>Store</span>
              <select
                value={selectedStoreId}
                onChange={(event) => {
                  setSelectedStoreId(event.target.value);
                  resetForm();
                }}
              >
                {stores.map((store) => (
                  <option key={store.id} value={store.id}>
                    {store.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Giorno</span>
              <select
                value={form.dayOfWeek}
                onChange={(event) => setForm((current) => ({ ...current, dayOfWeek: event.target.value }))}
                disabled={!editingRuleId && dayOptions.length === 0}
              >
                {dayOptions.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Inizio mattina</span>
              <input
                type="time"
                value={form.morningStartTime}
                onChange={(event) => setForm((current) => ({ ...current, morningStartTime: event.target.value }))}
                disabled={isClosedDay}
                required={!isClosedDay}
              />
            </label>

            <label className="field">
              <span>Chiusura mattina</span>
              <input
                type="time"
                value={form.morningEndTime}
                onChange={(event) => setForm((current) => ({ ...current, morningEndTime: event.target.value }))}
                disabled={isClosedDay}
                required={!isClosedDay}
              />
            </label>

            <label className="field">
              <span>Inizio pomeriggio</span>
              <input
                type="time"
                value={form.afternoonStartTime}
                onChange={(event) => setForm((current) => ({ ...current, afternoonStartTime: event.target.value }))}
                disabled={isClosedDay}
                required={!isClosedDay}
              />
            </label>

            <label className="field">
              <span>Chiusura pomeriggio</span>
              <input
                type="time"
                value={form.afternoonEndTime}
                onChange={(event) => setForm((current) => ({ ...current, afternoonEndTime: event.target.value }))}
                disabled={isClosedDay}
                required={!isClosedDay}
              />
            </label>

            <label className="field">
              <span>Durata slot</span>
              <input
                type="number"
                min="5"
                step="5"
                value={form.slotMinutes}
                onChange={(event) => setForm((current) => ({ ...current, slotMinutes: event.target.value }))}
                disabled={isClosedDay}
                required
              />
            </label>

            <label className="field">
              <span>Capacità</span>
              <input
                type="number"
                min="1"
                value={form.capacityPerSlot}
                onChange={(event) => setForm((current) => ({ ...current, capacityPerSlot: event.target.value }))}
                disabled={isClosedDay}
                required
              />
            </label>

            <label className="check-field">
              <input
                type="checkbox"
                checked={form.closed}
                onChange={(event) => setForm((current) => ({ ...current, closed: event.target.checked }))}
              />
              <span>Giorno chiuso</span>
            </label>

            <label className="check-field">
              <input
                type="checkbox"
                checked={form.active}
                onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
                disabled={isClosedDay}
              />
              <span>Attiva</span>
            </label>

            <div className="form-actions">
              <button className="button primary" type="submit" disabled={!editingRuleId && dayOptions.length === 0}>
                <CheckCircle2 size={18} />
                {editingRuleId ? "Aggiorna" : "Crea"}
              </button>
              {editingRuleId && (
                <button className="button" type="button" onClick={resetForm}>
                  <RotateCcw size={18} />
                  Annulla
                </button>
              )}
            </div>
          </form>

          {!editingRuleId && dayOptions.length === 0 && (
            <Message type="success" text="Questo store ha già una regola per tutti i giorni della settimana." />
          )}

          {selectedStoreMissingDays.length > 0 && (
            <Message
              type="info"
              text={`Per questo store mancano: ${selectedStoreMissingDays.join(", ")}`}
            />
          )}

          {loadingRules ? (
            <InlineLoader text="Caricamento regole" />
          ) : rules.length === 0 ? (
            <EmptyState icon={<Clock size={24} />} title="Nessuna regola configurata" />
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Giorno</th>
                    <th>Mattina</th>
                    <th>Pomeriggio</th>
                    <th>Slot</th>
                    <th>Capacità</th>
                    <th>Stato</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {rules.map((rule) => (
                    <tr key={rule.id}>
                      <td>{WEEK_DAYS.find(([value]) => value === rule.dayOfWeek)?.[1] || rule.dayOfWeek}</td>
                      <td>
                        {rule.closed
                          ? "Chiuso"
                          : `${rule.morningStartTime?.slice(0, 5)} - ${rule.morningEndTime?.slice(0, 5)}`}
                      </td>
                      <td>
                        {rule.closed
                          ? "-"
                          : rule.afternoonStartTime && rule.afternoonEndTime
                          ? `${rule.afternoonStartTime.slice(0, 5)} - ${rule.afternoonEndTime.slice(0, 5)}`
                          : "-"}
                      </td>
                      <td>{rule.closed ? "-" : `${rule.slotMinutes} min`}</td>
                      <td>{rule.closed ? "-" : rule.capacityPerSlot}</td>
                      <td>
                        <StatusBadge status={rule.closed ? "CLOSED" : rule.active ? "ACTIVE" : "SUSPENDED"} />
                      </td>
                      <td className="action-cell">
                        <button className="button compact" type="button" onClick={() => editRule(rule)}>
                          <Edit3 size={16} />
                          Modifica
                        </button>
                        <button className="button compact danger" type="button" onClick={() => deleteRule(rule.id)}>
                          <Trash2 size={16} />
                          Elimina
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function AdminDashboard({ user, token }) {
  const [activeTab, setActiveTab] = useState("users");
  const [users, setUsers] = useState([]);
  const [stores, setStores] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [roleRequests, setRoleRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [suspendStore, setSuspendStore] = useState(null);
  const [suspendForm, setSuspendForm] = useState(() => ({
    from: toDateTimeLocalValue(new Date(Date.now() + 60 * 60 * 1000)),
    to: toDateTimeLocalValue(new Date(Date.now() + 25 * 60 * 60 * 1000)),
    reason: "",
  }));

  useEffect(() => {
    loadAdminData();
  }, []);

  async function loadAdminData() {
    setLoading(true);
    setMessage("");

    try {
      const [userData, storeData, bookingData, roleRequestData] = await Promise.all([
        apiFetch("/api/admin/users", { token }),
        apiFetch("/api/admin/stores", { token }),
        apiFetch("/api/admin/bookings", { token }),
        apiFetch("/api/admin/role-requests", { token }),
      ]);
      setUsers(userData);
      setStores(storeData);
      setBookings(bookingData);
      setRoleRequests(roleRequestData);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function updateUserRole(id, role) {
    try {
      await apiFetch(`/api/admin/users/${id}/role`, {
        method: "PATCH",
        token,
        body: { role },
      });
      setMessage("Ruolo aggiornato.");
      await loadAdminData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function patchStore(id, action, body) {
    try {
      await apiFetch(`/api/admin/stores/${id}/${action}`, {
        method: "PATCH",
        token,
        body,
      });
      setMessage("Store aggiornato.");
      setSuspendStore(null);
      setSuspendForm((current) => ({ ...current, reason: "" }));
      await loadAdminData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function reviewRoleRequest(id, action) {
    try {
      await apiFetch(`/api/admin/role-requests/${id}/${action}`, {
        method: "PATCH",
        token,
      });
      setMessage(action === "approve" ? "Richiesta approvata." : "Richiesta rifiutata.");
      await loadAdminData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  function submitSuspension(event) {
    event.preventDefault();
    if (!suspendStore) return;
    patchStore(suspendStore.id, "suspend", suspendForm);
  }

  return (
    <div className="dashboard admin-dashboard">
      <section className="panel wide-panel">
        <SectionHeader
          icon={<ShieldCheck size={20} />}
          title="Area super admin"
          subtitle="Utenti, store e prenotazioni."
          action={
            <button className="button secondary" type="button" onClick={loadAdminData}>
              <RefreshCw size={18} />
              Aggiorna
            </button>
          }
        />

        {message && <Message type={message.includes("aggiornato") ? "success" : "info"} text={message} />}

        <div className="metric-grid">
          <Metric icon={<ShieldCheck size={20} />} label="Utenti" value={users.length} />
          <Metric icon={<Store size={20} />} label="Store" value={stores.length} />
          <Metric icon={<Ticket size={20} />} label="Prenotazioni" value={bookings.length} />
          <Metric
            icon={<Clock size={20} />}
            label="Richieste in attesa"
            value={roleRequests.filter((request) => request.status === "PENDING").length}
          />
        </div>

        <div className="tabs">
          <button
            type="button"
            className={activeTab === "users" ? "active" : ""}
            onClick={() => setActiveTab("users")}
          >
            <ShieldCheck size={17} />
            Utenti
          </button>
          <button
            type="button"
            className={activeTab === "stores" ? "active" : ""}
            onClick={() => setActiveTab("stores")}
          >
            <Store size={17} />
            Store
          </button>
          <button
            type="button"
            className={activeTab === "bookings" ? "active" : ""}
            onClick={() => setActiveTab("bookings")}
          >
            <CalendarCheck size={17} />
            Prenotazioni
          </button>
          <button
            type="button"
            className={activeTab === "requests" ? "active" : ""}
            onClick={() => setActiveTab("requests")}
          >
            <Clock size={17} />
            Richieste
          </button>
        </div>

        {loading ? (
          <InlineLoader text="Caricamento dati admin" />
        ) : (
          <>
            {activeTab === "users" && (
              <AdminUsers users={users} currentUser={user} onRoleChange={updateUserRole} />
            )}
            {activeTab === "stores" && (
              <AdminStores
                stores={stores}
                suspendStore={suspendStore}
                suspendForm={suspendForm}
                onSuspendSelect={setSuspendStore}
                onSuspendFormChange={setSuspendForm}
                onSuspendSubmit={submitSuspension}
                onPatchStore={patchStore}
              />
            )}
            {activeTab === "bookings" && <AdminBookings bookings={bookings} />}
            {activeTab === "requests" && (
              <AdminRoleRequests requests={roleRequests} onReview={reviewRoleRequest} />
            )}
          </>
        )}
      </section>
    </div>
  );
}

function AdminUsers({ users, currentUser, onRoleChange }) {
  if (users.length === 0) {
    return <EmptyState icon={<ShieldCheck size={24} />} title="Nessun utente" />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Email</th>
            <th>Ruolo</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>{user.email}</td>
              <td>
                <RoleBadge role={user.role} />
              </td>
              <td className="align-right">
                <select
                  value={user.role}
                  disabled={user.id === currentUser.id}
                  onChange={(event) => onRoleChange(user.id, event.target.value)}
                >
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role} value={role}>
                      {ROLE_LABELS[role]}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AdminStores({
  stores,
  suspendStore,
  suspendForm,
  onSuspendSelect,
  onSuspendFormChange,
  onSuspendSubmit,
  onPatchStore,
}) {
  if (stores.length === 0) {
    return <EmptyState icon={<Store size={24} />} title="Nessuno store" />;
  }

  return (
    <div className="admin-store-area">
      {suspendStore && (
        <form className="suspend-form" onSubmit={onSuspendSubmit}>
          <div>
            <strong>Sospendi {suspendStore.name}</strong>
            <span>{suspendStore.merchantEmail}</span>
          </div>
          <label className="field">
            <span>Da</span>
            <input
              type="datetime-local"
              value={suspendForm.from}
              onChange={(event) =>
                onSuspendFormChange((current) => ({ ...current, from: event.target.value }))
              }
              required
            />
          </label>
          <label className="field">
            <span>A</span>
            <input
              type="datetime-local"
              value={suspendForm.to}
              onChange={(event) =>
                onSuspendFormChange((current) => ({ ...current, to: event.target.value }))
              }
              required
            />
          </label>
          <label className="field grow">
            <span>Motivo</span>
            <input
              value={suspendForm.reason}
              onChange={(event) =>
                onSuspendFormChange((current) => ({ ...current, reason: event.target.value }))
              }
              placeholder="Motivo sospensione"
              required
            />
          </label>
          <button className="button warning" type="submit">
            <PauseCircle size={18} />
            Conferma
          </button>
        </form>
      )}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Store</th>
              <th>Merchant</th>
              <th>Stato</th>
              <th>Sospensione</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {stores.map((store) => (
              <tr key={store.id}>
                <td>{store.name}</td>
                <td>{store.merchantEmail}</td>
                <td>
                  <StatusBadge status={store.status} />
                </td>
                <td>
                  {store.suspendedReason ? (
                    <span className="muted-text">{store.suspendedReason}</span>
                  ) : (
                    <span className="muted-text">-</span>
                  )}
                </td>
                <td className="action-cell">
                  <button
                    className="button compact warning"
                    type="button"
                    onClick={() => onSuspendSelect(store)}
                  >
                    <PauseCircle size={16} />
                    Sospendi
                  </button>
                  <button
                    className="button compact"
                    type="button"
                    onClick={() => onPatchStore(store.id, "unsuspend")}
                  >
                    <RotateCcw size={16} />
                    Riattiva
                  </button>
                  <button
                    className="button compact danger"
                    type="button"
                    onClick={() => onPatchStore(store.id, "close")}
                  >
                    <Ban size={16} />
                    Chiudi
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AdminBookings({ bookings }) {
  if (bookings.length === 0) {
    return <EmptyState icon={<CalendarCheck size={24} />} title="Nessuna prenotazione" />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Store</th>
            <th>Cliente</th>
            <th>Inizio</th>
            <th>Fine</th>
            <th>Stato</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map((booking) => (
            <tr key={booking.id}>
              <td>{booking.storeName}</td>
              <td>{booking.customerEmail}</td>
              <td>{formatDateTime(booking.startDateTime)}</td>
              <td>{formatDateTime(booking.endDateTime)}</td>
              <td>
                <StatusBadge status={booking.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function AdminRoleRequests({ requests, onReview }) {
  if (requests.length === 0) {
    return <EmptyState icon={<Clock size={24} />} title="Nessuna richiesta merchant" />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Utente</th>
            <th>Richiesta</th>
            <th>Motivo</th>
            <th>Stato</th>
            <th>Valutata</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {requests.map((request) => {
            const isPending = request.status === "PENDING";

            return (
              <tr key={request.id}>
                <td>{request.requesterEmail}</td>
                <td>
                  <RoleBadge role={request.requestedRole} />
                  <span className="table-subtext">{formatDateTime(request.createdAt)}</span>
                </td>
                <td>
                  <span className="muted-text">{request.reason || "-"}</span>
                </td>
                <td>
                  <StatusBadge status={request.status} />
                </td>
                <td>
                  <span className="muted-text">
                    {request.reviewedAt ? formatDateTime(request.reviewedAt) : "-"}
                  </span>
                </td>
                <td className="action-cell">
                  <button
                    className="button compact primary"
                    type="button"
                    disabled={!isPending}
                    onClick={() => onReview(request.id, "approve")}
                  >
                    <CheckCircle2 size={16} />
                    Approva
                  </button>
                  <button
                    className="button compact danger"
                    type="button"
                    disabled={!isPending}
                    onClick={() => onReview(request.id, "reject")}
                  >
                    <XCircle size={16} />
                    Rifiuta
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function SectionHeader({ icon, title, subtitle, action }) {
  return (
    <div className="section-header">
      <div className="section-title">
        <span className="section-icon">{icon}</span>
        <div>
          <h1>{title}</h1>
          {subtitle && <p>{subtitle}</p>}
        </div>
      </div>
      {action && <div className="section-action">{action}</div>}
    </div>
  );
}

function Metric({ icon, label, value }) {
  return (
    <div className="metric">
      <span>{icon}</span>
      <div>
        <strong>{value}</strong>
        <small>{label}</small>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  return <span className={`status-badge status-${status?.toLowerCase()}`}>{status}</span>;
}

function RoleBadge({ role }) {
  return <span className={`role-badge role-${role?.toLowerCase()}`}>{ROLE_LABELS[role] || role}</span>;
}

function Message({ type = "info", text }) {
  const Icon = type === "error" ? XCircle : type === "success" ? CheckCircle2 : Search;
  return (
    <div className={`message ${type}`}>
      <Icon size={17} />
      <span>{text}</span>
    </div>
  );
}

function InlineLoader({ text }) {
  return (
    <div className="inline-loader">
      <LoaderCircle className="spin" size={18} />
      <span>{text}</span>
    </div>
  );
}

function LoadingScreen() {
  return (
    <section className="panel loading-panel">
      <InlineLoader text="Apertura SmartMall" />
    </section>
  );
}

function EmptyState({ icon, title }) {
  return (
    <div className="empty-state">
      <span>{icon}</span>
      <strong>{title}</strong>
    </div>
  );
}

export default App;
