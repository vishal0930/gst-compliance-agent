-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notification_created ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_expires ON notifications(expires_at) WHERE expires_at IS NOT NULL;

-- Add comment
COMMENT ON TABLE notifications IS 'Stores user notifications for deadlines, penalties, and system events';
