"""Verify the shipped APK's source identity, original build evidence and all-in-one ZIP."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

from check_source_delivery import require, sha

ORIGINAL_SIGNED_CHECKER = '011448b41084f9de82d535ce3b5011f1c8c1ee0672490911380cc8947f120779'
INDEX = 'ZIP_MEMBERS_SHA256.json'


def regular(path):
    require(path.is_file() and not path.is_symlink(), 'DELIVERY_ORIGINAL_REQUIRED:' + path.name)
    return path


def archive_file(candidate, name, supplied):
    if supplied is not None:
        return regular(supplied)
    for p in (candidate / name, candidate.parent / name):
        if p.exists():
            return regular(p)
    raise ValueError('DOWNLOADABLE_DELIVERY_ZIP_REQUIRED:' + name)


def archive_evidence(candidate, delivery, archive):
    names = {delivery[k]['file'] for k in ('apk', 'sourceArchive', 'gitBundle')}
    require(len(names) == 3 and all(n == Path(n).name and '\\' not in n for n in names),
            'DELIVERY_ARTIFACT_NAMES')
    names.update(('DELIVERY.json', 'CI_BUILD_RECEIPT.json', 'gradle-build.log', 'build-identity-r334.json',
                  'public-certificate.txt', 'badging.txt', 'baseline033-certificate.txt', 'baseline033-badging.txt'))
    local = {name: sha(regular(candidate / name)) for name in names}
    index = regular(candidate / INDEX)
    require(json.loads(index.read_bytes()) == local, 'DOWNLOADED_MEMBER_INDEX_DIFFERS')
    local[INDEX] = sha(index)
    for key in ('apk', 'sourceArchive', 'gitBundle'):
        entry = delivery[key]
        require(local[entry['file']] == entry['sha256'], 'DELIVERY_RECEIPT_MEMBER_DIFFERS:' + key)
    outer_sha = sha(archive)
    with ZipFile(archive) as z:
        entries = z.namelist()
        require(len(entries) == len(local) and set(entries) == set(local), 'DOWNLOADABLE_ZIP_MEMBER_SET')
        for name, expected in local.items():
            with z.open(name) as stream:
                require(hashlib.file_digest(stream, 'sha256').hexdigest() == expected,
                        'DOWNLOADABLE_ZIP_ORIGINAL_DIFFERS:' + name)
    require(sha(archive) == outer_sha, 'DOWNLOADABLE_ZIP_CHANGED')
    return outer_sha


def verify(package, plan, repo, candidate, sources, supplied=None):
    delivery = package.binding.read(regular(candidate / 'DELIVERY.json'))
    identity = package.binding.identity(repo, plan, repo / 'release/r33_plan_binding.json', require_clean=True)
    head, tree = identity['source_commit'], identity['source_tree']
    require(delivery.get('schema') == 'STARCORE_R33_4_CANDIDATE_DELIVERY_1'
            and head == delivery['sourceCommit'] == sources['sourceCommit']
            and tree == delivery['sourceTree'] == sources['sourceTree']
            and identity['plan_manifest_sha256'] == delivery['planManifestSha256']
            and sources['sourceArchiveSha256'] == delivery['sourceArchive']['sha256']
            and sources['gitBundleSha256'] == delivery['gitBundle']['sha256'],
            'DELIVERY_SOURCE_OR_PLAN_IDENTITY')
    apk = delivery['apk']
    require(apk['file'] == Path(apk['file']).name and '\\' not in apk['file'], 'DELIVERY_APK_NAME')
    path = regular(candidate / apk['file'])
    require(package.binding.read(regular(candidate / 'build-identity-r334.json')) == identity,
            'BUILD_IDENTITY_ORIGINAL_DIFFERS')
    package.embedded(path, identity)
    log = regular(candidate / 'gradle-build.log')
    build_file = regular(candidate / 'CI_BUILD_RECEIPT.json')
    build = package.binding.read(build_file)
    command = build.get('command')
    require(build.get('schema') == 'STARCORE_R33_4_RELEASE_BUILD_1' and build.get('gradleRelease') == 'PASS'
            and type(build.get('exitCode')) is int and build['exitCode'] == 0
            and type(build.get('wrapperExitCode')) is int and build['wrapperExitCode'] == 0
            and build.get('sourceClean') is True and build.get('sourceCleanBeforeBuild') is True
            and build.get('sourceUnchangedDuringBuild') is True and not build.get('identityError')
            and not build.get('buildLaunchError') and build.get('sourceCommit') == head
            and build.get('sourceTree') == tree and build.get('apkSha256') == apk['sha256'] == sha(path)
            and build.get('planManifestSha256') == identity['plan_manifest_sha256']
            and log.stat().st_size > 0 and sha(log) == build.get('gradleLogSha256') == delivery['gradleLogSha256']
            and isinstance(command, list) and ':app:assembleRelease' in command, 'BUILD_ORIGINALS_MISMATCH')
    name = 'STARCORE-R33-{}-{}-APK-SOURCE-EVIDENCE.zip'.format(apk['versionCode'], head[:12])
    archive = archive_file(candidate, name, supplied)
    return dict(sourceCommit=head, sourceTree=tree, apkSha256=apk['sha256'], buildReceiptSha256=sha(build_file),
                gradleLogSha256=build['gradleLogSha256'], embeddedBuildIdentityVerified=True,
                downloadFile=archive.name, downloadSha256=archive_evidence(candidate, delivery, archive))


def match_signature(receipt, delivery, build):
    require(receipt['sourceCommit'] == build['sourceCommit'] and receipt['apk'] == delivery['apk']
            and {k: v for k, v in receipt['baseline033'].items() if k != 'file'} == delivery['baseline033']
            and receipt['buildReceiptSha256'] == build['buildReceiptSha256'],
            'SIGNED_BYTES_DIFFER_FROM_RECEIVED_BUILD')
