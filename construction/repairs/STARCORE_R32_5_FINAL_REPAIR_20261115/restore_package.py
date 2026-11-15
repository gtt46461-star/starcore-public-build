#!/usr/bin/env python3
"""Reassemble the exact STARCORE plan archive from repository parts and verify it."""
from pathlib import Path,PurePosixPath
import argparse,hashlib,json,os,shutil,stat,tempfile,zipfile

def digest(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  while block:=f.read(1024*1024):h.update(block)
 return h.hexdigest()

def main():
 ap=argparse.ArgumentParser(description=__doc__)
 ap.add_argument('--manifest',type=Path,default=Path(__file__).with_name('PACKAGE_PARTS.json'))
 ap.add_argument('--output',type=Path,help='Destination ZIP; defaults to the manifest directory')
 ap.add_argument('--extract',type=Path,help='New extraction directory; existing destinations are not overwritten')
 args=ap.parse_args();manifest=args.manifest.resolve();root=manifest.parent;data=json.loads(manifest.read_text())
 output=(args.output if args.output else root/data['archive']).resolve();output.parent.mkdir(parents=True,exist_ok=True)
 parts=data['parts'];seen=set();total=0
 for row in parts:
  rel=PurePosixPath(row['name'])
  if rel.is_absolute() or '..' in rel.parts or '\\' in row['name'] or row['name'] in seen:raise ValueError('UNSAFE_OR_DUPLICATE_PART')
  seen.add(row['name']);p=root/rel
  if p.is_symlink() or not p.is_file() or p.stat().st_size!=row['bytes'] or digest(p)!=row['sha256']:raise ValueError('PART_MISMATCH '+row['name'])
  total+=row['bytes']
 if total!=data['bytes']:raise ValueError('PART_SIZE_SUM_MISMATCH')
 if output.exists():
  if output.is_symlink() or output.stat().st_size!=data['bytes'] or digest(output)!=data['sha256']:raise ValueError('REFUSE_OVERWRITE_UNKNOWN_OUTPUT')
 else:
  fd,tmp=tempfile.mkstemp(prefix='.starcore-package-',dir=output.parent)
  try:
   with os.fdopen(fd,'wb') as out:
    for row in parts:
     with (root/row['name']).open('rb') as src:shutil.copyfileobj(src,out,1024*1024)
    out.flush();os.fsync(out.fileno())
   if digest(Path(tmp))!=data['sha256']:raise ValueError('FINAL_SHA256_MISMATCH')
   os.replace(tmp,output)
  finally:
   if os.path.exists(tmp):os.unlink(tmp)
 with zipfile.ZipFile(output) as z:
  names=set()
  for i in z.infolist():
   p=PurePosixPath(i.filename)
   if p.is_absolute() or '..' in p.parts or '\\' in i.filename or i.filename in names or stat.S_ISLNK(i.external_attr>>16):raise ValueError('UNSAFE_ZIP_MEMBER')
   names.add(i.filename)
  bad=z.testzip()
  if bad:raise ValueError('ZIP_CRC_MISMATCH '+bad)
  if args.extract:
   target=args.extract.resolve()
   if target.exists():raise ValueError('EXTRACTION_DESTINATION_MUST_BE_NEW')
   target.mkdir(parents=True)
   for i in z.infolist():
    dest=target/PurePosixPath(i.filename)
    if i.is_dir():dest.mkdir(parents=True,exist_ok=True);continue
    dest.parent.mkdir(parents=True,exist_ok=True)
    with z.open(i) as src,dest.open('xb') as out:shutil.copyfileobj(src,out,1024*1024)
    dest.chmod(stat.S_IMODE(i.external_attr>>16) & 0o777 or 0o644)
 print(json.dumps({'archive':str(output),'sha256':data['sha256'],'bytes':data['bytes'],'parts':len(parts),'zip_crc':'PASS','extracted_to':str(args.extract.resolve()) if args.extract else None},ensure_ascii=False,indent=2))
if __name__=='__main__':main()
