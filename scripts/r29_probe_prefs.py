"""Bounded retry for a SharedPreferences XML snapshot during Android's atomic file replacement.
Unreadable/corrupt data stays unknown after retries. A real FAILED state is returned unchanged.
"""
import time
import xml.etree.ElementTree as ET


def read_preferences(read, attempts=3, sleep=time.sleep):
    for attempt in range(attempts):
        raw = read()
        try:
            root = ET.fromstring(raw or '')
            if root.tag != 'map': raise ET.ParseError('preferences map required')
            return {e.attrib['name']: e.text or e.attrib.get('value', '') for e in root if 'name' in e.attrib}
        except ET.ParseError:
            if attempt + 1 < attempts: sleep(.1)
    return {}
