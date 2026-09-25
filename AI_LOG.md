# AI collaboration log

These entries describe actual events. They distinguish user corrections from defects found by the assistant/tests. No deliberate bugs were planted. Review this document before submission and retain only statements you can personally explain.

## 1. Editing workflow did not match the learning objective — user correction

The assistant interpreted an earlier request for code as permission to edit the project directly. I clarified that I wanted complete code to read and paste, with explanations, so I could understand and run the application myself. The assistant switched to separate code handoffs. Later, when time became scarce, I explicitly changed the workflow again: preserve a learning copy, then implement in the main project. These are different permissions at different stages, not a claim that I wrote every generated line.

## 2. Container tool choice — user correction

I explicitly requested Docker after questioning whether the assistant was using Podman. The delivered project uses Docker Compose and documents Docker commands. I chose consistency with my local setup over adopting an additional container workflow. This is a rejected tool choice/preference, not a claim that Podman is technically incapable.

## 3. Backup sequencing failure — assistant error, disclosed and recovered

When making the requested learning copy, the first copy failed because the new sibling folder was not writable. The following command still applied batch 1. The assistant should have stopped after the failed backup instead of proceeding. It disclosed the failure and recovered the learning source from preserved pre-batch resources and unchanged files. The recovery is documented in the learning folder's BACKUP_RECOVERY.md and is not represented as a verified byte-for-byte original snapshot. The later final-batch baseline was separately saved before editing.

## 4. Boxed Java ID comparison — assistant-found defect

Earlier catalogue code compared two Long objects using !=. Object identity is not the same as numeric identity, especially for IDs outside the small boxed-value cache. It was corrected to compare numeric values. A regression test uses TMDB ID 27205. This was identified by the assistant, not by me; I should explain the Java distinction during review.

## 5. Reorder validation on immutable lists — test-found defect

The final-batch implementation used ids.contains(null). Java List.of lists can throw NullPointerException when queried for null, even when all their contents are valid. The PostgreSQL integration test failed on a valid reorder. The implementation now uses ids.stream().anyMatch(Objects::isNull), and the test remains. This was found during automated verification.

## 6. Fractional rating coercion — validation improvement

Using an Integer JSON field can allow a JSON library to coerce a fractional number. The rating request now uses BigDecimal.intValueExact and rejects 8.5 instead of accepting 8. Both a focused unit test and an HTTP integration test cover this.

## Responsibility and remaining review

The assistant generated significant code and tests. I remain responsible for reviewing the source, running the app with my own TMDB token, understanding the chosen scoring rule and explaining the implementation. The report distinguishes mocked TMDB verification from live-service verification. No real TMDB token was read or included in AI-generated artifacts.
