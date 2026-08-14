// @wangeditor/editor-for-vue 未通过 package.json "exports" 正确暴露类型，这里补模块声明
declare module '@wangeditor/editor-for-vue' {
  import type { Component } from 'vue'

  export const Editor: Component
  export const Toolbar: Component
}
