#
# Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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



