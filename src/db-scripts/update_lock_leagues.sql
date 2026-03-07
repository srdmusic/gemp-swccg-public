-- Migration: Lock-in League Decks
-- Adds support for locking player decks after their first game in a league.

-- Flag on league table to enable deck lock-in (only meaningful for constructed leagues)
ALTER TABLE `league` ADD COLUMN `lockDecks` BIT DEFAULT 0 AFTER `invitationOnly`;

-- Locked deck storage on league_participation (per player per league)
ALTER TABLE `league_participation` ADD COLUMN `locked_ls_deck_name` VARCHAR(255) CHARACTER SET 'utf8' COLLATE 'utf8_bin' NULL;
ALTER TABLE `league_participation` ADD COLUMN `locked_ls_deck` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_bin' NULL;
ALTER TABLE `league_participation` ADD COLUMN `locked_ds_deck_name` VARCHAR(255) CHARACTER SET 'utf8' COLLATE 'utf8_bin' NULL;
ALTER TABLE `league_participation` ADD COLUMN `locked_ds_deck` TEXT CHARACTER SET 'utf8' COLLATE 'utf8_bin' NULL;
