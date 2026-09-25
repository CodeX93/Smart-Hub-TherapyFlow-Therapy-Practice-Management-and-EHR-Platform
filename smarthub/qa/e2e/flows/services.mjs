import { expect } from '@playwright/test';
import { fillText } from '../lib/forms.mjs';
import { journey, markBuilt, note } from '../lib/journey.mjs';
import { service } from '../lib/fixtures.mjs';

/**
 * A new tenant is seeded with one service, Consultation, and that one is tied to the
 * public-site consultation hours. Ordinary clinical bookings read the schedule rows
 * that carry no service, so the clinic has to add a service of its own before anyone
 * can book an hour with a therapist.
 */
export async function openServicesSettings() {
  await journey.page.goto('/admin/settings?tab=services');
}

export async function fillServiceForm(values) {
  const { page } = journey;
  await page.getByRole('button', { name: 'Add Service Code' }).first().click();
  await expect(page.getByRole('heading', { name: 'Add New Service Code' }), 'the service form opens')
    .toBeVisible({ timeout: 30000 });
  await fillText(page, 'Service Code', values.code);
  await fillText(page, 'Service Name', values.name);
  await fillText(page, 'Description', values.description);
  await fillText(page, 'Session Duration (minutes)', values.duration);
  await fillText(page, 'Base Rate (USD)', values.rate);
}

export async function submitServiceForm() {
  await journey.page.getByRole('button', { name: 'Add Service Code' }).last().click();
}

export async function createService() {
  const { page } = journey;
  await openServicesSettings();
  await fillServiceForm(service);
  await submitServiceForm();
  await expect(page.getByText(service.name).first(), 'the service is listed after saving')
    .toBeVisible({ timeout: 30000 });
  markBuilt('service');
  note(`service ${service.code} — ${service.name} (${service.duration} min) created`);
}
