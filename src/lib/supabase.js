import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://iccnnypgksrckyougufw.supabase.co';
const supabaseKey = process.env.NEXT_PUBLIC_SUPABASE_KEY;

if (!supabaseKey) {
  console.warn('NEXT_PUBLIC_SUPABASE_KEY is not set');
}

export const supabase = supabaseKey ? createClient(supabaseUrl, supabaseKey) : null;
