/**
 * Validation functions for ThePlace form submissions.
 */

export function validateEmail(email) {
  if (!email || typeof email !== 'string') return { valid: false, error: 'L\'email est requis' };
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!re.test(email.trim())) return { valid: false, error: 'Format d\'email invalide' };
  return { valid: true, error: null };
}

export function validatePhone(telephone) {
  if (!telephone || typeof telephone !== 'string') return { valid: false, error: 'Le téléphone est requis' };
  const cleaned = telephone.replace(/[\s\-().+]/g, '');
  if (!/^\d{8,15}$/.test(cleaned)) return { valid: false, error: 'Numéro de téléphone invalide (8-15 chiffres)' };
  return { valid: true, error: null };
}

export function validateAge(age) {
  const n = Number(age);
  if (!age && age !== 0) return { valid: false, error: 'L\'âge est requis' };
  if (!Number.isInteger(n) || n < 18 || n > 120) return { valid: false, error: 'L\'âge doit être entre 18 et 120' };
  return { valid: true, error: null };
}

export function validateRequired(value, fieldName) {
  if (!value || (typeof value === 'string' && !value.trim())) {
    return { valid: false, error: `${fieldName} est requis` };
  }
  return { valid: true, error: null };
}

export function validateForm({ nom, email, telephone, genre, age, profession, chambre, message }) {
  const errors = {};

  const nomResult = validateRequired(nom, 'Le nom');
  if (!nomResult.valid) errors.nom = nomResult.error;

  const emailResult = validateEmail(email);
  if (!emailResult.valid) errors.email = emailResult.error;

  const phoneResult = validatePhone(telephone);
  if (!phoneResult.valid) errors.telephone = phoneResult.error;

  const genreResult = validateRequired(genre, 'Le genre');
  if (!genreResult.valid) errors.genre = genreResult.error;

  const ageResult = validateAge(age);
  if (!ageResult.valid) errors.age = ageResult.error;

  const professionResult = validateRequired(profession, 'La profession');
  if (!professionResult.valid) errors.profession = professionResult.error;

  const chambreResult = validateRequired(chambre, 'La chambre');
  if (!chambreResult.valid) errors.chambre = chambreResult.error;

  return {
    valid: Object.keys(errors).length === 0,
    errors,
  };
}
