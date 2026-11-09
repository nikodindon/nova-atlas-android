"""Pinned official Anchore tool binaries; reports are potential pending triage."""
import collections,hashlib,io,json,tarfile,urllib.request
from pathlib import Path
TOOLS={
 'syft':('1.55.0','bb65249542db5fedeeb4d3f9b018b75e7264dbfbbb10486f044581a92f90490d'),
 'grype':('0.121.2','9c8965ced9df657e5e851c14021bae838802f711edd7a48f9bc3c923a8d28f12')}

def install(directory):
    directory.mkdir(mode=0o700)
    for name,(version,sha) in TOOLS.items():
        url=f'https://github.com/anchore/{name}/releases/download/v{version}/{name}_{version}_linux_amd64.tar.gz'
        with urllib.request.urlopen(url,timeout=60) as response:raw=response.read(100*1024*1024+1)
        if len(raw)>100*1024*1024 or hashlib.sha256(raw).hexdigest()!=sha:raise ValueError('tool_checksum_mismatch')
        with tarfile.open(fileobj=io.BytesIO(raw),mode='r:gz') as archive:
            member=archive.getmember(name)
            if not member.isfile() or member.size>200*1024*1024:raise ValueError('invalid_tool_archive')
            body=archive.extractfile(member).read()
        path=directory/name
        with path.open('xb') as out:out.write(body)
        path.chmod(0o700)
    return {name:str((directory/name).resolve()) for name in TOOLS}

def summarize(report):
    matches=report.get('matches')
    if not isinstance(matches,list) or not isinstance(report.get('descriptor'),dict):raise ValueError('invalid_scan_report')
    severity=collections.Counter(m.get('vulnerability',{}).get('severity','Unknown') for m in matches)
    return {'status':'WARNING' if matches else 'PASS_WITHIN_SCANNER_COVERAGE','matches':len(matches),'severity':dict(severity),
        'classification':'potential_pending_applicability_review' if matches else 'no_matches_in_this_database_not_a_security_guarantee',
        'automatic_suppressions':0,'public_release_authorized':False}

