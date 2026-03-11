# Design Agent Memory

## Project: Chess Application (JavaFX)

### Theme System
- 6 themes: 3 dark (Midnight/purple, Ember/charcoal, Abyss/ocean) + 3 light (Manuscript/paper, Fjord/arctic, Sakura/cherry)
- CSS custom properties use bare names (no `--` prefix): `app-primary`, `board-light`, etc.
- Theme files at: `modules/application/src/main/resources/css/themes/`
- Base CSS (structure only, zero colors): `modules/application/src/main/resources/css/base.css`
- JavaFX CSS type selectors (`Label`) have lower specificity than class selectors (`.player-name`)

### Board Color Palettes (finalized 2026-03-11)
- Midnight: light #c4b5d6 / dark #5c4478 (4.28:1)
- Ember: light #d9b88c / dark #7a4e2e (3.78:1)
- Abyss: light #9abcc8 / dark #2c5a6e (3.72:1)
- Manuscript: light #e8d5b8 / dark #8a6842 (3.54:1)
- Fjord: light #d4e0ec / dark #4f7a96 (3.44:1)
- Sakura: light #f0d0d6 / dark #a0586e (3.57:1)
- Badge win: #15803d, Badge draw: #b45309 (both 5.02:1 vs white)

### Key Constraint
- Coordinate labels on chess squares use opposite square color as text
- Therefore light/dark square contrast ratio = label readability ratio
- Minimum 3:1 required for small bold text labels
