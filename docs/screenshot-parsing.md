# Screenshot Parsing

How the app reads a payment receipt image and turns it into a saved transaction.

---

## End-to-end flow

```
User shares / picks a receipt image
          │
          ▼
  ShareReceiverActivity
  detects MIME type = image/*
          │
          ▼
    ImageImportRoute
          │
          ▼
    OcrViewModel.processImage(bitmap)
    ┌──────────────────────────────┐
    │  ML Kit TextRecognition      │
    │  (Latin script, on-device)   │
    │  → result.text  (raw string) │
    └──────────────────────────────┘
          │  raw OCR text
          ▼
    ScreenshotParser.parse(rawText)
    ┌──────────────────────────────┐
    │  1. Normalize lines          │
    │  2. Extract each field       │
    │  3. Build ParsedTransaction  │
    └──────────────────────────────┘
          │  ParsedTransaction
          ▼
    OcrState.Ready(parsed)
          │
          ▼
    OcrReviewScreen
    AddTransactionViewModel.prefill(parsed, funds)
          │  AddTxnUiState populated
          ▼
    User reviews / edits fields
          │
          ▼
    AddTransactionViewModel.save(TxnSource.SCREENSHOT)
          │
          ▼
    SaveTransactionUseCase → Room database
```

---

## Step 1 — OCR (ML Kit)

`OcrViewModel` passes the bitmap to ML Kit's on-device Latin text recogniser.

```kotlin
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
// result.text is a single string with newlines between detected text blocks
```

ML Kit preserves the visual top-to-bottom, left-to-right reading order of the receipt.
The parser depends on this line order.

---

## Step 2 — Line normalisation

Before any field extraction, every line is cleaned by `normalizeLine()`:

| Operation | Reason |
|---|---|
| Replace `\u20B9` → `₹` | Some fonts encode the rupee sign as a different codepoint |
| Replace `â‚¹` → `₹` | UTF-8 mis-encoding that some OCR engines produce |
| `trim()` | Remove leading/trailing whitespace |
| Collapse internal whitespace to single space | OCR sometimes inserts extra spaces inside words |

Empty lines are discarded. The result is a clean `List<String>` where each entry is one receipt line.

---

## Step 3 — Field extraction

### Amount

**Regex:**
```
(?i)(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)
```

Rules:
- A currency marker (`₹`, `Rs`, `Rs.`, `INR`) is **mandatory** — bare numbers are never treated as amounts.
- Commas are stripped before parsing (`1,000` → `1000`).
- The value must be `> 0` and `< 100,000,000` to exclude garbage matches.
- Only the **first** matching line wins.

Supported formats:

| Input | Parsed amount |
|---|---|
| `₹70` | `70.0` |
| `₹ 70` | `70.0` |
| `₹1,000` | `1000.0` |
| `₹ 1,000.50` | `1000.5` |
| `Rs. 70` | `70.0` |
| `INR 70` | `70.0` |
| `₹70.00` | `70.0` |

What is **never** parsed as an amount:
- UPI transaction IDs (12-digit numbers with no currency prefix)
- Google transaction IDs (alphanumeric, no currency prefix)
- Dates, times, account numbers

---

### Description

Two strategies are tried in order:

**Strategy 1 — explicit label**

If any line matches `^description\s*:?\s*` (case-insensitive), the text after the colon on
that line (or the next line if the label is alone) is used.

**Strategy 2 — position relative to amount (Google Pay pattern)**

Google Pay displays the user's payment note directly below the large amount line with no label:

```
₹70
juce          ← description
Pay again
Completed
```

The parser finds the amount line index, then scans every subsequent line and returns the first
one that passes `isValidDescription()`.

**`isValidDescription` — a line is rejected if:**

| Condition | Examples rejected |
|---|---|
| Blank or longer than 120 chars | — |
| Matches the date/time regex | `6 Sept 2026, 6:36 pm` |
| Contains a currency amount | `₹70` |
| Matches any UI chrome pattern (see table below) | `Pay again`, `Completed` |

**UI chrome patterns — lines that are never a description:**

| Pattern | Matches |
|---|---|
| `^pay again$` | Pay again |
| `^completed$` | Completed |
| `^pending$` | Pending |
| `^failed$` | Failed |
| `^cancell?ed$` | Cancelled / Canceled |
| `^share$` | Share |
| `^split expense$` | Split expense |
| `^having issues\??$` | Having issues? |
| `^upi\s+(?:transaction\s+)?id` | UPI transaction ID |
| `^google\s+transaction\s+id` | Google transaction ID |
| `^google\s+pay` | Google Pay … |
| `^from\s*:` | From: … |
| `^to\s*:` | To: … |
| `^federal\s+bank` | Federal Bank … |
| `^upi$` | UPI |
| `^[0-9]{6,}$` | `661597442530` (numeric IDs) |
| `[A-Za-z0-9._-]+@[A-Za-z0-9._-]+` | UPI IDs like `••••2025@fbl` |
| `^[•*\s]+` | Bullet/dot masked account lines |

