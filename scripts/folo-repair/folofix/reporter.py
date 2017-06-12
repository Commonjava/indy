#!/usr/bin/env python

import os
import json
import requests
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

def pull_folo_report(url, tracking_id):
    """Pull the Folo tracking report associated with the current build"""

    resp = requests.get("%s/api/folo/admin/%s/record" % (url, tracking_id))
    if resp.status_code == 200:
        return resp.json()
    elif resp.status_code == 404:
        return None
    else:
        resp.raise_for_status()

def verify_report(tracking_id, report_json_dir, report):
    downloads = report.get('downloads') or []
    uploads = report.get('uploads') or []

    print "Got %d downloads and %d uploads" % (len(downloads), len(uploads))

    entries = []
    for e in uploads:
        entries.append({'dataset': 'upload', 'entry': e})

    for e in downloads:
        entries.append({'dataset': 'download', 'entry': e})

    print "Processing %s entries for tracking report: %s" % (len(entries), tracking_id)

    result = {'results': []}
    _process_partition(entries, result['results'])

    if len(result['results']) > 0:
        print "Writing %s failed entries for tracking report: %s" % (len(result['results']), tracking_id)

        output_name = os.path.join(report_json_dir, "%s-mismatched.json" % tracking_id)
        with open(output_name, 'w') as f:
            f.write(json.dumps(result, indent=2))

    print "Tracking report analysis completed for: %s" % tracking_id
    return len(result['results']) < 1

def _process_partition(entries, results):
    for p in entries:
        entry = p['entry']
        path = entry['path'][1:]
        proceed = False
        for e in EXTS:
            if path.endswith(e):
                proceed = True
                break

        if proceed:
            try:
                dest = entry['cached_content']
                url = entry['content_url']

                print "Checking written size against report record for: %s" % url
                entry_data = {'path': path, 'local_url': url, 'type':p['dataset']}

                dest_sz = os.path.getsize(dest)
                if entry['size'] != dest_sz:
                    entry_data['size'] = {'success': False, 'record': entry['size'], 'calculated': dest_sz}
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


class DownloadLoader(Thread):
    def __init__(self, tracking_record_queue, download_queue, report_queue, reports_dir, indy_url, content_cache):
        Thread.__init__(self)
        self.record_queue = tracking_record_queue
        self.download_queue = download_queue
        self.reports_dir = reports_dir
        self.indy_url = indy_url
        self.content_cache = content_cache
        self.report_queue = report_queue

    def run(self):
        while True:
            try:
                tracking_id = self.record_queue.get()

                report = pull_folo_report(self.indy_url, tracking_id)

                if report is None:
                    print "Cannot retrieve: %s" % tracking_id
                    return

                print "Writing copy of tracking report: %s" % tracking_id

                record_json_file = os.path.join(self.reports_dir, "%s.json" % tracking_id)
                with open(record_json_file, 'w') as f:
                    f.write(json.dumps(report, indent=2))

                downloads = report.get('downloads') or []
                uploads = report.get('uploads') or []

                print "Got %d downloads and %d uploads" % (len(downloads), len(uploads))

                for section in [uploads, downloads]:
                    for entry in section:
                        url = entry.get('localUrl')
                        if url is None:
                            key_parts = entry['storeKey'].split(':')
                            url = "%s/api/%s/%s/%s" % (base_url, key_parts[0], key_parts[1], path)

                        dest = os.path.join(self.content_cache, tracking_id, entry['path'][1:])
                        entry['cached_content'] = dest
                        entry['content_url'] = url

                        self.download_queue.put((dest,url))

                self.report_queue.put((tracking_id, record_json_file, report))
            except (KeyboardInterrupt,SystemExit,Exception) as e:
                print e
                break
            finally:
                self.record_queue.task_done()

class Reporter(Thread):
    def __init__(self, report_queue, reports_dir):
        Thread.__init__(self)
        self.queue = report_queue
        self.reports_dir = reports_dir

    def run(self):
        while True:
            try:
                (tracking_id, record_json_file, report) = self.queue.get()
                verified = verify_report(tracking_id, self.reports_dir, report)
                if verified is True:
                    os.remove(record_json_file)

            except (KeyboardInterrupt,SystemExit,Exception) as e:
                print e
                break
            finally:
                self.queue.task_done()
