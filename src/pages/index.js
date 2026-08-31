import React, { useState } from "react";
import { supabase } from "@/lib/supabase";
import emailjs from "@emailjs/browser";
import { validateForm } from "@/lib/validation";

const EMAILJS_SERVICE = process.env.NEXT_PUBLIC_EMAILJS_SERVICE || "service_msvkaw5";
const EMAILJS_TEMPLATE = process.env.NEXT_PUBLIC_EMAILJS_TEMPLATE || "template_6jimt3k";
const EMAILJS_KEY = process.env.NEXT_PUBLIC_EMAILJS_KEY || "1pUvYhxBcUC3NbH2k";

export default function HomePage() {
  const [formData, setFormData] = useState({
    nom: "", email: "", telephone: "", genre: "",
    age: "", profession: "", chambre: "", message: "",
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    setErrors((prev) => ({ ...prev, [e.target.name]: undefined }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validation = validateForm(formData);
    if (!validation.valid) {
      setErrors(validation.errors);
      return;
    }

    setSubmitting(true);
    try {
      if (supabase) {
        const { error } = await supabase.from("submissions").insert([formData]);
        if (error) throw new Error(`Supabase: ${error.message}`);
      }

      await emailjs.send(EMAILJS_SERVICE, EMAILJS_TEMPLATE, formData, EMAILJS_KEY);
      setSubmitted(true);
    } catch (err) {
      console.error("Erreur d'envoi :", err);
      setErrors({ submit: "Échec de l'envoi. Veuillez réessayer." });
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <main style={{ padding: 50, fontFamily: "sans-serif" }}>
        <h1>Merci !</h1>
        <p>Votre demande a été envoyée avec succès.</p>
      </main>
    );
  }

  return (
    <main style={{ padding: 50, fontFamily: "sans-serif" }}>
      <h1>ThePlace – Formulaire de contact</h1>
      <form onSubmit={handleSubmit} noValidate>
        {["nom", "email", "telephone", "genre", "profession", "chambre"].map((field) => (
          <div key={field} style={{ marginBottom: 12 }}>
            <label htmlFor={field}>{field.charAt(0).toUpperCase() + field.slice(1)}</label>
            <input
              id={field}
              name={field}
              value={formData[field]}
              onChange={handleChange}
              aria-invalid={!!errors[field]}
              aria-describedby={errors[field] ? `${field}-error` : undefined}
            />
            {errors[field] && <p id={`${field}-error`} role="alert" style={{ color: "red" }}>{errors[field]}</p>}
          </div>
        ))}
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="age">Age</label>
          <input id="age" name="age" type="number" value={formData.age} onChange={handleChange}
            aria-invalid={!!errors.age} aria-describedby={errors.age ? "age-error" : undefined} />
          {errors.age && <p id="age-error" role="alert" style={{ color: "red" }}>{errors.age}</p>}
        </div>
        <div style={{ marginBottom: 12 }}>
          <label htmlFor="message">Message</label>
          <textarea id="message" name="message" value={formData.message} onChange={handleChange} />
        </div>
        {errors.submit && <p role="alert" style={{ color: "red" }}>{errors.submit}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "Envoi en cours..." : "Envoyer"}
        </button>
      </form>
    </main>
  );
}
