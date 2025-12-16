---
name: cube-builder
description: This skill should be used when building cube draft configurations for GEMP-SWCCG from CSV files containing card names. It handles mapping card names to IDs, generating cube configurations with add-on card support, and integrating the cube into the application (swccgDrafts.json registration and frontend dropdown setup).
---

# Cube Builder

## Overview

Build cube draft configurations for GEMP-SWCCG by mapping CSV files of card names to card IDs and generating properly structured cube configuration JSON files. This skill covers the complete workflow from CSV preparation through deployment, including the critical playtesting card exclusion requirement and add-on card pack structure.

## When to Use This Skill

Use this skill when:
- Creating a new cube draft configuration from card lists
- Updating an existing cube with new cards
- Troubleshooting cube draft issues (missing cards, playtesting cards appearing, etc.)
- Understanding how multi-column CSVs create add-on card packs

## Workflow

### Step 1: Prepare CSV Files

Create three CSV files in the cube source directory:

1. **default_cards.csv** - Single column of card names (core cards all players start with)
2. **cube_worlds_lightside.csv** - Light side packs (supports multi-column for add-ons)
3. **cube_worlds_darkside.csv** - Dark side packs (supports multi-column for add-ons)

**CSV Column Structure:**
- **Single column** → Produces flat array: `["card1_id", "card2_id"]`
- **Multiple columns** → Produces array of arrays: `[["primary_id", "addon1_id"], ["card2_id"]]`
  - Column 1 = primary card in the pack
  - Columns 2+ = add-on cards that come with the primary card
  - When a player picks a pack, they receive ALL cards in that array
  - Empty cells are skipped

**Example multi-column CSV:**
```csv
Admiral Ackbar,Home One
Obi-Wan Kenobi,Kenobi's Lightsaber
Luke Skywalker,,
```

**Produces:**
```json
[
  ["admiral_ackbar_id", "home_one_id"],
  ["obi_wan_id", "lightsaber_id"],
  ["luke_id"]
]
```

**Directory Structure:**
```
src/gemp-swccg-server/src/main/resources/draft/cube_building/
└── <cube_name>/
    ├── default_cards.csv
    ├── cube_worlds_lightside.csv
    └── cube_worlds_darkside.csv
```

### Step 2: Update Card Database (Critical)

The card mapping process requires an up-to-date card database that EXCLUDES playtesting cards (set 501).

```bash
./bin/gemp export-cards src/gemp-swccg-cards/src/main/resources/card_blueprint_database.json
```

**Why this matters:** The CardDataExporter automatically excludes set501 cards via a filter. If this step is skipped or uses old data, playtesting cards will appear in the cube.

**Verification:**
```bash
grep -c '"cardId": "501_' src/gemp-swccg-cards/src/main/resources/card_blueprint_database.json
# Must output: 0
```

### Step 3: Map Card Names to IDs

Map each CSV file to card IDs using the `map_card_names_to_ids` script:

```bash
CUBE_DIR="src/gemp-swccg-server/src/main/resources/draft/cube_building/<cube_name>"

# Map default/core cards
./bin/map_card_names_to_ids $CUBE_DIR/default_cards.csv $CUBE_DIR/default_cards_mapped.json

# Map light side packs
./bin/map_card_names_to_ids $CUBE_DIR/cube_worlds_lightside.csv $CUBE_DIR/cube_worlds_lightside_mapped.json

# Map dark side packs
./bin/map_card_names_to_ids $CUBE_DIR/cube_worlds_darkside.csv $CUBE_DIR/cube_worlds_darkside_mapped.json
```

**Output Files:**
- `*_mapped.json` - Card ID arrays (flat or nested based on CSV structure)
- `*_mapped_log.json` - Processing logs with missing cards and warnings

**Review the logs** for:
- Missing cards (slug shown for debugging)
- Multiple matches (script auto-selects first match)
- Success rate (should be >95%)

**Script Behavior:**
- Detects single vs multi-column by checking for non-empty columns
- Slugifies card names (lowercase, special chars → `-`)
- Tries exact slug match, then slug + set name
- Logs all issues for review

### Step 4: Generate Cube Configuration

Use `create_cube` to build the final configuration:

