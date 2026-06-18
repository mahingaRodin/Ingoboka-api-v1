INSERT INTO integration_adapters (code, adapter_type, name, enabled, config_json)
VALUES (
    'MOMO_SANDBOX',
    'PAYMENT',
    'MTN MoMo Sandbox Payment',
    TRUE,
    '{"mode":"sandbox","provider":"momo","currency":"RWF","callbackPath":"/api/payments/webhooks/momo"}'
)
ON CONFLICT (code) DO NOTHING;
