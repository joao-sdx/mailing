<script lang="ts">
  import { onMount } from 'svelte';

  let healthStatus = $state('checking…');

  onMount(async () => {
    try {
      const res = await fetch('/api/health');
      const data: { status: string } = await res.json();
      healthStatus = res.ok ? `ok — ${data.status}` : `error ${res.status}`;
    } catch {
      healthStatus = 'unreachable';
    }
  });
</script>

<main>
  <h2>Dashboard</h2>
  <p>Backend: <code>{healthStatus}</code></p>
</main>

<style>
  main { padding: 2rem 1.5rem; }
  code { background: #f3f4f6; padding: 0.15em 0.4em; border-radius: 4px; font-size: 0.9em; }
</style>
