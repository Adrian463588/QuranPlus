# Anti-Slop UI Playbook for AI Android Developer Agents

I reviewed current guidance from **Android Developers, Material Design 3, W3C/WCAG, Nielsen Norman Group, and the Agent Skills specification**. The strongest conclusion is that preventing “AI slop” is less about choosing a particular visual style and more about **constraining the agent's design decisions**.

A good AI developer agent should not be instructed merely to “make the UI beautiful.” It should operate under a small design system, explicit hierarchy rules, density limits, accessibility requirements, and a mandatory review/refinement loop.

Material Design emphasizes consistent grids, spacing, typography, color roles, and adaptive layouts. Nielsen Norman Group similarly recommends maximizing useful information while minimizing visual noise and unnecessary decoration. ([Material Design][1])

---

## 1. First define what “AI slop UI” means

For an AI agent, I would operationally define **UI slop** as:

> A UI where visual decisions are generated independently instead of being derived from hierarchy, content, interaction priority, and a coherent design system.

Typical symptoms include:

* card inside card inside card
* everything having a rounded background
* excessive pills and chips
* too many icons
* gradients without functional meaning
* shadows on almost every component
* multiple accent colors competing for attention
* excessive headings and subheadings
* oversized hero sections on utility screens
* every feature being visible simultaneously
* too many buttons having primary styling
* repeated labels describing obvious controls
* arbitrary spacing values
* arbitrary corner radii
* decorative illustrations that do not help the task
* excessive animation
* excessive badges
* excessive dividers
* screens built as collections of “components” rather than as a coherent information hierarchy

NN/g's minimalist-design guidance captures the core principle well: maximize **signal** and minimize **noise**. Decorative elements, redundant information, unnecessary typography changes, and irrelevant graphics increase cognitive load rather than improving usability. ([Nielsen Norman Group][2])

---

# 2. The most important rule: Design hierarchy before components

The agent should **never start by generating Compose components**.

Force this sequence:

```text
User goal
    ↓
Primary task
    ↓
Information hierarchy
    ↓
Interaction hierarchy
    ↓
Screen structure
    ↓
Components
    ↓
Styling
    ↓
Motion / polish
```

Not:

```text
"Build dashboard"
    ↓
Card
Card
Gradient Card
Chip
FAB
Badge
Card
Card
```

Before writing UI code, require the agent to answer internally:

```text
What is the single primary purpose of this screen?

What is the primary action?

What information must be visible immediately?

What information can wait?

What is supporting information?

What is optional/advanced information?

What can be removed entirely?
```

This matches NN/g's recommendation to remove unnecessary information and progressively disclose uncommon functionality instead of presenting everything simultaneously. ([Nielsen Norman Group][2])

---

# 3. One screen should have one dominant visual priority

Require this hierarchy:

```text
Level 1 — Primary purpose / content
Level 2 — Primary action
Level 3 — Supporting information
Level 4 — Secondary actions
Level 5 — Metadata / tertiary information
```

Do not allow ten elements to have equal emphasis.

For example, avoid:

```text
[ BIG BUTTON ]
[ BIG BUTTON ]
[ BIG CARD ]
[ BRIGHT CHIP ]
[ BIG BUTTON ]
[ BRIGHT CARD ]
```

Prefer:

```text
Screen title

Important content

Primary action

Supporting content
Secondary action
Metadata
```

Consistent typography, refined color usage, alignment, and clear hierarchy are recurring characteristics of visually strong interfaces. ([Nielsen Norman Group][3])

---

# 4. Use a strict visual budget

This is one of the most useful constraints you can give an AI agent.

For an ordinary Android screen, use something like:

| Property                      | Agent default       |
| ----------------------------- | ------------------- |
| Primary CTA                   | **1**               |
| Strong accent color           | **1 family**        |
| Typography emphasis levels    | **3–4**             |
| Surface elevation levels      | **2–3 max**         |
| Major content groups          | **3–5**             |
| Simultaneously visible badges | minimal             |
| Decorative illustrations      | normally **0**      |
| FAB                           | only when justified |
| Gradient                      | normally **0**      |
| Nested cards                  | avoid               |
| Unique corner-radius values   | **2–3 max**         |

These numbers are intentionally more restrictive than Material's available component set. They are an **agent policy**, not Android requirements.

