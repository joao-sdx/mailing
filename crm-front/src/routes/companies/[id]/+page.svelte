<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';

  const rawId = $page.params.id;
  const isNew = rawId === 'new';
  const companyId = isNew ? null : Number(rawId);

  let form = $state({ name: '', website: '', phone: '' });
  let saving = $state(false);
  let deleting = $state(false);
  let error = $state('');

  onMount(async () => {
    if (isNew) return;
    const res = await fetch(`/api/companies/${companyId}`);
    if (res.status === 401) { goto('/login'); return; }
    if (res.ok) {
      const c = await res.json();
      form = { name: c.name ?? '', website: c.website ?? '', phone: c.phone ?? '' };
    }
  });

  async function save(e: SubmitEvent) {
    e.preventDefault();
    saving = true;
    error = '';
    try {
      const res = isNew
        ? await fetch('/api/companies', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form) })
        : await fetch(`/api/companies/${companyId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form) });
      if (res.ok) {
        goto('/companies');
      } else {
        error = `Save failed (${res.status})`;
      }
    } finally {
      saving = false;
    }
  }

  async function remove() {
    if (!confirm('Delete this company? Contacts linked to it will be unlinked.')) return;
    deleting = true;
    const res = await fetch(`/api/companies/${companyId}`, { method: 'DELETE' });
    if (res.ok) {
      goto('/companies');
    } else {
      error = `Delete failed (${res.status})`;
      deleting = false;
    }
  }
</script>

<main>
  <div class="header">
    <h2>{isNew ? 'New company' : 'Edit company'}</h2>
    <button class="btn-ghost" onclick={() => goto('/companies')}>← Back</button>
  </div>

  <form class="card" onsubmit={save}>
    <label>
      Name <span class="req">*</span>
      <input bind:value={form.name} required />
    </label>

    <label>
      Website
      <input type="url" bind:value={form.website} placeholder="https://…" />
    </label>

    <label>
      Phone
      <input type="tel" bind:value={form.phone} />
    </label>

    {#if error}
      <p class="error">{error}</p>
    {/if}

    <div class="footer">
      <button type="submit" class="btn-primary" disabled={saving}>
        {saving ? 'Saving…' : 'Save'}
      </button>
      {#if !isNew}
        <button type="button" class="btn-danger" disabled={deleting} onclick={remove}>
          {deleting ? 'Deleting…' : 'Delete'}
        </button>
      {/if}
    </div>
  </form>
</main>

<style>
  main { padding: 1.5rem; max-width: 480px; }

  .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
  h2 { margin: 0; font-size: 1.25rem; }

  .card { background: #fff; border-radius: 8px; box-shadow: 0 1px 3px #0000001a; padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

  label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.85rem; font-weight: 500; color: #374151; }

  .req { color: #ef4444; }

  input {
    padding: 0.45rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
    font-family: inherit;
  }

  input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 2px #e0e7ff; }

  .footer { display: flex; gap: 0.75rem; padding-top: 0.5rem; }

  .btn-primary { padding: 0.45rem 1.1rem; background: #4f46e5; color: #fff; border: none; border-radius: 6px; font-size: 0.9rem; cursor: pointer; }
  .btn-primary:hover:not(:disabled) { background: #4338ca; }
  .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

  .btn-danger { padding: 0.45rem 1.1rem; background: #fff; color: #dc2626; border: 1px solid #fca5a5; border-radius: 6px; font-size: 0.9rem; cursor: pointer; }
  .btn-danger:hover:not(:disabled) { background: #fef2f2; }
  .btn-danger:disabled { opacity: 0.6; cursor: not-allowed; }

  .btn-ghost { background: none; border: none; color: #6b7280; font-size: 0.9rem; cursor: pointer; padding: 0.3rem 0.5rem; border-radius: 5px; }
  .btn-ghost:hover { background: #f3f4f6; color: #111827; }

  .error { color: #dc2626; font-size: 0.85rem; margin: 0; }
</style>
