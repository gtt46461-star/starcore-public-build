#!/usr/bin/env python3
"""Bind a sealed versioned plan to a real checkout without rewriting historical R31 pins."""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import re
import stat
import subprocess
import tempfile

BASE = 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'
PLAN_SCHEMA = 'STARCORE_R33_4_PLAN_BINDING_1'
PRODUCT_INPUT_ROOTS = ('app/src', 'app/libs', 'scripts', 'tools', 'release',
                       'gradle', 'buildSrc', 'src', 'libs', 'config', 'cmake', 'native')


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def sha(path):
    h=hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda:stream.read(1<<20),b''):h.update(block)
    return h.hexdigest()


def read(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))


def safe(root, relative):
    posix=PurePosixPath(relative)
    require(relative and not posix.is_absolute() and posix.as_posix()==relative
            and not any(x in ('.','..') for x in posix.parts), 'UNSAFE_PLAN_MEMBER')
    path=Path(root).joinpath(*posix.parts)
    require(path.resolve().is_relative_to(Path(root).resolve()) and not path.is_symlink()
            and not any(p.is_symlink() for p in path.parents if p.is_relative_to(root)), 'PLAN_SYMLINK_OR_ESCAPE')
    return path


def git(repo, *args):
    env=dict(os.environ,GIT_NO_REPLACE_OBJECTS='1',GIT_OPTIONAL_LOCKS='0',GIT_TERMINAL_PROMPT='0')
    for key in ('GIT_DIR','GIT_WORK_TREE','GIT_COMMON_DIR','GIT_INDEX_FILE','GIT_OBJECT_DIRECTORY',
                'GIT_ALTERNATE_OBJECT_DIRECTORIES','GIT_PREFIX'):env.pop(key,None)
    completed=subprocess.run(['git','-c','core.hooksPath=/dev/null','-C',str(repo),*args],
                             capture_output=True,env=env,check=False)
    require(completed.returncode==0,'GIT_FAILED:'+args[0])
    return completed.stdout


