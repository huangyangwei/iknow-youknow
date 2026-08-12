<script setup lang="ts">import { RouterView } from 'vue-router';</script>
<template><main class="auth"><section><div class="brand">iKnow · youKnow</div><RouterView /></section></main></template>
<style scoped>.auth{min-height:100vh;display:grid;place-items:center;background:linear-gradient(135deg,#eff8ff,#f9f5ff)}section{width:min(420px,calc(100vw - 36px));background:#fff;padding:36px;border-radius:16px;box-shadow:0 16px 48px #10182818}.brand{color:#175cd3;margin-bottom:28px}</style>
