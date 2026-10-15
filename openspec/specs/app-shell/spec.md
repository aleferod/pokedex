# app-shell Specification

## Purpose
The frame every page renders in: accessible layout and navigation, fallbacks for unknown routes and rendering errors, document titles and the favicon.

## Requirements

### Requirement: Accessible layout
Every page SHALL render inside one layout: a header with the application name linking home and the main navigation, a "Skip to content" link as the first focusable element, and a single `main` landmark that the skip link targets. Interactive elements SHALL show a visible focus indicator, and the layout SHALL work from 360 px wide without horizontal scrolling.

#### Scenario: Keyboard user skips the navigation
- **WHEN** a keyboard user presses Tab once on any page
- **THEN** the focused element is the "Skip to content" link, and activating it moves focus to the `main` landmark

#### Scenario: Navigation is announced
- **WHEN** a screen reader lists landmarks
- **THEN** it finds a banner, a navigation labelled "Main" and one main region

### Requirement: Unknown routes
A path that matches no route SHALL render a "Page not found" page inside the layout, with a link back home, and set the document title accordingly.

#### Scenario: Mistyped address
- **WHEN** a user opens `/no-such-page`
- **THEN** the page shows "Page not found" and a link home, and the title is "Page not found · Pokédex"

### Requirement: Unexpected rendering errors
A rendering error in a page SHALL be caught by an error boundary that keeps the layout, shows "Something went wrong" with a "Try again" button that re-renders the page, and never shows a stack trace.

#### Scenario: A page crashes
- **WHEN** a page component throws while rendering
- **THEN** the user sees "Something went wrong" and a "Try again" button inside the layout, and the header navigation still works

### Requirement: Document titles and favicon
Each page SHALL set the document title to "<page> · Pokédex". The application SHALL ship a favicon, so browsers never request a missing one.

#### Scenario: Home page title
- **WHEN** the home page is shown
- **THEN** the document title is "Home · Pokédex"

### Requirement: Current page in the navigation
In the main navigation, the link to the page being shown SHALL be marked as the current page. The link to the section a page belongs to (such as "Catalog" on a Pokémon's page, or "My Pokédex" on a local record) SHALL be marked as the current item without claiming to be the current page.

#### Scenario: Current page and current section
- **WHEN** a visitor is on `/pokemon`
- **THEN** the "Catalog" link has `aria-current="page"`
- **AND WHEN** the visitor is on a Pokémon's page, such as `/pokemon/25`
- **THEN** the "Catalog" link has `aria-current="true"`, so only the evolution stage being viewed is the current page

#### Scenario: My Pokédex
- **WHEN** a signed-in user is on `/local`
- **THEN** the "My Pokédex" link has `aria-current="page"`
- **AND WHEN** the user is on a local record's page
- **THEN** the "My Pokédex" link has `aria-current="true"`