def verify_checkout_bytes(repo, head):
    """Compare actual files with HEAD, independently of index trust flags.

    Read-only: never clear assume-unchanged/skip-worktree, refresh an index or
    overwrite a developer's files. A release may not consume ignored source
    bytes that its Git source archive would omit. Symlinks are verified as link
    bytes, never by accidentally hashing an external target's contents.
    """
    repo = Path(repo).resolve()
    entries = {}
    for record in git(repo, 'ls-tree', '-r', '-z', '--full-tree', head).split(b'\0'):
        if not record:
            continue
        metadata, encoded_name = record.split(b'\t', 1)
        mode, kind, oid = metadata.decode('ascii').split()
        name = os.fsdecode(encoded_name)
        relative = PurePosixPath(name)
        require(name and not relative.is_absolute() and relative.as_posix() == name
                and not any(p in ('.', '..', '.git') for p in relative.parts), 'UNSAFE_TRACKED_PATH')
        require(name not in entries, 'DUPLICATE_TRACKED_PATH')
        require(kind == 'blob' and mode in ('100644', '100755', '120000'),
                'UNSUPPORTED_SOURCE_TREE_ENTRY:' + name)
        require(re.fullmatch('[0-9a-f]{40}', oid), 'UNSUPPORTED_GIT_OBJECT_FORMAT')
        entries[name] = (mode, oid)
    require(entries, 'EMPTY_SOURCE_TREE')

    tracked_directories = set()
    for name in entries:
        tracked_directories.update(p.as_posix() for p in PurePosixPath(name).parents if str(p) != '.')
    links = 0
    source_bytes = 0
    linked_directories = set()
    for name, (mode, expected) in entries.items():
        path = repo.joinpath(*PurePosixPath(name).parts)
        require(not any(p.is_symlink() for p in path.parents if p != repo and p.is_relative_to(repo)),
                'TRACKED_PATH_HAS_SYMLINK_PARENT:' + name)
        try:
            observed = path.lstat()
        except FileNotFoundError:
            raise ValueError('TRACKED_FILE_MISSING:' + name)
        if mode == '120000':
            require(stat.S_ISLNK(observed.st_mode), 'TRACKED_SYMLINK_CHANGED_TYPE:' + name)
            value = os.readlink(os.fsencode(path))
            actual = hashlib.sha1(b'blob ' + str(len(value)).encode('ascii') + b'\0' + value).hexdigest()
            require(actual == expected, 'HEAD_LINK_BYTES_MISMATCH:' + name)
            require(not Path(os.fsdecode(value)).is_absolute(), 'ABSOLUTE_SOURCE_SYMLINK:' + name)
            try:
                target = path.resolve()
            except (OSError, RuntimeError):
                raise ValueError('UNRESOLVABLE_SOURCE_SYMLINK:' + name)
            require(target.is_relative_to(repo), 'SOURCE_SYMLINK_ESCAPES_REPOSITORY:' + name)
            target_name = target.relative_to(repo).as_posix()
            require(target_name in entries or target_name in tracked_directories,
                    'SOURCE_SYMLINK_TARGET_NOT_TRACKED:' + name)
            if target_name in tracked_directories:
                linked_directories.add(target_name)
            source_bytes += len(value)
            links += 1
            continue
        require(stat.S_ISREG(observed.st_mode), 'TRACKED_REGULAR_FILE_CHANGED_TYPE:' + name)
        if os.name != 'nt':
            require(bool(observed.st_mode & stat.S_IXUSR) == (mode == '100755'),
                    'HEAD_EXECUTABLE_MODE_MISMATCH:' + name)
        flags = os.O_RDONLY | getattr(os, 'O_NOFOLLOW', 0)
        with os.fdopen(os.open(path, flags), 'rb') as stream:
            descriptor = os.fstat(stream.fileno())
            require(stat.S_ISREG(descriptor.st_mode), 'SOURCE_FILE_NOT_REGULAR:' + name)
            total = 0
            digest = hashlib.sha1(b'blob ' + str(descriptor.st_size).encode('ascii') + b'\0')
            for block in iter(lambda: stream.read(1 << 20), b''):
                total += len(block)
                digest.update(block)
            require(total == descriptor.st_size and digest.hexdigest() == expected,
                    'HEAD_FILE_BYTES_MISMATCH:' + name)
        source_bytes += total

    inspected = set()
    for relative in (*PRODUCT_INPUT_ROOTS, *sorted(linked_directories)):
        top = repo / relative
        if not top.exists() and not top.is_symlink():
            continue
        if top.is_symlink():
            require(relative in entries and entries[relative][0] == '120000',
                    'UNTRACKED_PRODUCT_SYMLINK:' + relative)
            top = top.resolve()
        if top.is_file():
            require(top.relative_to(repo).as_posix() in entries, 'UNTRACKED_PRODUCT_INPUT:' + relative)
            continue
        for directory, names, files in os.walk(top, followlinks=False):
            base = Path(directory)
            names[:] = [n for n in names if n != '__pycache__']
            for dirname in list(names):
                candidate = base / dirname
                if candidate.is_symlink():
                    logical = candidate.relative_to(repo).as_posix()
                    require(logical in entries and entries[logical][0] == '120000',
                            'UNTRACKED_PRODUCT_SYMLINK:' + logical)
                    names.remove(dirname)
            for filename in files:
                candidate = base / filename
                if candidate.suffix in ('.pyc', '.pyo'):
                    continue
                logical = candidate.relative_to(repo).as_posix()
                if logical in inspected:
                    continue
                inspected.add(logical)
                require(logical in entries, 'UNTRACKED_PRODUCT_INPUT:' + logical)
    return {'trackedFiles': len(entries), 'trackedSymlinks': links, 'trackedBytes': source_bytes,
            'headBytesVerified': True, 'untrackedProductInputs': 0}


def plan_descriptor(plan):
    """R33.4 fixtures remain readable; every R33.5 delivery carries the sealed descriptor."""
    path = safe(Path(plan).resolve(), 'control/PLAN_IDENTITY.json')
    if not path.exists():
        return {'planRevision': 'R33.4', 'sourceBaseline': BASE,
                'scenarioRegistry': 'control/required_scenarios_R334.json',
                'gateRegistry': 'control/32_gate_contracts.json'}
    value = read(path)
    require(value.get('schema') == 'STARCORE_PLAN_DESCRIPTOR_1'
            and value.get('sourceBaseline') == BASE
            and re.fullmatch(r'R33\.[0-9]+', str(value.get('planRevision'))), 'PLAN_DESCRIPTOR_INVALID')
    require(value.get('scenarioRegistry') == 'control/required_scenarios_R334.json'
            and value.get('gateRegistry') == 'control/32_gate_contracts.json', 'PLAN_REGISTRY_PATH_DRIFT')
    return value


