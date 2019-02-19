#!/usr/bin/env python

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

headers = {
	'Accept': '*'
}

resp = requests.get(f"{URL}/api/diag/threads", headers=headers)
if resp.status_code != 200:
	print(f"Failed to get thread information from Indy: {resp}")

thread_info = resp.text
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

for k,v in sorted(names.items(), key=operator.itemgetter(1)):
	print(f"{k}: {v}")

print(f"Total: {total}")

