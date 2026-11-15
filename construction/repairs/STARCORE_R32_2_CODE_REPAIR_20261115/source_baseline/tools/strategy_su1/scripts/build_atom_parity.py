#!/usr/bin/env python3
"""Golden values are computed by the independent NumPy implementation on real source history."""
from pathlib import Path
import json, sys
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'reference/replay'))
import hre, atoms

def main():
    history=ROOT/'history.csv'
    if not history.exists():history=ROOT.parents[1]/'research_data/ethpk10_draws_20260316_201059.csv'
    _,_,draws=hre.load_draws(history);draws=draws[:1800]
    cat=json.loads((ROOT/'reference/fixtures/atom_catalog_lifted.json').read_text())
    bank=hre.ExpertBank(draws);lib=atoms.AtomLibrary(draws,bank)
    path=ROOT/'reference/fixtures/atom_parity.txt'
    with path.open('w') as out:
        out.write(str(len(draws))+'\n')
        for row in draws:out.write(' '.join(map(str,row))+'\n')
        for family,params in cat['score']:
            key=tuple(map(tuple,params));value=lib.score(family,key)
            for t in [3,100,800,1300,1799]:
                out.write('S '+family+' '+';'.join(k+'='+str(v) for k,v in key)+' '+str(t)+' '+' '.join(map(str,value[t].ravel()))+'\n')
        for family,q,params in cat['cond']:
            key=tuple(map(tuple,params));value=lib.cond(family,q,key)
            for t in [3,100,800,1300,1799]:out.write('C '+family+' '+str(q)+' '+';'.join(k+'='+str(v) for k,v in key)+' '+str(t)+' '+str(int(value[t,0,0]))+'\n')
    print('SU1_ATOM_GOLDEN_GENERATED scores=84000 conditions=750')
if __name__=='__main__':main()
