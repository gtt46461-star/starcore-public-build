# R26 verifier SQL fixture order repair

The original verifier scanned every eight-space Java string literal across the entire `RuntimeSchemaR26.java`. R31 feedback declarations precede the base DDL declaration in the source file, so that fixture attempted to create `r31_feedback_model_receipt` before `r26_consumption` existed. Its locally reproduced raw error is preserved in `original-ddl-order-failure.log`: `sqlite3.OperationalError: no such table: main.r26_consumption`.

Production `RuntimeSchemaR26.ensure` executes its named base DDL array, then invokes the versioned feedback migration. The verifier now extracts those two named arrays explicitly and executes them in that owner order, resolving their `FEEDBACK` constant member. Unsupported array syntax or missing constants fail explicitly. No production migration order changed, and no new DDL was filtered away.

All previous checks remain. An additional real SQLite check verifies the new feedback trigger's exact receipt fields, deduplication, matching source and draw-role joins, missing/too-early artifact rejection, atomic consumption/receipt rollback, event UPDATE/REPLACE/DELETE guards, and preservation after repeating the additive migration. Its rows are synthetic mechanism fixtures and establish no natural model quality.

The full invocation and raw output are in `full-verifier-receipt.json` and `full-verifier.log`; the original verifier JSON is copied to `verification-original-output.json` only after actual success. The receipt separately records source and already compiled app dependency hashes before and after execution. This is a HOST source recompile and native SQLite fixture result, not an Android migration execution, final frozen release, device, or continuous-run result.
