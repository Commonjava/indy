import click
import os
import folofix.reporter as reporter
import folofix.downloader as downloader
from Queue import Queue

@click.command()
@click.argument('indy_url')
@click.option('--threads', '-T', type=click.INT, default=4, help='Number of threads to use in verifying reports')
def check(indy_url, threads):
    cwd = os.getcwd()
    reports_dir = os.path.join(cwd, 'mismatched')
    content_cache = os.path.join(cwd, 'content-cache')

    if not os.path.isdir(reports_dir):
        os.makedirs(reports_dir)

    if not os.path.isdir(content_cache):
        os.makedirs(content_cache)

    tracking_record_queue = Queue()
    download_file_queue = Queue()
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

        # wait for content to be cached
        download_file_queue.join()

        # threads for verifying content checksums / sizes
        for t in range(threads):
            thread = reporter.Reporter(report_queue, content_cache)
            thread.daemon = True
            thread.start()

        # wait for all reports to be verified
        report_queue.join()
    except (KeyboardInterrupt, SystemExit) as e:
        print e
        print "Quitting."

