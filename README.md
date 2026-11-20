# leonatkinson/tigrisgame

A fork of the Tigris & Euphrates computer game — a Java/AWT computer
adaptation of the classic Reiner Knizia board game. It can be played
with a graphical client against one to three built-in computer
opponents (`MiniMaxAI`, `MultiMaxAI` or `CraigAI`), or solitaire in a
text-only client.

The original game, Copyright (c) 2000-2007, Craig Moffitt, is
licensed under the Academic Free License ("AFL") v. 3.0 — the full
license text is in the `LICENSE` file at the project root. This
repository (`leonatkinson/tigrisgame`) contains modifications made
after that release, governed by the same license; see the Change Log
below for what changed.

The original repository is at https://sourceforge.net/projects/tigrisgame/

## Requirements

* Java (the build is routinely run on OpenJDK 21 and 25; any JDK or JRE
  that can run compiled `.class` files at those versions works)
* [Apache Ant](http://ant.apache.org/) 1.10.x for building from source

## Building

```bash
ant compile     # compile into ./class
ant jar         # build dist/lib/tigrisgame.jar (includes compile)
```

## Running

### Graphical client (recommended)

```bash
java -jar dist/lib/tigrisgame.jar              # default: 4 players, you vs. 3 computer opponents
java -jar dist/lib/tigrisgame.jar <savefile>   # resume a saved game
```

or through Ant:

```bash
ant run                      # runs the GUI client from the freshly built jar
ant run -Drunargs=<savefile> # run with a saved-game file
```

The default `run` target runs `com.tigrisgame.awtui.GUIGameClient`.
You can run the text client instead with
`ant run -Drunclass=com.tigrisgame.textui.TextGameClient`.

### Text-only client

```bash
java -cp dist/lib/tigrisgame.jar com.tigrisgame.textui.TextGameClient
```

This is a 1-player solitaire game. Type `help` at the prompt for the
command list, `help <command>` for details on a single command, and
`help move` to see the move syntax for the current game state.

### Command-line switches

| Switch                                       | Effect                                                                                                                                                       |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `-Dtigris.scale=N`                           | (GUI) override the automatic UI scaling with a factor `N`, e.g. `-Dtigris.scale=2.5`                                                                         |
| `<savefile>` argument                        | (GUI) load a previously saved game instead of starting a new one                                                                                             |
| `-Dtigris.players=N`                         | (GUI, new games only) number of seats, 2–4 (default 4). Seat 0 is shown as **You**; the other seats are AI opponents named **Computer 1**, **Computer 2**, … |
| `-Dtigris.delay=S`                           | (GUI) seconds to pause after each opponent move so you can see the change to the board (default 3; fractions allowed, `0` turns pacing off)                  |
| `-Dtigris.ai=MiniMaxAI\|MultiMaxAI\|CraigAI` | choose the AI opponent class (default: `MiniMaxAI` for 2 players, `MultiMaxAI` for 3–4 players)                                                              |
| `-Dtigris.ai.actionDepth=N`                  | how many full actions deep the AI searches (default 2 for MiniMaxAI, 1 for MultiMaxAI)                                                                       |
| `-Dtigris.ai.trueDepth=N`                    | how many total moves (plies) deep the AI searches (default 20 for MiniMaxAI, 10 for MultiMaxAI)                                                              |
| `-Dtigris.ai.prune=N`                        | branch factor: candidate moves kept per node (default 80; 1000 ≈ unlimited) — for MultiMaxAI, values above ~150 are a no-op                                                                                  |
| `-Dtigris.ai.keepProb=X`                     | prune assumed opponent support hands below probability X (default 0.01; mostly meaningful for MultiMaxAI)                                                    |
| `-Dtigris.ai.thinkMillis=N`                  | (**CraigAI only**) wall-clock think time per move (default 30000)                                                                                            |

Example — a deeper, wider search:

```bash
java -Dtigris.ai.actionDepth=3 -Dtigris.ai.prune=200 -jar dist/lib/tigrisgame.jar
```

Note that the AI has **no time cutoff** — larger depths and prune
sizes can make it think noticeably longer per move (alpha–beta pruning
helps, but a 3-deep, wide search can take tens of seconds).  The ten
evaluation weights (treasure bonus, monument score, support score,
etc. — see `ai/MiniMaxAI.setEvalWeights()`) are also tunable per class
if you want to experiment with its "personality", but they are not
exposed via properties (the in-class defaults were numerically tuned).

### Multi-player games

The default game is a 4-player game: **You** at seat 0 and three
computer opponents (Computer 1, Computer 2, Computer 3).  Use
`-Dtigris.players=2` or `=3` to shrink the table; with 2 players the
default AI switches to `MiniMaxAI` (head-to-head play), while 3–4
player games default to `MultiMaxAI`.  Each AI seat is an independent
instance configured by the `-Dtigris.ai.*` properties above.

The **starting player is chosen at random** — you don't always move
first.  If a computer goes first you will see its moves in the game
log, paced by `-Dtigris.delay`; the teal roster icon shows whose turn
it is.  (If a fixed seed is used in custom setups, the same seed
always yields the same first player.)

One caveat: `MiniMaxAI` scores positions as *player 0 minus
player 1*, so it only plays correctly in 2-player games — the program
prints a warning if you select it for larger games; use `-Dtigris.ai=MultiMaxAI`
or `-Dtigris.ai=CraigAI` for 3–4 player games, as those evaluate
every seat's position.  CraigAI thinks for a bounded number of
milliseconds rather than by search size, so it is the cheapest
opponent to run (see `-Dtigris.ai.thinkMillis`).

By default the GUI measures your screen and scales a 1080p display to
1.0×, 4K to 2.0×, etc. (rounded to the nearest 0.25 and clamped to
1–4). This scaling applies to the window size, all in-game fonts and
panels, and the menu bar and menu item text. Use `-Dtigris.scale` if
the automatic detection gets it wrong for your display configuration.

Example combining both:

```bash
java -Dtigris.scale=2.5 -jar dist/lib/tigrisgame.jar mygame.tigris
```

---

## The GUI — a user's guide

### Screen layout

```
+---------------------------------------------------+
|                        |  player labels (status)  |
|                        |  selection panel         |
|      game board        |   (current selection     |
|   (11 rows x 16 cols,  |    and instructions)     |
|    water, kingdoms)    |                          |
|                        |  Tile Bag  |  OK / Cancel|
+------------------------+--------------------------+
|     player screen (your tiles, leaders, VP)       |
+---------------------------------------------------+
```

* **Board (center)** — columns are numbered along the top and bottom,
  rows are lettered along the sides (an 11×16 board). Together they
  give each space an address (e.g. `a1`), which is also the format the
  text client uses.
  Highlights show your **last action** (yellow) and, during conflicts,
  the tiles involved (red = attacker, green = defender).
* **Player screen (bottom)** — shows your current hand of tiles, your
  leaders in play, treasure counts, and your four victory-point
  tallies (one per color, treasures assigned to the best ones). You
  **select tiles and leaders here** by clicking on them.
* **Right panel** — the player roster at the top; the selection panel
  prompts you for what the game needs next; the **Tile Bag** button
  shows tiles remaining and is also the way to start a tile swap.
  * *Roster* — the icon's color marks the seat's state: gray is
    waiting, teal is the seat whose move it is, and magenta means one
    of that seat's decisions (e.g. conflict support) is pending.
  * *Game log* — everything the prompts say and every move made is
    appended to a scrolling log (newest at the bottom, which is kept
    in view). Opponent moves are spelled out, e.g.
    `Computer 1: blue tile at c5`, so you can follow play; the scroll
    back to review earlier turns. To watch comfortably, set a pacing
    delay with `-Dtigris.delay` (default 3 seconds after each
    opponent move).

### Menus

* *Game*
  * **Save (Local View)** — save the game you see to a file
  * **Undo Move** / **Undo 3 Moves** — request an undo; any human
    opponents must approve it (computer opponents approve
    automatically)
  * **Exit**
* *Options*
  * **Show Grid** — draw the coordinate labels around the board
  * **Show Kingdoms** — number and outline all kingdoms on the board
  * **Show Last Action** — highlight the space of the last move played

### Making moves

Each turn has **two actions**. The selection panel always tells you
what the game is waiting for. Moves are made by clicking: first select
something in your player screen (or the Tile Bag button), then click a
destination on the board.

* **Place a tile** — click a tile in your player screen, then click a
  board space.
* **Position a leader** — click a leader in your player screen, then
  click a board space. Clicking the empty leader slot instead
  *withdraws* the leader back to your hand.
* **Play a catastrophe** — click the catastrophe slot, then click a
  board space.
* **Swap tiles** — click the **Tile Bag** button, then click the tiles
  you want to give back, then press **OK**. **Cancel** abandons the
  swap.
* **Cancel** during a normal action just clears your current
  selection.

When it is not your turn (or the AI is thinking), moves are disabled
and the selection panel explains what is happening — e.g. during
conflicts or undo requests.

### Conflicts

When two kingdoms merge and leaders of the same color meet, a conflict
starts. The selection panel switches to a conflict view showing both
sides' board support and screen (hand) support.

* The **attacker** first, then the **defender**, may support their
  leader by clicking matching tiles in their hand and confirming with
  **OK**. (The English variant rules are enforced: you cannot dump
  support tiles that cannot possibly tie or win.)
* Next, the defender(s) choose the **order** in which the tied
  conflicts resolve by clicking a tile or leader of the desired color.
* Losers have their leaders removed; monuments on losing temples may
  lose points.

### Treasures

If ending your action leaves a kingdom with two or more treasures and
a green (trader) leader, the trader's owner is prompted to take a
treasure: simply click the space holding it.

### Monuments

After you place a tile that completes a 2×2 square of four plain
tiles, the game enters the *Build Monument* phase. You may either
build or decline:

1. **Click a board space** — the upper-left corner of where the
   monument should go.
2. **Click a tile on the board** to choose the monument's *second*
   sphere color. (The first sphere is the color of the tile you just
   placed.)
3. Or press **Cancel** to decline building.

The rules engine enforces:

* the monument square must include the tile you just played,
* both spheres must be different colors,
* at least one of them must match the color of the last tile you
  placed,
* no duplicate monument types (sphere combinations) may already exist.

Monument victory points are awarded at the end of your turn; any
leaders orphaned by the monument placement are removed.

### Undoing moves

Choose *Game → Undo Move* (or *Undo 3 Moves*). Every other player is
asked to approve the undo; computer opponents approve automatically.
When a human opponent is asked, the status field shows "Waiting for
permission from other players" while they decide, and on their side
the request appears as a prompt to accept with **OK** or reject with
**Cancel**. Undo requests sent to you appear the same way.

### End of game

The game ends when the tile bag runs out or fewer than three
treasures remain on the board. Final scores (victory points in each
of the four colors, best color counting) appear in the player list on
the right.

---

## Text client cheat sheet

Commands: `help`, `rules`, `list`, `save <filename>`, `show`, `prompt`,
`move`, `undo <n>`, `quit`. `/` is shorthand for `show`.

Colors: `B`lack (King), `R`ed (Priest), bl`U`e (Farmer), `G`reen
(Trader). Spaces: `<letter><number>`, e.g. `c5`.

Move syntax by game state (also shown by `help move`):

| State          | Moves                                                                                             |
| -------------- | ------------------------------------------------------------------------------------------------- |
| your action    | `leader <color> <space>`, `tile <color> <space>`, `catastrophe <space>`, `swap <color> <num> ...` |
| treasure       | `<space>`                                                                                         |
| support        | `<number>` of matching tiles                                                                      |
| conflict order | `<color>`                                                                                         |
| monument       | `none`, `<color>`, or `<color> <space>`                                                           |

Shorthand (no spaces, case-insensitive): `Tb3`, `Lkc10`, `Cf10`,
`S1200` (swap 1 black, 2 red, 0 blue, 0 green).

---

## Project layout

```
build.xml                 Ant build file
src/com/tigrisgame/
    awtui/                AWT graphical interface
    textui/               plain-text interface
    rules/                game server, board, moves, conflict resolution
    ai/                   computer opponents (MiniMaxAI, MultiMaxAI, ...)
    util/                 helpers (saves, resource loading, sorting)
dist/lib/tigrisgame.jar   runnable jar (built by `ant jar`)
```

## Troubleshooting

* **Everything looks tiny on a 4K screen** — run with an explicit
  scale (e.g. `java -Dtigris.scale=2.5 -jar dist/lib/tigrisgame.jar`).
* **`ant run` fails with a manifest error** — rebuild the manifest
  with `ant jar`; `src/MANIFEST.MF` must exist with
  `Main-Class: com.tigrisgame.awtui.GUIGameClient`.
* **Windows / Cygwin** — `setup.cygwin.bash` sets up the environment
  variables (`SWING_HOME`, `LOG4J_HOME`) used by the historical class
  path in `build.xml`; on modern JDKs the default paths suffice and
  those variables are simply ignored.

---

## Opponent AI Models

All three opponents are `GameObserver`s that think in a background
thread, but they differ substantially in architecture, philosophy,
and maturity.

### MiniMaxAI — two-player minimax, alpha–beta, tuned evaluator

*Search:* Depth-limited minimax over a tree of cloned games. Two depth
axes (`maxActionDepth`, the number of full actions, plus
`maxTrueDepth`, a total ply cap) and a branch-factor cap
(`maxPruneSize`, 80 best moves kept per node, pre-sorted by
"promisingness"). Has **alpha–beta pruning** (`useAlphaBeta = true`).

*Hidden information:* When a conflict reaches a support decision under
a shrouded hand, it branches over "assumed" hands (0 … n matching
tiles the player could reveal), assigns each a probability, prunes
assumptions below `keepProb` (1%), and lets the minimax treat them
like normal opponent moves.

*Evaluation:* Composite — `VPEval + LeaderEval + MonumentEval +
HandEval` (plus `WinnerEval` at game over). These weights are the
result of hill-climbing tuning (`G44s0`: e.g. treasure bonus 2811,
monument score 2580).

*Limitation:* Its evaluation is literally
`score(player0) − score(player1)`, so it is only correct in 2-player
games (hence the warning and the 2-player-only default).

### MultiMaxAI — the multi-player generalization

Structurally a sibling of MiniMaxAI (same `MM_Info`/`MM_Move`
machinery, action/true depth, prune size, `keepProb`, chance-branching
with assumed hands, same tuned weights). Key differences:

* **N-player correct.** `evaluateGame()` builds a full per-player
  score array and converts it into relative standings: *my score × 2 −
  1st − 2nd* — explicitly reasoning that 1st wants to pull away from
  2nd, and 3rd is happy to throw points at 4th. This king-making-aware
  formulation is the right shape for Tigris & Euphrates.
* **No alpha–beta** — it does not apply cleanly when scores are not
  2-player zero-sum, so searches rely on the prune cap and depth
  limits.
* Slightly shallower defaults (1 action / 10 plies / 80 prune) vs.
  MiniMax's fixed 2/20/80.
* Verified-info subtlety: MiniMax's `generateAssumeMoves` has a fast
  path that computes guaranteed win amounts for the attacker when the
  defender's hand is fully known (which it is "without swap or
  depth>3"); MultiMax relies on the generic assumption machinery.

### CraigAI — the original: time-limited expected-value search

Marked "First AI implementation" and built differently in every
respect:

* **Time-driven, not node-driven.** Thinks until `maxThinkMillis`
  (30 s) expires, then throws `ForceMoveException` to unwind the
  current branch and returns the best move found so far. Adjustable
  via `setThinkTime()`/`setThinkActionDepth()`. There is no prune-size
  cap — instead it bounds support moves to "go hard or go home":
  attacker plays 0 or max, defender ties or nothing.
* **Enumeration + relative scoring, no minimax.** For each possible
  move it clones the game and evaluates recursively; at the depth cut
  it just calls the static evaluator rather than switching
  perspective. Best move = highest score *for the currently moving
  player* — a greedy "each seat maximizes its own score" model rather
  than explicit opposition play, which is cheap and surprisingly
  reasonable in multi-player games. Scoring is per-player **relative
  to the table average** (`score − avg`).
* **Probability, but cruder.** Full expected-value branching over
  revealed support tiles using a **binomial distribution** over an
  estimated per-tile likelihood (a TODO admits it wants the better
  probability model the Max AIs have), and it even tracks the standard
  deviation (for logging).
* **Lean evaluation.** Only `VPEval` — no leader/monument/hand
  compounding.

### Practical summary

|                  | MiniMaxAI                  | MultiMaxAI                            | CraigAI                             |
| ---------------- | -------------------------- | ------------------------------------- | ----------------------------------- |
| Players          | 2 only                     | 2–4                                   | 2–4                                 |
| Opposition model | true minimax, alpha–beta   | minimax, no alpha–beta (multi-player) | greedy per-seat maximization        |
| Cut condition    | depth + branch cap         | depth + branch cap                    | wall-clock + depth                  |
| Chance model     | assumed hands, `keepProb`  | assumed hands, `keepProb`             | binomial expectation                |
| Evaluation       | 4-module composite (tuned) | same composite                        | VP only                             |
| Typical cost     | heaviest (deep, wide)      | heavy but shallower                   | bounded by seconds, large branching |
| Default when     | `-Dtigris.players=2`       | `tigris.players=3,4` (default 4)      | only when explicitly selected       |

One integration note: only MiniMaxAI and MultiMaxAI are configurable
through the `-Dtigris.ai.*` depth/prune/probability switches. CraigAI
takes the game server in its constructor (it has no `setGameServer`)
and is bounded by `-Dtigris.ai.thinkMillis` and

### Tuning the AI Opponents

The AI behavior is controlled by the following command‑line switches (see the **Command‑line switches** table). Adjusting them will make the opponents stronger or faster.

- **`-Dtigris.ai.actionDepth=N`** – number of full actions the AI looks ahead.
  Default: 2 (MiniMaxAI), 1 (MultiMaxAI).  Raise to 3–4 for deeper planning.

- **`-Dtigris.ai.trueDepth=N`** – total plies (half‑moves) searched.
  Default: 20 (MiniMaxAI), 10 (MultiMaxAI).  Larger values increase depth exponentially.

- **`-Dtigris.ai.prune=N`** – maximum candidate moves kept at each node.
  Default: 80 (MiniMaxAI/MultiMaxAI).  Set to 200–500 or 1000 for “unlimited” to explore more possibilities.

  Note: this cap matters differently for each AI.  `MiniMaxAI` combines it with
  alpha–beta pruning (and now move ordering), so a smaller `prune` mainly trims
  the frontier; `MultiMaxAI` has no alpha–beta cutoffs, so `prune` is the
  *only* thing bounding its search tree — lowering it (e.g. to 80–200) is the
  most effective way to keep `MultiMaxAI` fast at deeper `actionDepth`.

  Empirical rule of thumb for `MultiMaxAI`: the move generator naturally
  produces fewer than ~150 candidates per node, so `prune` values above ~150
  are effectively a no-op — depth 3 at `prune=1000` takes the same ~6 s as
  `prune=200`.  You must drop below ~150 (try `prune=80`–`120`) to get any
  speed benefit.  Depth 4 is not practical for `MultiMaxAI` at any prune.

- **`-Dtigris.ai.keepProb=X`** – for MultiMaxAI, probability threshold below which assumed opponent support hands are pruned.
  Default: 0.01.  Lower it to 0.001 or 0.0001 to let the AI consider rarer hands.

- **`-Dtigris.ai.thinkMillis=N`** – only for CraigAI, wall‑clock time the AI spends per move.
  Default: 30000 ms.  Reduce to speed up play or increase for a tougher CraigAI.

**Example – a stronger MiniMaxAI**

```bash
java -Dtigris.ai.actionDepth=3 -Dtigris.ai.trueDepth=25 -Dtigris.ai.prune=300 -jar dist/lib/tigrisgame.jar
```

**Example – a stronger MultiMaxAI**

```bash
java -Dtigris.ai.actionDepth=2 -Dtigris.ai.trueDepth=15 -Dtigris.ai.prune=200 -Dtigris.ai.keepProb=0.001 -jar dist/lib/tigrisgame.jar
```

Remember that higher values mean longer thinking times per move.  Balance the parameters to keep gameplay enjoyable while giving the AI a competitive edge.
`-Dtigris.ai.actionDepth` instead; it was written first, and the Max
family was created specifically to supersede it.

---

## Change Log

* **1.0** — Original Craig Moffitt Release circa 2007.
* **1.1** — Modifications by leonatkinson:
  * automatic UI scaling for HiDPI/4K displays, with a
    `-Dtigris.scale` override (fonts, panels, tile sizes, menu text)
  * fixed the menu bar never appearing (`main()` was showing a frame
    without the menus) and scaled the menu fonts
  * fixed the `MiniMaxAI()` default constructor passing its two search
    depth arguments swapped, which limited the opponent to a 2-ply
    search
  * selectable opponent: `-Dtigris.ai=MiniMaxAI|MultiMaxAI|CraigAI`
    with configurable search parameters (`-Dtigris.ai.*`)
  * 2–4 player games (`-Dtigris.players`, default 4): you at seat 0
    ("You"), AI opponents named "Computer 1"…, seat-count-aware AI
    defaults plus a warning when `MiniMaxAI` is used with 3+ players
  * restored `src/MANIFEST.MF` so `ant jar` produces a runnable jar
  * player roster icons drawn with dedicated state colors that don't
    collide with any tile, terrain, or sphere color, and that stay
    distinguishable under common color-vision deficiencies — neutral
    gray waiting, teal seat to move, magenta pending decision, with
    the dynasty glyph picking black or white for contrast
  * the game-over screen now shows the real results instead of a
    "scores..." placeholder: each player's actual score (lowest
    sphere total after treasure assignment) with the per-sphere
    breakdown, best score listed first
  * a scrollable game log: every move (yours and the computers',
    described in words) and every prompt is appended in the
    selection panel, newest at the bottom and auto-scrolled into
    view; victory-point awards (tiles, conflicts, monuments) are
    logged too, right under the move that earned them, e.g.
    "You: +1 red VP (tile)"; `-Dtigris.delay=S` adds a pause
    (default 3 seconds) after each opponent move so a human can
    follow the play
  * fixed victory-point tokens on your own screen not updating
    after your first move of the game (the panel was re-bound to a
    throwaway copy of the game each time your turn came up, so
    points earned later never appeared until the next turn)
  * the starting player is chosen at random instead of always being
    seat 0
  * added `README.md` and the plain-text `LICENSE` (AFL 3.0)
