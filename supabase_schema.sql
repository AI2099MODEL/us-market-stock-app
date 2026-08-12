-- Run this script in your Supabase SQL Editor

-- 1. Create Virtual Trades Table
CREATE TABLE public.virtual_trades (
    id SERIAL PRIMARY KEY,
    ticker TEXT NOT NULL,
    name TEXT NOT NULL,
    entry_price NUMERIC NOT NULL,
    current_price NUMERIC NOT NULL,
    entry_time BIGINT NOT NULL,
    exit_time BIGINT,
    status TEXT NOT NULL,
    target_price NUMERIC NOT NULL,
    stop_loss NUMERIC NOT NULL,
    trailing_sl_threshold NUMERIC NOT NULL,
    highest_price NUMERIC NOT NULL,
    profit_percent NUMERIC NOT NULL,
    profit_amount NUMERIC NOT NULL,
    is_partial_booked BOOLEAN NOT NULL DEFAULT FALSE,
    allocated_amount NUMERIC NOT NULL,
    is_btst BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. Create Scanned Breakouts Table
CREATE TABLE public.scanned_breakouts (
    ticker TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price NUMERIC NOT NULL,
    strategies TEXT NOT NULL,
    score INTEGER NOT NULL,
    reasons TEXT NOT NULL,
    signal_strength TEXT NOT NULL,
    stop_loss NUMERIC,
    target1 NUMERIC,
    target2 NUMERIC,
    historical_prices TEXT, -- Stored as JSON or comma-separated
    previous_close NUMERIC,
    open_price NUMERIC,
    change NUMERIC NOT NULL DEFAULT 0.0,
    change_percent NUMERIC NOT NULL DEFAULT 0.0,
    is_btst BOOLEAN NOT NULL DEFAULT FALSE,
    asset_type TEXT NOT NULL DEFAULT 'COMMODITY'
);

-- 3. Set Row Level Security (RLS) policies
ALTER TABLE public.virtual_trades ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scanned_breakouts ENABLE ROW LEVEL SECURITY;

-- Allow anonymous read/write for demo purposes
CREATE POLICY "Allow public select on virtual_trades" ON public.virtual_trades FOR SELECT USING (true);
CREATE POLICY "Allow public insert on virtual_trades" ON public.virtual_trades FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on virtual_trades" ON public.virtual_trades FOR UPDATE USING (true);
CREATE POLICY "Allow public delete on virtual_trades" ON public.virtual_trades FOR DELETE USING (true);

CREATE POLICY "Allow public select on scanned_breakouts" ON public.scanned_breakouts FOR SELECT USING (true);
CREATE POLICY "Allow public insert on scanned_breakouts" ON public.scanned_breakouts FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow public update on scanned_breakouts" ON public.scanned_breakouts FOR UPDATE USING (true);
CREATE POLICY "Allow public delete on scanned_breakouts" ON public.scanned_breakouts FOR DELETE USING (true);
