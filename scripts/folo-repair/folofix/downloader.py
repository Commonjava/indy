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

import requests
import os
import shutil
from threading import Thread
import errno

class Downloader(Thread):
    def __init__(self, queue):
        Thread.__init__(self)
        self.queue = queue

    def run(self):
        while True:
            try:
                (dest,url) = self.queue.get()

                if os.path.exists(dest):
                    print "Something else is writing / has already written: %s" % dest
                    continue

                print "Calculating dir of: %s" % dest
                destdir = os.path.dirname(dest)

                print "Calculated temp dir as: %s" % destdir
                if not os.path.isdir(destdir):
                    print "Creating directory: %s" % destdir
                    try:
                        os.makedirs(destdir)
                    except OSError as exception:
                        if exception.errno != errno.EEXIST:
                            raise

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

