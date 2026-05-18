<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';

  interface Company { id: number; name: string; }

  const rawId = $page.params.id;
  const isNew = rawId === 'new';
  const contactId = isNew ? null : Number(rawId);

  let form = $state({ firstName: '', lastName: '', email: '', phone: '', jobTitle: '', companyId: '' });
  let companies = $state<Company[]>([]);
  let saving = $state(false);
  let deleting = $state(false);
  let error = $state('');

  onMount(async () => {
    const [companiesRes, contactRes] = await Promise.all([
      fetch('/api/companies?size=200'),
      isNew ? Promise.resolve(null) : fetch(`/api/contacts/${contactId}`)
    ]);

    if (companiesRes.status === 401) { goto('/login'); return; }
    const companyData = await companiesRes.json();
    companies = companyData.content;

    if (contactRes) {
      if (contactRes.status === 401) { goto('/login'); return; }
      const c = await contactRes.json();
      form = {
        firstName: c.firstName ?? '',
        lastName: c.lastName ?? '',
        email: c.email ?? '',
        phone: c.phone ?? '',
        jobTitle: c.jobTitle ?? '',
        companyId: c.company?.id?.toString() ?? ''
      };
    }
  });

  async function save(e: SubmitEvent) {
    e.preventDefault();
    saving = true;
    error = '';
    try {
      const body = { ...form, companyId: form.companyId ? Number(form.companyId) : null };
      const res = isNew
        ? await fetch('/api/contacts', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
        : await fetch(`/api/contacts/${contactId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      if (res.ok) {
        goto('/contacts');
      } else {
        error = `Save failed (${res.status})`;
      }
    } finally {
      saving = false;
    }
  }

  async function remove() {
    if (!confirm('Delete this contact?')) return;
    deleting = true;
    const res = await fetch(`/api/contacts/${contactId}`, { method: 'DELETE' });
    if (res.ok) {
      goto('/contacts');
    } else {
      error = `Delete failed (${res.status})`;
      deleting = false;
    }
  }
</script>

<main>
  <div class="header">
    <h2>{isNew ? 'New contact' : 'Edit contact'}</h2>
    <button class="btn-ghost" onclick={() => goto('/contacts')}>← Back</button>
  </div>

  <form class="card" onsubmit={save}>
    <div class="row">
      <label>
        First name <span class="req">*</span>
        <input bind:value={form.firstName} required />
      </label>
      <label>
        Last name <span class="req">*</span>
        <input bind:value={form.lastName} required />
      </label>
    </div>

    <div class="row">
      <label>
        Email
        <input type="email" bind:value={form.email} />
      </label>
      <label>
        Phone
        <input type="tel" bind:value={form.phone} />
      </label>
    </div>

    <label>
      Job title
      <input bind:value={form.jobTitle} />
    </label>

    <label>
      Company
      <select bind:value={form.companyId}>
        <option value="">— none —</option>
        {#each companies as c}
          <option value={c.id.toString()}>{c.name}</option>
        {/each}
      </select>
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
  main { padding: 1.5rem; max-width: 680px; }

  .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
  h2 { margin: 0; font-size: 1.25rem; }

  .card { background: #fff; border-radius: 8px; box-shadow: 0 1px 3px #0000001a; padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }

  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }

  label { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.85rem; font-weight: 500; color: #374151; }

  .req { color: #ef4444; }

  input, select {
    padding: 0.45rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 0.9rem;
    font-family: inherit;
    background: #fff;
  }

  input:focus, select:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 2px #e0e7ff; }

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
