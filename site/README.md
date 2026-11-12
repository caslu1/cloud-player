# Orbita Play – site de ativação

Site do cliente (MAC + chave, listas, Pix), painel admin e API usada pelos apps.
Roda na Cloudflare (Workers + banco D1), no plano grátis.

## Publicar (uma vez)
1. Crie uma conta grátis em https://dash.cloudflare.com
2. Nesta pasta: `npm install` e `npx wrangler login`
3. Crie o banco: `npx wrangler d1 create orbita` → copie o `database_id` para o `wrangler.toml`
4. Crie as tabelas: `npx wrangler d1 execute orbita --remote --file schema.sql`
5. Senhas (não ficam no código):
   - `npx wrangler secret put ADMIN_PASSWORD` (senha do painel)
   - `npx wrangler secret put SESSION_SECRET` (qualquer texto longo e aleatório)
   - `npx wrangler secret put MP_ACCESS_TOKEN` (Access Token de produção do Mercado Pago, para o Pix)
6. `npx wrangler deploy` → o endereço aparece no final (ex.: https://orbita-ativar.SEU-USUARIO.workers.dev)
7. (Opcional) Domínio próprio: no painel da Cloudflare, Workers → orbita-ativar → Domínios.

## Endereços
- `/` site do cliente · `/admin` painel · `/ajuda`
- `/api/app/register` e `/api/app/status` usados pelos apps
- `/webhook/mercadopago` confirmação automática do Pix

## Rodar no computador
`npx wrangler d1 execute orbita --local --file schema.sql` e `npx wrangler dev`
(senhas de teste em `.dev.vars`: `ADMIN_PASSWORD=...` e `SESSION_SECRET=...`)
