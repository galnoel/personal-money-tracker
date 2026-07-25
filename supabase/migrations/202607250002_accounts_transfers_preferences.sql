-- Financial accounts, transfers and synchronized presentation preferences.
-- Run after 202607250001_create_transactions.sql.
create extension if not exists pgcrypto;

create table if not exists public.payment_accounts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null check (length(trim(name)) between 1 and 60),
    opening_balance bigint not null default 0,
    sort_order integer not null default 0,
    archived boolean not null default false,
    created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint),
    updated_at bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

create unique index if not exists payment_accounts_user_name_ci
    on public.payment_accounts (user_id, lower(name));
create index if not exists payment_accounts_user_sort
    on public.payment_accounts (user_id, archived, sort_order);

alter table public.transactions
    add column if not exists client_id uuid,
    add column if not exists account_id uuid references public.payment_accounts(id) on delete restrict,
    add column if not exists updated_at bigint;

update public.transactions
set client_id = gen_random_uuid()
where client_id is null;

update public.transactions
set updated_at = coalesce(created_at, (extract(epoch from now()) * 1000)::bigint)
where updated_at is null;

alter table public.transactions alter column client_id set default gen_random_uuid();
alter table public.transactions alter column client_id set not null;
alter table public.transactions alter column updated_at
    set default ((extract(epoch from now()) * 1000)::bigint);
alter table public.transactions alter column updated_at set not null;
create unique index if not exists transactions_user_client_id
    on public.transactions(user_id, client_id);
create index if not exists transactions_user_account
    on public.transactions(user_id, account_id);

-- One account per case-insensitive legacy payment method.
insert into public.payment_accounts (user_id, name, sort_order)
select user_id, min(trim(payment_method)),
       row_number() over (partition by user_id order by lower(min(trim(payment_method)))) - 1
from public.transactions
where trim(coalesce(payment_method, '')) <> ''
group by user_id, lower(trim(payment_method))
on conflict do nothing;

update public.transactions t
set account_id = a.id
from public.payment_accounts a
where t.account_id is null
  and a.user_id = t.user_id
  and lower(a.name) = lower(trim(t.payment_method));

create table if not exists public.transfers (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    source_account_id uuid not null references public.payment_accounts(id) on delete restrict,
    destination_account_id uuid not null references public.payment_accounts(id) on delete restrict,
    amount bigint not null check (amount > 0),
    description text not null default '',
    date bigint not null,
    created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint),
    updated_at bigint not null default ((extract(epoch from now()) * 1000)::bigint),
    check (source_account_id <> destination_account_id)
);
create index if not exists transfers_user_date on public.transfers(user_id, date desc);

create table if not exists public.user_preferences (
    user_id uuid primary key references auth.users(id) on delete cascade,
    accent_hex text not null default '#4F46E5'
        check (accent_hex ~ '^#[0-9A-Fa-f]{6}$'),
    currency_code text not null default 'SGD'
        check (currency_code ~ '^[A-Z]{3}$'),
    updated_at bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

alter table public.payment_accounts enable row level security;
alter table public.transfers enable row level security;
alter table public.user_preferences enable row level security;

drop policy if exists "Users manage own payment accounts" on public.payment_accounts;
create policy "Users manage own payment accounts" on public.payment_accounts
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "Users manage own transfers" on public.transfers;
create policy "Users manage own transfers" on public.transfers
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
drop policy if exists "Users manage own preferences" on public.user_preferences;
create policy "Users manage own preferences" on public.user_preferences
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

grant select, insert, update, delete on public.payment_accounts to authenticated;
grant select, insert, update, delete on public.transfers to authenticated;
grant select, insert, update, delete on public.user_preferences to authenticated;

-- Existing users without history are seeded by the app on first authenticated load.
