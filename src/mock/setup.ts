/**
 * 条件启用 mock：
 * - 开发环境默认开启（.env.development VITE_USE_MOCK=true），后端就绪后可置 false 联调
 * - 生产构建默认关闭（.env.production VITE_USE_MOCK=false）
 */
export function setupMocks(): void {
  const enabled = import.meta.env.VITE_USE_MOCK !== 'false'
  if (enabled) {
    // 动态引入，确保生产构建可被 tree-shake 掉
    void import('./index').then(({ setupMock }) => setupMock())
  }
}
