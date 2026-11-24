"""Independent, formula-only TRAIN probe for the pinned R27 engineering witness.

The production AST, birth gate, optimizer and certificate retain their own contracts.
Only this small, explicitly registered test grammar is interpreted here.
"""
import math
import operator
import re
from dataclasses import dataclass

from continuity4 import need

ARITY = {"c": (1, 0), "freq": (2, 0), "gap": (1, 0), "cross": (2, 0),
         "repeat": (0, 0), "trans1": (0, 0), "trans2": (0, 0),
         "rank": (0, 1), "zscore": (0, 1), "add": (0, 2), "sub": (0, 2),
         "mul": (0, 2), "mix": (1, 2)}
LEAF_GROUP = {"freq": "frequency", "gap": "recurrence", "cross": "lagged_observation",
              "repeat": "lagged_observation", "trans1": "transition", "trans2": "transition"}
WINDOWS = {8, 32, 128, 512, 2048, 5000}


@dataclass(frozen=True)
class Node:
    op: str
    args: tuple = ()
    kids: tuple = ()

    def leaves(self):
        groups = {LEAF_GROUP[self.op]} if self.op in LEAF_GROUP else set()
        for child in self.kids:
            groups.update(child.leaves())
        return groups

    def shape(self):
        # Ignore numeric choices, including head position and lag. Preserve executed
        # operators. Collisions are conservative for this controlled breadth test.
        op = "cross" if self.op == "repeat" else self.op
        children = tuple(k.shape() for k in self.kids)
        if op in ("add", "mul"):
            children = tuple(sorted(children))
        return (op, children)


def parse(ast):
    need(isinstance(ast, str) and len(ast) <= 4096, "probe AST text missing or oversized")
    tokens = re.findall(r"\(|\)|[^\s()]+", ast)
    at = 0
    count = 0

    def token():
        nonlocal at
        need(at < len(tokens), "unexpected end of probe program")
        word = tokens[at]
        at += 1
        return word

    def read(depth):
        nonlocal count
        count += 1
        need(count <= 40 and depth <= 6 and token() == "(", "probe birth size, depth or syntax")
        op = token()
        need(op in ARITY, "operator outside the registered probe grammar")
        n, arity = ARITY[op]
        args = tuple((float if op in ("c", "mix") else int)(token()) for _ in range(n))
        need(all(math.isfinite(x) for x in args), "nonfinite parameter")
        if op in ("freq", "cross", "gap"):
            need(1 <= args[0] <= 10, "invalid source position")
        if op == "freq":
            need(args[1] in WINDOWS, "invalid frequency window")
        if op == "cross":
            need(1 <= args[1] <= 3, "noncausal or invalid lag")
        if op == "mix":
            need(0 <= args[0] <= 1, "invalid mixture")
        if op == "c":
            need(abs(args[0]) <= 1000, "constant outside contract")
        kids = tuple(read(depth + 1) for _ in range(arity))
        need(token() == ")", "wrong arity or extra literal")
        return Node(op, args, kids)

    root = read(1)
    need(at == len(tokens) and root.leaves(), "incomplete or constant-only formula")
    return root


def panel(spec, first_service_target):
    history = spec["draws"]
    first, start, pos = (spec[k] for k in ("first_ordinal", "probe_from_index", "target_position"))
    need(spec["role"] == "TRAIN" and type(pos) is int and 1 <= pos <= 10, "invalid probe role/target")
    need(type(first) is int and first >= 0 and type(start) is int and isinstance(history, list)
         and 8 <= start and 32 <= len(history) - start <= 500 and len(history) <= 5000
         and first + len(history) - 1 < first_service_target, "unregistered, insufficient or late TRAIN rows")
    for row in history:
        need(isinstance(row, list) and len(row) == 10 and all(type(x) is int for x in row)
             and set(row) == set(range(1, 11)), "invalid PK10 permutation")
    return history, start, pos


def scores(node, history, t, pos, cache):
    key = (node, t)
    if key in cache:
        return cache[key]
    op, args = node.op, node.args
    if op == "freq":
        src, window = args
        rows = history[max(0, t-window):t]
        counts = [0]*10
        for row in rows:
            counts[row[src-1]-1] += 1
        values = [x/len(rows) for x in counts]
    elif op == "gap":
        seen = [1.0]*10
        for i in range(max(0, t-100), t):
            seen[history[i][args[0]-1]-1] = (t-i)/100.0
        values = seen
    elif op in ("cross", "repeat"):
        src, lag = args if op == "cross" else (pos, 1)
        number = history[t-lag][src-1]
        values = [float(k == number) for k in range(1, 11)]
    elif op in ("trans1", "trans2"):
        lag = 1 if op == "trans1" else 2
        condition = history[t-lag][pos-1]
        counts = [1]*10
        for i in range(lag, t):
            if history[i-lag][pos-1] == condition:
                counts[history[i][pos-1]-1] += 1
        total = sum(counts)
        values = [x/total for x in counts]
    elif op == "c":
        values = [args[0]]*10
    else:
        inputs = [scores(k, history, t, pos, cache) for k in node.kids]
        x = inputs[0]
        if op == "rank":
            order = sorted(range(10), key=lambda k: (x[k], k))
            values = [order.index(k)/9 for k in range(10)]
        elif op == "zscore":
            mean = sum(x)/10
            sd = math.sqrt(sum((v-mean)**2 for v in x)/10)
            values = [(v-mean)/sd if sd >= 1e-12 else 0.0 for v in x]
        elif op == "mix":
            a = args[0]
            values = [a*u+(1-a)*v for u,v in zip(x, inputs[1])]
        else:
            f = {"add": operator.add, "sub": operator.sub, "mul": operator.mul}[op]
            values = [f(a,b) for a,b in zip(x, inputs[1])]
    cache[key] = values
    return values


def replay(node, bench, cache):
    history, start, pos = bench
    return [scores(node, history, t, pos, cache) for t in range(start, len(history))]


def check_vectors(measured, expected):
    need(isinstance(measured, list) and len(measured) == len(expected), "incomplete registered probe calendar")
    for actual, row in zip(measured, expected):
        need(isinstance(actual, list) and len(actual) == 10 and
             all(type(x) in (int, float) and math.isfinite(x) and
                 math.isclose(x, y, rel_tol=1e-10, abs_tol=1e-12)
                 for x, y in zip(actual, row)), "numerical scores differ from executable formula")


def actions(rows):
    return { (i, tuple(sorted(sorted(range(1, 11), key=lambda k: (-row[k-1], k))[:3])))
             for i, row in enumerate(rows) }


def separated(a, b):
    union = a | b
    return bool(union) and len(a & b)/len(union) < 0.95
