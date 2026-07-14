# Sleep scoring design

## Purpose

A future Snooze Reviews score may summarize one night of sleep, but the meaning of that score has not been finalized.

Possible interpretations include:

- How enjoyable the sleep felt
- How restorative it felt
- How closely it matched personal sleep goals
- How healthy or stable the pattern appeared

These ideas are related, but they are not identical. A pleasant night may not be long enough, and a long night may not feel restorative. The app should not expose a single score until the intended meaning is clear.

## Candidate inputs

Candidate inputs for later scoring experiments include:

- Duration
- Awakening count
- Sleep-quality rating
- Rested-after-waking rating
- Slept-through status
- Sleep location
- Descriptive tags by category
- Dream information only if later shown to be useful

No weights are defined yet.

## Missing-data policy

`null` means unknown.

`0` is a valid rating.

A future score must not treat unanswered fields as poor results. A future calculation should normally exclude missing components from both contribution totals and available-weight totals. For example, if a sleep-quality rating is missing, it should not contribute zero points and should not reserve weight that unfairly lowers the final score.

## Explainability

A future score should provide a breakdown showing why a result was produced. Conceptual components may include:

- Duration
- Awakenings
- Sleep quality
- Rested feeling
- Tag adjustment

This document does not define point values, weights, or thresholds.

## Location policy

Sleep location initially remains context. Built-in and custom locations should not receive fixed positive or negative score values by default.

Future statistics may compare outcomes by location using actual recorded data. For example, the app could later show whether a person tends to rate sleep differently in bed, on a couch, or while traveling.

## Tag policy

Tags remain descriptive observations.

Custom tags cannot safely be assigned automatic positive or negative meaning. Tag effects, if introduced later, must be explicit and reviewable. Selecting several tags must not allow tag modifiers to overwhelm the core score.

This task does not add polarity or scoring metadata to tag entities.

## Versioning policy

Each scoring algorithm must have its own identifier, such as:

```text
experimental-v1
```

A changed formula must receive a new identifier. This identifier is independent of the app version, Room database version, and backup format version.

## Persistence policy

Scores should initially be calculated from the sleep log rather than stored.

Reasons include:

- Formula changes
- Profile changes
- Avoiding stale values
- Recalculation
- Historical comparison

Derived metrics and scores should be calculated on demand until there is a clear need to persist them.

## Open design questions

- What should the score represent?
- Should there be one score or separate experience/restoration scores?
- What amount of data is sufficient?
- Should duration use a personal target range?
- Should tags affect the score or only explain it?
- Should location influence the score only after enough personal data exists?

## Experiment plan

1. Build basic statistics.
2. Collect real logs.
3. Create representative test nights.
4. Compare several candidate formulas.
5. Review whether rankings feel reasonable.
6. Only then expose a score in the UI.
