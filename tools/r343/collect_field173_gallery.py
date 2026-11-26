#!/usr/bin/env python3
"""Preserve current emulator chart images and SQL observations as supplemental originals."""
import json
from pathlib import Path
import re
import subprocess

import run_native_field_case as native

SELECTORS = {
    'R343-040-POS': 'collectGalleryAndPagedSqlFromActualAndroidOwners',
    'R343-040-NEG': 'staleFrameAndMissingTableRemainExplicitInNativeGallery',
    'R343-040-KEEP': 'galleryRefreshPreservesOriginalSourceRows',
}
GALLERY_SCOPE = 'SYNTHETIC_ANDROID_GALLERY_COLLECTION'


def read_device(serial, path, limit):
    size = subprocess.check_output(native.adb(serial, 'shell', 'wc', '-c', path),
                                   text=True, timeout=20).strip().split()
    native.contract.require(size and size[0].isdecimal() and 0 < int(size[0]) <= limit,
                            'FIELD173_GALLERY_FILE_SIZE')
    data = subprocess.check_output(native.adb(serial, 'exec-out', 'cat', path), timeout=20)
    native.contract.require(len(data) == int(size[0]), 'FIELD173_GALLERY_DURING_READ_SIZE_CHANGED')
    return data


def collect(expected, serial, output, observation):
    seen = observation['readback']
    scope = observation['dataScope']
    gallery = seen.get('galleryDirectory')
    suffix = native.PACKAGE + '/files/field173-galleries/R343_' + expected['executionId']
    native.contract.require(isinstance(gallery, str) and gallery in {
        '/storage/emulated/0/Android/data/' + suffix,
        '/sdcard/Android/data/' + suffix,
    }, 'FIELD173_GALLERY_EXTERNAL_SCOPE')
    native.contract.require(scope.get('role') == 'SYNTHETIC'
                            and seen.get('scope') == GALLERY_SCOPE
                            and seen.get('chartCount') == 47 and seen.get('logicalTableCount') == 16,
                            'FIELD173_GALLERY_SCOPE')
    raw = read_device(serial, gallery + '/gallery.json', 4 * 1024 * 1024)
    native.contract.require(native.contract.digest(raw) == seen.get('gallerySha256'), 'FIELD173_GALLERY_HASH')
    manifest = json.loads(raw)
    native.contract.require(manifest.get('scope') == GALLERY_SCOPE
                            and manifest.get('sourceCommit') == expected['sourceCommit']
                            and manifest.get('databaseIdentity') == scope['databaseIdentity'], 'FIELD173_GALLERY_IDENTITY')
    catalog_path = native.ROOT / native.contract.PLAN_PATH / 'coverage/chart_source_mapping_63.json'
    catalog = json.loads(catalog_path.read_bytes())['objects']
    chart_ids = {item['id'] for item in catalog if item['category'] in {'legacyCharts', 'researchCharts'}}
    table_ids = {'T%02d' % index for index in range(1, 17)}
    charts = manifest['charts']
    tables = manifest['logicalTables']
    native.contract.require(len(charts) == 47 and {chart['id'] for chart in charts} == chart_ids,
                            'FIELD173_GALLERY_EXACT_CHART_SET')
    native.contract.require(len(tables) == 16 and {table['id'] for table in tables} == table_ids,
                            'FIELD173_GALLERY_EXACT_TABLE_SET')

    prefix = 'field173-' + expected['caseId'] + '-'
    refs = [native.write_original(output / (prefix + 'gallery.json'), raw)]
    for chart in charts:
        chart_id = chart['id']
        native.contract.require(re.fullmatch(r'[A-Za-z0-9_-]{1,48}', chart_id), 'FIELD173_GALLERY_INVALID_ID')
        name = chart_id + '.png'
        native.contract.require(chart['screenshotFile'] == name, 'FIELD173_GALLERY_INVALID_IMAGE_NAME')
        png = read_device(serial, gallery + '/' + name, 8 * 1024 * 1024)
        native.contract.require(png.startswith(b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR')
                                and native.contract.digest(png) == chart['screenshotSha256'],
                                'FIELD173_GALLERY_IMAGE_OR_HASH_INVALID:' + chart_id)
        refs.append(native.write_original(output / (prefix + name), png))
    return refs


if __name__ == '__main__':
    import os
    context = json.loads(Path(os.environ['STARCORE_FIELD173_CASE_CONTEXT']).read_bytes())
    method = SELECTORS[context['caseId']]
    native.run('com.ethk3.novaonepro.Field173ChartGalleryCapture#' + method, collect=collect)
