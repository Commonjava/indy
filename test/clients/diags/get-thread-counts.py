#!/usr/bin/env python
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


import requests
import os
import sys
import re
import operator

if len(sys.argv) < 2:
	print(f"Usage: {sys.argv[0]} <INDY-URL>")
	exit(1)

URL=sys.argv[1]
THREAD_NAME_PATTERN = re.compile('^\S+')
THREAD_GROUP_PATTERN = re.compile('\s+Group: \S+')

headers = {
	'Accept': '*'
}

resp = requests.get(f"{URL}/api/diag/threads", headers=headers)
if resp.status_code != 200:
	print(f"Failed to get thread information from Indy: {resp}")

thread_info = resp.text
groups = {}
names = {}
total = 0
for line in thread_info.splitlines():
	if THREAD_NAME_PATTERN.match(line):
		name = line.rstrip()
		while name[-1].isdigit() or name[-1] == '-':
			name = name[0:-1]
		count = names.get(name) or 0
		names[name] = count+1
		total+=1

	if THREAD_GROUP_PATTERN.match(line):
		name = line.rstrip().split()[1]
		count = names.get(name) or 0
		groups[name] = count+1

print("Counts by thread group:\n------------------------------------------------")
for k,v in sorted(groups.items(), key=operator.itemgetter(1)):
	print(f"{k}: {v}")

print("\nCounts by name-inferred groupings:\n------------------------------------------------")
for k,v in sorted(names.items(), key=operator.itemgetter(1)):
	print(f"{k}: {v}")

print(f"\nTotal threads: {total}")

