# Offline card images

The local server can serve its complete image library without contacting
`res.starwarsccg.org`. Images are stored outside the packaged web tree at:

```text
.local/card-images/
```

This keeps the multi-gigabyte library out of Git and `web.zip`. The dedicated
`/gemp-swccg/localimage/` route reads each image from disk without retaining the
whole library in GEMP's static-file memory cache.

## Install or refresh while online

From the repository root:

```bash
python3 tools/install-offline-card-images.py
```

The installer inventories both GEMP clients plus server-generated image links,
preserves the CDN directory layout, validates image signatures, and writes a
SHA-256 manifest. Existing valid images are kept, so interrupted runs resume at
the next unfinished image and the same command fetches newly referenced art
after a card update.

## Verify without downloading

```bash
python3 tools/install-offline-card-images.py --verify-only
```

Verification fails if any currently referenced image is missing, invalid, or no
longer matches the installed manifest.

## Before traveling

Start localhost GEMP once, open the standard game client and Epic Duel, and run
the verification command. The `.local/` directory is intentionally ignored by
Git. Do not run `git clean -fdx`, which would remove ignored local files. Keeping
a copy of `.local/card-images/` on an external drive gives you a quick recovery
without internet access.
