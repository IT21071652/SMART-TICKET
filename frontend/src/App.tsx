import { useEffect, useState, type FormEvent } from 'react'
import { useAppDispatch, useAppSelector } from './app/hooks'
import { clearNotice, createTicket, fetchStats, fetchTickets, setFilter, setPage } from './features/tickets/ticketsSlice'
import type { Ticket } from './types'

function App() {
  const dispatch = useAppDispatch()
  const { tickets, stats, filters, page, totalPages, totalElements, loading, submitting, error, notice } =
    useAppSelector((state) => state.tickets)
  const [formOpen, setFormOpen] = useState(false)

  useEffect(() => {
    void dispatch(fetchTickets({ filters, page }))
    void dispatch(fetchStats())
    const poll = window.setInterval(() => {
      void dispatch(fetchTickets({ filters, page }))
      void dispatch(fetchStats())
    }, 8000)
    return () => window.clearInterval(poll)
  }, [dispatch, filters, page])

  async function submitTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const result = await dispatch(createTicket({
      title: String(data.get('title')),
      description: String(data.get('description')),
      customerEmail: String(data.get('customerEmail')),
    }))
    if (createTicket.fulfilled.match(result)) {
      event.currentTarget.reset()
      setFormOpen(false)
      dispatch(setPage(0))
      void dispatch(fetchTickets({ filters, page: 0 }))
      void dispatch(fetchStats())
    }
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">S</span>
          <span>smart<span className="brand-light">desk</span></span>
        </div>
        <div className="workspace-label">WORKSPACE</div>
        <button className="nav-item active"><span className="nav-icon">▦</span> Ticket desk <span className="nav-count">{stats.total}</span></button>
        <div className="sidebar-bottom">
          <div className="avatar">SS</div>
          <div><strong>Support team</strong><small>Workspace admin</small></div>
          <span className="more">···</span>
        </div>
      </aside>

      <section className="main">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><span className="crumb-sep">/</span><strong>Ticket desk</strong></div>
          <div className="top-actions"><span className="live-indicator"><i /> Live updates</span><span className="top-avatar">S</span></div>
        </header>

        <div className="content">
          <div className="page-heading">
            <div>
              <div className="eyebrow">SUPPORT OPERATIONS</div>
              <h1>Ticket desk <span className="heading-spark">✳</span></h1>
              <p>Every conversation, understood and ready for action.</p>
            </div>
            <button className="primary-button" onClick={() => { setFormOpen(!formOpen); dispatch(clearNotice()) }}>
              <span className="plus">+</span> New ticket
            </button>
          </div>

          {notice && <div className="notice" role="status"><span>✓</span>{notice}<button onClick={() => dispatch(clearNotice())} aria-label="Dismiss">×</button></div>}
          {error && <div className="error-banner" role="alert"><span>{error}</span><button onClick={() => dispatch(clearNotice())}>Dismiss</button></div>}

          {formOpen && <form className="ticket-form" onSubmit={submitTicket}>
            <div className="form-heading"><div><span className="eyebrow">CUSTOMER REQUEST</span><h2>Create a ticket</h2></div><button type="button" className="close-button" onClick={() => setFormOpen(false)} aria-label="Close form">×</button></div>
            <label>Subject<input name="title" required maxLength={160} placeholder="What does the customer need help with?" /></label>
            <label>Customer email<input name="customerEmail" required type="email" maxLength={254} placeholder="name@company.com" /></label>
            <label>Details<textarea name="description" required maxLength={10000} rows={4} placeholder="Add the details that will help us resolve this..." /></label>
            <div className="form-footer"><span>✳ AI will categorize and prioritize this ticket</span><button className="primary-button" type="submit" disabled={submitting}>{submitting ? 'Submitting…' : 'Submit ticket'}</button></div>
          </form>}

          <div className="stats-grid">
            <StatCard label="Total tickets" value={stats.total} hint="Across all conversations" icon="▤" tone="lavender" />
            <StatCard label="Awaiting analysis" value={stats.byStatus.OPEN ?? 0} hint="AI processing queue" icon="◷" tone="amber" />
            <StatCard label="Analyzed" value={stats.byStatus.ANALYZED ?? 0} hint="Ready for your team" icon="✳" tone="mint" />
            <StatCard label="High priority" value={stats.byPriority.HIGH ?? 0} hint="Needs quick attention" icon="↗" tone="rose" />
          </div>

          <section className="inbox">
            <div className="inbox-heading">
              <div><h2>All tickets <span className="result-count">{totalElements}</span></h2><p>Review, filter, and prioritize customer conversations.</p></div>
              <div className="inbox-tools">
                <label className="search-box"><span>⌕</span><input value={filters.q} onChange={(event) => dispatch(setFilter({ name: 'q', value: event.target.value }))} placeholder="Search tickets..." aria-label="Search tickets" /></label>
                <select value={filters.status} onChange={(event) => dispatch(setFilter({ name: 'status', value: event.target.value }))} aria-label="Filter by status">
                  <option value="">All status</option><option value="OPEN">Awaiting analysis</option><option value="ANALYZED">Analyzed</option>
                </select>
                <select value={filters.category} onChange={(event) => dispatch(setFilter({ name: 'category', value: event.target.value }))} aria-label="Filter by category">
                  <option value="">All categories</option><option value="BILLING">Billing</option><option value="TECHNICAL">Technical</option><option value="ACCOUNT">Account</option><option value="GENERAL">General</option>
                </select>
                <select value={filters.priority} onChange={(event) => dispatch(setFilter({ name: 'priority', value: event.target.value }))} aria-label="Filter by priority">
                  <option value="">All priorities</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option>
                </select>
              </div>
            </div>
            <div className="table-wrap">
              <table>
                <thead><tr><th>SUBJECT</th><th>CATEGORY</th><th>PRIORITY</th><th>SENTIMENT</th><th>STATUS</th><th>CREATED</th></tr></thead>
                <tbody>
                  {loading && tickets.length === 0 ? <tr><td className="empty-state" colSpan={6}>Loading tickets…</td></tr>
                    : tickets.length === 0 ? <tr><td className="empty-state" colSpan={6}><span className="empty-icon">✳</span><strong>No tickets found</strong><span>Try changing your filters or submit a new ticket.</span></td></tr>
                      : tickets.map((ticket) => <TicketRow key={ticket.id} ticket={ticket} />)}
                </tbody>
              </table>
            </div>
            <div className="table-footer">
              <span>Showing {tickets.length ? page * 20 + 1 : 0}–{Math.min((page + 1) * 20, totalElements)} of {totalElements}</span>
              <div className="pagination"><button disabled={page === 0} onClick={() => dispatch(setPage(page - 1))} aria-label="Previous page">←</button><span>{page + 1} / {Math.max(totalPages, 1)}</span><button disabled={page + 1 >= totalPages} onClick={() => dispatch(setPage(page + 1))} aria-label="Next page">→</button></div>
            </div>
          </section>
          <footer className="footer-note"><span>✳</span> Smart analysis, human-first support.</footer>
        </div>
      </section>
    </main>
  )
}

