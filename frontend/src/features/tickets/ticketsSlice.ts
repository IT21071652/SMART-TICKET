import { createAsyncThunk, createSlice, type PayloadAction } from '@reduxjs/toolkit'
import type { CreateTicket, Ticket, TicketPage, TicketStats } from '../../types'

const API = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')

interface Filters {
  q: string
  status: string
  category: string
  priority: string
}

interface TicketsState {
  tickets: Ticket[]
  stats: TicketStats
  filters: Filters
  page: number
  totalPages: number
  totalElements: number
  loading: boolean
  submitting: boolean
  error: string | null
  notice: string | null
}

const initialState: TicketsState = {
  tickets: [],
  stats: { total: 0, byStatus: {}, byCategory: {}, byPriority: {} },
  filters: { q: '', status: '', category: '', priority: '' },
  page: 0,
  totalPages: 0,
  totalElements: 0,
  loading: false,
  submitting: false,
  error: null,
  notice: null,
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}

export const fetchTickets = createAsyncThunk<
  TicketPage,
  { filters: Filters; page: number },
  { rejectValue: string }
>('tickets/fetch', async ({ filters, page }, { rejectWithValue }) => {
  try {
    const params = new URLSearchParams({ page: String(page), size: '20' })
    Object.entries(filters).forEach(([key, value]) => {
      if (value.trim()) params.set(key, value.trim())
    })
    return await request<TicketPage>(`/tickets?${params}`)
  } catch (error) {
    return rejectWithValue(error instanceof Error ? error.message : 'Could not load tickets')
  }
})

export const fetchStats = createAsyncThunk<TicketStats, void, { rejectValue: string }>(
  'tickets/fetchStats',
  async (_, { rejectWithValue }) => {
    try {
      return await request<TicketStats>('/stats')
    } catch (error) {
      return rejectWithValue(error instanceof Error ? error.message : 'Could not load analytics')
    }
  },
)

export const createTicket = createAsyncThunk<Ticket, CreateTicket, { rejectValue: string }>(
  'tickets/create',
  async (ticket, { rejectWithValue }) => {
    try {
      return await request<Ticket>('/tickets', {
        method: 'POST',
        body: JSON.stringify(ticket),
      })
    } catch (error) {
      return rejectWithValue(error instanceof Error ? error.message : 'Could not submit ticket')
    }
  },
)

const ticketsSlice = createSlice({
  name: 'tickets',
  initialState,
  reducers: {
    setFilter(state, action: PayloadAction<{ name: keyof Filters; value: string }>) {
      state.filters[action.payload.name] = action.payload.value
      state.page = 0
      state.notice = null
    },
    setPage(state, action: PayloadAction<number>) {
      state.page = action.payload
    },
    clearNotice(state) {
      state.notice = null
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchTickets.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchTickets.fulfilled, (state, action) => {
        state.loading = false
        state.tickets = action.payload.content
        state.totalElements = action.payload.totalElements
        state.totalPages = action.payload.totalPages
      })
      .addCase(fetchTickets.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload ?? 'Could not load tickets'
      })
      .addCase(fetchStats.fulfilled, (state, action) => {
        state.stats = action.payload
      })
      .addCase(fetchStats.rejected, (state, action) => {
        state.error = action.payload ?? 'Could not load analytics'
      })
      .addCase(createTicket.pending, (state) => {
        state.submitting = true
        state.error = null
        state.notice = null
      })
      .addCase(createTicket.fulfilled, (state) => {
        state.submitting = false
        state.notice = 'Ticket submitted — AI analysis is on its way.'
      })
      .addCase(createTicket.rejected, (state, action) => {
        state.submitting = false
        state.error = action.payload ?? 'Could not submit ticket'
      })
  },
})

export const { setFilter, setPage, clearNotice } = ticketsSlice.actions
export default ticketsSlice.reducer
