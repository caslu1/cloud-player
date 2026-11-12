// Pix pelo Mercado Pago (API de pagamentos).
const API = "https://api.mercadopago.com/v1/payments";

export async function createPix(env, { amount, description, email, reference, notifyUrl }) {
  const res = await fetch(API, {
    method: "POST",
    headers: { authorization: `Bearer ${env.MP_ACCESS_TOKEN}`, "content-type": "application/json", "x-idempotency-key": crypto.randomUUID() },
    body: JSON.stringify({
      transaction_amount: Number(amount),
      description,
      payment_method_id: "pix",
      payer: { email },
      external_reference: String(reference),
      notification_url: notifyUrl,
      date_of_expiration: new Date(Date.now() + 30 * 60 * 1000).toISOString().replace("Z", "-00:00"),
    }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || "Erro no Mercado Pago");
  const t = data.point_of_interaction?.transaction_data || {};
  return { id: String(data.id), status: data.status, qr: t.qr_code, qrB64: t.qr_code_base64 };
}

export async function getPayment(env, id) {
  const res = await fetch(`${API}/${id}`, { headers: { authorization: `Bearer ${env.MP_ACCESS_TOKEN}` } });
  if (!res.ok) return null;
  return res.json();
}
