import { useEffect, useState } from "react";
import {
  Ban,
  CalendarCheck,
  CheckCircle2,
  Clock,
  PauseCircle,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  Store,
  Ticket,
  Trash2,
  XCircle,
} from "lucide-react";
import { apiFetch } from "../api.js";
import {
  formatDateTime,
  ROLE_LABELS,
  ROLE_OPTIONS,
  toDateTimeLocalValue,
} from "../utils.js";
import {
  EmptyState,
  InlineLoader,
  Message,
  Metric,
  RoleBadge,
  SectionHeader,
  StatusBadge,
} from "../components/Common.jsx";

export default function AdminPage({ user }) {
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
        apiFetch("/api/admin/users"),
        apiFetch("/api/admin/stores"),
        apiFetch("/api/admin/bookings"),
        apiFetch("/api/admin/role-requests"),
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
        body: { role },
      });
      setMessage("Ruolo aggiornato.");
      await loadAdminData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function deleteUser(userToDelete) {
    if (!window.confirm(`Eliminare definitivamente ${userToDelete.email}?`)) {
      return;
    }

    try {
      await apiFetch(`/api/admin/users/${userToDelete.id}`, {
        method: "DELETE",
      });
      setMessage("Utente eliminato.");
      await loadAdminData();
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function patchStore(id, action, body) {
    try {
      await apiFetch(`/api/admin/stores/${id}/${action}`, {
        method: "PATCH",
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
      });
      setMessage(
        action === "approve"
          ? "Richiesta approvata. Lo store non viene assegnato automaticamente."
          : "Richiesta rifiutata."
      );
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

        {message && (
          <Message
            type={message.includes("aggiornato") || message.includes("eliminato") ? "success" : "info"}
            text={message}
          />
        )}

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
              <AdminUsers
                users={users}
                currentUser={user}
                onRoleChange={updateUserRole}
                onDeleteUser={deleteUser}
              />
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

function AdminUsers({ users, currentUser, onRoleChange, onDeleteUser }) {
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
          {users.map((user) => {
            const canManageUser = user.id !== currentUser.id && user.role !== "SUPER_ADMIN";
            const roleOptions = [...new Set([user.role, ...ROLE_OPTIONS])];

            return (
              <tr key={user.id}>
                <td>{user.email}</td>
                <td>
                  <RoleBadge role={user.role} />
                </td>
                <td className="action-cell">
                  <select
                    value={user.role}
                    disabled={!canManageUser}
                    onChange={(event) => onRoleChange(user.id, event.target.value)}
                  >
                    {roleOptions.map((role) => (
                      <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                      </option>
                    ))}
                  </select>
                  <button
                    className="button compact danger"
                    type="button"
                    disabled={!canManageUser}
                    onClick={() => onDeleteUser(user)}
                  >
                    <Trash2 size={16} />
                    Elimina
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
            <strong>Sospendi subito {suspendStore.name}</strong>
            <span>{suspendStore.merchantEmail}</span>
          </div>
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
