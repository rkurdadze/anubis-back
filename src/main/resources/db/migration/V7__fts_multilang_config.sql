-- ==========================================================
-- მულტილინგვალური FTS კონფიგურაცია (ქართული → English → Russian)
-- ==========================================================

-- 1. დამატებითი მოდულების გააქტიურება
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2. ძველი კონფიგურაციის წაშლა (თუ არსებობს)
DROP TEXT SEARCH CONFIGURATION IF EXISTS multilang;

-- 3. ვქმნით ახალს
CREATE TEXT SEARCH CONFIGURATION multilang ( COPY = simple );

-- 4. დასუფთავება, დიდი ასოების შემცირება, დიაკრიტიკის მოხსნა
ALTER TEXT SEARCH CONFIGURATION multilang
    ALTER MAPPING FOR hword, hword_part, word
        WITH unaccent, simple;

-- 5. დამატებით ვცდილობთ ჩავრთოთ ინგლისური და რუსული,
-- მაგრამ უსაფრთხოდ (თუ არ არსებობს, გამოვიყენოთ simple)
DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_catalog.pg_ts_dict WHERE dictname = 'english') THEN
            IF EXISTS (SELECT 1 FROM pg_catalog.pg_ts_dict WHERE dictname = 'russian') THEN
                EXECUTE $q$
                ALTER TEXT SEARCH CONFIGURATION multilang
                ALTER MAPPING FOR word
                WITH unaccent, simple, english, russian;
            $q$;
            ELSE
                EXECUTE $q$
                ALTER TEXT SEARCH CONFIGURATION multilang
                ALTER MAPPING FOR word
                WITH unaccent, simple, english;
            $q$;
            END IF;
        ELSE
            RAISE NOTICE '⚠️ English/Russian dictionaries not found, fallback to simple';
        END IF;
    END $$;

-- 6. ტესტი:
-- SELECT to_tsvector('multilang', 'ქართული ტექსტი English слова');
-- SELECT to_tsquery('multilang', 'დოკუმენტი | document | документ');