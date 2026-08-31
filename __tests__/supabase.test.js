describe('Supabase client', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('creates client when SUPABASE_KEY is set', () => {
    process.env.NEXT_PUBLIC_SUPABASE_KEY = 'test-key-123';
    const { supabase } = require('../src/lib/supabase');
    expect(supabase).not.toBeNull();
  });

  it('returns null when SUPABASE_KEY is missing', () => {
    delete process.env.NEXT_PUBLIC_SUPABASE_KEY;
    const { supabase } = require('../src/lib/supabase');
    expect(supabase).toBeNull();
  });

  it('logs warning when SUPABASE_KEY is missing', () => {
    delete process.env.NEXT_PUBLIC_SUPABASE_KEY;
    const spy = jest.spyOn(console, 'warn').mockImplementation();
    require('../src/lib/supabase');
    expect(spy).toHaveBeenCalledWith(expect.stringContaining('SUPABASE_KEY'));
    spy.mockRestore();
  });
});