Valid descriptions that pass through:

```
juce    juice    dinner    groceries    rent
Lunch with friends    electricity bill    Uber    screen guard
```

---

### Merchant / recipient

Looks for a label line matching `^(?:paid to|sent to|payment to|to)\s*:?\s*` and takes the
text after it (same line or next line). Falls back to `^(?:received from|from)\s*:?\s*` for
incoming payments.

```
To: TECLYN MOBILE SALES AND SERVICE   →  merchant = "TECLYN MOBILE SALES AND SERVICE"
To MR CHAI                            →  merchant = "MR CHAI"
```

---

### UPI transaction ID

Label regex: `^upi\s+(?:transaction\s+)?id\s*:?\s*`

The value is on the same line after the label, or on the next line if the label is alone:

```
UPI transaction ID
661597442530          ← txnId
```

---

### Google transaction ID

Label regex: `^google\s+transaction\s+id\s*:?\s*`

```
Google transaction ID
CICAgPilUMSaTA        ← googleTransactionId
```

This field is an opaque identifier and is **never** parsed as a number.

---

### UPI ID

Scanned from all lines using:
```
(?:[•*]{2,}\s*)?[A-Za-z0-9._-]+@[A-Za-z0-9._-]+
```

Leading bullet characters (`••••`) are stripped from the result.

```
••••4946@myesaf   →   upiId = "4946@myesaf"
```

---

### Date and time

Single regex over the full joined text:
```
\b(\d{1,2}\s+[A-Za-z]{3,9}\.?\s+\d{4})\s*,?\s*(\d{1,2}:\d{2}\s*(?:am|pm))\b
```

- Group 1 → date string → `parseDate()`
- Group 2 → time string → `to24HourTime()`

**`parseDate` normalisation** — applied before handing to `SimpleDateFormat`:

| OCR output | Normalised to |
|---|---|
| `Sept` | `Sep` |
| `June` | `Jun` |
| `July` | `Jul` |
| Trailing `.` | removed |

Formats tried in order: `d MMM yyyy`, `d MMMM yyyy`, `d/M/yyyy`.

**`to24HourTime`** — parses `h:mm a` (12-hour) and reformats as `HH:mm` (24-hour):

```
6:36 pm  →  18:36
2:17 pm  →  14:17
```

---

### Transaction type

Keyword scan on the full lowercased text:

| Keywords present | Result |
|---|---|
| `received`, `credit`, `money received` (and none of the expense keywords) | `INCOME` |
| `paid`, `sent`, `debit`, `payment successful`, `completed` | `EXPENSE` |
| Neither or both | `EXPENSE` (safe default) |

---

### Payment app

| Text contains | paymentApp |
|---|---|
| `google pay` | `Google Pay` |
| `phonepe` | `PhonePe` |
| `paytm` | `Paytm` |
| `bhim` | `BHIM` |
| `amazon pay` | `Amazon Pay` |

---

### Payment method

If the text contains `"upi"` → `"UPI"`, otherwise empty string.

---

### Status

The first line that equals `"Completed"` (case-insensitive) is used verbatim.

---

### Confidence map

Each successfully extracted field is recorded in `ParsedTransaction.confidence` as
`FieldConfidence.HIGH`. Fields that could not be extracted are absent from the map.

---

## Step 4 — ParsedTransaction model

```kotlin
data class ParsedTransaction(
    val amount: Double?,          // null if not found
    val type: TxnType?,           // INCOME or EXPENSE
    val merchant: String,
    val upiId: String,
    val txnId: String,
    val description: String,
    val date: Long?,              // epoch millis, null if not found
    val paymentApp: String,
    val googleTransactionId: String,
    val time: String,             // "HH:mm" 24-hour, empty if not found
    val paymentMethod: String,
    val status: String,
    val confidence: Map<String, FieldConfidence>
)
```

All string fields default to `""` (never null) so the UI can display them without null checks.

---

## Step 5 — Review Payment screen

`OcrReviewScreen` observes `OcrState`. When `OcrState.Ready(parsed)` arrives it calls
`AddTransactionViewModel.prefill(parsed, funds)`.

`prefill` maps every `ParsedTransaction` field into `AddTxnUiState`:

