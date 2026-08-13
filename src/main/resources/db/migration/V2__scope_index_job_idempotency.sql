ALTER TABLE job_task ADD COLUMN operation VARCHAR(24) NOT NULL DEFAULT 'PUBLISH';
ALTER TABLE job_task DROP CONSTRAINT job_task_idempotency_key_key;
ALTER TABLE job_task ADD CONSTRAINT uq_job_task_operation_key UNIQUE (document_id, target_revision, operation, idempotency_key);
