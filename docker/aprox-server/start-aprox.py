#!/usr/bin/python

import os
import sys
from urllib2 import urlopen
# import tarfile
import shutil
import fnmatch

def run(cmd, fail_message='Error running command', fail=True):
  print cmd
  ret = os.system(cmd)
  if fail and ret != 0:
    print "%s (failed with code: %s)" % (fail_message, ret)
    sys.exit(ret)



def runIn(cmd, workdir, fail_message='Error running command', fail=True):
  olddir = os.getcwd()
  os.chdir(workdir)
  
  print "In: %s, executing: %s" % (workdir, cmd)
  
  ret = os.system(cmd)
  if fail and ret != 0:
    print "%s (failed with code: %s)" % (fail_message, ret)
    sys.exit(ret)
  
  os.chdir(olddir)



def move_and_link(src, target, replaceIfExists=False):
  srcParent = os.path.dirname(src)
  if not os.path.isdir(srcParent):
    os.mkdir(srcParent)
  
  if not os.path.isdir(target):
    os.mkdir(target)
  
  print "Source: %s\nTarget: %s" % (src, target)
  if os.path.isdir(src):
    for f in os.listdir(src):
      targetFile = os.path.join(target, f)
      srcFile = os.path.join(src, f)
      if os.path.exists(targetFile):
        if not replaceIfExists:
          print "Target dir exists: %s. NOT replacing." % targetFile
          continue
        else:
          print "Target dir exists: %s. Replacing." % targetFile
        
        if os.path.isdir(targetFile):
          shutil.rmtree(targetFile)
        else:
          os.remove(targetFile)
        
      if os.path.isdir(srcFile):
        shutil.copytree(srcFile, targetFile)
      else:
        shutil.copy(srcFile, targetFile)
    
    shutil.rmtree(src)
  
  os.symlink(target, src)




# Envar for reading development binary volume mount point
APROX_DEV_VOL = '/tmp/aprox'
SSH_CONFIG_VOL = '/tmp/ssh-config'
APROX_DEV_BINARIES_PATTERN='aprox*-launcher.tar.gz'


# Envars that can be set using -e from 'docker run' command.
APROX_VERSION_ENVAR='APROX_VERSION'
APROX_FLAVOR_ENVAR='APROX_FLAVOR'
APROX_URL_ENVAR = 'APROX_BINARY_URL'
APROX_ETC_URL_ENVAR = 'APROX_ETC_URL'
APROX_OPTS_ENVAR = 'APROX_OPTS'
APROX_DEVMODE_ENVAR = 'APROX_DEV'


# Defaults
DEF_APROX_VERSION='0.18.4'
DEF_APROX_FLAVOR='savant'

DEF_APROX_BINARY_URL_FORMAT = 'http://repo.maven.apache.org/maven2/org/commonjava/aprox/launch/aprox-launcher-{flavor}/{version}/aprox-launcher-{flavor}-{version}-launcher.tar.gz'


# locations for expanded aprox binary
APROX_DIR = '/opt/aprox'
BOOT_PROPS = 'boot.properties'
APROX_BIN = os.path.join(APROX_DIR, 'bin')
APROX_ETC = os.path.join(APROX_DIR, 'etc/aprox')
APROX_STORAGE = os.path.join(APROX_DIR, 'var/lib/aprox/storage')
APROX_DATA = os.path.join(APROX_DIR, 'var/lib/aprox/data')
APROX_LOGS = os.path.join(APROX_DIR, 'var/log/aprox')


# locations on global fs
ETC_APROX = '/etc/aprox'
VAR_APROX = '/var/lib/aprox'
VAR_STORAGE = os.path.join(VAR_APROX, 'storage')
VAR_DATA = os.path.join(VAR_APROX, 'data')
LOGS = '/var/log/aprox'


# if true, attempt to use aprox distro tarball or directory from attached volume
devmode = os.environ.get(APROX_DEVMODE_ENVAR)

# aprox release to use
version=os.environ.get(APROX_VERSION_ENVAR) or DEF_APROX_VERSION

