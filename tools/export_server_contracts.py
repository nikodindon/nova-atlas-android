import json
from pathlib import Path
from tests.test_admin_security import app,password_hash,client,get
from tests.test_frontend_public_api import content
from tests.test_editorial_api import prepare
OUT=Path(__file__).resolve().parents[1]/'app/src/test/resources/contracts'
def test_contracts(app,client):
    paths,day,_=content(app)
    _,result=prepare(app)
    for name,url in [('legacy','/api/articles?date='+day),('site','/api/public/site'),('articles','/api/public/articles?date='+day),('events','/api/public/events'),('archives','/api/public/archives'),('subscription','/api/subscription/status?device_id=synthetic-installation')]:
        response=get(client,url);assert response.status_code==200
        (OUT/(name+'.json')).write_text(json.dumps(response.json,ensure_ascii=False,indent=2)+'\n')
    event=get(client,'/api/public/events').json['events'][0]['event_id']
    (OUT/'event.json').write_text(json.dumps(get(client,'/api/public/events/'+event).json,ensure_ascii=False,indent=2)+'\n')
    (OUT/'provenance.json').write_text(json.dumps({'server_commit':'0e8752622bbbe16cf9b3b187bb7d69a9b1e4533b','android_baseline':'d6fd28d2b3a3b787f13d6bc042d259d66ced2740','method':'Flask test client, synthetic fixtures, no server/network started'},indent=2)+'\n')

# Execute under the exact server checkout, using only its temporary pytest fixtures:
# PYTHONPATH=/path/to/Nova-Atlas .venv-baseline/bin/python -m pytest -q /path/to/this/file
