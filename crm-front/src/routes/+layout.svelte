<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  let username = $state('');
  let ready = $state(false);

  async function logout() {
    await fetch('/api/logout', { method: 'POST' });
    username = '';
    goto('/login');
  }

  onMount(async () => {
    if ($page.url.pathname === '/login') {
      ready = true;
      return;
    }
    const res = await fetch('/api/me');
    if (res.status === 401) {
      goto('/login');
      return;
    }
    const me: { username: string } = await res.json();
    username = me.username;
    ready = true;
  });
</script>

{#if ready}
  {#if username}
    <header>
      <nav>
        <a href="/" class:active={$page.url.pathname === '/'}>CRM</a>
        <a href="/contacts" class:active={$page.url.pathname.startsWith('/contacts')}>Contacts</a>
        <a href="/companies" class:active={$page.url.pathname.startsWith('/companies')}>Companies</a>
      </nav>
      <span class="user">{username} <button onclick={logout}>Sign out</button></span>
    </header>
  {/if}
  {@render children()}
{/if}

<style>
  :global(*, *::before, *::after) { box-sizing: border-box; }
  :global(body) { margin: 0; font-family: system-ui, sans-serif; background: #f9fafb; color: #111827; }

  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 1.5rem;
    height: 52px;
    background: #fff;
    border-bottom: 1px solid #e5e7eb;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  nav { display: flex; gap: 0.25rem; }

  nav a {
    text-decoration: none;
    color: #6b7280;
    font-size: 0.9rem;
    font-weight: 500;
    padding: 0.35rem 0.75rem;
    border-radius: 6px;
    transition: background 0.15s, color 0.15s;
  }

  nav a:hover { background: #f3f4f6; color: #111827; }
  nav a.active { background: #eff6ff; color: #1d4ed8; }

  .user { font-size: 0.85rem; color: #6b7280; display: flex; align-items: center; gap: 0.5rem; }

  button {
    background: none;
    border: 1px solid #d1d5db;
    border-radius: 5px;
    padding: 0.2em 0.6em;
    cursor: pointer;
    font-size: 0.8rem;
    color: #374151;
  }

  button:hover { background: #f3f4f6; }
</style>
