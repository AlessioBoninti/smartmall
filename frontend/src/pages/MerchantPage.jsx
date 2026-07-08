import { useEffect, useMemo, useState } from "react";
import {
  Building2,
  CalendarCheck,
  CheckCircle2,
  Clock,
  Edit3,
  RefreshCw,
  RotateCcw,
  Store,
  Ticket,
  Trash2,
  XCircle,
} from "lucide-react";
import { apiFetch } from "../api.js";
import {
  formatDateTime,
  isAtLeastHoursAway,
  WEEK_DAYS,
} from "../utils.js";
import {
  EmptyState,
  InlineLoader,
  Message,
  Metric,
  SectionHeader,
  StatusBadge,
} from "../components/Common.jsx";

export default function MerchantPage() {
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
        apiFetch("/api/merchant/stores"),
        apiFetch("/api/merchant/bookings"),
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
          <>
            <EmptyState icon={<Store size={24} />} title="Nessuno store assegnato" />
            <Message
              type="info"
              text="Il ruolo merchant non crea automaticamente uno store: per la demo usa l'account merchant con store già assegnato."
            />
          </>
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

      <MerchantAvailabilityRules stores={stores} loadingStores={loading} />

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

function MerchantAvailabilityRules({ stores, loadingStores }) {
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
      const data = await apiFetch(`/api/merchant/stores/${storeId}/availability-rules`);
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
          const data = await apiFetch(`/api/merchant/stores/${store.id}/availability-rules`);
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
          body: payload,
        });
        setMessage("Regola aggiornata.");
      } else {
        await apiFetch(`/api/merchant/stores/${selectedStoreId}/availability-rules`, {
          method: "POST",
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
        <>
          <EmptyState icon={<Store size={24} />} title="Nessuno store assegnato" />
          <Message
            type="info"
            text="Le regole di disponibilità si configurano solo dopo l'assegnazione di almeno uno store."
          />
        </>
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
