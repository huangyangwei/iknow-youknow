<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <main class="auth">
    <div class="auth-brand">
      <div class="logo">K</div>
      <span>KnowledgeHub</span>
    </div>
    <section>
      <RouterView />
    </section>
  </main>
</template>

<style scoped>
.auth {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 28px;
  background: linear-gradient(160deg, var(--primary-subtle) 0%, var(--surface-0) 45%, var(--accent-light) 100%);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-brand .logo {
  width: 40px;
  height: 40px;
  background: var(--primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 20px;
}

.auth-brand span {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1.375rem;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

section {
  width: min(420px, calc(100vw - 36px));
  background: #fff;
  padding: 36px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-light);
}
</style>
