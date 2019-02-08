import click
from os import getcwd
from os.path import join
from ruamel.yaml import YAML
import requests
import json

URL = 'url'

@click.command()
@click.option('-c', '--config', type=click.File('r'), default=lambda: join(getcwd(), 'config.yml'), show_default="$PWD/config.yml")
def run(config):
	yaml = YAML(typ='safe')
	cfg = yaml.load(config)

	base_url=cfg[URL]

	headers = {
		'Content-Type': 'application/json',
		'Accept': 'application/json'
	}

	r = requests.get(f"{base_url}/api/admin/stores/maven/group/public", headers=headers)
	if r.status_code != 200:
		print(f"Cannot retrieve public group from: {base_url}, status: {r.status_code}")
		exit(-1)

	group = r.json()
	group['constituents'] = ['maven:hosted:local-deployments']

	r = requests.put(f"{base_url}/api/admin/stores/maven/group/public", data=json.dumps(group), headers=headers)
	if r.status_code != 200:
		print(f"Failed to update public group membership from: {base_url}, status: {r.status_code}")
		exit(-2)



