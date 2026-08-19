# Changelog

Newest first. Jars look like `vanillaplusaccents-1.0.6-Minecraft26.2.jar`. GitHub tags look like `v1.0.6-mc26.2`.

Do not treat 1.0.7 as released — **1.0.6** is the current 26.2 patch.

## Minecraft version notes

Vanilla Plus Accents started on **Minecraft 26.1.2** (official Mojang names, Java 25, Fabric Loader 0.19.2, Fabric API `0.149.0+26.1.2`) and was retargeted to **26.2** (Loader 0.19.3, Fabric API `0.157.0+26.2`).

**For players / pack makers:**

- Use the jar whose Minecraft number matches the instance.
- **1.0.6 exists for 26.2 only.** On 26.1.2 the latest tagged build is **1.0.4**.
- 26.2 needed extra renderer work so sign items stay flush, flower stems stay spaced, and fence ropes match animal-lead rib spacing. Gameplay commands and recipes stay the same idea as 26.1.2.

**For developers (code / API):**

- 26.2 dropped `submitSignWithText`. Sign items inject `submit` **before** `submitSignText` at ordinal **0** (front) and **1** (back).
- Piggyback, fence leads, and flower patches were remapped to 26.2 render / entity APIs (`3cfbdf9`).
- Fence-to-fence leads draw **one** 24-step catenary ribbon (vanilla `LEASH_RENDER_STEPS` rib count), not eight `submitLeash` calls.
- Flower patches: stem spacing ≥ ~0.60, vanilla model size, shared yaw so the two cross planes do not slice each other.

---

## Mod versions

### 1.0.6 (Minecraft 26.2)

GitHub: [v1.0.6-mc26.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.6-mc26.2) · jar `vanillaplusaccents-1.0.6-Minecraft26.2.jar`

Bugfix over 1.0.5 on 26.2.

**For players:**

- Fence-to-fence leads stay visible after looking away, walking off, chunk reload, and world rejoin. You no longer need to right-click a fence with a lead to make the ropes come back.
- The brown **ties (knots)** on each post come back after reconnect. Ropes meet those ties instead of ending in mid-air.
- Knots stay as long as a decorative span (or a hitched animal) is still on that post. They still disappear when the last span and last animal are gone.
- Starting a new rope is unchanged: lead an animal first to hitch it; otherwise a lead on a fence starts a pending rope.

**For developers:**

- Join/dimension sync sends `SyncFenceLeadsPayload` after the play handler is ready (`server.execute`); chunk load respawns markers and knots. SavedData remains the source of truth.
- `LeashFenceKnotEntityMixin` cancels `notifyLeasheeRemoved` while `FenceLeadVisuals.isKnotNeeded` (SavedData link or pending). Vanilla still discards empty knots with no VPA link.
- Completed `FenceLeadEntity` markers persist (`shouldBeSaved`); pending markers do not. Backup world renderer skips only when `isDrawingCompleted` (in-range + `DATA_TO` present), not merely because a matching entity is in the camera AABB. Entity renderer sets `affectedByCulling` false so looking away from the from-post still submits the span.

### 1.0.5 (Minecraft 26.2)

GitHub: [v1.0.5-mc26.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.5-mc26.2) · jar `vanillaplusaccents-1.0.5-Minecraft26.2.jar`

Patch over 1.0.4 on 26.2.

**For players:**

- Items on signs sit **flush** on standing, wall, and hanging signs (26.2 renderer catch-up).
- Flower patches stay vanilla-sized. Stems are farther apart so typical small flowers (poppies, dandelions, and similar) do not mostly cover each other — three stay a triangle, four a quad. Petals may spill into neighboring blocks.
- Fence-to-fence leads still droop as catenaries; the rope now uses the **same rib spacing** as a lead on an animal.
- README screenshots: stonecutter woodcutting and flush signs.

**For developers:**

- `AbstractSignRendererMixin`: inject `submit` before `submitSignText` ordinal 0/1 (`submitSignWithText` is gone on 26.2).
- `FlowerPatchOffsets.MIN_DISTANCE = 0.60`; shared patch yaw + small per-stem jitter.
- `Catenary.SEGMENTS = 24`; single ribbon in `FenceLeadRender` matching animal-lead ribs.

### 1.0.4 (Minecraft 26.2 / 26.1.2)

GitHub: [v1.0.4-mc26.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.4-mc26.2) · [v1.0.4-mc26.1.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.4-mc26.1.2)

**For players:**

- Right-click a fence while **leading a mob** hitches that animal (vanilla). Extra leads in hand do not start a decorative rope.
- Shears on a fence or knot drop every fence-to-fence span on that post (animals still use vanilla shear).
- Completed ropes droop as distance-aware catenaries (more sag in mid-range gaps, tighter on long runs).
- Includes 1.0.3 flush signs (on 26.1.2) and 1.0.2 flower-patch / woodcutting fixes.

**For developers:**

- Hitch the led entity before opening a pending fence rope. Catenary math in `Catenary`; 26.2 build is the same features retargeted (Loader 0.19.3 / API `0.157.0+26.2`).

### 1.0.3 (Minecraft 26.2 / 26.1.2)

GitHub: [v1.0.3-mc26.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.3-mc26.2) · [v1.0.3-mc26.1.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.3-mc26.1.2)

**For players:**

- Sign-displayed items sit flush on the sign face (the extra 1-pixel offset is gone). 26.2 jar is the same 1.0.3 features retargeted; flush rendering on 26.2 was finished in **1.0.5**.

**For developers:**

- 26.1.2 used the then-current sign submit path. 26.2 jar at 1.0.3 was a version retarget; see 1.0.5 for the `submitSignText` injects.

### 1.0.2 (Minecraft 26.1.2)

GitHub: [v1.0.2](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.2)

**For players:**

- Flower patches break like vanilla flowers when the block beneath them is removed.
- 3–4 flower patches no longer form a straight line. Flowers keep vanilla size; petals may overlap neighboring blocks.
- Stonecutter woodcutting recipes actually load (the mixin was never registered before).
- Stonecutter: planks → 1 stair or 2 slabs; log / wood / stem / hyphae / bamboo (including stripped) → matching fence or fence gate. Strip and log-to-planks recipes still apply.

**For developers:**

- Register the woodcutting mixin. Flower placement rejects collinear 3-stems and row-like quads.

### 1.0.1 (Minecraft 26.1.2)

GitHub: [v1.0.1](https://github.com/marcosdherrero/VanillaPlusAccents/releases/tag/v1.0.1)

**For players:**

- Piggyback works in multiplayer, stacks (A on B on C), and sitting only on **upright** stairs (not upside-down).
- World-wide grief toggles (`enderman_grief`, `creeper_grief`), dirt-path / mud / Happy Ghast speed commands, stonecutter woodcutting, and fence-lead fixes.
- Path speed stays through jumps and Slow Falling, then clears when you land off-path.
- Path / mud speed no longer zooms FOV (attribute speed still can).

**For developers:**

- Path-speed FOV exclusion; path speed must not drop mid-air. Operator settings live in overworld `SavedData`.

### 1.0.0 (Minecraft 26.1.2)

Initial public release (git `d023cff`). No separate GitHub `v1.0.0` tag — 1.0.1 is the first tagged GitHub release.

**For players:**

- Shear item frames to hide the backing. Place items on empty signs. Fence-to-fence leads, sitting on slabs/stairs, flower patches (up to 4 per block).

**For developers:**

- Fabric 26.1.2, official Mojang names, `main` + `client` source sets.
