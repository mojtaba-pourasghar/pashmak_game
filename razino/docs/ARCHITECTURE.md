# معماری رازینو — Architecture

Razino is a Persian, right-to-left, 2D mystery-adventure for ages 8–12, built
on **Godot 4.3** with **GDScript**. This document describes what is actually in
the repository.

## The one idea the whole design rests on

**Chapters are data, not code.** A chapter is a JSON file plus its assets. The
engine understands a fixed, closed vocabulary of conditions and effects, and
nothing else. That single decision buys three things the GDD asks for:

1. A writer can add chapters 11, 12, 13 without a programmer and without a
   store release.
2. A downloaded package can never execute anything, because there is nothing
   in the format that can express "run this".
3. The offline validator and the runtime validator can be checked against each
   other — `validate_content.py` reads the vocabulary straight out of
   `story_engine.gd`, so the two cannot drift apart.

## Layout

```
razino/
├── project.godot            RTL forced engine-wide, GL-compatibility renderer
├── scenes/
│   ├── boot/                UI-01 splash and first-run routing
│   ├── menus/               UI-02..05, 08, 10..16
│   ├── gameplay/            UI-06, the screen the game happens on
│   ├── dialogue/            UI-07 standalone conversation host
│   ├── puzzles/             UI-09 standalone puzzle host
│   └── common/              RazinoScreen, the shell every screen shares
├── scripts/
│   ├── core/                Log, Settings, GameState, Router, ParentalSettings,
│   │                        Palette, Fa (Persian numerals)
│   ├── story_engine/        StoryEngine, ClueJournal
│   ├── save_system/         SaveManager
│   ├── audio/               AudioManager
│   ├── content_delivery/    ContentManager, ContentEndpoints
│   ├── purchases/           PurchaseManager, StoreBackend, SandboxStoreBackend
│   ├── avatar/              AvatarManager
│   └── ui/                  Style, SceneStage, CharacterFigure, AvatarPortrait,
│                            DialoguePanel, PuzzlePanel, PlaceholderArt
├── content/                 Authoring source for all ten chapters
│   ├── manifest/catalog.json
│   └── season_01 … season_10/story.json
├── content_tools/           Validator, chapter builder, packer/signer, server,
│                            asset report
├── tests/                   Headless suite + content-delivery integration test
├── tools/                   Screenshot rig, integration runner
└── docs/
```

## Autoloads, in load order

| Autoload | Responsibility |
|---|---|
| `Log` | Levelled logging to `user://logs/`. Never records personal data. |
| `Settings` | Volumes, subtitles, text size, reduced motion. Stored separately from the save so wiping progress never resets accessibility. |
| `SaveManager` | Atomic save with verify-then-swap, a backup slot, and forward migration of old schemas. |
| `GameState` | The single source of truth for everything a save contains. |
| `AudioManager` | Three independent buses. A missing clip is an expected state, not a crash. |
| `ContentManager` | Catalogue, download, validation, atomic activation, offline resolution. |
| `StoryEngine` | Scenes, dialogue, conditions, choices, puzzles, endings. |
| `ClueJournal` | Clue definitions, categories and combinations. |
| `AvatarManager` | Parametric avatar; optional, consent-gated photo hints. |
| `PurchaseManager` | Entitlements, server verification, restore. |
| `ParentalSettings` | PIN gate and the consent flags everything sensitive checks. |
| `Style` | Builds the theme from `Palette` and attaches it to each screen. |
| `Router` | Screen navigation by GDD UI id, with a fade. |

## The story format

A scene is a dictionary. `scene_type` is one of `dialogue`, `explore`,
`puzzle`, `cinematic`. See `content_tools/content_schema.json` for the full
contract and `docs/CONTENT_AUTHORING.md` for how to write one.

**Conditions** (`StoryEngine.CONDITIONS`): `has_clue`, `lacks_clue`,
`has_item`, `lacks_item`, `flag`, `not_flag`, `trust_at_least`, `trust_below`,
`chapter_complete`, `any`, `all`.

**Effects** (`StoryEngine.EFFECTS`): `add_clue`, `add_item`, `set_flag`,
`add_trust`, `unlock_chapter`, `play_sfx`, `play_music`, `complete_chapter`,
`set_ending`.

An unknown condition evaluates to **false** and is logged. Failing closed
matters: a typo in content data must never open a path that was meant to be
locked.

## Events between systems

Systems talk through signals rather than reaching into each other:

```
clue_discovered(clue_id)            GameState  -> journal, HUD toast
story_choice_selected(choice_id)    StoryEngine -> save, analytics hook
chapter_completed(chapter_id)       GameState  -> chapter map, unlocks
content_download_completed(id)      ContentManager -> downloads screen
purchase_succeeded(product_id)      PurchaseManager -> store, chapter map
avatar_changed(avatar_id)           AvatarManager -> every portrait on screen
```

## Save format

`user://save_01.json`, with `user://save_01.bak.json` as the previous good
copy. Writes go to a temp file, are read back and parsed to prove they are
intact, and only then replace the live save — so an interruption mid-write
cannot destroy what was already there. `schema` is versioned and migrated
forward in `SaveManager._migrate`; old saves are never discarded.

## Content packages

See `docs/BACKEND.md`. In short: a package is staged in
`user://content_staging/`, every file is checked against its recorded size and
SHA-256, the manifest's RSA signature is verified against the public key
bundled in the app, and only then is the package moved into place in one
rename. A failure at any step leaves the previously installed version live.

## Right-to-left

RTL is forced at the engine level
(`internationalization/rendering/force_right_to_left_layout_direction`), not
per-screen. `Style` builds one theme from `Palette`; screens use
`Style.make_title/make_body/make_button/make_card`. Persian numerals go through
`Fa`. `tests/cases/test_rtl_and_fonts.gd` checks that joined text measures
narrower than the isolated letters, which is a real test that shaping works
rather than an assumption that it does.
