#!/usr/bin/python
#
# Copyright (C) 2015 John Casey (jdcasey@commonjava.org)
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
from urllib2 import urlopen
# import tarfile
import shutil
import fnmatch

def run(cmd, fail_message='Error running command', fail=True):
  cmd += " 2>&1"
  print cmd
  ret = os.system(cmd)
  if fail is True and ret != 0:
    print "%s (failed with code: %s)" % (fail_message, ret)
    sys.exit(ret)



def runIn(cmd, workdir, fail_message='Error running command', fail=True):
  cmd += " 2>&1"
  olddir = os.getcwd()
  os.chdir(workdir)
  
  print "In: %s, executing: %s" % (workdir, cmd)
  
  ret = os.system(cmd)
  if fail is True and ret != 0:
    print "%s (failed with code: %s)" % (fail_message, ret)
    sys.exit(ret)
  
  os.chdir(olddir)



def move_and_link(src, target, replaceIfExists=False):
  srcParent = os.path.dirname(src)
  if not os.path.isdir(srcParent):
    print "mkdir -p %s" % srcParent
    os.makedirs(srcParent)
  
  if not os.path.isdir(target):
    print "mkdir -p %s" % target
    os.makedirs(target)
  
  if os.path.isdir(src):
    for f in os.listdir(src):
      targetFile = os.path.join(target, f)
      srcFile = os.path.join(src, f)
      print "%s => %s" % (srcFile, targetFile)
      if os.path.exists(targetFile):
        if not replaceIfExists:
          print "Target dir exists: %s. NOT replacing." % targetFile
          continue
        else:
          print "Target dir exists: %s. Replacing." % targetFile
        
        if os.path.isdir(targetFile):
          print "rm -r %s" % targetFile
          shutil.rmtree(targetFile)
        else:
          print "rm %s" % targetFile
          os.remove(targetFile)
      
      if os.path.isdir(srcFile):
        print "cp -r %s %s" % (srcFile, targetFile)
        shutil.copytree(srcFile, targetFile)
      else:
        print "cp %s %s" % (srcFile, targetFile)
        shutil.copy(srcFile, targetFile)
    
    print "rm -r %s" % src
    shutil.rmtree(src)
  
  print "ln -s %s %s" % (target, src)
  os.symlink(target, src)




# Envar for reading development binary volume mount point
#INDY_DEPLOY_VOL = '/tmp/indy'
SSH_CONFIG_VOL = '/tmp/ssh-config'
#INDY_BINARIES_PATTERN='indy.tar.gz'


INDY_ETC_URL_ENVAR = 'INDY_ETC_URL'
INDY_ETC_BRANCH_ENVAR = 'INDY_ETC_BRANCH'
INDY_ETC_SUBPATH_ENVAR = 'INDY_ETC_SUBPATH'

INDY_OPTS_ENVAR = 'INDY_OPTS'


# locations for expanded indy binary
INDY_DIR = '/opt/indy'
BOOT_PROPS = 'boot.properties'
INDY_BIN = os.path.join(INDY_DIR, 'bin')
INDY_ETC = os.path.join(INDY_DIR, 'etc/indy')
INDY_STORAGE = os.path.join(INDY_DIR, 'var/lib/indy/storage')
INDY_DATA = os.path.join(INDY_DIR, 'var/lib/indy/data')
INDY_LOGS = os.path.join(INDY_DIR, 'var/log/indy')


# locations on global fs
ETC_INDY = '/etc/indy'
VAR_INDY = '/var/lib/indy'
VAR_STORAGE = os.path.join(VAR_INDY, 'storage')
VAR_DATA = os.path.join(VAR_INDY, 'data')
LOGS = '/var/log/indy'


# Git location supplying /opt/indy/etc/indy
indyEtcUrl = os.environ.get(INDY_ETC_URL_ENVAR)
indyEtcBranch = os.environ.get(INDY_ETC_BRANCH_ENVAR) or 'master'
indyEtcSubpath = os.environ.get(INDY_ETC_SUBPATH_ENVAR)

# command-line options for indy
opts = os.environ.get(INDY_OPTS_ENVAR) or ''

print "Read environment:\n  indy etc Git URL: %s\n  indy cli opts: %s" % (indyEtcUrl, opts)

if os.path.isdir(SSH_CONFIG_VOL) and len(os.listdir(SSH_CONFIG_VOL)) > 0:
  print "Importing SSH configurations from volume: %s" % SSH_CONFIG_VOL
  run("cp -vrf %s /root/.ssh" % SSH_CONFIG_VOL)
  run("chmod -v 700 /root/.ssh", fail=False)
  run("chmod -v 600 /root/.ssh/*", fail=False)


if os.path.isdir(INDY_DIR) is False:
  print "Cannot start, %s does not exist!" % INDY_DIR
  exit(1)
  # parentDir = os.path.dirname(INDY_DIR)
  # if os.path.isdir(parentDir) is False:
  #   os.makedirs(parentDir)
  
  # unpacked=False
  # for file in os.listdir(INDY_DEPLOY_VOL):
  #   if fnmatch.fnmatch(file, INDY_BINARIES_PATTERN):
  #     devTarball = os.path.join(INDY_DEPLOY_VOL, file)
  #     print "Unpacking development binary of Indy: %s" % devTarball
  #     run('tar -zxvf %s -C /opt' % devTarball)
  #     unpacked=True
  #     break
  # if unpacked is False:
  #   if not os.path.exists(os.path.join(INDY_DEPLOY_VOL, 'bin/indy.sh')):
  #     print "Deployment volume %s exists but doesn't appear to contain expanded Indy (can't find 'bin/indy.sh'). Ignoring." % INDY_DEPLOY_VOL
  #   else:
  #     print "Using expanded Indy deployment, in volume: %s" % INDY_DEPLOY_VOL
  #     shutil.copytree(INDY_DEPLOY_VOL, INDY_DIR)
    
  # etcBootOpts = os.path.join(INDY_ETC, BOOT_PROPS)
  # if os.path.exists(etcBootOpts):
  #   binBootOpts = os.path.join(INDY_BIN, BOOT_PROPS)
  #   if os.path.exists(binBootOpts):
  #     os.remove(binBootOpts)
  #   os.symlink(binBootOpts, etcBootOpts)

if indyEtcUrl is not None:
  if os.path.isdir(INDY_ETC):
    print "clearing pre-existing Indy etc directory"
    shutil.rmtree(INDY_ETC)

  print "Cloning: %s" % indyEtcUrl
  run("git clone --branch %s --verbose --progress %s %s 2>&1" % (indyEtcBranch, indyEtcUrl, INDY_ETC), "Failed to checkout %s branch of indy/etc from: %s" % (indyEtcBranch, indyEtcUrl))
  if indyEtcSubpath is not None and indyEtcSubpath != '.':
    run("git read-tree -um --aggressive `git write-tree`: HEAD:%s" % indyEtcSubpath, "Failed to relocate %s subpath to %s", (indyEtcSubpath, INDY_ETC))
  
move_and_link(INDY_ETC, ETC_INDY, replaceIfExists=True)
move_and_link(INDY_STORAGE, VAR_STORAGE)
move_and_link(INDY_DATA, VAR_DATA)
move_and_link(INDY_LOGS, LOGS)


run("%s %s" % (os.path.join(INDY_DIR, 'bin', 'indy.sh'), opts), fail=False)