The idea is important: **available components are a toolbox, not a checklist of things that should appear on every screen.**

---

# 5. Stop “Card Soup”

One of the most recognizable AI-generated patterns is:

```text
Card
 └─ Card
     └─ Row
         └─ Pill
```

Do **not** use a Card simply because several elements belong together.

First attempt grouping through:

1. proximity
2. spacing
3. alignment
4. typography
5. background separation

Only afterward consider explicit containers.

Material says spacing helps group content and direct attention, while layout grids establish the structural foundation of the interface. ([Material Design][1])

A useful agent rule is:

```text
Do not add a Surface or Card unless the container communicates
meaning, interaction, state, or separation that spacing alone
cannot communicate.
```

---

# 6. Make whitespace structural, not decorative

Whitespace should explain relationships.

Use:

```text
small gap   = elements belong together
medium gap  = related groups
large gap   = new section
```

Do not generate values like:

```kotlin
7.dp
13.dp
19.dp
27.dp
```

without a specific reason.

Material's 2026 spacing guidance describes its spacing system as based on an **8dp scale**, providing a systematic foundation for adaptive design. ([Material Design][4])

Your own system could therefore use tokens such as:

```kotlin
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

The important point is **tokenized consistency**, not blindly applying 8dp everywhere.

---

# 7. Typography should create hierarchy—not decoration

Do not let the agent invent:

```text
13sp semibold
15sp bold
17sp medium
19sp semibold
21sp extra bold
26sp medium
```

per screen.

Use Material typography roles through your theme:

```kotlin
MaterialTheme.typography.titleLarge
MaterialTheme.typography.titleMedium
MaterialTheme.typography.bodyLarge
MaterialTheme.typography.bodyMedium
MaterialTheme.typography.labelLarge
```

Material's typography system is explicitly designed around hierarchy, font styles, and line height. Android also recommends using scalable `sp` units so text respects the user's font-size preferences. ([Material Design][5])

A strong agent constraint is:

> **Never introduce a new font size inside a feature screen unless the design system itself is being modified.**

---

# 8. Color should communicate meaning

A common AI mistake is using color to make the interface “interesting.”

Instead:

```text
color = hierarchy / state / brand / feedback
```

not:

```text
color = empty area looks boring
```

Use semantic Material roles:

```text
primary
onPrimary
secondary
surface
surfaceVariant
error
outline
```

instead of scattering hardcoded hex values throughout Compose code.

Material describes color roles as the connective system determining which colors belong to which UI elements, and its color guidance emphasizes hierarchy, state, accessibility, and brand. ([Material Design][6])

---

# 9. Progressive disclosure is essential

Do not show advanced settings because there is available space.

Show:

```text
Common controls
```

first.

Then:

```text
Advanced options →
```

when necessary.

NN/g specifically recommends staged/progressive disclosure for secondary functionality because displaying every setting simultaneously increases visual complexity and cognitive effort. ([Nielsen Norman Group][7])

For Android this often means using appropriately:

* bottom sheets
* dialogs
* expandable sections
* contextual menus
* overflow actions
* secondary screens
* supporting panes on large screens

Android's canonical **supporting-pane** pattern is specifically useful for keeping primary content focused rather than crowding all controls into the same view. ([Android Developers][8])

---

# 10. Navigation must stay boring

“Boring” navigation is usually good navigation.

Do not let an AI invent a custom floating navigation concept unless there is compelling UX evidence for it.

Android currently recommends standard navigation patterns such as navigation bars, rails, and drawers depending on available space. Android guidance also recommends keeping bottom navigation focused on primary destinations, generally around five or fewer. ([Android Developers][9])

Therefore the agent should default to:

```text
Compact
→ NavigationBar

Medium
→ NavigationRail where appropriate

Expanded
→ NavigationRail / NavigationDrawer
```

rather than simply stretching the mobile layout.

---

# 11. Do not simply stretch phone UI onto tablets

This is particularly important for AI-generated Android apps.

Android's current adaptive guidance explicitly recommends:

* reflowing layouts
* revealing appropriate additional content
* changing component presentation
* using panes
* constraining maximum content width
* avoiding stretched buttons/inputs
* avoiding portrait-only assumptions ([Android Developers][10])

For example:

```text
PHONE

