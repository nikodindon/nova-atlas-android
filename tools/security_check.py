"""Read committed blobs only; never private env, keystore or untracked files."""
import re,subprocess,json
files=subprocess.check_output(['git','ls-files','-z']).split(b'\0');bad=[]
for f in files:
 if not f:continue
 body=subprocess.check_output(['git','show','HEAD:'+f.decode()])
 if len(body)>2_000_000:continue
 if any(re.search(p,body) for p in [rb'sk-or-v1-[A-Za-z0-9]{24,}',rb'-----BEGIN (?:RSA |EC )?PRIVATE KEY-----',rb'AKIA[A-Z0-9]{16}']):bad.append('candidate_redacted')
print(json.dumps({'scope':'committed_patterns_only','candidates':len(bad)}))
raise SystemExit(bool(bad))
