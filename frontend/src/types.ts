export type TicketStatus = 'OPEN' | 'ANALYZED'
export type TicketCategory = 'BILLING' | 'TECHNICAL' | 'ACCOUNT' | 'GENERAL'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Ticket {
  id: string
  title: string
  description: string
  customerEmail: string
  status: TicketStatus
  category: TicketCategory | null
  priority: TicketPriority | null
  sentiment: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | null
  summary: string | null
  createdAt: string
  updatedAt: string
}

export interface TicketPage {
  content: Ticket[]
  totalElements: number
  totalPages: number
  number: number
}

export interface TicketStats {
  total: number
  byStatus: Record<string, number>
  byCategory: Record<string, number>
  byPriority: Record<string, number>
}

export interface CreateTicket {
  title: string
  description: string
  customerEmail: string
}
