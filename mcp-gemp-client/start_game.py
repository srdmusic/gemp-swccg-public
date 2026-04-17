#!/usr/bin/env python3
"""
Quick-start script: Login, admin setup, list decks, and create a game vs Rando.
Run from your Mac terminal:  python3 ~/gemp-swccg-public/mcp-gemp-client/start_game.py
"""

import asyncio
import sys
import os
import time

# Add parent dir so we can import the session class
sys.path.insert(0, os.path.dirname(__file__))
from gemp_mcp import GempSession


async def main():
    base_url = os.environ.get("GEMP_BASE_URL", "http://localhost:17001")
    session = GempSession(base_url)

    # 1. Login as admin (test1)
    print("=" * 60)
    print("Step 1: Logging in as test1...")
    result = await session.login("test1", "test")
    print(f"  Login: {result}")
    if result["status"] != "ok":
        print("  FAILED - is GEMP running at localhost:17001?")
        return

    # 2. Admin setup - enable all settings
    print("\nStep 2: Running admin setup...")
    result = await session.admin_setup()
    for label, status in result.get("results", {}).items():
        print(f"  {label}: {status}")

    # 3. List decks
    print("\nStep 3: Listing decks for test1...")
    result = await session.list_decks()
    decks = result.get("decks", [])
    if decks:
        for i, d in enumerate(decks):
            print(f"  [{i}] {d}")
    else:
        print(f"  Raw response: {result}")
        print("  No decks found! Import some decks first.")
        return

    # ---------------------------------------------------------------
    # Hardcoded deck assignments per user request:
    #   Player (test1) = Light Side
    #   Rando (AI)     = Dark Side, specifically "DARK DEAL"
    #
    # Light Side decks: HIDDEN PATH CHARGE, LUKE SAGA TATOOINE
    # Dark Side decks:  DARK DEAL, Hunt Down V
    # ---------------------------------------------------------------

    player_deck = "LUKE SAGA TATOOINE"   # Light Side
    ai_deck = "DARK DEAL"                # Dark Side for Rando

    print(f"\n{'=' * 60}")
    print(f"  Player deck (Light Side): {player_deck}")
    print(f"  AI deck (Dark Side):      {ai_deck}")
    print(f"{'=' * 60}")

    # Verify player deck exists on test1
    if player_deck not in decks:
        print(f"\n  WARNING: '{player_deck}' not found in test1's decks!")
        print(f"  Available: {decks}")
        print(f"  Trying first available deck instead...")
        player_deck = decks[0]

    # 4. Create game vs Rando
    # Note: ai_deck_sample=True means use Librarian's library deck.
    # If Dark Deal isn't on the Librarian account, try ai_deck_sample=False
    # which may use the deck from the player's account.
    print(f"\nStep 4: Creating game vs RANDO...")

    # Decks are on test1's account, not Librarian's — use ai_deck_sample=False
    result = await session.create_game_vs_ai(
        game_format="open",
        deck_name=player_deck,
        ai_skill="RANDO",
        ai_deck_name=ai_deck,
        ai_deck_sample=False,
        sample_deck=False,
    )
    print(f"  Create game: {result}")

    if result["status"] == "ok":
        # Give server a moment to set up the table
        print("\n  Waiting 2 seconds for table setup...")
        await asyncio.sleep(2)

        # 5. Find our game
        print("\nStep 5: Finding our game...")
        result = await session.find_my_game()
        print(f"  Find game: {result}")

        if result.get("gameId"):
            game_id = result["gameId"]
            print(f"\n{'=' * 60}")
            print(f"  GAME READY!  Game ID: {game_id}")
            print(f"  Players: {result['table'].get('players', '?')}")
            print(f"  Status:  {result['table'].get('statusDescription', '?')}")
            print(f"{'=' * 60}")
            print(f"\n  Open in browser:")
            print(f"  {base_url}/gemp-swccg/game.html?gameId={game_id}")

            # 6. Join the game and get initial state
            print(f"\nStep 6: Joining game {game_id}...")
            result = await session.signup_for_game(game_id)
            print(f"  Initial state: {result}")
        else:
            print("\n  Game not found in hall.")
            hall = await session.get_hall()
            print(f"  Full hall: {hall}")
    else:
        print(f"\n  Game creation failed: {result}")


if __name__ == "__main__":
    asyncio.run(main())
