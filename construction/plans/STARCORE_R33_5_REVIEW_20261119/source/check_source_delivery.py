"""Reopen the exact source ZIP and restore the delivered Git bundle in an empty store."""
import importlib.util
import json
from pathlib import Path
import sys
import tempfile

import hashlib

PACKAGER = '9b2f8d646c5e7aa71a56110dd94ea9f81a9db74954b6b2c6a680e9f48cea79e3'
BINDING = 'a6199ad0a78bdd3fd27f5dbb79a54b8bd359a9b64387f56ca5d6788650a2b4d3'


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def packager(plan):
    folder = plan / 'P00/after_full/tools/r33'
    for name, expected in [('verify_plan_binding', BINDING), ('package_ci_candidate', PACKAGER)]:
        path = folder / (name + '.py')
        require(sha(path) == expected, 'WRONG_SOURCE_VERIFIER:' + name)
        spec = importlib.util.spec_from_file_location(name, path)
        module = importlib.util.module_from_spec(spec)
        if name == 'verify_plan_binding':
            sys.modules[name] = module
        spec.loader.exec_module(module)
    return module


def member(candidate, entry, suffix):
    name = entry['file']
    require(isinstance(name, str) and name == Path(name).name and '\\' not in name and name.endswith(suffix),
            'SOURCE_DELIVERY_MEMBER_NAME')
    path = candidate / name
    require(path.is_file() and not path.is_symlink() and sha(path) == entry['sha256'],
            'SOURCE_DELIVERY_MEMBER_MISSING_OR_CHANGED:' + name)
    return path


def verify(package, repo, candidate, baseline):
    delivery = json.loads((candidate / 'DELIVERY.json').read_bytes())
    git = package.binding.git
    head = git(repo, 'rev-parse', 'HEAD').decode().strip()
    tree = git(repo, 'rev-parse', head+'^{tree}').decode().strip()
    archive, bundle = delivery['sourceArchive'], delivery['gitBundle']
    require(delivery['sourceCommit'] == bundle['sourceCommit'] == head
            and delivery['sourceTree'] == archive['gitTree'] == bundle['sourceTree'] == tree,
            'SOURCE_DELIVERY_COMMIT_OR_TREE')
    source_zip = member(candidate, archive, '.zip')
    history = member(candidate, bundle, '.bundle')
    entries = package.source_tree_entries(repo, head)
    require(archive['members'] == len(entries), 'SOURCE_DELIVERY_COUNT')
    package.verify_source_archive(source_zip, entries)
    with tempfile.TemporaryDirectory(prefix='r335-receive-history-') as directory:
        empty = Path(directory)
        git(empty, 'init', '--quiet', '--bare')
        git(empty, 'bundle', 'verify', str(history.resolve()))
        git(empty, 'fetch', '--quiet', '--no-tags', str(history.resolve()), 'HEAD')
        require(git(empty, 'rev-parse', 'FETCH_HEAD').decode().strip() == head and
                git(empty, 'rev-parse', 'FETCH_HEAD^{tree}').decode().strip() == tree and
                git(empty, 'rev-parse', '--is-shallow-repository').strip() == b'false', 'SOURCE_BUNDLE_RESTORED_IDENTITY')
        git(empty, 'merge-base', '--is-ancestor', baseline, head)
        git(empty, 'fsck', '--full', '--no-reflogs', '--no-dangling', head)
    require(sha(source_zip) == archive['sha256'] and sha(history) == bundle['sha256'],
            'SOURCE_ARTIFACT_CHANGED_DURING_CHECK')
    return dict(sourceCommit=head, sourceTree=tree, archiveMembers=len(entries),
                sourceArchiveSha256=archive['sha256'], gitBundleSha256=bundle['sha256'],
                restored033Ancestor=baseline, emptyRepositoryVerified=True)
