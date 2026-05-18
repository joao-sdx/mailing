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
  <h1>CRM</h1>
  <p>Backend health: <code>{healthStatus}</code></p>
</main>

<style>
  main {
    font-family: sans-serif;
    padding: 2rem;
  }

  code {
    background: #f4f4f4;
    padding: 0.2em 0.4em;
    border-radius: 3px;
  }
</style>
