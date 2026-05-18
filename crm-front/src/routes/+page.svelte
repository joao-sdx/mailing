<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  let username = $state('');
  let healthStatus = $state('checking…');

  async function logout() {
    await fetch('/api/logout', { method: 'POST' });
    goto('/login');
  }

  onMount(async () => {
    const meRes = await fetch('/api/me');
    if (meRes.status === 401) {
      goto('/login');
      return;
    }
    const me: { username: string } = await meRes.json();
    username = me.username;

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
  <header>
    <h1>CRM</h1>
    {#if username}
      <span>{username} <button onclick={logout}>Sign out</button></span>
    {/if}
  </header>
  <p>Backend health: <code>{healthStatus}</code></p>
</main>

<style>
  main {
    font-family: sans-serif;
    padding: 2rem;
  }

  header {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-bottom: 1rem;
  }

  h1 {
    margin: 0;
  }

  code {
    background: #f4f4f4;
    padding: 0.2em 0.4em;
    border-radius: 3px;
  }

  button {
    background: none;
    border: 1px solid #ccc;
    border-radius: 4px;
    padding: 0.25em 0.5em;
    cursor: pointer;
    font-size: 0.85rem;
  }
</style>
