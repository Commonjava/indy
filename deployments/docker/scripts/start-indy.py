#!/usr/bin/python
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

from __future__ import print_function

import os
import sys
import shutil
import subprocess
import signal
import shlex

INDY_ETC_URL_ENVAR = 'INDY_ETC_URL'
INDY_ETC_BRANCH_ENVAR = 'INDY_ETC_BRANCH'
INDY_ETC_SUBPATH_ENVAR = 'INDY_ETC_SUBPATH'

INDY_OPTS_ENVAR = 'INDY_OPTS'

DEFAULT_DATA = '/opt/indy-archives/indy-launcher-data.tar.gz'
DEFAULT_ETC = '/opt/indy-archives/indy-launcher-etc.tar.gz'

# locations for expanded indy binary
INDY_DIR = '/opt/indy'
BOOT_PROPS = 'boot.properties'
INDY_BIN = os.path.join(INDY_DIR, 'bin')
INDY_ETC = os.path.join(INDY_DIR, 'etc/indy')
INDY_STORAGE = os.path.join(INDY_DIR, 'var/lib/indy/storage')
INDY_DATA = os.path.join(INDY_DIR, 'var/lib/indy/data')
INDY_LOGS = os.path.join(INDY_DIR, 'var/log/indy')


# volume mount locations
VOL_ETC = '/indy/etc'
VOL_STORAGE = '/indy/storage'
VOL_DATA = '/indy/data'
VOL_LOGS = '/indy/logs'
VOL_SSH = '/indy/ssh'

# location where git checkout happens
GIT_ETC = '/opt/indy-git-etc'

# Git location supplying /opt/indy/etc/indy
ETC_URL = os.environ.get(INDY_ETC_URL_ENVAR)
ETC_BRANCH = os.environ.get(INDY_ETC_BRANCH_ENVAR) or 'master'
ETC_SUBPATH = os.environ.get(INDY_ETC_SUBPATH_ENVAR)

# command-line options for indy
OPTS = os.environ.get(INDY_OPTS_ENVAR) or ''


def handle_shutdown(signum, frame):
    print("SIGTERM: Stopping Indy.")
    process.send_signal(signal.SIGTERM)

def handle_output(process):
    try:
        for c in iter(lambda: process.stdout.read(1), ''):
          sys.stdout.write(c)
    except KeyboardInterrupt:
        print("")
    return

def run(cmd, fail_message='Error running command', fail=True):
    cmd += " 2>&1"
    print(cmd)
    ret = os.system(cmd)
    if fail is True and ret != 0:
        print("%s (failed with code: %s)" % (fail_message, ret))
        sys.exit(ret)

def runIn(cmd, workdir, fail_message='Error running command', fail=True):
    cmd += " 2>&1"
    olddir = os.getcwd()
    os.chdir(workdir)

    print("In: %s, executing: %s" % (workdir, cmd))

    ret = os.system(cmd)
    if fail is True and ret != 0:
        print("%s (failed with code: %s)" % (fail_message, ret))
        sys.exit(ret)

    os.chdir(olddir)

def link(src, target):
    print("Source: %s (exists? %s)" % (src, os.path.isdir(src)))
    print("Target: %s (exists? %s)" % (target, os.path.exists(target)))

    targetParent = os.path.dirname(target)
    if os.path.isdir(targetParent) is False:
        os.makedirs(targetParent, 0o755)

    if os.path.islink(target):
        print("rm -f %s" % src)
        os.unlink(target)

    if os.path.isdir(target):
        print("rm -rf %s" % src)
        shutil.rmtree(target)

    print("ln -s %s %s" % (src, target))
    os.symlink(src, target)



print("Read environment:\n  indy cli opts: %s" % (OPTS))

if os.path.isdir(INDY_DIR) is False:
    print("Cannot start, %s does not exist!" % INDY_DIR)
    exit(1)

if os.path.isdir(VOL_SSH) and len(os.listdir(VOL_SSH)) > 0:
    print("Importing SSH configurations from volume: %s" % VOL_SSH)
    run("cp -vrf %s /root/.ssh" % VOL_SSH)
    run("chmod -v 700 /root/.ssh", fail=False)
    run("chmod -v 600 /root/.ssh/*", fail=False)
    run("restorecon -R /root/.ssh/*", fail=False)

if os.path.exists(os.path.join(VOL_DATA, 'indy')) is False:
    print("Extracting default Indy data content: %s to: %s" % (DEFAULT_DATA, VOL_DATA))
    run("tar -zxvf %s -C /indy" % DEFAULT_DATA)

etc_src=VOL_ETC
if ETC_URL is not None:
    if os.path.isdir(GIT_ETC):
        print("clearing pre-existing Indy etc directory")
        shutil.rmtree(GIT_ETC)

    print("Cloning: %s" % ETC_URL)
    run("git clone --branch %s --verbose --progress %s %s 2>&1" % (ETC_BRANCH, ETC_URL, GIT_ETC), "Failed to checkout %s branch of indy/etc from: %s" % (ETC_BRANCH, ETC_URL))

    if ETC_SUBPATH is not None and ETC_SUBPATH != '.':
        runIn("git read-tree -um --aggressive `git write-tree`: HEAD:%s" % ETC_SUBPATH, GIT_ETC, "Failed to relocate %s subpath to %s", (ETC_SUBPATH, GIT_ETC))

    etc_src=GIT_ETC

elif os.path.exists(os.path.join(VOL_ETC, 'main.conf')) is False:
    print("Extracting default etc/indy content: %s to: %s" % (DEFAULT_ETC, VOL_ETC))
    run("tar -zxvf %s -C /indy" % DEFAULT_ETC)

link(etc_src, INDY_ETC)
link(VOL_STORAGE, INDY_STORAGE)
link(VOL_DATA, INDY_DATA)
link(VOL_LOGS, INDY_LOGS)

cmd_parts = [os.path.join(INDY_DIR, 'bin', 'indy.sh')]
cmd_parts += shlex.split(OPTS)

print("Command parts: %s" % cmd_parts)
process = subprocess.Popen(cmd_parts, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

signal.signal(signal.SIGTERM, handle_shutdown)

handle_output(process)
