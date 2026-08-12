export interface ApiPage<T> { items: T[]; total: number }
export interface SearchResult { id: string; title: string; excerpt: string; score: number; updatedAt: string; tags: string[] }
export interface Citation { id: string; title: string; content: string; source?: string }
export interface ChatMessage { id: string; role: 'user' | 'assistant'; content: string; citations?: Citation[]; isStreaming?: boolean }
export interface KnowledgeItem { id: string; title: string; status: 'published' | 'draft'; chunks: number; updatedAt: string }
export interface FeedbackItem { id: string; question: string; rating: 'up' | 'down'; comment?: string; createdAt: string }