[List]
```

can become:

```text
TABLET

[List] | [Detail]
```

rather than:

```text
TABLET

[                     giant list item                     ]
[                     giant list item                     ]
```

Android provides canonical starting points including **list-detail, feed, and supporting-pane** layouts. ([Android Developers][8])

---

# 12. Accessibility should act as a design constraint

Accessibility checks are also excellent **anti-slop checks**, because they force the agent to clarify interaction boundaries and information hierarchy.

Require at minimum:

```text
Touch target >= 48dp × 48dp
Text uses sp
Normal text contrast >= 4.5:1
Meaningful semantics
Content descriptions where required
No color-only state communication
Font scaling test
TalkBack test
```

Android recommends at least **48dp × 48dp** touch targets and a minimum **4.5:1** text contrast ratio for typical text. Compose Material components implement many accessibility defaults automatically, but custom composables still require verification. ([Android Developers][11])

WCAG 2.2 provides the broader accessibility baseline for web interfaces. ([W3C][12])

---

# 13. Use standard Compose components before custom ones

Agent rule:

```text
Material component
        ↓ if insufficient
Configured Material component
        ↓ if insufficient
Reusable app component
        ↓ only if necessary
Custom primitive
```

Not:

```text
Every screen creates its own custom button.
```

Android's current core app-quality guidance recommends Material components where applicable for consistent interaction and visual behavior. ([Android Developers][13])

This alone eliminates a lot of AI-generated inconsistency.

---

# 14. Create a small application design system

Your AI agent should know about:

```text
AppTheme
Typography
ColorScheme
Spacing
Shape
Elevation
Icon policy
Button hierarchy
Content-width rules
Animation rules
```

For example:

```text
Design System
├── Color.kt
├── Type.kt
├── Shape.kt
├── Spacing.kt
├── Dimensions.kt
└── components/
    ├── AppButton.kt
    ├── AppTopBar.kt
    ├── AppListItem.kt
    └── AppEmptyState.kt
```

The goal is **not** creating fifty wrapper components.

The goal is preventing every AI-generated feature from inventing another visual language.

---

# 15. Don't blindly use Material 3 Expressive

This matters especially in 2026.

Material 3 now contains considerably more expressive capabilities, including stronger typography, new shapes, motion systems, button groups, and other expressive components. ([Material Design][14])

Those capabilities should not become:

```text
expressive = use everything expressive
```

Your AI agent should apply:

```text
Expressiveness ∝ importance
```

Meaning:

**high-value moment**

```text
onboarding completion
achievement
major confirmation
brand moment
```

can justify stronger expression.

But:

```text
settings
forms
search results
account details
transaction history
configuration screens
```

should generally remain quieter.

---

# 16. Motion must explain change

Agent rule:

> Animation must communicate state, continuity, hierarchy, causality, or feedback.

Reject animation whose justification is simply:

```text
"makes it feel modern"
```

Examples of useful motion:

```text
Item added → animate placement
Panel opens → preserve spatial relationship
Button changes state → communicate completion
Navigation changes → clarify hierarchy
```

Avoid:

```text
everything fades
everything slides
cards constantly float
icons pulse
gradient moves forever
```

---

# 17. Performance is part of visual quality

A beautiful screen that stutters is not high quality.

For Compose, Android currently recommends practices such as:

* moving expensive calculations outside composables
* using `remember` appropriately
* stable keys for lazy layouts
* `derivedStateOf` where rapid state changes would cause unnecessary recomposition
* delaying state reads where possible
* using Baseline Profiles
* measuring rather than guessing about performance ([Android Developers][15])

So your AI UI agent should have a **performance gate**, not just a screenshot gate.

---

# 18. Require the AI agent to perform a deletion pass

This is perhaps the most useful AI-specific technique.

After generating a screen, force another pass with this instruction:

```text
Perform a subtraction review.

For every visible element ask:

1. Does this support the user's current task?
2. Does it communicate necessary information?
3. Does it communicate hierarchy or state?
4. Would removing it reduce comprehension?
5. Can spacing replace this container?
6. Can typography replace this divider?
7. Can one action replace multiple actions?
8. Can this information appear later?
9. Is this icon redundant with its text?
10. Is this visual treatment repeated unnecessarily?