def verify_plan(plan, binding):
    plan=Path(plan).resolve()
    descriptor=plan_descriptor(plan)
    require(binding.get('schema')==PLAN_SCHEMA and binding.get('planRevision')==descriptor['planRevision'],'R334_BINDING_SCHEMA')
    require(binding.get('sourceBaseline')==BASE,'R334_SOURCE_BASELINE')
    manifest=safe(plan,'verification/SHA256.json')
    require(manifest.is_file() and sha(manifest)==binding.get('planManifestSha256'),'R334_PLAN_MANIFEST_MISMATCH')
    members=read(manifest)
    require(isinstance(members,dict) and members,'R334_EMPTY_MANIFEST')
    for relative,digest in members.items():
        path=safe(plan,relative)
        require(path.is_file() and re.fullmatch('[0-9a-f]{64}',str(digest)) and sha(path)==digest,
                'R334_PLAN_MEMBER_MISMATCH:'+relative)
    actual={p.relative_to(plan).as_posix() for p in plan.rglob('*') if p.is_file()
            and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.pyo')}
    require(actual==set(members)|{'verification/SHA256.json'},'R334_UNSEALED_OR_MISSING_MEMBERS')
    for field,relative in [('gateRegistrySha256','control/32_gate_contracts.json'),
                           ('scenarioRegistrySha256','control/required_scenarios_R334.json')]:
        require(sha(plan/relative)==binding.get(field),'R334_'+field+'_MISMATCH')
    return {'planRevision':descriptor['planRevision'],'plan_manifest_sha256':sha(manifest),
            'gate_registry_sha256':binding['gateRegistrySha256'],
            'scenario_registry_sha256':binding['scenarioRegistrySha256'],'plan_member_count':len(members)}


def release_owner(repo, directory):
    from importlib.util import module_from_spec, spec_from_file_location
    path = safe(repo, f'tools/{directory}/build_identity.py')
    require(path.is_file(), directory.upper()+'_BUILD_IDENTITY_OWNER_REQUIRED')
    spec = spec_from_file_location(directory + '_build_identity', path)
    owner = module_from_spec(spec)
    spec.loader.exec_module(owner)
    return owner


def identity(repo, plan, binding_file, require_clean=False, *, r34_plan=None, r341_plan=None, r342_plan=None):
    repo=Path(repo).resolve();binding=read(binding_file)
    result=verify_plan(plan,binding)
    require(Path(git(repo,'rev-parse','--show-toplevel').decode().strip()).resolve()==repo,'ACTUAL_REPOSITORY_ROOT_REQUIRED')
    head=git(repo,'rev-parse','HEAD').decode().strip()
    require(re.fullmatch('[0-9a-f]{40}',head),'SOURCE_COMMIT_INVALID')
    git(repo,'merge-base','--is-ancestor',BASE,head)
    dirty=bool(git(repo,'status','--porcelain=v1','-z','--untracked-files=all'))
    require(not require_clean or not dirty,'R334_DIRTY_SOURCE_NOT_PUBLISHABLE')
    checkout = verify_checkout_bytes(repo, head) if require_clean else {'headBytesVerified': False, 'scope': 'DEVELOPMENT_NOT_PUBLISHABLE'}
    result.update(schema='STARCORE_R33_4_BUILD_IDENTITY_1',source_commit=head,
                  source_tree=git(repo,'rev-parse','HEAD^{tree}').decode().strip(),source_dirty=dirty,
                  source_baseline=BASE,plan_binding_sha256=sha(binding_file),
                  checkoutBytes=checkout,
                  engineeringAcceptance='NOT_INFERRED',deviceAcceptance='NOT_INFERRED',naturalQuality='NOT_INFERRED')
    release = read(repo/'release/release_manifest.json')
    version_name = release['versionName']
    has_prior_binding = (repo/'release/r34_plan_binding.json').exists()
    if version_name.startswith(('R34.1-', 'R34.2-')):
        prior = release_owner(repo, 'r34').verify_seal(repo, head, r34_plan)
        legacy = {'r33_5': dict(planRevision=result['planRevision'],
                               planSha256=result['plan_manifest_sha256']), 'r34': prior}
        if version_name.startswith('R34.2-'):
            legacy['r341'] = release_owner(repo, 'r341').verify_seal(repo, head, r341_plan)
            result['r342'] = release_owner(repo, 'r342').identity(
                repo, head, result['source_tree'], release, r342_plan, legacy_plan_seals=legacy)
        else:
            result['r341'] = release_owner(repo, 'r341').identity(
                repo, head, result['source_tree'], release, r341_plan, legacy_plan_seals=legacy)
    elif version_name.startswith('R34-') or has_prior_binding:
        result['r34'] = release_owner(repo, 'r34').identity(repo, head, result['source_tree'], release, r34_plan)
    require(not (repo/'release/r341_plan_binding.json').exists() or 'r341' in result or 'r342' in result,
            'R341_BOUND_SOURCE_REQUIRES_RELEASE_SERIES')
    require(not (repo/'release/r342_plan_binding.json').exists() or 'r342' in result,
            'R342_BOUND_SOURCE_REQUIRES_RELEASE_SERIES')
    # Supplemental field plan is sealed separately; keep the original series gates.
    if release.get('versionCode', 0) > 577311173 or (repo/'release/field173_plan_binding.json').exists():
        result['fieldRepair173'] = release_owner(repo, 'r343').identity(
            repo, head, result['source_tree'], release)
    return result


def write_json(path, value):
    path=Path(path);path.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.NamedTemporaryFile(mode='w',encoding='utf-8',dir=path.parent,prefix='.r334-',delete=False) as f:
        name=Path(f.name);json.dump(value,f,ensure_ascii=False,sort_keys=True,indent=2);f.write('\n');f.flush();os.fsync(f.fileno())
    try:os.replace(name,path)
    finally:name.unlink(missing_ok=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--plan',type=Path,default=os.environ.get('STARCORE_R33_PLAN_ROOT'))
    parser.add_argument('--r34-plan',type=Path,help='Sealed R34 directory, defaults to STARCORE_R34_PLAN_ROOT')
    parser.add_argument('--r341-plan',type=Path,help='Sealed R34.1 directory, defaults to STARCORE_R341_PLAN_ROOT')
    parser.add_argument('--r342-plan',type=Path,help='Sealed R34.2 directory, defaults to STARCORE_R342_PLAN_ROOT')
    parser.add_argument('--binding',type=Path)
    parser.add_argument('--init-binding',action='store_true',help='Write a new, reviewable pin from an already validated sealed plan; never overwrite.')
    parser.add_argument('--require-clean',action='store_true')
    parser.add_argument('--out',type=Path)
    args=parser.parse_args();require(args.plan is not None,'STARCORE_R33_PLAN_ROOT_REQUIRED')
    binding=args.binding or args.repo/'release/r33_plan_binding.json'
    if args.init_binding:
        value={'schema':PLAN_SCHEMA,'planRevision':plan_descriptor(args.plan)['planRevision'],'sourceBaseline':BASE,
               'planManifestSha256':sha(args.plan/'verification/SHA256.json'),
               'gateRegistrySha256':sha(args.plan/'control/32_gate_contracts.json'),
               'scenarioRegistrySha256':sha(args.plan/'control/required_scenarios_R334.json')}
        verify_plan(args.plan,value)
        binding.parent.mkdir(parents=True,exist_ok=True)
        with binding.open('x',encoding='utf-8') as f:json.dump(value,f,indent=2,sort_keys=True);f.write('\n')
        print(json.dumps({'status':'NEW_REVIEWABLE_PIN_CREATED','file':str(binding),'sha256':sha(binding)}));return
    result=identity(args.repo,args.plan,binding,args.require_clean,r34_plan=args.r34_plan,
                    r341_plan=args.r341_plan,r342_plan=args.r342_plan)
    if args.out:write_json(args.out,result)
    else:print(json.dumps(result,ensure_ascii=False,indent=2))


if __name__=='__main__':main()
