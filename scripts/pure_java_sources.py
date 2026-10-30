"""Select the deployed Java kernels supported by the independent HOST suites.

Android adapters are compiled by the Android build, not by these SDK-free suites.
Comments and literals are ignored: mentioning an Android class is not a dependency.
The supplied Root helpers are explicit so a new integration dependency is visible.
"""
from pathlib import Path
import re

ROOT_PACKAGE = 'com.ethk3.novaonepro.'
STRATEGY_PACKAGE = ROOT_PACKAGE + 'strategyv2.'
PURE_ROOT_TYPES = frozenset(ROOT_PACKAGE + name for name in (
    'ResearchSemanticsGeneratedR967',
    'r952.AtomEvaluatorCP4', 'r952.AtomWindowCP4', 'r952.MemoryBudgetCP4',
    'r952.MatrixMetricsCP11', 'r952.StrictCanonicalJson',
))
_NON_CODE = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\r\n]*|/\*[\s\S]*?\*/')
_PLATFORM = re.compile(r'\b(?:android|androidx)\s*\.\s*[A-Za-z_$]')
_ROOT_REFERENCE = re.compile(r'\bcom\s*\.\s*ethk3\s*\.\s*novaonepro\s*\.\s*(?:[A-Za-z_$][\w$]*\s*\.\s*)*(?:[A-Za-z_$][\w$]*|\*)')


def exclusion_reason(source):
    code = _NON_CODE.sub(lambda match: '\n' * match.group(0).count('\n') or ' ', source)
    platform = _PLATFORM.search(code)
    if platform:
        return 'Android platform dependency: ' + platform.group(0).replace(' ', '')
    for match in _ROOT_REFERENCE.finditer(code):
        reference = re.sub(r'\s+', '', match.group(0))
        if reference == STRATEGY_PACKAGE.rstrip('.') or reference.startswith(STRATEGY_PACKAGE):
            continue
        if any(reference == name or reference.startswith(name + '.') for name in PURE_ROOT_TYPES):
            continue
        return 'Root integration dependency outside supplied HOST helpers: ' + reference
    return None


def select_pure_strategy_sources(directory):
    selected, excluded = [], []
    for path in sorted(Path(directory).glob('*.java')):
        reason = exclusion_reason(path.read_text(encoding='utf-8'))
        if reason:
            excluded.append({'source': path.name, 'reason': reason})
        else:
            selected.append(path)
    report = {'selected_count': len(selected), 'selected': [path.name for path in selected],
              'excluded_android_or_root_adapters': excluded}
    print('pure_java_source_selection: selected=' + str(len(selected)) + '; excluded_adapters='
          + ','.join(item['source'] for item in excluded), flush=True)
    return selected, report
