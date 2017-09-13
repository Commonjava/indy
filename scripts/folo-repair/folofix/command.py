#
# Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import os
import folofix.reporter as reporter
import folofix.downloader as downloader
from Queue import Queue

@click.command()
@click.argument('indy_url')
@click.option('--threads', '-T', type=click.INT, default=4, help='Number of threads to use in verifying reports')
@click.option('--storage_dir', '-S', help='Indy storage folder, e.g., ${indy.home}/var/lib/indy/storage. This is to check files in storage folder')
def check(indy_url, threads, storage_dir):
    cwd = os.getcwd()
    reports_dir = os.path.join(cwd, 'mismatched')
    content_cache = os.path.join(cwd, 'content-cache')

    if not os.path.isdir(reports_dir):
        os.makedirs(reports_dir)

    if not os.path.isdir(content_cache):
        os.makedirs(content_cache)

    tracking_record_queue = Queue()
    download_queue = Queue()
    report_queue = Queue()

    try:
        task_ids = reporter.get_sealed_reports(indy_url)

        print "\n".join(task_ids)

        if threads < 1:
            threads = 1

        # threads for loading list of content to download/verify
        for t in range(threads):
            thread = reporter.DownloadLoader(tracking_record_queue, download_queue, report_queue, reports_dir, indy_url, content_cache)
            thread.daemon = True
            thread.start()

        # threads for downloading content
        for t in range(threads):
            thread = downloader.Downloader(download_queue)
            thread.daemon = True
            thread.start()

        for tid in task_ids:
            tracking_record_queue.put(tid)

        # wait for all tracking records to be processed for content to be cached
        tracking_record_queue.join()
        print "tracking_record_queue processed"

        # wait for content to be cached
        download_queue.join()
        print "download_queue processed"

        # threads for verifying content checksums / sizes
        for t in range(threads):
            thread = reporter.Reporter(report_queue, reports_dir, storage_dir)
            thread.daemon = True
            thread.start()

        # wait for all reports to be verified
        report_queue.join()
        print "report_queue processed"
    except (KeyboardInterrupt, SystemExit) as e:
        print e
        print "Quitting."

