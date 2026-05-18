<script lang="ts">
  import { onMount } from 'svelte';
  import { goto, afterNavigate } from '$app/navigation';
  import { page } from '$app/stores';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  let username = $state('');
  let ready    = $state(false);
  let fetching = false;

  const navLinks = [
    { href: '/',          label: 'Dashboard' },
    { href: '/contacts',  label: 'Contacts'  },
    { href: '/companies', label: 'Companies' },
  ];

  function isActive(href: string) {
    const p = $page.url.pathname;
    return href === '/' ? p === '/' : p.startsWith(href);
  }

  async function fetchUser() {
    if (fetching) return;
    fetching = true;
    try {
      const res = await fetch('/api/me');
      if (res.status === 401) { goto('/login'); return; }
      const me: { username: string } = await res.json();
      username = me.username;
      ready = true;
    } finally {
      fetching = false;
    }
  }

  async function logout() {
    await fetch('/api/logout', { method: 'POST' });
    username = '';
    goto('/login');
  }

  // Handles direct page loads (hard refresh, first visit).
  onMount(async () => {
    if ($page.url.pathname === '/login') { ready = true; return; }
    await fetchUser();
  });

  // Handles all client-side navigations (login→app, logout→login, internal).
  // The fetching flag prevents double-fetch when afterNavigate fires
  // concurrently with an in-progress onMount call.
  afterNavigate(async () => {
    if ($page.url.pathname === '/login') { ready = true; return; }
    if (!username) await fetchUser();
  });
</script>

<!--
  $page.url.pathname used directly in the template — store subscriptions
  are always reactive here. $derived wrapping a store is not reliable
  after client-side navigation in Svelte 5 runes mode.
-->
{#if ready}
  {#if $page.url.pathname === '/login'}

    {@render children()}

  {:else}

    <div class="shell">

      <header class="topbar">
        <span class="brand">CRM</span>
        <span class="user-info">
          <span class="user-name">{username}</span>
          <button onclick={logout}>Sign out</button>
        </span>
      </header>

      <nav class="sidebar">
        {#each navLinks as link}
          <a href={link.href} class:active={isActive(link.href)}>{link.label}</a>
        {/each}
      </nav>

      <main>
        {@render children()}
      </main>

      <footer class="footer">
        <span>CRM &copy; {new Date().getFullYear()}</span>
      </footer>

    </div>

  {/if}
{/if}

<style>
  :global(*, *::before, *::after) { box-sizing: border-box; }
  :global(body) { margin: 0; font-family: system-ui, sans-serif; color: #111827; }

  /* ── Shell ────────────────────────────────────── */
  .shell {
    display: grid;
    grid-template-rows: 64px 1fr 48px;
    grid-template-columns: 260px 1fr;
    grid-template-areas:
      "topbar  topbar"
      "sidebar main"
      "footer  footer";
    height: 100dvh;
  }

  /* ── Top bar ──────────────────────────────────── */
  .topbar {
    grid-area: topbar;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 2rem 0 1.5rem;
    background: #1e293b;
    color: #f1f5f9;
    box-shadow: 0 1px 4px #0003;
    z-index: 10;
  }

  .brand {
    font-size: 1.1rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #fff;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 1rem;
    font-size: 0.9rem;
  }

  .user-name { color: #94a3b8; }

  .topbar button {
    background: transparent;
    border: 1px solid #475569;
    border-radius: 6px;
    padding: 0.3em 0.9em;
    color: #cbd5e1;
    font-size: 0.85rem;
    cursor: pointer;
    transition: background 0.15s;
  }

  .topbar button:hover { background: #334155; }

  /* ── Sidebar ──────────────────────────────────── */
  .sidebar {
    grid-area: sidebar;
    background: #f8fafc;
    border-right: 1px solid #e2e8f0;
    padding: 1.5rem 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    overflow-y: auto;
  }

  .sidebar a {
    display: block;
    padding: 0.7rem 1.1rem;
    border-radius: 8px;
    text-decoration: none;
    font-size: 0.95rem;
    font-weight: 500;
    color: #475569;
    transition: background 0.12s, color 0.12s;
  }

  .sidebar a:hover { background: #e2e8f0; color: #0f172a; }

  .sidebar a.active {
    background: #eff6ff;
    color: #1d4ed8;
    border-left: 3px solid #1d4ed8;
    padding-left: calc(1.1rem - 3px);
  }

  /* ── Main content ─────────────────────────────── */
  main {
    grid-area: main;
    background: #f1f5f9;
    overflow-y: auto;
  }

  /* ── Footer ───────────────────────────────────── */
  .footer {
    grid-area: footer;
    display: flex;
    align-items: center;
    padding: 0 1.5rem;
    background: #f8fafc;
    border-top: 1px solid #e2e8f0;
    font-size: 0.8rem;
    color: #94a3b8;
  }
</style>
