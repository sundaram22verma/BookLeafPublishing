---
name: Editorial Guild
colors:
  surface: '#fcf9f3'
  surface-dim: '#dcdad4'
  surface-bright: '#fcf9f3'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3ed'
  surface-container: '#f0eee8'
  surface-container-high: '#ebe8e2'
  surface-container-highest: '#e5e2dc'
  on-surface: '#1c1c18'
  on-surface-variant: '#424843'
  inverse-surface: '#31312d'
  inverse-on-surface: '#f3f0ea'
  outline: '#727973'
  outline-variant: '#c1c8c2'
  surface-tint: '#456553'
  primary: '#032517'
  on-primary: '#ffffff'
  primary-container: '#1b3b2b'
  on-primary-container: '#83a590'
  inverse-primary: '#abcfb8'
  secondary: '#904d00'
  on-secondary: '#ffffff'
  secondary-container: '#fe932c'
  on-secondary-container: '#663500'
  tertiary: '#122132'
  on-tertiary: '#ffffff'
  tertiary-container: '#283649'
  on-tertiary-container: '#909fb5'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c7ebd4'
  primary-fixed-dim: '#abcfb8'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#2d4d3c'
  secondary-fixed: '#ffdcc3'
  secondary-fixed-dim: '#ffb77d'
  on-secondary-fixed: '#2f1500'
  on-secondary-fixed-variant: '#6e3900'
  tertiary-fixed: '#d5e3fc'
  tertiary-fixed-dim: '#b9c7df'
  on-tertiary-fixed: '#0d1c2e'
  on-tertiary-fixed-variant: '#3a485b'
  background: '#fcf9f3'
  on-background: '#1c1c18'
  surface-variant: '#e5e2dc'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.025em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0.005em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.04em
  mono-code:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 0.75rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system serves an author support and publishing lifecycle management platform. The brand identity fuses the intellectual dignity of heritage literary publishing with the responsive precision of modern editorial workflows. The experience communicates quiet authority, craft, and empathy, mitigating the anxiety authors often feel during publication.

The aesthetic follows an **Editorial Minimalist** philosophy with structured, tactile surface layers. It balances generous breathing room with the high data density required for manuscript reviews, ISBN ticketing, proofing revisions, and multi-party author correspondence. The visual tone is scholarly, reliable, and deliberate, avoiding transient UI trends in favor of timeless typographic rigor and understated structural framing.

## Colors

The palette grounds high-stakes editorial communications in organic, archival tones.

- **Primary (`#1B3B2B`)**: Deep Forest Green. Represents editorial authority, permanence, and calm guidance. Applied to primary CTAs, active navigation items, brand anchor points, and critical manuscript milestones.
- **Secondary (`#D97706`)**: Rich Amber. Signifies warm editorial attentiveness. Deployed for pending reviews, proof revision notices, warnings, and time-sensitive author queries.
- **Tertiary (`#475569`)**: Warm Slate Gray. Functions as an objective counterweight for technical metadata, manuscript versioning hash tags, timeline nodes, and secondary controls.
- **Neutral (`#F9F6F0`)**: Warm Parchment Cream. Provides an archival background canvas that reduces eye strain during prolonged reading and copy editing.

### Semantic & Status Application
- **Published / Resolved**: Emerald Sage (`#15803D` on `#F0FDF4`, border `#BBF7D0`).
- **In Production / Reviewing**: Archive Amber (`#B45309` on `#FFFBEB`, border `#FDE68A`).
- **Critical / Action Required**: Crimson Rust (`#B91C1C` on `#FEF2F2`, border `#FECACA`).
- **Draft / Inactive**: Slate Ochre (`#475569` on `#F1F5F9`, border `#E2E8F0`).
- **Canvas Base**: `#F9F6F0` (Parchment).
- **Surface Elevation**: `#FFFFFF` (Crisp Paper White).
- **Text Primary**: `#0F172A` (Deep Ink).
- **Text Secondary**: `#475569` (Slate Ink).
- **Subtle Borders**: `#E5E0D8` (Warm Deckle Line).

## Typography

The typographic hierarchy pairs `Plus Jakarta Sans` for titles and structure with `Inter` for operational text and correspondence.

- **Plus Jakarta Sans**: Delivers an articulate, contemporary title presence. Its subtle geometric warmth avoids cold clinical corporate tones and establishes editorial structure across section headers, book titles, and metrics.
- **Inter**: Optimizes continuous readability across author queries, legal terms, revision diffs, and proof notes.
- **JetBrains Mono**: Assigned strictly to production metadata (ISBNs, ticket codes, revision hashes, and layout specs).
- High visual contrast is maintained: continuous body prose uses `body-lg` with a generous 1.625 line-height ratio (`26px` on `16px`), mimicking the eye-tracking cadence of printed trade books.

## Layout & Spacing

The layout is built on a responsive 12-column fluid grid governed by an 8pt architectural rhythm, with a strict 4pt baseline for compact component interiors.

- **Desktop (>= 1280px)**: 12-column layout with `2rem` (32px) exterior margin, `1.5rem` (24px) gutters, and a maximum operational canvas width of `1440px`. Author communication channels use an asymmetrical layout: an 8-column primary work surface paired with a 4-column contextual rail (manuscript specs, assigned editor contacts, production SLAs).
- **Tablet (768px - 1279px)**: 8-column layout with `1.5rem` (24px) gutters. The contextual rail transitions to collapsible drawers or stacks below the main author thread.
- **Mobile (< 768px)**: 4-column layout with `1rem` (16px) margins and `0.75rem` (12px) gutters. Multi-step publishing steps switch to horizontal linear progress tracks.