# currently one of: rest-min, easyprox, savant
flavor=os.environ.get(APROX_FLAVOR_ENVAR) or DEF_APROX_FLAVOR

# URL for aprox distro tarball to download and expand
aproxBinaryUrl = os.environ.get(APROX_URL_ENVAR) or DEF_APROX_BINARY_URL_FORMAT.format(version=version, flavor=flavor)

# Git location supplying /opt/aprox/etc/aprox
aproxEtcUrl = os.environ.get(APROX_ETC_URL_ENVAR)

# command-line options for aprox
opts = os.environ.get(APROX_OPTS_ENVAR) or ''

print "Read environment:\n  devmode: %s\n  aprox version: %s\n  aprox flavor: %s\n  aprox binary URL: %s\n  aprox etc Git URL: %s\n  aprox cli opts: %s" % (devmode, version, flavor, aproxBinaryUrl, aproxEtcUrl, opts)

if not os.path.isdir('/root/.ssh') and os.path.isdir(SSH_CONFIG_VOL):
  print "Importing SSH configurations from volume: %s" % SSH_CONFIG_VOL
  run("cp -rf %s /root/.ssh" % SSH_CONFIG_VOL)
  run("chmod 700 /root/.ssh")
  run("chmod 600 /root/.ssh/*")


if os.path.isdir(APROX_DIR) is False:
  parentDir = os.path.dirname(APROX_DIR)
  if os.path.isdir(parentDir) is False:
    os.mkdir(parentDir)
  
  if devmode is not None:
    unpacked=False
    for file in os.listdir(APROX_DEV_VOL):
      if fnmatch.fnmatch(file, APROX_DEV_BINARIES_PATTERN):
        devTarball = os.path.join(APROX_DEV_VOL, file)
        print "Unpacking development binary of AProx: %s" % devTarball
        run('tar -zxvf %s -C /opt' % devTarball)
        unpacked=True
        break
    if unpacked is False:
      if not os.path.exists(os.path.join(APROX_DEV_VOL, 'bin/aprox.sh')):
        print "Development volume %s exists but doesn't appear to contain expanded AProx (can't find 'bin/aprox.sh'). Ignoring." % APROX_DEV_VOL
      else:
        print "Using expanded AProx deployment, in development volume: %s" % APROX_DEV_VOL
        shutil.copytree(APROX_DEV_VOL, APROX_DIR)
  else:
      print 'Downloading: %s' % aproxBinaryUrl
      
      download = urlopen(aproxBinaryUrl)
      with open('/tmp/aprox.tar.gz', 'wb') as f:
        f.write(download.read())
      run('ls -alh /tmp/')
      run('tar -zxvf /tmp/aprox.tar.gz -C /opt')
    #  with tarfile.open('/tmp/aprox.tar.gz') as tar:
    #    tar.extractall(parentDir)
    
  if aproxEtcUrl is not None:
    print "Cloning: %s" % aproxEtcUrl
    shutil.rmtree(APROX_ETC)
    run("git clone %s %s" % (aproxEtcUrl, APROX_ETC), "Failed to checkout aprox/etc from: %s" % aproxEtcUrl)
  
  move_and_link(APROX_ETC, ETC_APROX, replaceIfExists=True)
  move_and_link(APROX_STORAGE, VAR_STORAGE)
  move_and_link(APROX_DATA, VAR_DATA)
  move_and_link(APROX_LOGS, LOGS)
  
  etcBootOpts = os.path.join(APROX_ETC, BOOT_PROPS)
  if os.path.exists(etcBootOpts):
    binBootOpts = os.path.join(APROX_BIN, BOOT_PROPS)
    if os.path.exists(binBootOpts):
      os.remove(binBootOpts)
    os.symlink(binBootOpts, etcBootOpts)
else:
  if os.path.isdir(os.path.join(APROX_ETC, ".git")) is True:
    print "Updating aprox etc/ git repository..."
    runIn("git pull", APROX_ETC, "Failed to pull updates to etc git repository.")


run("%s -p 8081 %s" % (os.path.join(APROX_DIR, 'bin', 'aprox.sh'), opts), fail=False)