```kotlin
amount              = parsed.amount?.toString() ?: ""
description         = parsed.description
merchant            = parsed.merchant
txnId               = parsed.txnId
googleTransactionId = parsed.googleTransactionId
paymentApp          = parsed.paymentApp
paymentMethod       = parsed.paymentMethod
status              = parsed.status
time                = parsed.time
dateText            = parsed.date?.let(::formatDate) ?: ""
type                = parsed.type ?: TxnType.EXPENSE
```

Every field is shown as an editable `OutlinedTextField`. The user can correct anything before
confirming. `prefill` only runs once — it is guarded by `state.amount.isBlank()` so a
recompose does not overwrite user edits.

---

## Step 6 — Saving the transaction

`AddTransactionViewModel.save(TxnSource.SCREENSHOT)`:

1. Validates amount > 0 and a fund is selected.
2. Runs `CheckDuplicateUseCase` — shows a warning dialog if a matching transaction already exists.
3. Calls `SaveTransactionUseCase` which writes to Room via `TransactionRepository`.

The saved `Transaction` contains:

```kotlin
Transaction(
    amount              = 70.0,
    description         = "juce",
    txnId               = "661597442530",
    googleTransactionId = "CICAgPilUMSaTA",
    paymentApp          = "Google Pay",
    paymentMethod       = "UPI",
    status              = "Completed",
    source              = TxnSource.SCREENSHOT,
    date                = /* epoch millis for 6 Sept 2026 18:36 */
)
```

---

## Worked example — Google Pay receipt

**Raw OCR text:**
```
To MR CHAI
₹70
juce
Pay again
Completed
6 Sept 2026, 6:36 pm
Federal Bank 9340
UPI transaction ID
661597442530
To: MR CHAI
••••2025@fbl
From: MINHAJ P K (Federal Bank)
Google Pay • ••••kkal@okaxis
Google transaction ID
CICAgPilUMSaTA
```

**Parsing trace:**

| Line | What happens |
|---|---|
| `To MR CHAI` | Matches `^to\s*:?\s*` → merchant = `"MR CHAI"` |
| `₹70` | Matches currency regex → amount = `70.0`; index saved as amountLine |
| `juce` | First line after amount; passes all `isValidDescription` checks → description = `"juce"` |
| `Pay again` | Matches `^pay again$` UI pattern → rejected |
| `Completed` | Matches `^completed$` UI pattern → rejected for description; recorded as status |
| `6 Sept 2026, 6:36 pm` | Matches dateTimeRegex; `Sept` → `Sep` → date = 6 Sep 2026, time = `"18:36"` |
| `Federal Bank 9340` | Matches `^federal\s+bank` UI pattern → rejected |
| `UPI transaction ID` | Label line |
| `661597442530` | Next line after label → txnId = `"661597442530"` |
| `To: MR CHAI` | Merchant already set; skipped |
| `••••2025@fbl` | Matches UPI regex; bullets stripped → upiId = `"2025@fbl"` |
| `From: MINHAJ P K (Federal Bank)` | Matches `^from\s*:` UI pattern → rejected |
| `Google Pay • ••••kkal@okaxis` | Contains `"google pay"` → paymentApp = `"Google Pay"` |
| `Google transaction ID` | Label line |
| `CICAgPilUMSaTA` | Next line after label → googleTransactionId = `"CICAgPilUMSaTA"` |

**Final ParsedTransaction:**

```
amount              = 70.0
description         = "juce"
merchant            = "MR CHAI"
txnId               = "661597442530"
googleTransactionId = "CICAgPilUMSaTA"
upiId               = "2025@fbl"
date                = 6 Sep 2026 (epoch millis)
time                = "18:36"
paymentApp          = "Google Pay"
paymentMethod       = "UPI"
status              = "Completed"
type                = EXPENSE
```

---

## Key files

| File | Role |
|---|---|
| `parsing/ScreenshotParser.kt` | All OCR text → field extraction logic |
| `ui/viewmodel/OcrViewModel.kt` | Runs ML Kit, calls parser, exposes `OcrState` |
| `ui/screen/OcrReviewScreen.kt` | Review Payment UI |
| `ui/viewmodel/AddTransactionViewModel.kt` | `prefill()` maps parsed fields; `save()` writes to DB |
| `domain/model/ParsedTransaction.kt` | Intermediate parsed model (not yet saved) |
| `domain/model/Transaction.kt` | Final saved model |
| `ShareReceiverActivity.kt` | Entry point when a receipt image is shared into the app |
| `parsing/ScreenshotParserTest.kt` | 19 unit tests covering all fields and edge cases |
