---
title: GEMP-SWCCG HTTP API Reference
updated: 2026-03-03
purpose: Complete API contracts for building game clients and MCP servers
---

# GEMP-SWCCG HTTP API Reference

Base URL: `http://localhost:17001/`

## 1. Authentication

### Login
**POST** `/gemp-swccg-server/login`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| login | string | yes | Username |
| password | string | yes | Password |

**Response:** Sets `loggedUser` session cookie.

| HTTP Code | Meaning |
|-----------|---------|
| 200 | Success (cookie set) |
| 401 | Wrong credentials |
| 202 | Password reset needed |
| 403 | Permanently banned |
| 409 | Temporarily banned |

---

## 2. Hall (Lobby)

### List Tables
**GET** `/gemp-swccg-server/hall`

| Parameter | Type | Required |
|-----------|------|----------|
| participantId | string | yes |

**Response:** XML with tables, formats, player info.

### Create Table (vs AI)
**POST** `/gemp-swccg-server/hall`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| participantId | string | yes | Your username |
| format | string | yes | Format code (e.g., "open") |
| deckName | string | yes | Your deck name |
| sampleDeck | boolean | no | Use library deck (default: false) |
| tableDesc | string | no | Table description |
| isPrivate | boolean | no | Private game (default: false) |
| playVsAi | boolean | no | Play against bot (default: false) |
| aiSkill | string | conditional | "BEGINNER", "ADVANCED", or "RANDO" |
| aiDeckName | string | conditional | AI's deck name |
| aiDeckSample | boolean | no | AI uses library deck (default: true) |

### Join Table
**POST** `/gemp-swccg-server/hall/{tableId}`

| Parameter | Type | Required |
|-----------|------|----------|
| participantId | string | yes |
| deckName | string | yes |
| sampleDeck | boolean | no |

### Leave Table
**POST** `/gemp-swccg-server/hall/{tableId}/leave`

### Update Hall (Long-Poll)
**POST** `/gemp-swccg-server/hall/update`

| Parameter | Type | Required |
|-----------|------|----------|
| participantId | string | yes |
| channelNumber | int | yes |

---

## 3. Game Play

### Get Initial Game State (Signup)
**GET** `/gemp-swccg-server/game/{gameId}`

| Parameter | Type | Required |
|-----------|------|----------|
| participantId | string | yes |

**Response:** XML `<gameState cn="[channelNumber]">` with game events and clocks.

### Poll for Updates / Submit Decision
**POST** `/gemp-swccg-server/game/{gameId}`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| participantId | string | yes | Your username |
| channelNumber | int | yes | From previous response `cn` attribute |
| decisionId | int | no | Decision ID to answer |
| decisionValue | string | no | Your response |

**Cookie:** `autoPassPhases` — pipe-delimited phase names or "false"

**Response:** XML `<update cn="[newChannelNumber]">` with game events.

**Long-poll timeout:** ~2.5 seconds

| HTTP Code | Meaning |
|-----------|---------|
| 200 | Success |
| 404 | Game not found |
| 409 | Subscription conflict (different session) |
| 410 | Subscription expired |
| 403 | Private game, spectators denied |

### Concede
**POST** `/gemp-swccg-server/game/{gameId}/concede`

### Cancel
**POST** `/gemp-swccg-server/game/{gameId}/cancel`

### Extend Timer
**POST** `/gemp-swccg-server/game/{gameId}/extendGameTimer`

| Parameter | Type |
|-----------|------|
| participantId | string |
| minutesToExtend | int |

### Disable Action Timer
**POST** `/gemp-swccg-server/game/{gameId}/disableActionTimer`

### Get Card Info
**GET** `/gemp-swccg-server/game/{gameId}/cardInfo`

| Parameter | Type | Description |
|-----------|------|-------------|
| participantId | string | Player |
| cardId | int/string | Card ID (or "extra", "anim") |

---

## 4. Decision Types

### MULTIPLE_CHOICE
Choose one option from a list.
- **Parameters:** `results[]` (option strings), `defaultIndex`
- **Response:** Index as string ("0", "1", "2")

### INTEGER
Choose a number in range.
- **Parameters:** `min`, `max`, `defaultValue`
- **Response:** Number as string ("5")

### CARD_SELECTION
Select cards from the game board.
- **Parameters:** `cardId[]` (physical card IDs), `min`, `max`
- **Response:** Comma-separated card IDs ("1,3,5")

