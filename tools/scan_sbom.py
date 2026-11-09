"""CI only: full resolved Maven graph, not six direct versions. No app is launched."""
import os,json,subprocess
from pathlib import Path
from security_tools import install,summarize
if os.environ.get('GITHUB_ACTIONS')!='true':raise SystemExit('Disposable CI only')
root=Path('app/build/reports/sbom')
bom=json.loads((root/'android.cdx.json').read_text())
assert any(c.get('group')=='com.google.guava' and c.get('name')=='guava' and c.get('version')=='33.7.2-android' for c in bom['components']), 'Guava patch not resolved'
tools=install(root/'tools')
def command(args):return subprocess.check_output(args,text=True,timeout=1200)
versions={name:json.loads(command([path,'version','-o','json'])) for name,path in tools.items()}
raw=command([tools['grype'],'sbom:'+str(root/'android.cdx.json'),'-o','json']);(root/'grype.json').write_text(raw)
db=json.loads(command([tools['grype'],'db','status','-o','json']))
result={'tools':versions,'database':db,'summary':summarize(json.loads(raw)),'native_coverage':'NOT_ESTABLISHED; see hashes of actual packaged .so files in apk-inventory.json','licenses':'NOT_ESTABLISHED'}
(root/'security-summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result['summary']))
