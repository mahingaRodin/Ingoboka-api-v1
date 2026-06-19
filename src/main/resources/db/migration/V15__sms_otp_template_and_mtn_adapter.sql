INSERT INTO integration_adapters (code, adapter_type, name, enabled, config_json)
VALUES (
    'MTN_BULK_SMS',
    'MESSAGING',
    'MTN Rwanda Bulk SMS',
    TRUE,
    '{"provider":"mtn-bulk","channel":"sms"}'
)
ON CONFLICT (code) DO NOTHING;