Remove anything that fails the test.
```

This directly operationalizes minimalist-design and cognitive-load principles into something an agent can execute. ([nngroup.com][2])

---

# 19. Recommended AI Agent workflow

I would implement this pipeline:

```text
USER REQUEST
      ↓
1. REQUIREMENTS ANALYSIS
      ↓
2. PRIMARY USER JOB
      ↓
3. CONTENT INVENTORY
      ↓
4. PRIORITIZATION
      ↓
5. REMOVE NONESSENTIAL CONTENT
      ↓
6. INFORMATION ARCHITECTURE
      ↓
7. SCREEN WIREFRAME
      ↓
8. MATERIAL / ANDROID PATTERN SELECTION
      ↓
9. DESIGN SYSTEM TOKEN MAPPING
      ↓
10. COMPOSE IMPLEMENTATION
      ↓
11. ADAPTIVE LAYOUT PASS
      ↓
12. ACCESSIBILITY PASS
      ↓
13. SUBTRACTION / ANTI-SLOP PASS
      ↓
14. PERFORMANCE PASS
      ↓
15. UI TESTS
```

The important part is that:

```text
Compose implementation
```

happens relatively late.

---

# 20. The Agent Skill I recommend implementing

Android now officially supports reusable **Agent Skills** based on the open Agent Skills standard. A skill consists primarily of a `SKILL.md` file containing YAML metadata plus Markdown instructions, and Android Studio can dynamically activate relevant skills when working in Agent Mode. ([Android Developers][16])

I would create:

```text
android-clean-ui/
└── SKILL.md
```

with roughly this behavior:

```yaml
---
name: android-clean-ui
description: >
  Design, implement, review, and refactor Android Jetpack Compose
  interfaces to be minimal, coherent, adaptive, accessible, and
  consistent with Material 3. Use when creating or reviewing Android
  screens, components, navigation, layouts, or design systems.
metadata:
  version: "1.0"
---
```

And make these its mandatory instructions:

```text
# Android Clean UI Skill

## Objective

Produce Android interfaces with high information signal and low
visual noise. Never add decorative complexity merely to make a UI
appear modern, premium, expressive, or AI-generated.

## Before coding

Identify:

- user's primary goal
- screen's primary task
- primary action
- required information
- supporting information
- optional information
- information that can be removed

Do not implement UI before establishing hierarchy.

## Hierarchy

Each screen must have:

- one dominant purpose
- at most one visually dominant primary action
- clearly subordinate secondary actions
- visibly quieter metadata

Avoid competing emphasis.

## Component policy

Prefer:

1. Material 3 standard component
2. configured Material component
3. shared application component
4. custom component only when necessary

Never create custom controls only for visual novelty.

## Container policy

Do not automatically wrap sections in Cards.

First attempt grouping with:

- proximity
- alignment
- spacing
- typography

Use Cards/Surfaces only when containment communicates meaningful
grouping, interaction, state, or elevation.

Avoid nested cards.

## Color policy

Use semantic MaterialTheme color roles.

Do not introduce hardcoded colors inside feature composables.

Do not use gradients unless required by branding, visualization,
state, or a deliberately approved visual concept.

## Typography policy

Use MaterialTheme typography tokens.

Do not invent arbitrary font sizes inside individual screens.

Use typography to communicate hierarchy rather than decoration.

## Spacing policy

Use project spacing tokens.

Never introduce arbitrary dp spacing when an existing token is
sufficient.

## Navigation

Prefer standard Android navigation patterns.

Keep primary navigation focused on genuine top-level destinations.

Do not invent unconventional navigation without explicit UX reason.

## Progressive disclosure

Do not expose rare or advanced controls by default.

Move secondary complexity into an appropriate:

- secondary screen
- bottom sheet
- dialog
- overflow menu
- expandable section
- supporting pane

when this improves focus.

## Adaptive UI

Never assume phone portrait only.

Test compact, medium, and expanded environments.

Do not simply stretch phone layouts.

Use reflow, panes, presentation changes, max content width, and
adaptive navigation where appropriate.

## Accessibility

Require:

- minimum 48dp touch targets
- scalable text
- sufficient contrast
- meaningful semantics
- TalkBack-compatible interaction
- non-color-only state indicators
- font scaling support

