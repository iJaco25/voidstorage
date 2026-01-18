# Art Style Description: Ethereal Void-Tech

The art style for Void Covenant must bridge the gap between the grounded, blocky fantasy world of _Hytale_ and an alien, infinite dimension of pure magic. It must feel ancient, powerful, and slightly dangerous, rejecting machinery in favor of bound arcane forces.

### 1. Core Aesthetic Pillars

- **Arcane, Not Industrial:** There are no gears, pistons, smoky exhausts, or copper wires. Movement is achieved through floating runes, shimmering rifts, and bound spirits. The aesthetic is closer to high-fantasy wizardry than steampunk engineering.
- **Weight vs. Weightlessness:** A central visual conflict exists between the **Void** elements and the **Anchor** elements.
- _The Void_ (Anomaly, Sigils, Fairies) is weightless, translucent, emissive, and constantly shifting.
- _The Anchors_ (Chains, pillars) are incredibly heavy, dark, rugged, and static, visually grounding the volatile magic.

- **The "Hytale" Filter:** All elements must adhere to _Hytale’s_ stylized voxel fidelity. Textures should have painted details, visible brushstrokes in the normal maps, and chunky, satisfying modeling, even when depicting energy.

### 2. Color Palette

The palette is dominated by cool, deep tones contrasted by searing bright energy.

- **Primary (The Void):** Abyssal Black (Vantablack), Deep Indigo, Ultraviolet, and shimmering Magenta. These colors should feel deep and consuming.
- **Secondary (The Energy):**
- _Absorption (Input):_ Electric Cyan fading to deep Sapphire.
- _Manifestation (Output):_ Fiery Arcane Orange fading to bright Gold.

- **Tertiary (The Anchors):** Obsidian, dark basalt gray, weathered wrought iron. Stressed areas might have faint, glowing purple cracks.

### 3. Materiality & Lighting

- **Emissive Dominance:** Almost every active component of this mod should emit light. The Anomaly should cast deep purple shadows, while Sigils should light up the chests they hover over.
- **Crystalline Energy:** The "Living Sigils" and the "Void Fairy" should look like solidified mana—semi-transparent, with internal facets that refract light like quartz, rather than looking like gaseous smoke.
- **Ancient Stress:** Physical blocks (Anchors) should look old and under immense pressure. Cracks shouldn't look like natural weathering, but like the stone is being pulled apart by gravitational forces.

---

# Texture Concepts & Implementation Strategy

Here we will define the initial textures needed to realize this vision within the _Hytale_ engine.

### Texture Set 1: The Anomaly (The Heart)

The Anomaly is not a solid block; it is a VFX-heavy entity. The texture needs to convey a tear in reality.

**Texture Name:** `void_tear_core_anim`

- **Description:** A seamless, animated texture representing the event horizon of the tear. It should not look like a flat picture of space. It should look like a window into a turbulent, dark dimension.
- **Visual Details:**
- **Base Layer:** Swirling nebulae of deep violet and black.
- **Animation:** A slow, hypnotic inward spiral. It should feel like it is actively pulling nearby pixels into it.
- **Edge Detail:** The edges of the texture should be ragged and glowing brightly with magenta energy, where reality is fraying.

- **Technical Properties:**
- _Emissive Map:_ 100% emission on the bright edges, pulsing emission on the inner swirls.
- _Transparency:_ Semi-transparent center, allowing the background world to distort slightly behind it.

### Texture Set 2: The Anchor Pillars & Chains

These need to look incredibly heavy to visually justify holding the Anomaly in place.

**Texture Name:** `voidstone_pillar_stressed`

- **Description:** A dark, dense stone texture for the pillars that physically connect to the Anomaly.
- **Visual Details:**
- **Base Material:** Dark basalt or obsidian. It should feel cold and hard. High-frequency noise in the normal map to give it a rough, chiseled feel consistent with Hytale stone.
- **The "Stress" Factor:** Deep, jagged cracks running through the stone. Unlike normal cracked stone, these cracks should glow faintly with the purple energy of the Void, showing that the stone is barely containing the energy.

- **Technical Properties:**
- _Normal Map:_ Deep grooves for the cracks.
- _Emissive Map:_ Low-level, pulsing glow only within the deepest cracks.

**Texture Name:** `titan_chain_link`

- **Description:** For the massive chains connecting the pillars to the Anomaly.
- **Visual Details:** Wrought, blackened iron. It shouldn't look shiny or new. It should look pitted, ancient, and incredibly thick. Perhaps slight crystalline purple residue has formed in the links where they rub together.

### Texture Set 3: The Living Sigils (Automation)

These are floating, rotating runic constructs. They are pure energy.

**General Style:** These textures are meant to be applied to flat, floating geometric planes that rotate above target blocks. They must be transparent and highly emissive.

**Texture Name:** `sigil_absorption_cyan (Input)`

- **Description:** The "suck" rune.
- **Visual Details:** A complex, angular runic pattern glowing electric cyan. The visual language should be "implosive." Arrows or energy lines within the rune should point inward toward the center.
- **Animation:** The texture itself should have a subtle scrolling effect toward the center, reinforcing the "pulling" action, on top of the actual 3D rotation of the model.

**Texture Name:** `sigil_manifestation_orange (Output)`

- **Description:** The "push" rune.
- **Visual Details:** A similar runic complexity to the absorption sigil, but glowing fiery orange/gold. The visual language is "explosive," with energy lines radiating outward from the center core.
- **Animation:** A subtle outward scrolling pulse effect.
