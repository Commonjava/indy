#!/usr/bin python

import json
import re
import sys
import os.path
import requests

args=sys.argv

#indy="http://indy-master-indy-preview.apps.ocp-c1.prod.psi.redhat.com"

indy = args[1]
input_json = args[2]

headers = {}

if len(args) > 3:
    token = args[3]
    headers = {'Authorization': 'Bearer ' + token}

api=indy + "/api/admin/stores"

with open(input_json) as f:
    data = json.load(f)

items=data["items"]

out='a.out.' + os.path.basename(input_json)

completed=[]

file_exists = os.path.exists(out)
if file_exists:
    with open(out) as f:
        lines = f.readlines()
    for l in lines:
        words = l.split()
        completed.append(words[0])

with open(out, 'a') as outfile:
    for item in items:
        if item["key"] in completed:
            print("{} - skip".format(item["key"]))
            continue
        url = api + "/" + item["packageType"] + "/" + item["type"]
        x = requests.post(url, headers = headers, data = json.dumps(item))
        txt = "{} - {}".format(item["key"], x.status_code) 
        outfile.write("%s\n" % txt)
        print(txt)
        #print("handle - " + item["key"])
