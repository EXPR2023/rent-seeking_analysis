<template>
  <section class="panel placeholder-panel">
    <h2>{{ route.meta.title }}</h2>
    <p>{{ route.path }}</p>
  </section>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>
