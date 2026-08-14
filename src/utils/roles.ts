import type { RoleCode } from '@/types/api'

/** 后端角色码 → 展示名 */
export const ROLE_LABELS: Record<string, string> = {
  ADMIN: '后端研发',
  EDITOR: '知识管理员',
  MEMBER: '一线运营',
}

/** 角色权限点映射（与后端 RBAC 对齐，前端仅用于菜单/路由显隐，接口仍由后端兜底鉴权） */
export const ROLE_PERMISSIONS: Record<string, string[]> = {
  MEMBER: [],
  EDITOR: ['knowledge:manage', 'feedback:manage', 'analytics:read'],
  ADMIN: ['knowledge:manage', 'feedback:manage', 'analytics:read', 'user:manage'],
}

const ROLE_ORDER: RoleCode[] = ['ADMIN', 'EDITOR', 'MEMBER']

export function primaryRole(roles: string[]): RoleCode {
  for (const code of ROLE_ORDER) {
    if (roles.includes(code)) return code
  }
  return 'MEMBER'
}

export function roleLabel(roles: string[]): string {
  return ROLE_LABELS[primaryRole(roles)] ?? roles[0] ?? '未知角色'
}

export function hasPermission(roles: string[], permission: string): boolean {
  return roles.some((r) => (ROLE_PERMISSIONS[r] ?? []).includes(permission))
}

/** 是否可访问「管理」区块（知识管理/反馈/仪表盘） */
export function isAdminRole(roles: string[]): boolean {
  return roles.some((r) => r === 'ADMIN' || r === 'EDITOR')
}