### ARBITRARY_CARDS
Choose cards from a popup/list.
- **Parameters:** `cardId[]` (temp IDs), `blueprintId[]`, `testingText[]`, `min`, `max`, `selectable[]`
- **Response:** Comma-separated temp IDs ("temp0,temp2,temp5")

### CARD_ACTION_CHOICE
Choose which card action to perform.
- **Parameters:** `actionId[]`, `blueprintId[]`, `actionText[]`, `testingText[]`
- **Response:** Action index ("2")

### ACTION_CHOICE
Choose an action type.
- **Parameters:** Similar to CARD_ACTION_CHOICE
- **Response:** Action index

### EMPTY
Auto-transition, no input needed.
- **Parameters:** `timeoutValue` (ms)
- **Response:** Empty string or auto-submit

---

## 5. Game Events (XML Serialization)

Events are `<ge>` elements with attributes:

| Attribute | Description |
|-----------|-------------|
| type | Event type code |
| blueprintId | Card definition ID |
| cardId | Physical card instance ID |
| participantId | Player who triggered |
| phase | Current game phase |
| zone | Card zone |
| index / locationIndex | Position |
| testingText | Card rules text |
| message | Text message |

**Key Event Types:**
- `P` = Participant, `GPC` = Phase change, `M` = Message
- `PCIP` = Card enters play, `RCFP` = Card removed from play
- `SB` = Start battle, `EB` = End battle
- `DD` = Destiny draw, `D` = Decision (awaiting player input)
- `GS` = Game stats

**Decision events include:**
```xml
<ge id="[decisionId]" decisionType="[TYPE]" text="[prompt]">
  <parameter name="results" value="Option1"/>
  <parameter name="results" value="Option2"/>
  <parameter name="cardId" value="12345"/>
</ge>
```

---

## 6. Deck API

### List Decks
**GET** `/gemp-swccg-server/deck/list` (participantId)

### Get Deck
**GET** `/gemp-swccg-server/deck` (participantId, deckName)

### Save Deck
**POST** `/gemp-swccg-server/deck` (participantId, deckName, deckContents)

### Delete Deck
**POST** `/gemp-swccg-server/deck/delete` (participantId, deckName)

### Rename Deck
**POST** `/gemp-swccg-server/deck/rename` (participantId, oldName, newName)

### Deck Stats
**POST** `/gemp-swccg-server/deck/stats` (participantId, deckContents)

---

## 7. Admin API

All require admin privileges.

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/admin/clearcache` | POST | Clear server caches |
| `/admin/shutdown` | POST | Toggle server shutdown state |
| `/admin/motd/get` | GET | Get message of the day |
| `/admin/motd/update` | POST | Set MOTD |
| `/admin/collections/additems` | POST | Add items to player collection |
| `/admin/collections/addcurrency` | POST | Add currency to player |
| `/admin/user/ban/permanent` | POST | Permanent ban |
| `/admin/user/ban/temporary` | POST | Temp ban (minutes) |
| `/admin/user/ban/acquit` | POST | Unban |

---

## 8. Client Flow (Minimal Implementation)

```python
# 1. Login
session.post('/login', data={'login': user, 'password': pass})
# → Store session cookie

# 2. Create game vs Rando
session.post('/hall', data={
    'participantId': user,
    'format': 'open',
    'deckName': 'MyDeck',
    'playVsAi': 'true',
    'aiSkill': 'RANDO',
    'aiDeckName': 'RandoDeck',
    'aiDeckSample': 'true'
})

# 3. Get game state (assigns channelNumber)
resp = session.get(f'/game/{gameId}', params={'participantId': user})
channel = parse_xml(resp).attrib['cn']

# 4. Game loop
while not game_over:
    resp = session.post(f'/game/{gameId}', data={
        'participantId': user,
        'channelNumber': channel,
        'decisionId': current_decision_id,     # if answering
        'decisionValue': chosen_response         # if answering
    })
    channel = parse_xml(resp).attrib['cn']
    # Parse events, find new decisions, choose response
```

---

## 9. Key Implementation Notes

- **Session cookies**: `loggedUser` cookie must be sent with every request
- **Channel numbers**: Increment each response; mismatch = HTTP 409/410
- **Long-polling**: Server blocks ~2.5s waiting for state changes
- **Referer header**: Required for most endpoints (e.g., `Referer: http://localhost:17001/gemp-swccg/hall.html`)
- **AI decks from Librarian**: AI uses decks from the "Librarian" player account
- **Game timer**: 40-60 min per player (format-dependent), extendable
- **XML responses**: All game data returned as XML, parse with xmltodict or ElementTree
