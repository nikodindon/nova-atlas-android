"""Check the actual packaged debug manifest, not just source merge directives."""
from pathlib import Path
import xml.etree.ElementTree as E
import json
base=Path('app/build/intermediates/packaged_manifests/debug')
files=list(base.rglob('AndroidManifest.xml'))
assert len(files)==1,'packaged_manifest_missing_or_ambiguous'
root=E.parse(files[0]).getroot();android='{http://schemas.android.com/apk/res/android}'
app=root.find('application');assert app is not None
assert root.get('package')=='com.niko.novaatlas'
assert app.get(android+'allowBackup')=='false'
assert all(p.get(android+'name')!='com.google.android.gms.ads.MobileAdsInitProvider' for p in app.findall('provider'))
permissions={p.get(android+'name') for p in root.findall('uses-permission')}
assert not permissions.intersection({'com.google.android.gms.permission.AD_ID','android.permission.ACCESS_ADSERVICES_AD_ID','android.permission.ACCESS_ADSERVICES_ATTRIBUTION','android.permission.ACCESS_ADSERVICES_TOPICS'})
config=E.parse('app/src/main/res/xml/network_security_config.xml').getroot()
assert config.find('base-config').get('cleartextTrafficPermitted')=='false'
assert not config.findall('domain-config')
assert [c.get('src') for c in config.findall('.//certificates')]==['system']
result={'status':'pass','application_id':root.get('package'),'ads_provider':'absent','advertising_permissions':'absent','backup':False,'tls':'system_anchors_only','permissions':sorted(permissions)}
Path('manifest-validation.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
