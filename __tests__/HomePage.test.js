import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import HomePage from '../src/pages/index';

// Mock external dependencies
jest.mock('@emailjs/browser', () => ({
  send: jest.fn(),
}));

jest.mock('../src/lib/supabase', () => ({
  supabase: {
    from: jest.fn(() => ({
      insert: jest.fn(() => Promise.resolve({ error: null })),
    })),
  },
}));

const emailjs = require('@emailjs/browser');
const { supabase } = require('../src/lib/supabase');

const validFormData = {
  nom: 'Jean Dupont',
  email: 'jean@example.com',
  telephone: '0612345678',
  genre: 'Homme',
  age: '28',
  profession: 'Ingénieur',
  chambre: 'Chambre 2',
  message: 'Bonjour',
};

async function fillForm(user) {
  await user.type(screen.getByLabelText(/nom/i), validFormData.nom);
  await user.type(screen.getByLabelText(/email/i), validFormData.email);
  await user.type(screen.getByLabelText(/telephone/i), validFormData.telephone);
  await user.type(screen.getByLabelText(/genre/i), validFormData.genre);
  await user.type(screen.getByLabelText(/^age$/i), validFormData.age);
  await user.type(screen.getByLabelText(/profession/i), validFormData.profession);
  await user.type(screen.getByLabelText(/chambre/i), validFormData.chambre);
  await user.type(screen.getByLabelText(/message/i), validFormData.message);
}

describe('HomePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    emailjs.send.mockResolvedValue({ status: 200 });
    supabase.from.mockReturnValue({
      insert: jest.fn().mockResolvedValue({ error: null }),
    });
  });

  describe('Rendering', () => {
    it('renders the form with all fields', () => {
      render(<HomePage />);
      expect(screen.getByText(/ThePlace/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/nom/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/telephone/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/genre/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^age$/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/profession/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/chambre/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /envoyer/i })).toBeInTheDocument();
    });

    it('submit button is not disabled initially', () => {
      render(<HomePage />);
      expect(screen.getByRole('button', { name: /envoyer/i })).not.toBeDisabled();
    });
  });

  describe('Validation', () => {
    it('shows validation errors when submitting empty form', async () => {
      const user = userEvent.setup();
      render(<HomePage />);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      const alerts = screen.getAllByRole('alert');
      expect(alerts.length).toBeGreaterThan(0);
    });

    it('does not call emailjs when form is invalid', async () => {
      const user = userEvent.setup();
      render(<HomePage />);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      expect(emailjs.send).not.toHaveBeenCalled();
      expect(supabase.from).not.toHaveBeenCalled();
    });

    it('clears field error when user types in that field', async () => {
      const user = userEvent.setup();
      render(<HomePage />);

      // Submit empty to trigger errors
      await user.click(screen.getByRole('button', { name: /envoyer/i }));
      expect(screen.getByText(/nom est requis/i)).toBeInTheDocument();

      // Type in the name field
      await user.type(screen.getByLabelText(/nom/i), 'Jean');

      // Error should be cleared
      expect(screen.queryByText(/nom est requis/i)).not.toBeInTheDocument();
    });
  });

  describe('Successful submission', () => {
    it('calls Supabase and EmailJS with form data', async () => {
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      await waitFor(() => {
        expect(supabase.from).toHaveBeenCalledWith('submissions');
        expect(emailjs.send).toHaveBeenCalledWith(
          expect.any(String),
          expect.any(String),
          expect.objectContaining({ nom: 'Jean Dupont', email: 'jean@example.com' }),
          expect.any(String)
        );
      });
    });

    it('shows success message after submission', async () => {
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      await waitFor(() => {
        expect(screen.getByText(/merci/i)).toBeInTheDocument();
        expect(screen.getByText(/succès/i)).toBeInTheDocument();
      });
    });

    it('disables button during submission', async () => {
      emailjs.send.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      expect(screen.getByRole('button', { name: /envoi en cours/i })).toBeDisabled();
    });
  });

  describe('Error handling', () => {
    it('shows error message when EmailJS fails', async () => {
      emailjs.send.mockRejectedValue(new Error('Network error'));
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      await waitFor(() => {
        expect(screen.getByText(/échec/i)).toBeInTheDocument();
      });
    });

    it('shows error message when Supabase fails', async () => {
      supabase.from.mockReturnValue({
        insert: jest.fn().mockResolvedValue({ error: { message: 'DB error' } }),
      });
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      await waitFor(() => {
        expect(screen.getByText(/échec/i)).toBeInTheDocument();
      });
    });

    it('re-enables button after failed submission', async () => {
      emailjs.send.mockRejectedValue(new Error('fail'));
      const user = userEvent.setup();
      render(<HomePage />);
      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /envoyer/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /envoyer/i })).not.toBeDisabled();
      });
    });
  });
});