function StatCard({ label, value, hint, icon, tone }: { label: string; value: number; hint: string; icon: string; tone: string }) {
  return <article className="stat-card"><div className={`stat-icon ${tone}`}>{icon}</div><div className="stat-copy"><span>{label}</span><strong>{value.toLocaleString()}</strong><small>{hint}</small></div><span className="stat-dots">···</span></article>
}

function TicketRow({ ticket }: { ticket: Ticket }) {
  const category = ticket.category?.toLowerCase() ?? 'pending'
  return <tr>
    <td className="subject-cell"><span className={`ticket-avatar ${category}`}>{ticket.category ? ticket.category[0] : '…'}</span><span className="subject-text"><strong>{ticket.title}</strong><small>{ticket.customerEmail}</small></span></td>
    <td>{ticket.category ? <span className={`category-pill ${category}`}>{ticket.category.toLowerCase()}</span> : <span className="muted">Processing</span>}</td>
    <td>{ticket.priority ? <span className={`priority ${ticket.priority.toLowerCase()}`}><i />{ticket.priority.toLowerCase()}</span> : <span className="muted">—</span>}</td>
    <td>{ticket.sentiment ? <span className={`sentiment ${ticket.sentiment.toLowerCase()}`}>{ticket.sentiment.toLowerCase()}</span> : <span className="muted">—</span>}</td>
    <td><span className={`status-pill ${ticket.status.toLowerCase()}`}><i />{ticket.status === 'OPEN' ? 'Awaiting AI' : 'Analyzed'}</span></td>
    <td className="date-cell">{new Date(ticket.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</td>
  </tr>
}

export default App