```bash
python3 bin/create_cube \
  src/gemp-swccg-server/src/main/resources/draft/cube_building/<cube_name> \
  src/gemp-swccg-server/src/main/resources/draft/<outputFile>.json
```

**Required Files in Cube Directory:**
- `default_cards_mapped.json`
- `cube_worlds_lightside_mapped.json`
- `cube_worlds_darkside_mapped.json`

**Configuration Parameters (hardcoded in script):**
- `count: 9` - Pack choices shown per round
- `repeat: 56` - Number of draft rounds
- `objChoiceCountPerSide: 0` - No objective picks (standard cube)

**Output Structure:**
```json
{
  "format": "cube",
  "fixedCount": 0,
  "objChoiceCountPerSide": 0,
  "choiceCountPerSide": 56,
  "lightObjCards": [],
  "darkObjCards": [],
  "startingPool": {
    "type": "cubeDraftRun",
    "data": {
      "coreCards": ["5_164", "12_164", ...]
    }
  },
  "choices": [
    {
      "type": "cubePackObjPick",
      "repeat": 56,
      "data": {
        "count": 9,
        "packs": [
          ["110_6", "107_5"],
          ["213_31"],
          ...
        ]
      }
    },
    { /* dark side choice with same structure */ }
  ]
}
```

### Step 5: Register Draft Format

Add the cube to the draft registry:

**File:** `src/gemp-swccg-server/src/main/resources/swccgDrafts.json`

```json
[
  {
    "type": "c7_draft",
    "location": "/draft/cube7Draft.json"
  },
  {
    "type": "<cube_identifier>_draft",
    "location": "/draft/<outputFile>.json"
  }
]
```

**Important:**
- `type` - Format identifier (used in code and database)
- `location` - Path relative to resources directory

### Step 6: Update Frontend

Add the cube to the league admin dropdown:

**File:** `src/gemp-swccg-async/src/main/web/includes/admin/leagueAdmin.html`

**Location:** Around line 459 in the solo draft form

```html
<select name="format">
  <option value="c7_draft">Cube v7 Draft</option>
  <option value="c7obj_draft">Cube v7 Draft + Obj</option>
  <option value="<cube_identifier>_draft">Your Cube Name</option>
</select>
```

**Critical:** The `value` attribute MUST match the `type` field in swccgDrafts.json

### Step 7: Deploy and Verify

```bash
# Rebuild server module (contains cube config)
./bin/gemp reload-fast

# Verify in browser
# 1. Access http://localhost:17001/gemp-swccg/
# 2. Login as admin
# 3. Navigate to League Admin → Add Solo Draft League
# 4. Verify cube appears in format dropdown
# 5. Create test league and access draft to confirm no errors
```

## Verification Checklist

Before finalizing:

- [ ] Card database excludes playtesting cards (grep check shows 0)
- [ ] Cube configuration excludes playtesting cards (grep check shows 0)
- [ ] All CSVs mapped with >95% success rate
- [ ] Cube config has correct structure (coreCards flat, packs nested)
- [ ] Draft registered in swccgDrafts.json with correct type/location
- [ ] Frontend dropdown includes cube with matching value attribute
- [ ] Server rebuilt and restarted
- [ ] Draft loads without errors in browser

## Troubleshooting

### Issue: Playtesting Cards Appear in Draft

**Root Cause:** Old card database contains set 501 cards

**Solution:**
1. Regenerate card database (Step 2)
2. Re-run all mapping scripts (Step 3)
3. Regenerate cube config (Step 4)
4. Verify with grep: `grep '"501_' <cube_config>.json`

### Issue: "Card database not found"

**Solution:** Run card export command from Step 2

### Issue: Multiple Cards Found (Warnings)

**Cause:** Card name matches multiple versions/sets

**Solution:** Script auto-selects first match. Review `*_mapped_log.json` to verify correct card. If wrong, update CSV with more specific name or set identifier.

### Issue: Missing Cards in Log

**Cause:** Card doesn't exist or is misspelled

**Solution:**
1. Check log for attempted slug
2. Search card database for correct name
3. Update CSV and re-run mapping

### Issue: Draft Doesn't Appear in Dropdown

**Cause:** Registration mismatch or cache issue

