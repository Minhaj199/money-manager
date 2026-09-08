# Money Manager MVP architecture

The app is local-first: Compose screens call Hilt view models, which use domain use cases and repositories backed by Room. OCR and PDF parsers produce `ParsedTransaction` values only; a review screen always requires confirmation before persistent `Transaction` records are written.

## Primary data and navigation

- `Fund` is a logical allocation, with a balance derived from income, expenses, and transfers.
- `Transaction` stores the amount, fund, category, merchant, UPI/reference identifiers, source, and date.
- `Transfer` writes an offsetting transaction in each fund and a transfer record. Its `TRANSFER` source lets future financial analytics exclude it while preserving fund balances.
- `Category` is seeded with common income and expense defaults. `ImportBatch` is reserved for future import provenance and undo support.
- Home leads to Funds, History, a fast manual entry, transfers, screenshot import, and PDF import. Fund detail drills into one fund.

## Import safeguards

Screenshot and PDF data is never saved silently. OCR results are editable before confirmation; PDF rows are selectable and let the user choose a fund. Potential duplicates use a transaction ID when present, or otherwise a same-merchant, same-amount, one-day window match. Possible duplicates are unselected/warned before import.

## Recommended next increments

1. Add transaction editing/deletion and category management.
2. Add an import-batch audit screen and per-row PDF category editing.
3. Build spending analytics that excludes `TRANSFER` sources.
4. Add statement-format adapters and automated category suggestions.
