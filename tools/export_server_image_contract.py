import json
from pathlib import Path
from tests.test_admin_security import app,password_hash,client,get
from tests.test_frontend_public_api import content
from deploy.rc_validation import seed_media
from modules.web.atlas_web import create_app
OUT=Path(__file__).resolve().parents[1]/'app/src/test/resources/contracts'
def test_approved(app):
 paths,day,_=content(app)
 rows=json.loads((paths['articles']/f'{day}_articles.json').read_text())
 seed_media(paths['root'],rows)
 configured=create_app({'media':{'mode':'approved_only'}},root=paths['root']).test_client()
 response=get(configured,'/api/public/articles?date='+day)
 assert response.status_code==200
 assert response.json['articles'][0]['image']['fallback'] is False
 (OUT/'approved-articles.json').write_text(json.dumps(response.json,ensure_ascii=False,indent=2)+'\n')
