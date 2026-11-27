import os, sqlite3, struct, sys
from pathlib import Path
root=Path(sys.argv[1])
source=root/'source/com.starcore.research.r13/databases'
source.mkdir(parents=True)
p=source/'fixture.db'
size=4778991616
page=4096
with sqlite3.connect(p) as db:
    db.execute('CREATE TABLE tag(value TEXT)')
    db.execute("INSERT INTO tag VALUES ('signed-size-test')")
    db.commit()
    assert db.execute('PRAGMA page_size').fetchone()==(page,)
    used=db.execute('PRAGMA page_count').fetchone()[0]
pages=size//page
assert pages*page==size
with p.open('r+b') as raw:
    raw.truncate(size)
    raw.seek(28)
    skip=0x40000000//page+1
    raw.write(struct.pack('>III',pages, used+1, pages-used-1))
    start=used+1
    while start<=pages:
        count=min(pages-start,page//4-8)
        end=start+count+1
        raw.seek((start-1)*page)
        leaves=[i for i in range(start+1,end) if i!=skip]
        if end==skip:end+=1
        raw.write(struct.pack('>'+str(len(leaves)+2)+'I',end if end<=pages else 0,len(leaves),*leaves))
        start=end
with sqlite3.connect(f'file:{p}?mode=ro',uri=True) as db:
    result=db.execute('PRAGMA integrity_check').fetchall();assert result==[('ok',)],result[:4]
print('VALID_SQLITE_SPARSE_FREELIST',p,size,flush=True)
