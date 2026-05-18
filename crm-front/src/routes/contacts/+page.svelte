<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  interface CompanyRef { id: number; name: string; }
  interface Contact { id: number; firstName: string; lastName: string; email: string; phone: string; jobTitle: string; company: CompanyRef | null; }

  let contacts = $state<Contact[]>([]);
  let total = $state(0);
  let q = $state('');
  let loading = $state(false);
  let timer: ReturnType<typeof setTimeout>;

  async function load() {
    loading = true;
    try {
      const res = await fetch(`/api/contacts?q=${encodeURIComponent(q)}&size=50`);
      if (res.status === 401) { goto('/login'); return; }
      const data = await res.json();
      contacts = data.content;
      total = data.totalElements;
    } finally {
      loading = false;
    }
  }

  function onInput() {
    clearTimeout(timer);
    timer = setTimeout(load, 300);
  }

  onMount(load);
</script>

<main>
  <div class="toolbar">
    <h2>Contacts <span class="badge">{total}</span></h2>
    <div class="actions">
      <input placeholder="Search name or email…" bind:value={q} oninput={onInput} />
      <button class="btn-primary" onclick={() => goto('/contacts/new')}>+ New contact</button>
    </div>
  </div>

  {#if loading}
    <p class="hint">Loading…</p>
  {:else if contacts.length === 0}
    <p class="hint">No contacts found.</p>
  {:else}
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Job title</th>
          <th>Company</th>
          <th>Email</th>
          <th>Phone</th>
        </tr>
      </thead>
      <tbody>
        {#each contacts as c}
          <tr onclick={() => goto(`/contacts/${c.id}`)}>
            <td class="name">{c.firstName} {c.lastName}</td>
            <td>{c.jobTitle ?? '—'}</td>
            <td>{c.company?.name ?? '—'}</td>
            <td>{c.email ?? '—'}</td>
            <td>{c.phone ?? '—'}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</main>

<style>
  main { padding: 1.5rem; }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.25rem;
    flex-wrap: wrap;
    gap: 0.75rem;
  }

  h2 { margin: 0; font-size: 1.25rem; display: flex; align-items: center; gap: 0.5rem; }

  .badge {
    background: #e5e7eb;
    color: #374151;
    font-size: 0.75rem;
    font-weight: 600;
    padding: 0.15em 0.55em;
    border-radius: 999px;
  }

  .actions { display: flex; gap: 0.5rem; }

  input {
    padding: 0.4rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
    width: 240px;
  }

  input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 2px #e0e7ff; }

  .btn-primary {
    padding: 0.4rem 0.9rem;
    background: #4f46e5;
    color: #fff;
    border: none;
    border-radius: 6px;
    font-size: 0.9rem;
    cursor: pointer;
    white-space: nowrap;
  }

  .btn-primary:hover { background: #4338ca; }

  .hint { color: #6b7280; padding: 1rem 0; }

  table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px #0000001a; }

  th {
    text-align: left;
    padding: 0.65rem 1rem;
    font-size: 0.8rem;
    font-weight: 600;
    color: #6b7280;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    border-bottom: 1px solid #e5e7eb;
    background: #f9fafb;
  }

  td { padding: 0.75rem 1rem; font-size: 0.9rem; border-bottom: 1px solid #f3f4f6; }

  tbody tr { cursor: pointer; }
  tbody tr:hover td { background: #f5f3ff; }
  tbody tr:last-child td { border-bottom: none; }

  .name { font-weight: 500; color: #1d4ed8; }
</style>