Internal container padding adheres strictly to `space-md` (`1rem`) and `space-lg` (`1.5rem`) to maintain balanced visual density.

## Elevation & Depth

This system avoids heavy shadows, using a tactile paper-stacking paradigm with fine structural outlines.

- **Canvas Ground**: Parchment cream (`#F9F6F0`) serves as the base layer.
- **Surface Level 1 (Panels & Cards)**: Crisp white (`#FFFFFF`) layered on parchment with a 1px solid hairline border (`#E5E0D8`) and an ambient, micro-tinted contact shadow: `0 1px 3px 0 rgba(27, 59, 43, 0.04), 0 1px 2px -1px rgba(27, 59, 43, 0.02)`.
- **Surface Level 2 (Active Modals, Popovers, Flyouts)**: Elevated crisp white (`#FFFFFF`) with a 1px border (`#D8D1C5`) and a diffused shadow: `0 10px 15px -3px rgba(27, 59, 43, 0.08), 0 4px 6px -4px rgba(27, 59, 43, 0.03)`.
- **Surface Level 3 (Manuscript Lightbox & Critical Overlays)**: Crisp white with deep forest ambient scrim (`rgba(15, 23, 42, 0.45)`) and diffused depth: `0 20px 25px -5px rgba(27, 59, 43, 0.12), 0 8px 10px -6px rgba(27, 59, 43, 0.06)`.
- **Dividers & Structural Rules**: Subtle paper seams use `#EFECE6`. Active focus elements and elevated hover states use a 1px border transition to `#1B3B2B`.

## Shapes

The interface uses restrained, crisp corners reflecting cut paper stock and editorial proofs.

- **Level 1 (Soft)**: The default corner radius is `0.25rem` (4px).
- **Cards, Panels, and Modals**: Utilize `rounded-lg` (`0.5rem` / 8px) to soften larger data surfaces without compromising structural grid alignment.
- **Badges, Tags, and Pill Counters**: Retain a subtle compact rounding of `0.25rem` (4px) or full circular containment (`9999px`) for numeric author notification counts only.
- **Input Fields & Buttons**: Standardize on `0.375rem` (6px) to pair seamlessly with modern Shadcn design patterns while upholding an archival tone.

## Components

### Buttons
- **Primary**: Solid Deep Forest Green (`#1B3B2B`) fill, pure white text, 6px radius (`0.375rem`), with a subtle inner border highlight (`inset 0 1px 0 rgba(255,255,255,0.15)`). Hover shifts to `#142C20`.
- **Secondary**: Crisp white background, hairline border in `#D1CAC0`, slate text (`#1E293B`). Hover shifts to subtle parchment tint (`#F4EFEA`).
- **Destructive**: Pale crimson tint (`#FEF2F2`) with dark red text (`#991B1B`) and border (`#FCA5A5`). Hover darkens to `#B91C1C` fill with white text.
- **Ghost/Link**: Transparent background, text colored with `#1B3B2B`, underline appearing on hover.

### Cards (Shadcn-Style Architectural Containers)
- Surfaces feature an opaque white base (`#FFFFFF`), hairline outline (`#E5E0D8`), and standard `0.5rem` radius.
- Cards are split into distinct, low-contrast header, content, and footer regions separated by 1px rules (`#F1ECE4`).
- Header displays category/status labels alongside clean action triggers.

### Status Badges
- Compact badges use uppercase `label-sm` (`11px`), 4px corner rounding, and semi-transparent tinted backgrounds:
  - **Published**: Text `#15803D`, background `#ECFDF5`, border `#A7F3D0`.
  - **In Production**: Text `#B45309`, background `#FFFBEB`, border `#FDE68A`.
  - **Critical**: Text `#B91C1C`, background `#FEF2F2`, border `#FECACA`.
  - **Resolved**: Text `#1B3B2B`, background `#F0FDF4`, border `#BBF7D0`.

### Form Inputs & Textareas
- Crisp white ground with a warm border (`#D8D2C7`). Focus state features a sharp 1px ring in `#1B3B2B` with zero offset blur.
- Field labels use `Inter` Medium (`13px`) in Deep Slate (`#334155`). Helper and character counts use `mono-code` (`12px`) in `#64748B`.

### Lists & Activity Feeds
- Message threads between authors and editors use an interleaved, card-free conversational feed.
- Author messages anchor on a warm paper tone (`#FAF8F5`) aligned left; editor responses display on crisp white with a forest green boundary strip (`3px` solid `#1B3B2B`) on the leading edge.

### Checkboxes & Radio Controls
- Checkbox elements use a 4px radius with `#E5E0D8` borders. Selected states snap to `#1B3B2B` fill with an off-white checkmark.
- Radio buttons maintain a concentric ring layout, activating a centered forest green dot on selection.

### Editorial Additions
- **Proofing Milestone Trackers**: A horizontal chain of manuscript milestones (Typesetting, Proofing, Indexing, Print Run) with amber-tinted active markers and forest green completion indicators.
- **ISBN & Metadata Strip**: Dedicated data cells using `JetBrains Mono` with one-click copy actions and clear legal registration indicators.