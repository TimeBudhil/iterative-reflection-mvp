-- TEST-ONLY fixture careers (loaded via src/test/resources/config/application.properties).
-- Titles are prefixed "Fixture"; ids are generated, so look careers up by title. default_order is 9000+.
INSERT INTO tag_categories (name, display_order) VALUES ('Fixture cluster', 9001);
INSERT INTO tag_categories (name, display_order) VALUES ('Fixture work style', 9002);
INSERT INTO tags (category_id, name) SELECT id, 'Fixture health' FROM tag_categories WHERE name = 'Fixture cluster';
INSERT INTO tags (category_id, name) SELECT id, 'Fixture technology' FROM tag_categories WHERE name = 'Fixture cluster';
INSERT INTO tags (category_id, name) SELECT id, 'Fixture hands-on' FROM tag_categories WHERE name = 'Fixture work style';

INSERT INTO careers (title, summary, estimated_minutes, default_order, active) VALUES
 ('Fixture Career Alpha', 'Alpha professionals solve puzzles all day. They work with a small team. Every project ends with something real. Many people find it deeply satisfying.', 7, 9001, TRUE);
INSERT INTO careers (title, summary, estimated_minutes, default_order, active) VALUES
 ('Fixture Career Beta', 'Beta workers help people directly. The days are busy and varied. Training takes a few years.', 5, 9002, TRUE);
INSERT INTO careers (title, summary, estimated_minutes, default_order, active) VALUES
 ('Fixture Career Retired', 'This career has been retired from the catalog. It no longer shows up for students. Old reflections about it stay in place.', 6, 9003, FALSE);

INSERT INTO career_tags (career_id, tag_id) SELECT c.id, t.id FROM careers c, tags t
 WHERE c.title = 'Fixture Career Alpha' AND t.name IN ('Fixture technology', 'Fixture hands-on');
INSERT INTO career_tags (career_id, tag_id) SELECT c.id, t.id FROM careers c, tags t
 WHERE c.title = 'Fixture Career Beta' AND t.name IN ('Fixture health');
INSERT INTO career_tags (career_id, tag_id) SELECT c.id, t.id FROM careers c, tags t
 WHERE c.title = 'Fixture Career Retired' AND t.name IN ('Fixture health');

INSERT INTO career_pages (career_id, page_number, title) SELECT id, 1, 'A day in the life' FROM careers WHERE title = 'Fixture Career Alpha';
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 2, 'Listen and look' FROM careers WHERE title = 'Fixture Career Alpha';
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 3, 'Go deeper' FROM careers WHERE title = 'Fixture Career Alpha';
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 1, 'Meet the job' FROM careers WHERE title = 'Fixture Career Beta';
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 2, 'Next steps' FROM careers WHERE title = 'Fixture Career Beta';
INSERT INTO career_pages (career_id, page_number, title) SELECT id, 1, 'Old page' FROM careers WHERE title = 'Fixture Career Retired';

INSERT INTO content_blocks (page_id, block_order, block_type, body) SELECT p.id, 1, 'TEXT', 'Welcome to the job.' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Alpha' AND p.page_number = 1;
INSERT INTO content_blocks (page_id, block_order, block_type, url) SELECT p.id, 2, 'IMAGE_URL', 'https://images.example.org/alpha.jpg' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Alpha' AND p.page_number = 1;
INSERT INTO content_blocks (page_id, block_order, block_type, url) SELECT p.id, 1, 'VIDEO_EMBED', 'https://www.youtube.com/embed/abc123' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Alpha' AND p.page_number = 2;
INSERT INTO content_blocks (page_id, block_order, block_type, url) SELECT p.id, 2, 'AUDIO_EMBED', 'https://open.spotify.com/embed/episode/xyz' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Alpha' AND p.page_number = 2;
INSERT INTO content_blocks (page_id, block_order, block_type, url, label) SELECT p.id, 1, 'EXTERNAL_LINK', 'https://www.bls.gov/ooh/', 'Occupational Outlook Handbook' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Alpha' AND p.page_number = 3;
INSERT INTO content_blocks (page_id, block_order, block_type, body) SELECT p.id, 1, 'TEXT', 'Beta page one.' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Beta' AND p.page_number = 1;
INSERT INTO content_blocks (page_id, block_order, block_type, body) SELECT p.id, 1, 'TEXT', 'Beta page two.' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Beta' AND p.page_number = 2;
INSERT INTO content_blocks (page_id, block_order, block_type, body) SELECT p.id, 1, 'TEXT', 'Retired page.' FROM career_pages p JOIN careers c ON c.id = p.career_id WHERE c.title = 'Fixture Career Retired' AND p.page_number = 1;
