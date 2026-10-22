#!/usr/bin/env python3
"""Deterministic model of R912 current-exact priority plus bounded backlog fairness."""
from collections import deque

backlog = deque(range(1, 305))
settled_backlog = []
live_closed = []

# Simulate 120 one-minute arrivals. Every live issue gets first priority. Seven selected live
# issues need three attempts; the bounded fairness slice must still close older runnable rows.
attempts = {}
for minute in range(1, 121):
    live = 1000 + minute
    attempts[live] = 0
    for pulse in range(50):
        if live not in live_closed:
            attempts[live] += 1
            required = 3 if minute % 17 == 0 else 1
            if attempts[live] >= required:
                live_closed.append(live)
        # R912 fairness: one old exact-draw/pre-draw task per pulse even while current retries.
        if backlog:
            settled_backlog.append(backlog.popleft())
        if live in live_closed and not backlog:
            break

assert len(live_closed) == 120
assert len(settled_backlog) == 304
assert not backlog
assert max(attempts.values()) == 3
print("R912_CURRENT_EXACT_PRIORITY=PASS")
print("R912_BACKLOG_FAIRNESS_WHILE_CURRENT_RETRIES=PASS")
print("R912_304_READY_CONVERGENCE=PASS")
