# Sentinel AML — Explained for Someone Who Knows Nothing About It

No jargon. No code. Just what this thing does and why it matters.

## The Problem, in Plain English

Criminals who get money illegally (drugs, fraud, corruption, whatever) can't
just walk into a bank and deposit a suitcase of cash without questions. So
they try to make dirty money look clean — moving it around, splitting it up,
routing it through shady countries — a process called **money laundering**.

Banks are legally required to *watch for this and report it*. Doing that by
hand, for millions of transactions a day, is impossible. So banks build
software that watches every transaction and raises a flag ("alert") when
something looks like a known laundering trick.

**Sentinel AML is that software.** It's a small, working version of a
real-time transaction-monitoring system.

## The Analogy: Airport Security, But for Money

Imagine airport security doesn't use one X-ray machine — it uses five
different specialist dogs, each trained to sniff out one specific thing.
Sentinel AML works the same way: every transaction that happens gets sniffed
by **five specialist "dogs" (rules)**, each looking for a different
laundering trick:

1. **"Big Suitcase" dog — Large Single Transaction.** Any single transaction
   over a certain amount (think: $10,000) gets flagged automatically. Big
   moves of money always deserve a second look, no exceptions.

2. **"Sneaky Splitting" dog — Structuring (aka Smurfing).** Launderers know
   that one $50,000 transaction gets noticed, so they split it into five
   $9,500 transactions instead, hoping each one flies under the radar. This
   dog specifically watches for a bunch of transactions that all suspiciously
   sit *just under* the reporting threshold, happening close together.

3. **"In and Out Fast" dog — Rapid Movement of Funds.** Money lands in an
   account and almost all of it leaves again within a couple of days. Clean
   money tends to sit around and get spent slowly (rent, groceries, bills).
   Money that's "just passing through" to hide its trail moves fast — that's
   called *layering*, and this dog catches it.

4. **"Bad Neighborhood" dog — High-Risk Jurisdiction.** Some countries are
   flagged by international bodies as high-risk for money laundering or
   sanctions evasion. Any transaction touching one of those countries gets
   flagged automatically, no matter the amount.

5. **"That's Not Like You" dog — Behavioral Deviation.** The system learns
   each customer's normal spending pattern over time. If someone who
   typically spends a modest amount suddenly has a transaction three times
   bigger than their usual, that's worth a look — even if it's not a huge
   number in absolute terms, it's a huge number *for them*.

## What Happens When a Dog Barks?

1. **An alert is created** — not just "something's wrong," but a full
   explanation: *which* rule fired, *why*, and exactly which transactions are
   the evidence. Nothing is a mystery black box.

2. **It goes into a queue**, sorted by how serious it looks (a risk score
   from 0–100), so the most concerning things bubble to the top.

3. **A human analyst reviews it.** They can see everything the system saw,
   read the explanation, and decide: false alarm (clear it), needs deeper
   digging (escalate it), or open a full **case**.

4. **A case** bundles related alerts about the same customer into one
   investigation file, which can eventually be closed out or lead to filing
   a **SAR** (Suspicious Activity Report) — the formal document banks send
   to regulators.

5. **Every single decision is logged**, forever, with who did it and why.
   If a regulator ever asks "why did you clear this alert six months ago?",
   there's a permanent answer on file.

## Why "Explainable" Matters So Much

A system that just says " SUSPICIOUS " with no explanation is useless and
actively dangerous — analysts would either ignore it (too much noise, no
context) or blindly trust it (no way to double-check). Every alert in this
system tells you exactly *why* it exists, in plain language, with the exact
transactions that caused it. That's the difference between a tool analysts
trust and one they route around.

## Why It Also Tries *Not* to Cry Wolf

A system that flags everything is as useless as one that flags nothing —
analysts get "alert fatigue" and start ignoring the queue entirely. That's
why duplicate alerts get merged together instead of piling up, and why the
system was specifically tested with "clean," totally normal customer
activity to make sure it *doesn't* flag them. (During building this, we
actually caught and fixed a bug where normal payroll deposits were
accidentally tripping an alert — proof the "don't cry wolf" goal is taken
as seriously as the "catch the bad guys" goal.)

## The Short Version

Money moves in and out of accounts constantly. Five specialist checks watch
every movement for known bad patterns. When something looks suspicious, a
human gets a clear, evidence-backed explanation to review — not a guess.
Every decision anyone makes is permanently recorded. That's Sentinel AML.
