UPDATE integration_adapters
SET config_json = '{"mode":"sandbox","provider":"momo","currency":"RWF","callbackPath":"/api/v1/payments/webhooks/momo"}',
    updated_at = NOW()
WHERE code = 'MOMO_SANDBOX';
