<script lang="ts">
  import { onMount } from 'svelte';
  import { goto, afterNavigate } from '$app/navigation';
  import { page } from '$app/stores';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  let username = $state('');
  let ready = $state(false);

  const isLoginPage = $derived($page.url.pathname === '/login');

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
    const res = await fetch('/api/me');
    if (res.status === 401) { goto('/login'); return; }
    const me: { username: string } = await res.json();
    username = me.username;
    ready = true;
  }

  async function logout() {
    await fetch('/api/logout', { method: 'POST' });
    username = '';
    goto('/login');
  }

  onMount(async () => {
    if ($page.url.pathname === '/login') { ready = true; return; }
    await fetchUser();
  });

  // onMount does not re-run after client-side navigation; afterNavigate
  // catches the login → app transition and fetches the user for the first time.
  afterNavigate(async ({ from }) => {
    if (from?.url.pathname === '/login' && !username) {
      await fetchUser();
    }
  });
</script>

{#if ready}
  {#if isLoginPage}
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

      <div class="body">
        <nav class="sidebar">
          {#each navLinks as link}
            <a href={link.href} class:active={isActive(link.href)}>{link.label}</a>
          {/each}
        </nav>

        <main>
          {@render children()}
        </main>
      </div>

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
    grid-template-rows: 52px 1fr 40px;
    grid-template-columns: 220px 1fr;
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
    padding: 0 1.25rem 0 1rem;
    background: #1e293b;
    color: #f1f5f9;
    box-shadow: 0 1px 4px #0003;
    z-index: 10;
  }

  .brand {
    font-size: 1rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #fff;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    font-size: 0.85rem;
  }

  .user-name { color: #94a3b8; }

  .topbar button {
    background: transparent;
    border: 1px solid #475569;
    border-radius: 5px;
    padding: 0.2em 0.65em;
    color: #cbd5e1;
    font-size: 0.8rem;
    cursor: pointer;
    transition: background 0.15s;
  }

  .topbar button:hover { background: #334155; }

  /* ── Sidebar ──────────────────────────────────── */
  .sidebar {
    grid-area: sidebar;
    background: #f8fafc;
    border-right: 1px solid #e2e8f0;
    padding: 1rem 0.75rem;
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    overflow-y: auto;
  }

  .sidebar a {
    display: block;
    padding: 0.5rem 0.85rem;
    border-radius: 7px;
    text-decoration: none;
    font-size: 0.9rem;
    font-weight: 500;
    color: #475569;
    transition: background 0.12s, color 0.12s;
  }

  .sidebar a:hover { background: #e2e8f0; color: #0f172a; }

  .sidebar a.active {
    background: #eff6ff;
    color: #1d4ed8;
    border-left: 3px solid #1d4ed8;
    padding-left: calc(0.85rem - 3px);
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
    padding: 0 1.25rem;
    background: #f8fafc;
    border-top: 1px solid #e2e8f0;
    font-size: 0.75rem;
    color: #94a3b8;
  }
</style>
