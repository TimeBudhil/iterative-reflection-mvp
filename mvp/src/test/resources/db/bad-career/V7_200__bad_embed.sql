-- Used only by CatalogSeedValidationTest: an embed on a host that is not allowlisted.
INSERT INTO careers (title, summary, estimated_minutes, default_order, active) VALUES
 ('Bad Embed Career', 'One sentence here. Two sentences here. Three sentences here.', 6, 1, TRUE);
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 1, 'p' FROM careers WHERE title = 'Bad Embed Career';
INSERT INTO content_blocks (page_id, block_order, block_type, url) SELECT p.id, 1, 'VIDEO_EMBED', 'https://evil.example.com/embed/1' FROM career_pages p;
