# UI Specification and Approval Checklist

`web/index.html` is the approved design reference (V7), NOT yet a production React app. The visual goal is a polished, fast, data-rich yet understandable election explorer. Keep its colors, spacing, maps, dark shell, navigation and hover interactions unless the user requests a change.

## Four global modes in sticky header

1. **Live Results**: reported votes and current leads ONLY on the national Overview; state hover shows Senate/Governor status and House district counts. No prediction percentage on the national Overview.
2. **Past Results**: years 2000, 2002, ..., 2024. Every state, office, county and race view follows selected year. Unknown results show no record, not guessed votes.
3. **Polls & Forecasts**: individual polls, averages and publisher models are separately identifiable, include attribution, sample, dates, question and population. Outlet/provider filter propagates across applicable views.
4. **Exit Polls**: demographic/issue crosstabs from legitimate survey providers, with explicit population, sample and release time. No fake 2026 survey data before actual release.

## Navigation rules

- Click Overview map state → all-election **State Dashboard**, not Senate automatically.
- Click state on Senate/Governor/House national page → same State Dashboard with matching office tab preselected.
- State dashboard shows office cards: House, Senate and Governor. A state without a contest shows `NOT ON BALLOT`, not a 0% vote share.
- House → exact congressional district → candidate results + individually-scoped model panel. County map is for reporting context, **not House seat allocation**.
- Senate/Governor → statewide race → county map, candidates, reported vote margin + model panel in its own separate section.
- County map actual boundaries; coloring by **observed reported lead** blue/red; no data gray. Distinguish not started and provider unavailable through separate shape/tooltip.
- Hover county shows name, reported candidate leads, margin, **metric type and percentage counted**, as-of timestamp, source, and status; clicking pins the detail. Touch and keyboard must also work.
- Accessible focus states and empty/loading/error/stale states; responsive mobile maps with a county list fallback.

## Data + visual language

- Red and blue represent party lead only; never political recommendations.
- Poll support `%`, estimated counted `%`, precinct reporting `%`, vote share `%`, model win chance `%` must always have **explicit labels**.
- Model win probabilities only on individual race pages, visually marked as model output, with credible intervals or abstention reason; never treat them as election projections.
- Historical geography must be from the correct vintage; 2026 legal district boundaries need validation before rendering real House seats.
- Show an obvious `DEMO DATA` label whenever sample arrays are active; disable all external publication actions.

## Frontend engineering conversion

- Move V7 from 1.5MB inline HTML to Next.js/TypeScript components, but preserve screenshots as golden references.
- Suggested tree: `app/elections/[year]/page.tsx`, `app/elections/[year]/states/[state]/page.tsx`, `app/elections/[year]/races/[raceId]/page.tsx`, `components/ElectionShell`, `GlobalModeTabs`, `NationalMap`, `StateMap`, `CountyTooltip`, `OfficeCards`, `RaceVoteResults`, `ModelProbabilityPanel`, `PollTable`, `ExitSurveyView`, `SourceBadge`.
- Map static county paths can be lazily loaded by state and served as versioned vector data.
- Use URL/query parameters for mode + year + office + selected state + provider; deep link and back/forward navigation must work.

## Browser E2E acceptance

1. Mode switch changes page, card headings, available data and URL context without stale cross-mode data.
2. Overview TX hover shows separate Senate/Governor/House snapshot with vote counting progress and source labels, **no forecasts**.
3. Overview TX click opens TX all-office dashboard; Senate TX click opens TX with Senate selected; switching to House retains TX.
4. House TX district 07 click opens distinct district race; no fake county-wide House winner conversion.
5. County hover/click shows correct reported status and percent **type**; no-count counties do not show 0% share.
6. History 2000 stays 2000 on state/office/district navigation; unavailable races say not-on-ballot or not-imported.
7. Selecting poll provider persists; no statewide poll reused as a specific House-district poll.
8. Exit polls 2026 with no authorized feed displays unavailable, not fabricated demographics.
9. Real model endpoint unavailable => show `Model unavailable`, not a simulated prediction.
10. Mobile (390px and 768px), desktop (1440px), keyboard and screen reader accessible.
