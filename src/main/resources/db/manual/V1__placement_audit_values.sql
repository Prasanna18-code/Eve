BEGIN;

ALTER TABLE admin_audit_logs
    DROP CONSTRAINT IF EXISTS admin_audit_logs_action_check;

ALTER TABLE admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_action_check
        CHECK (action IN (
            'USER_ACTIVATED',
            'USER_DEACTIVATED',
            'USER_ROLE_CHANGED',
            'ORGANIZATION_ACTIVATED',
            'ORGANIZATION_DEACTIVATED',
            'CLUB_ACTIVATED',
            'CLUB_DEACTIVATED',
            'EVENT_CANCELLED',
            'PLACEMENT_CREATED',
            'PLACEMENT_UPDATED',
            'PLACEMENT_PUBLISHED',
            'PLACEMENT_CLOSED'
        ));

ALTER TABLE admin_audit_logs
    DROP CONSTRAINT IF EXISTS admin_audit_logs_target_type_check;

ALTER TABLE admin_audit_logs
    ADD CONSTRAINT admin_audit_logs_target_type_check
        CHECK (target_type IN (
            'USER',
            'ORGANIZATION',
            'CLUB',
            'EVENT',
            'PLACEMENT'
        ));

COMMIT;