Prefer accessible Material components over custom primitives.

## Motion

Animation must communicate:

- state
- hierarchy
- continuity
- feedback
- spatial relationship

Remove animation added only for decoration.

## Anti-slop review

Before finishing each UI, inspect every visible element.

Ask:

- Why does this exist?
- Why is it this prominent?
- Can it be removed?
- Can spacing replace its container?
- Is the icon redundant?
- Is this information needed now?
- Is this action really primary?
- Does this color communicate meaning?
- Does this elevation communicate hierarchy?
- Does this screen contain multiple competing focal points?

Remove unnecessary elements.

## Final review

The interface should feel:

- calm
- intentional
- familiar
- readable
- coherent
- accessible
- adaptive

It should not feel:

- decorative
- component-heavy
- card-heavy
- badge-heavy
- gradient-heavy
- excessively rounded
- visually competitive
- generated from unrelated UI patterns.
```

That structure is compatible with the `SKILL.md` model Android documents for reusable AI-agent expertise. ([Android Developers][17])

---

# 21. Add automated gates to the skill

Don't depend entirely on the LLM's visual judgment.

Require the agent to execute objective checks.

### Accessibility gate

Compose can now run automated accessibility checks using the Accessibility Test Framework through Compose testing, and Android Studio also provides UI/accessibility inspection tools. ([Android Developers][18])

For example:

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@Before
fun setup() {
    composeTestRule.enableAccessibilityChecks()
}
```

Then complement automation with TalkBack/manual testing because automated tools cannot identify every usability issue. ([Android Developers][19])

---

# 22. Add screenshot review dimensions

For every important screen, make the agent review at least:

```text
Compact phone
Compact phone + large font
Landscape phone
Foldable / medium width
Tablet
Dark theme
Light theme
Empty state
Loading state
Error state
Long content
Localized long text
```

Android's current quality guidance emphasizes testing across multiple screen sizes, orientations, resolutions, and form factors rather than optimizing for a single handset configuration. ([Android Developers][20])

---

# 23. Create an explicit “UI lint” score

This is not an official Google metric; it is a policy I recommend for the agent.

Score every screen from `0–2` for:

| Criterion                      | Score |
| ------------------------------ | ----: |
| Clear primary task             |    /2 |
| Clear visual hierarchy         |    /2 |
| Low visual noise               |    /2 |
| Consistent spacing             |    /2 |
| Consistent typography          |    /2 |
| Semantic color usage           |    /2 |
| Minimal unnecessary containers |    /2 |
| Standard interactions          |    /2 |
| Accessibility                  |    /2 |
| Adaptive behavior              |    /2 |

Maximum:

```text
20
```

Agent policy:

```text
18–20 → Accept
15–17 → Refine
<15   → Redesign before implementation
```

This gives the AI an objective reason to **iterate rather than stop after its first generated UI**.

---

# 24. The simplest design philosophy to put into AGENTS.md

In addition to a detailed on-demand skill, Android recommends using `AGENTS.md`-style project instructions for persistent general agent behavior, while skills are better suited to specialized workflows. ([Android Developers][21])

Your permanent project instruction can be as small as:

```text
UI DESIGN PRINCIPLE

Prefer clarity over decoration.
Prefer hierarchy over density.
Prefer spacing over containers.
Prefer standard patterns over novelty.
Prefer one strong action over several competing actions.
Prefer progressive disclosure over showing everything.
Prefer design-system tokens over arbitrary values.
Prefer subtraction before adding visual treatment.

Never add UI merely because empty space exists.
Never use gradients, cards, chips, badges, shadows, animations,
or icons without a functional or hierarchical reason.

All Android UI must use Material 3 principles, Jetpack Compose,
adaptive layouts, accessibility semantics, scalable typography,
and project design-system tokens.
```

That persistent rule + the dedicated `android-clean-ui` skill is the combination I would use.

---

# The final principle

The best single rule for an AI developer is:

> **Every pixel must earn its place.**

An AI agent naturally tends to **add** because adding something demonstrates work.

Professional UI design often requires the opposite:

```text
generate
→ organize
→ prioritize
→ remove
→ simplify
→ test
→ refine
```

rather than:

