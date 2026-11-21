"""Retry the unchanged project verifier after quiescing the same disposable guest."""
def attach(original, record):
    import hashlib
    import json
    import shutil
    import time
    from pathlib import Path
    native = original.snapshot
    def capture(destination):
        dest=Path(destination)
        for number in range(1,7):
            attempt=dest.with_name(dest.name + '.partial-' + str(number))
            assert not attempt.exists() and not dest.exists()
            # The original checker also force-stops. Wait for its app user to go quiescent before that check.
            original.text('shell','am','force-stop','--user','0',original.PKG)
            time.sleep(3)
            try:
                result=native(dest)
                original_process=original.text('shell','ps','-A','-o','PID,ARGS')
                in_use=any(len(v.split())>=2 and (v.split()[1]==original.PKG or v.split()[1].startswith(original.PKG+':')) for v in original_process.splitlines())
                if in_use:raise AssertionError('APPLICATION_RESTARTED_BEFORE_POST_COPY')
                for name,component in result.items():
                    if not component['present']:continue
                    remote='/data/user/0/'+original.PKG+'/databases/'+name
                    if original.text('shell','sha256sum',remote).split()[0]!=component['sha256']:
                        raise AssertionError('DATABASE_CAPTURE_GENERATION_CHANGED')
                pristine=dest.with_name(dest.name+'.pristine')
                shutil.copytree(dest,pristine)
                for filename,row in result.items():
                    if row['present']:
                        with (pristine/filename).open('rb') as stream:
                            assert hashlib.file_digest(stream,'sha256').hexdigest()==row['sha256']
                record('local-original-snapshot',snapshot=dest.name,attempt=number,components=result)
                return result
            except AssertionError as error:
                if dest.is_dir():dest.rename(attempt)
                reason=str(error)
                record('local-snapshot-attempt-refused',snapshot=dest.name,attempt=number,reason=reason)
                if reason not in ('PACKAGE_PROCESS_STILL_ALIVE_DURING_COPY','DATABASE_CAPTURE_GENERATION_CHANGED','APPLICATION_RESTARTED_BEFORE_POST_COPY') or number==6:raise
                time.sleep(2)
    original.snapshot=capture
