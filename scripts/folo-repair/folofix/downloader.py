import requests
import os
import json
from threading import Thread

class Downloader(Thread):
    def __init__(self, queue):
        self.queue = queue

    def run(self):
        while True:
            try:
                (dest,url) = self.queue.pop()

                if os.path.exists(dest):
                    print "Something else is writing / has already written: %s" % dest
                    continue

                print "Calculating dir of: %s" % dest
                destdir = os.path.dirname(dest)

                print "Calculated temp dir as: %s" % destdir
                if not os.path.isdir(destdir):
                    print "Creating directory: %s" % destdir
                    os.makedirs(destdir)

                r = requests.get(url, stream=True)
                if r.status_code != 200:
                    raise Exception("Failed to download: %s" % url)

                print "Writing to disk: %s" % url
                with open(dest, 'wb') as f:
                    #r.raw.decode_content = True
                    shutil.copyfileobj(r.raw, f)
            except (KeyboardInterrupt,SystemExit,Exception) as e:
                print e
                break
            except:
                print "Failed to download: %s" % url
            finally:
                self.queue.task_done()

