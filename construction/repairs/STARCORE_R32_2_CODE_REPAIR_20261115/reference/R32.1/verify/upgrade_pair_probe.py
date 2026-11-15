"""032-to-032 tests the retained-row comparator only; release acceptance needs a real upgrade."""
import sys
from pathlib import Path
from check_upgrade_rows import DRAW_FIELDS, compare_rows, snapshot
from verify_overlay_032 import APK_SHA, CODE, PACKAGE, SIGNER, check_transition


def fails(fn, label):
    try:
        fn()
    except ValueError:
        print("PASS", label)
    else:
        raise AssertionError(label)


def main(old, new):
    first, previous = snapshot(old)
    second, current = snapshot(new)
    assert first["apkSha256"] == second["apkSha256"] == APK_SHA
    report = compare_rows(previous,current)
    assert report["draws"]["count"] == report["v57_shadow_observation_r35"]["count"] == 32
    print("PASS 032-to-032 comparator: newest 32 original draws and 32 settled pre-draw records preserved")
    draw = report["draws"]["last"][0]
    rows = current["draws"]
    changed = [dict(r) if str(r["issue"]) == draw else r for r in rows]
    target = next(r for r in changed if str(r["issue"]) == draw)
    target[DRAW_FIELDS[-1]] = str(target[DRAW_FIELDS[-1]])+"!"
    fails(lambda: compare_rows(previous,dict(current,draws=changed)), "changed old draw cannot pass")
    fails(lambda: compare_rows(previous,dict(current,draws=[r for r in rows if str(r["issue"])!=draw])), "lost old draw cannot be omitted from both samples")
    issue,game=report["v57_shadow_observation_r35"]["last"]
    remove = [r for r in current["v57_shadow_observation_r35"] if (str(r["target_issue"]),str(r["game_code"]))!=(issue,game)]
    fails(lambda: compare_rows(previous,dict(current,v57_shadow_observation_r35=remove)), "lost old canonical settlement cannot pass")
    identity=dict(package=PACKAGE,versionCode=CODE,sha256=APK_SHA,certificate=SIGNER)
    fails(lambda:check_transition(identity,identity), "same APK evidence cannot stand in for an Android upgrade")


if __name__=="__main__":
    main(*(Path(a) for a in sys.argv[1:]))