**Solution:**
1. Verify `type` in swccgDrafts.json matches `value` in leagueAdmin.html
2. Rebuild: `./bin/gemp reload-fast`
3. Hard refresh browser (Cmd+Shift+R)

### Issue: Division by Zero Error

**Symptom:** `java.lang.ArithmeticException: / by zero` at DefaultSoloDraft.java:66

**Cause:** Missing null check for objChoiceCountPerSide

**Solution:** Verify line 66 has:
```java
if (_objChoiceCountPerSide > 0 && offset % _objChoiceCountPerSide == 0) {
```

If missing `> 0` check, apply fix and rebuild.

## File Locations

```
gemp-swccg/
├── bin/
│   ├── gemp                          # CLI helper
│   ├── map_card_names_to_ids         # CSV → IDs mapper
│   └── create_cube                   # Cube config generator
│
├── src/gemp-swccg-cards/src/main/resources/
│   └── card_blueprint_database.json  # Card database (no set501)
│
├── src/gemp-swccg-server/src/main/resources/
│   ├── draft/
│   │   ├── <outputFile>.json        # Your cube config
│   │   └── cube_building/
│   │       └── <cube_name>/         # Source files
│   │           ├── default_cards.csv
│   │           ├── default_cards_mapped.json
│   │           ├── cube_worlds_lightside.csv
│   │           ├── cube_worlds_lightside_mapped.json
│   │           ├── cube_worlds_darkside.csv
│   │           └── cube_worlds_darkside_mapped.json
│   │
│   └── swccgDrafts.json              # Draft registry
│
├── src/gemp-swccg-server/src/main/java/.../draft2/
│   └── DefaultSoloDraft.java         # Draft logic (line 66 fix)
│
└── src/gemp-swccg-async/src/main/web/includes/admin/
    └── leagueAdmin.html              # Frontend admin UI
```

## Technical Details

### Pack Structure

**Single card pack:**
```json
["card_id"]
```

**Pack with add-ons:**
```json
["primary_card_id", "addon1_id", "addon2_id"]
```

Player receives ALL cards when picking the pack.

### Draft Flow

1. Players start with all `coreCards` from startingPool
2. Alternates between light/dark for 56 rounds (`choiceCountPerSide`)
3. Each round shows 9 random packs (`count: 9`)
4. Player picks 1 pack, receives all cards in that array
5. Packs support add-ons via array-of-arrays structure

### Choice Types

- `cubePackObjPick` - Pack-based (supports add-ons)
- `cubePick` - Single card (no add-ons)

Always use `cubePackObjPick` for consistency, even without objectives.

## Complete Example

```bash
# 1. Create directory
mkdir -p src/gemp-swccg-server/src/main/resources/draft/cube_building/my_cube

# 2. Add CSV files (user provides)
# - default_cards.csv
# - cube_worlds_lightside.csv
# - cube_worlds_darkside.csv

# 3. Update card database
./bin/gemp export-cards src/gemp-swccg-cards/src/main/resources/card_blueprint_database.json

# 4. Map all cards
CUBE_DIR="src/gemp-swccg-server/src/main/resources/draft/cube_building/my_cube"
./bin/map_card_names_to_ids $CUBE_DIR/default_cards.csv $CUBE_DIR/default_cards_mapped.json
./bin/map_card_names_to_ids $CUBE_DIR/cube_worlds_lightside.csv $CUBE_DIR/cube_worlds_lightside_mapped.json
./bin/map_card_names_to_ids $CUBE_DIR/cube_worlds_darkside.csv $CUBE_DIR/cube_worlds_darkside_mapped.json

# 5. Generate config
python3 bin/create_cube $CUBE_DIR src/gemp-swccg-server/src/main/resources/draft/cubeMyCube.json

# 6. Verify no playtesting
grep '"501_' src/gemp-swccg-server/src/main/resources/draft/cubeMyCube.json
# Should return nothing

# 7. Register in swccgDrafts.json
# Add: {"type": "my_cube_draft", "location": "/draft/cubeMyCube.json"}

# 8. Add to leagueAdmin.html
# Add: <option value="my_cube_draft">My Cube Draft</option>

# 9. Deploy
./bin/gemp reload-fast
```

## Resources

See `references/` directory for additional documentation on cube draft mechanics and troubleshooting scenarios.
