-- Ensure kb_chunk supports chunk-level FTS even in environments without pgvector.
DO $ai$
BEGIN
    IF to_regclass('public.kb_chunk') IS NULL THEN
        IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
            EXECUTE $ddl$CREATE EXTENSION IF NOT EXISTS vector$ddl$;
            EXECUTE $ddl$
            CREATE TABLE kb_chunk (
                id         VARCHAR(64) PRIMARY KEY,
                content    TEXT,
                metadata   JSON,
                embedding  vector(1024),
                search_tsv tsvector
            )
            $ddl$;
        ELSE
            EXECUTE $ddl$
            CREATE TABLE kb_chunk (
                id         VARCHAR(64) PRIMARY KEY,
                content    TEXT,
                metadata   JSON,
                search_tsv tsvector
            )
            $ddl$;
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector')
        AND NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'kb_chunk' AND column_name = 'embedding'
        ) THEN
        EXECUTE $ddl$CREATE EXTENSION IF NOT EXISTS vector$ddl$;
        EXECUTE $ddl$ALTER TABLE kb_chunk ADD COLUMN embedding vector(1024)$ddl$;
    END IF;

    EXECUTE $ddl$ALTER TABLE kb_chunk ADD COLUMN IF NOT EXISTS content TEXT$ddl$;
    EXECUTE $ddl$ALTER TABLE kb_chunk ADD COLUMN IF NOT EXISTS metadata JSON$ddl$;
    EXECUTE $ddl$ALTER TABLE kb_chunk ADD COLUMN IF NOT EXISTS search_tsv tsvector$ddl$;

    EXECUTE $ddl$
    CREATE OR REPLACE FUNCTION kb_chunk_search_tsv_trigger() RETURNS trigger AS $func$
    BEGIN
        NEW.search_tsv := kb_knowledge_tsv('', NEW.content);
        RETURN NEW;
    END
    $func$ LANGUAGE plpgsql
    $ddl$;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_kb_chunk_search_tsv') THEN
        EXECUTE $ddl$
        CREATE TRIGGER trg_kb_chunk_search_tsv
            BEFORE INSERT OR UPDATE OF content ON kb_chunk
            FOR EACH ROW EXECUTE FUNCTION kb_chunk_search_tsv_trigger()
        $ddl$;
    END IF;

    EXECUTE $ddl$CREATE INDEX IF NOT EXISTS idx_kb_chunk_tsv_gin ON kb_chunk USING GIN (search_tsv)$ddl$;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'kb_chunk' AND column_name = 'embedding'
    ) THEN
        EXECUTE $ddl$CREATE INDEX IF NOT EXISTS idx_kb_chunk_hnsw ON kb_chunk USING hnsw (embedding vector_cosine_ops)$ddl$;
    END IF;
END
$ai$;
