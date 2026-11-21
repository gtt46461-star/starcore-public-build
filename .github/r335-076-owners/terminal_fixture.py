"""Build a separate end-to-end case with a genuine settlement owner between draw insertion and online training."""
from pathlib import Path
import hashlib
import sys

origin, destination = map(Path,sys.argv[1:])
original = origin.read_bytes()
assert hashlib.sha256(original).hexdigest() == '6761a7e36754e179e361519c592bf33f152b0206fc33855e52d54d9661fd3420'
text = original.decode()
assert text.count('class R335TrainedBackupTest {') == 1
assert text.count('void confirmedDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority()') == 1
text = text.replace('class R335TrainedBackupTest {','class R335TerminalBackupTest {')
text = text.replace('void confirmedDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority()',
                    'void terminalDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority()')
anchor = '            assertEquals(1,LivePoolR26.trainOne(f.store,()->f.claim,f.budget()));'
assert text.count(anchor) == 1
exercise = '''            assertEquals(0, LivePoolR26.trainOne(f.store, () -> f.claim, f.budget()));
            assertEquals(0, scalar(f.db, "SELECT count(*) FROM r26_consumption WHERE event_id='" + event + "'"));
            SettlementCoordinatorR815.settleNow(f.store, future, SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
            SettlementTaskStoreR36.Terminal terminal = SettlementTaskStoreR36.terminal(f.store, future.issue);
            assertNotNull(terminal);
            assertEquals(SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL, terminal.status);
'''
destination.write_text(text.replace(anchor,exercise+anchor))
assert destination.name=='R335TerminalBackupTest.java' and origin.read_bytes()==original