```text
generate
→ make prettier
→ add cards
→ add gradients
→ add animation
→ done
```

The strongest sources converge on this point: consistent systems, clear hierarchy, adaptive layouts, familiar patterns, accessibility, progressive disclosure, and minimized cognitive noise produce more usable interfaces than decorative complexity. ([Nielsen Norman Group][2])

### Primary references used

The most important sources for implementing this are **Android Developers' current Android skills documentation**, **Jetpack Compose/Material 3 documentation**, **Android adaptive-layout and accessibility guidance**, **Material Design 3's layout/spacing/typography/color systems**, **WCAG 2.2**, and **Nielsen Norman Group's aesthetic-minimalist and cognitive-load research**. Android's Agent Skills are particularly relevant because the proposed anti-slop behavior can be packaged directly as a reusable `SKILL.md` rather than relying only on one-off prompting. ([Android Developers][17])

[1]: https://m3.material.io/foundations/layout/grids-spacing/spacing?utm_source=chatgpt.com "Grids & spacing"
[2]: https://www.nngroup.com/articles/aesthetic-minimalist-design/?utm_source=chatgpt.com "Aesthetic and Minimalist Design (Usability Heuristic #8)"
[3]: https://www.nngroup.com/articles/why-does-design-look-good/?utm_source=chatgpt.com "Why Does a Design Look Good?"
[4]: https://m3.material.io/blog/whats-new-at-io26?utm_source=chatgpt.com "What's new at Google I/O 2026"
[5]: https://m3.material.io/styles/typography/overview?utm_source=chatgpt.com "Typography – Material Design 3"
[6]: https://m3.material.io/styles/color/overview?utm_source=chatgpt.com "Color - Material Design 3 - Create personal color schemes"
[7]: https://www.nngroup.com/articles/usability-heuristics-complex-applications/?utm_source=chatgpt.com "10 Usability Heuristics Applied to Complex Applications"
[8]: https://developer.android.com/design/ui/mobile/guides/layout-and-content/common-layouts?utm_source=chatgpt.com "Common layouts | Mobile"
[9]: https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns?hl=en&utm_source=chatgpt.com "Layouts and navigation patterns  |  Mobile  |  Android Developers"
[10]: https://developer.android.com/design/ui/mobile/guides/layout-and-content/adapt-layout?utm_source=chatgpt.com "Adapt layouts | Mobile"
[11]: https://developer.android.com/guide/topics/ui/accessibility/apps.html?utm_source=chatgpt.com "Make apps more accessible  |  App quality  |  Android Developers"
[12]: https://www.w3.org/TR/WCAG22/?utm_source=chatgpt.com "Web Content Accessibility Guidelines (WCAG) 2.2"
[13]: https://developer.android.com/docs/quality-guidelines/core-app-quality?utm_source=chatgpt.com "Core app quality guidelines  |  App quality  |  Android Developers"
[14]: https://m3.material.io/blog/building-with-m3-expressive?utm_source=chatgpt.com "Start building with Material 3 Expressive"
[15]: https://developer.android.com/develop/ui/compose/performance?utm_source=chatgpt.com "Jetpack Compose Performance  |  Android Developers"
[16]: https://developer.android.com/studio/gemini/skills?utm_source=chatgpt.com "Extend Agent Mode with skills  |  Android Studio  |  Android Developers"
[17]: https://developer.android.com/tools/agents/android-skills?authuser=8&hl=en&utm_source=chatgpt.com "Overview of Android skills  |  Android Studio  |  Android Developers"
[18]: https://developer.android.com/develop/ui/compose/accessibility/testing?utm_source=chatgpt.com "Testing  |  Jetpack Compose  |  Android Developers"
[19]: https://developer.android.com/develop/ui/compose/accessibility/inspect-debug?utm_source=chatgpt.com "Inspect and debug  |  Jetpack Compose  |  Android Developers"
[20]: https://developer.android.com/develop/adaptive-apps/quality-guidelines/adaptive-app-quality?utm_source=chatgpt.com "Adaptive app quality guidelines  |  Adaptive Apps  |  Android Developers"
[21]: https://developer.android.com/studio/gemini/agent-files?hl=en&utm_source=chatgpt.com "Customize Gemini using AGENTS.md files  |  Android Studio  |  Android Developers"
