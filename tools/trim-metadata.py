#!/usr/bin/env python3
"""
Cuts the bundled libretro metadata database down to the systems this app plays.

Upstream ships one database covering every system Lemuroid supports: 104,073 rows
and, uncompressed, the single largest file in the APK -- larger than the emulator
core. This app plays Game Boy, which is 4,157 of those rows, plus Game Boy Color at
2,671. The other 97,245 describe games it cannot load.

    12.7 MB  ->  860 KB

Run this again after pulling a newer database from upstream; it is not idempotent in
the sense of being needed only once, but it is safe to re-run on an already-trimmed
file.

    python3 tools/trim-metadata.py
"""

import os
import sqlite3
import subprocess
import sys

KEEP = ("gb", "gbc")
DB = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "lemuroid-metadata-libretro-db", "src", "main", "assets", "libretro-db.sqlite",
)


def main():
    if not os.path.isfile(DB):
        sys.exit("database not found at %s" % DB)
    before = os.path.getsize(DB)

    con = sqlite3.connect(DB)
    total = con.execute("SELECT COUNT(*) FROM games").fetchone()[0]
    con.execute(
        "DELETE FROM games WHERE system NOT IN (%s)" % ",".join("?" * len(KEEP)), KEEP
    )
    con.commit()
    kept = con.execute("SELECT COUNT(*) FROM games").fetchone()[0]
    # VACUUM has to run outside a transaction, and is the whole point: the DELETE only
    # frees pages inside the file, it does not shrink it.
    con.isolation_level = None
    con.execute("VACUUM")
    con.close()

    after = os.path.getsize(DB)
    print("rows  %d -> %d  (kept %s)" % (total, kept, ", ".join(KEEP)))
    print("bytes %d -> %d  (%.0f%% smaller)" % (before, after, 100 * (1 - after / before)))


if __name__ == "__main__":
    main()
