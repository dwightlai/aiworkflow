ALTER TABLE agi_generation_output
    ADD COLUMN IF NOT EXISTS content_docx_path VARCHAR(512);
