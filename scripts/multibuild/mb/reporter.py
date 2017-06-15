#!/usr/bin/env python
#
# Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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


import os
import sys
import json
import requests
import shutil
import hashlib
from threading import Thread

EXTS = ['.jar', '.pom', '.tar.gz', '.zip']

def get_sealed_reports(url):
    resp = requests.get("%s/api/folo/admin/report/ids/sealed" % url)
    if resp.status_code != 200:
        print resp.text

    resp.raise_for_status()

    report_list = resp.json()

    return [str(tid) for tid in report_list.get('sealed') or []]

def verify_report(builddir, url, tracking_id):
    print "Checking tracking report: %s" % tracking_id

    print "Retrieving tracking report: %s" % tracking_id

    report = _pull_folo_report({'url': url, 'id': tracking_id})
    if report is None:
        print "Cannot retrieve: %s" % tracking_id
        return

    report_name = os.path.basename(builddir)

    print "Writing copy of tracking report: %s" % tracking_id

    input_name = os.path.join(builddir, "tracking-report.json")
    with open(input_name, 'w') as f:
        f.write(json.dumps(report, indent=2))

    downloads = report.get('downloads') or []
    uploads = report.get('uploads') or []

    tmp = os.path.join(builddir, 'content-temp')
    if os.path.isdir(tmp):
        shutil.rmtree(tmp)

    os.makedirs(tmp)

    print "Got %d downloads and %d uploads" % (len(downloads), len(uploads))

    entries = []
    for e in uploads:
        entries.append({'dataset': 'upload', 'entry': e})

    for e in downloads:
        entries.append({'dataset': 'download', 'entry': e})

    print "Processing %s entries for tracking report: %s" % (len(entries), tracking_id)

    result = {'results': []}
    _process_partition(url, entries, result['results'], tmp)

    print "Writing %s failed entries for tracking report: %s" % (len(result['results']), tracking_id)

    output_name = os.path.join(builddir, "tracking-verify.json")
    with open(output_name, 'w') as f:
        f.write(json.dumps(result, indent=2))

    print "Tracking report analysis completed for: %s" % tracking_id

def _pull_folo_report(params):
    """Pull the Folo tracking report associated with the current build"""

    resp = requests.get("%(url)s/api/folo/admin/%(id)s/record" % params)
    if resp.status_code == 200:
        return resp.json()
    elif resp.status_code == 404:
        return None
    else:
        resp.raise_for_status()

def _process_partition(base_url, partition, results, tmp):
    for p in partition:
        entry = p['entry']
        path = entry['path'][1:]
        proceed = False
        for e in EXTS:
            if path.endswith(e):
                proceed = True
                break

        if proceed:
            url = entry.get('localUrl')
            if url is None:
                key_parts = entry['storeKey'].split(':')
                url = "%s/api/%s/%s/%s" % (base_url, key_parts[0], key_parts[1], path)

            print "Checking: %s" % url

            try:
                r = requests.get(url, stream=True)
                if r.status_code != 200:
                    raise Exception("Failed to download: %s" % url)

                print "Getting size header for: %s" % url
                header_size=int(r.headers['content-length'])

                print "Preparing temp directory for writing to disk: %s, using temp base: %s and path: %s" % (url, tmp, path)
                dest = os.path.join(tmp, path)
                print "Calculating dir of: %s" % dest
                destdir = os.path.dirname(dest)
                print "Calculated temp dir as: %s" % destdir
                if not os.path.isdir(destdir):
                    print "Creating directory: %s" % destdir
                    os.makedirs(destdir)

                print "Writing to disk: %s" % url
                with open(dest, 'wb') as f:
                    #r.raw.decode_content = True
                    shutil.copyfileobj(r.raw, f)

                print "Checking written size against report record for: %s" % url
                entry_data = {'path': path, 'local_url': url, 'type':p['dataset']}

                dest_sz = os.path.getsize(dest)
                if entry['size'] != dest_sz or entry['size'] != header_size or dest_sz != header_size:
                    entry_data['size'] = {'success': False, 'record': entry['size'], 'header': header_size, 'calculated': dest_sz}
                else:
                    entry_data['size'] = {'success': True}

                print "Calculating checksums for: %s" % url
                _compare_checksum('md5', url, dest, entry, entry_data)
                _compare_checksum('sha1', url, dest, entry, entry_data)
                # compare_checksum('sha256', url, dest, entry, entry_data)

                append=False
                for k in ['size', 'md5', 'sha1']:
                    if entry_data[k]['success'] is False:
                        print "FAIL: %s" % url
                        append = True
                        break

                if append is True:
                    results.append(entry_data)
            except:
                print "Failed to download: %s" % url

    return results

def _compare_checksum(checksum_type, url, dest, entry, entry_data):
    check_url = url + '.' + checksum_type
    print "Retrieving %s checksum: %s" % (checksum_type, check_url)
    
    r = requests.get(check_url)
    if r.status_code == 200:
        check_file = r.text
    else:
        check_file = None

    print "Calculating %s checksum from file on disk for: %s" % (checksum_type, url)
    check = hashlib.new(checksum_type, open(dest, 'rb').read()).hexdigest()
    if entry[checksum_type] != check: # or entry[checksum_type] != check_file or check != check_file:
        entry_data[checksum_type] = {'success': False, 'record': entry[checksum_type], 'file': check_file, 'calculated': check}
    else:
        entry_data[checksum_type] = {'success': True}


class Reporter(Thread):
    def __init__(self, report_queue):
        Thread.__init__(self)
        self.queue = report_queue

    def run(self):
        while True:
            try:
                (builddir, url, tracking_id) = self.queue.get()
                verify_report(builddir, url, tracking_id)
            except (KeyboardInterrupt,SystemExit,Exception) as e:
                print e
                break
            finally:
                self.queue.task_done()

