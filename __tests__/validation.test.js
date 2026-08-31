import { validateEmail, validatePhone, validateAge, validateRequired, validateForm } from '../src/lib/validation';

describe('validateEmail', () => {
  it('accepts a valid email', () => {
    expect(validateEmail('user@example.com')).toEqual({ valid: true, error: null });
  });

  it('accepts email with subdomains', () => {
    expect(validateEmail('user@mail.example.co.uk')).toEqual({ valid: true, error: null });
  });

  it('rejects empty string', () => {
    expect(validateEmail('')).toEqual({ valid: false, error: "L'email est requis" });
  });

  it('rejects null/undefined', () => {
    expect(validateEmail(null).valid).toBe(false);
    expect(validateEmail(undefined).valid).toBe(false);
  });

  it('rejects email without @', () => {
    expect(validateEmail('userexample.com').valid).toBe(false);
  });

  it('rejects email without domain', () => {
    expect(validateEmail('user@').valid).toBe(false);
  });

  it('rejects email with spaces', () => {
    expect(validateEmail('user @example.com').valid).toBe(false);
  });

  it('trims whitespace before validating', () => {
    expect(validateEmail('  user@example.com  ')).toEqual({ valid: true, error: null });
  });
});

describe('validatePhone', () => {
  it('accepts a valid French phone number', () => {
    expect(validatePhone('0612345678')).toEqual({ valid: true, error: null });
  });

  it('accepts international format with +', () => {
    expect(validatePhone('+33 6 12 34 56 78')).toEqual({ valid: true, error: null });
  });

  it('accepts number with dashes', () => {
    expect(validatePhone('06-12-34-56-78')).toEqual({ valid: true, error: null });
  });

  it('rejects empty string', () => {
    expect(validatePhone('').valid).toBe(false);
  });

  it('rejects too short number', () => {
    expect(validatePhone('12345').valid).toBe(false);
  });

  it('rejects too long number', () => {
    expect(validatePhone('1234567890123456').valid).toBe(false);
  });

  it('rejects letters', () => {
    expect(validatePhone('06-ABC-DEF').valid).toBe(false);
  });

  it('rejects null/undefined', () => {
    expect(validatePhone(null).valid).toBe(false);
    expect(validatePhone(undefined).valid).toBe(false);
  });
});

describe('validateAge', () => {
  it('accepts valid age (25)', () => {
    expect(validateAge(25)).toEqual({ valid: true, error: null });
  });

  it('accepts age as string', () => {
    expect(validateAge('30')).toEqual({ valid: true, error: null });
  });

  it('accepts minimum age (18)', () => {
    expect(validateAge(18)).toEqual({ valid: true, error: null });
  });

  it('accepts maximum age (120)', () => {
    expect(validateAge(120)).toEqual({ valid: true, error: null });
  });

  it('rejects age below 18', () => {
    expect(validateAge(17).valid).toBe(false);
  });

  it('rejects age above 120', () => {
    expect(validateAge(121).valid).toBe(false);
  });

  it('rejects negative age', () => {
    expect(validateAge(-5).valid).toBe(false);
  });

  it('rejects non-integer', () => {
    expect(validateAge(25.5).valid).toBe(false);
  });

  it('rejects empty/null/undefined', () => {
    expect(validateAge('').valid).toBe(false);
    expect(validateAge(null).valid).toBe(false);
    expect(validateAge(undefined).valid).toBe(false);
  });
});

describe('validateRequired', () => {
  it('accepts non-empty string', () => {
    expect(validateRequired('hello', 'Champ')).toEqual({ valid: true, error: null });
  });

  it('rejects empty string', () => {
    expect(validateRequired('', 'Le nom')).toEqual({ valid: false, error: 'Le nom est requis' });
  });

  it('rejects whitespace-only string', () => {
    expect(validateRequired('   ', 'Le nom').valid).toBe(false);
  });

  it('rejects null/undefined', () => {
    expect(validateRequired(null, 'Champ').valid).toBe(false);
    expect(validateRequired(undefined, 'Champ').valid).toBe(false);
  });
});

describe('validateForm', () => {
  const validData = {
    nom: 'Jean Dupont',
    email: 'jean@example.com',
    telephone: '0612345678',
    genre: 'Homme',
    age: 28,
    profession: 'Ingénieur',
    chambre: 'Chambre 2',
    message: 'Bonjour',
  };

  it('accepts valid complete form', () => {
    expect(validateForm(validData)).toEqual({ valid: true, errors: {} });
  });

  it('message is optional', () => {
    const data = { ...validData, message: '' };
    expect(validateForm(data)).toEqual({ valid: true, errors: {} });
  });

  it('returns all errors for empty form', () => {
    const result = validateForm({
      nom: '', email: '', telephone: '', genre: '',
      age: '', profession: '', chambre: '', message: '',
    });
    expect(result.valid).toBe(false);
    expect(Object.keys(result.errors)).toEqual(
      expect.arrayContaining(['nom', 'email', 'telephone', 'genre', 'age', 'profession', 'chambre'])
    );
  });

  it('returns specific error for invalid email only', () => {
    const data = { ...validData, email: 'invalid' };
    const result = validateForm(data);
    expect(result.valid).toBe(false);
    expect(result.errors.email).toBeDefined();
    expect(result.errors.nom).toBeUndefined();
  });

  it('returns specific error for underage', () => {
    const data = { ...validData, age: 15 };
    const result = validateForm(data);
    expect(result.valid).toBe(false);
    expect(result.errors.age).toContain('18');
  });
});
