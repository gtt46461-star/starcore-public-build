"""Bind the source of each outcome to the batch that can actually observe it."""
import collections
import copy
import csv
import json
from pathlib import Path

PLAN = Path(__file__).resolve().parents[1]


def read(name):
    return json.loads((PLAN / name).read_text())


def require(value, message):
    if not value:
        raise ValueError(message)


def index(rows):
    result = {r["id"]: r for r in rows}
    require(len(rows) == len(result), "duplicate contract ID")
    return result


def audit(items, consumers, cards, files, cases, legacy):
    require(set(items) == set(consumers) and len(items) == 222, "missing actual acceptance consumer")
    require(set(cards) == set(cases) and len(cards) == 18, "missing normal entry or case")
    for key, r in cases.items():
        c = cards[key]
        require(r["batch"] == c["batch"] and all(r.get(k) for k in ["entry","input","assertion","negative","receipt"]),
                key + " incomplete normal-path evidence")
        expected = [(a["path"],a["method"],a["line"]) for a in c["sourceAnchors"]]
        require(expected == [(a["path"],a["method"],a["line"]) for a in r["existingAnchors"]],key+" lost source method")
    for key, c in consumers.items():
        item = items[key]
        stage = c["runAt"]
        card = cards[c["card"]]
        require(c["textSha256"] == item["original_text_sha256"] and c["observable"].strip(), key+" lost exact condition")
        require(item["run_at"] == stage == card["batch"] == files[c["ownerFile"]]["owner"],
                key+" signoff runs before/after its actual outcome")
        require(c["ownerFile"] in card["writes"], key+" named observer outside this card")
        if c["oldRun"] != stage:
            require(c["oldRun"] < stage and c["oldRun"] in item["requires"] and item["level"]=="LOCAL",
                    key+" split evidence invalid")
        require(item["level"]=="LOCAL" or stage=="P5",key+" engineering closure consumes later device data")
    by_old = collections.defaultdict(list)
    for item in items.values():
        if item["level"] == "LOCAL":
            by_old[item["card"]].append(item["run_at"])
    for k,c in legacy.items():
        require(c["last_local_clause"] == max(by_old[k],default=None), k+" inconsistent old-card closure")
    with (PLAN/"ledger/all_production_java.csv").open(encoding="utf-8-sig") as stream:
        for row in csv.DictReader(stream):
            require(files[row["path"]]["owner"] == row["owner"], "contradictory file ownership "+row["path"])
    # These are concrete cross-layer expectations from the original words, independent of the new ledger's declared run_at.
    contracts = {"R31-B03-C03#A2":("P2","TrainingStepStoreR26.java"),
                 "R31-B03-C03#A1":("P3","DecisionGateEngine.java"),
                 "R31-B03-C03#A3":("P4","ReportR29.java"),
                 "R31-B08-C05#A4":("P4","ResearchTablesActivityR31.java"),
                 "R31-ENG-02#A2":("P5","run_current_acceptance.py")}
    for key,(batch,filename) in contracts.items():
        require(consumers[key]["runAt"]==batch and Path(consumers[key]["ownerFile"]).name==filename,
                key+" original production reader erased")


def main():
    items=index(read("ledger/acceptance_222.json"))
    observers=index(read("contracts/acceptance_consumers.json"))
    cards=index(read("contracts/repair_cards.json"))
    files={r["path"]:r for r in read("ledger/file_owners.json")}
    cases=index(read("contracts/field_cases.json"))
    legacy=index(read("ledger/cards_58_plus_20.json"))
    data=(items,observers,cards,files,cases,legacy)
    audit(*data)
    key="R31-B03-C03#A3"
    def backdate(i,o,c,f,t,l):
        i[key].update(run_at="P1",requires=[])
    def wrong_observer(i,o,c,f,t,l):
        o[key]["ownerFile"]=c["P1.03"]["writes"][0]
    def both_backdated(i,o,c,f,t,l):
        i[key].update(run_at="P1",requires=[])
        o[key].update(card="P1.03",oldRun="P1",runAt="P1",ownerFile=c["P1.03"]["writes"][0])
    def no_negative(i,o,c,f,t,l):
        t["P2.06"]["negative"]=""
    for change in (backdate,wrong_observer,both_backdated,no_negative):
        mutated=copy.deepcopy(data)
        change(*mutated)
        try:
            audit(*mutated)
        except ValueError:
            continue
        raise AssertionError("invalid consumer map accepted: "+change.__name__)
    print(json.dumps({"accepted":len(items),"productionCases":len(cases),"reassignedOriginalClauses":sum(c["oldRun"]!=c["runAt"] for c in observers.values()),
            "localByBatch":dict(sorted(collections.Counter(i["run_at"] for i in items.values() if i["level"]=="LOCAL").items())),
            "semanticNegativeMutationsRejected":4},ensure_ascii=False))


if __name__=="__main__":
    main()
