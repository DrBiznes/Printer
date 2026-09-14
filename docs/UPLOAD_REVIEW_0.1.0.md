# 0.1.0 image upload review

Reviewed 2026-09-13. This is a code and automated-test review, not a claim that arbitrary files are risk-free. Exact-artifact multiplayer and file-picker smoke tests remain in [SMOKE_TEST_0.1.0.md](SMOKE_TEST_0.1.0.md).

## Local files and authorization

- Only the client opens the native picker and reads the selected regular file. It checks size before reading and bounds the read to the configured limit plus one byte, including files that grow during the read.
- Packets contain a printer position, random transfer ID, declared byte count, bounded image bytes, and a user-entered title of at most 64 characters. No filename or client filesystem path is sent to the server. Uploads cannot request a server filesystem read.
- The server requires local uploads to be enabled, a living connected non-spectator player, and that player's open, nearby, still-valid Printer menu in the correct dimension.
- Every chunk and every server tick recheck the menu, policy, block identity, and job generation. Closing the menu, walking away, changing dimension, replacing the printer, cancellation, logout, timeout, or server shutdown cancels the upload. Completion rechecks job identity and menu authorization before storing the result or replacing the preset.
- Source previews are available through the matching open Printer menu. Printed variants are world-shared content retrievable by their content hash; a Photobook is storage and presentation, not a private-photo access-control feature.

## Resource bounds

| Limit | Enforcement |
| --- | --- |
| Input file | 10 MiB default; configurable 1–64 MiB; client and server both enforce it. |
| Upload packets | At most 24 KiB per chunk, bounded at packet decode before assembly. |
| Assembly | Exact ordered chunks, matching player/transfer ID, exact final chunk size; duplicate, skipped, truncated, and excess chunks reject. |
| Concurrent uploads | One per player; four globally; 64 MiB total reserved input bytes. |
| Upload deadline | 120 seconds total, not renewed by chunks. Client also times out after 150 seconds. |
| Load attempts | Shared two-second per-player cooldown for URL and file loads. |
| Processing | Two worker threads and a four-job queue; rejected submissions fail cleanly. |
| Decode | Supported raster formats only; first frame/page; header dimensions at most 4096 per axis and 16,777,216 pixels before full decode. |
| Output | Re-encoded PNG only, at most 4 MiB per source/variant; original file metadata is not retained. |
| World storage | SHA-256 deduplication; 256 MiB default encoded-byte budget (configurable 16–4096 MiB), shared across sources and variants. |
| Client delivery | At most 256 KiB chunks; content hash checked; bounded assembly and texture cache; request and response-byte budgets. |

Image processing is in-process Java. Deadlines prevent late results from committing, but cannot forcibly stop a decoder that ignores interruption. Worker/queue limits bound concurrency; server owners can disable local uploads. Decoded pixels, worker buffers, and cached SavedData use more memory than the encoded storage/transfer quotas, so those quotas are not a total JVM heap cap.

## URL loading

Only HTTP(S) URLs without credentials/fragments are accepted. Every redirect is revalidated, with at most three redirects. The resolver used by the actual connection rejects non-public addresses, avoiding a separate unchecked DNS resolution. No automatic redirects, cookies, system proxy routing, or retries bypass the policy. Body reads, MIME acceptance, download byte limits, and deadlines are bounded. Local uploads reuse the same canonical decoder and content-addressed store after receiving bytes.

## Review change and evidence

`ServerUploads.begin` now checks transfer size and capacity **before** starting or cancelling a printer job. Previously an invalid reservation could replace the printer's status even though no transfer was accepted. The new preflight runs on the same server thread as allocation; it does not reserve memory or mutate an existing transfer.

Coverage includes malformed packet lengths, declared sizes, ordering/duplicate/truncation rejection, concurrency limits, timeouts and reservation cleanup, client bounded file reads, raster dimension limits, deduplication, upload disable/menu checks, disconnect/cancel behavior, and successful upload-to-print integration. A new GameTest verifies that an invalid upload start leaves the printer's status unchanged. The final build counts and artifact checksum are recorded in the smoke-test document.

## Release decision

No unresolved code defect was identified in this review after the preflight fix. The implemented limits are suitable for a **0.1.0 release candidate**. Publication still requires the fresh-client/dedicated-server upload, cancellation, reconnect, rendering, and file-picker checks in the smoke matrix. Keep the new-world requirement: no old-save migrations or old-protocol negotiation are provided.
