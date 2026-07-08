import { useEffect, useMemo, useState } from "react";
import {
  CalendarCheck,
  CalendarDays,
  Clock,
  LoaderCircle,
  ShieldCheck,
  Store,
  Ticket,
  XCircle,
} from "lucide-react";
import { apiFetch } from "../api.js";
import {
  formatDateTime,
  formatTime,
  isAtLeastHoursAway,
  toDateInputValue,
} from "../utils.js";
import {
  EmptyState,
  InlineLoader,
  Message,
  SectionHeader,
  StatusBadge,
} from "../components/Common.jsx";

export default function CustomerPage() {
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="dashboard customer-dashboard">
      <MarketplacePanel
        role="CUSTOMER"
        onBookingCreated={() => setRefreshKey((value) => value + 1)}
      />
      <CustomerBookings
        refreshKey={refreshKey}
        onChanged={() => setRefreshKey((value) => value + 1)}
      />
      <CustomerRoleRequest />
    </div>
  );
}

function CustomerRoleRequest() {
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
      const data = await apiFetch("/api/me/merchant-request");

      if (data === null) {
        setRequest(null);
        return;
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
        subtitle="Invia una richiesta: il super admin può cambiare il tuo ruolo, ma lo store non viene assegnato automaticamente."
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

export function MarketplacePanel({ role, onBookingCreated }) {
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

function CustomerBookings({ refreshKey, onChanged }) {
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
      const data = await apiFetch("/api/bookings/my");
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
