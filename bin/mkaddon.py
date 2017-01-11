#!/usr/bin/python

import argparse
import os
import shutil
import re

BASE=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ADDONS_DIR='addons'
TEMPLATE_DIR='addons/template'

DEP_INSERTION='DO NOT REMOVE: append::depMgmt'
ADDON_INSERTION='DO NOT REMOVE: append::addon'

MODULE_INCLUSION = "<module>indy-%(addon)s-%(module)s</module>"
DEP="""
      <dependency>
        <groupId>org.commonjava.indy</groupId>
        <artifactId>indy-%(short_name)s-%(module)s</artifactId>
        <version>%(version)s</version>%(extra)s
      </dependency>
"""

DEFAULT_MODULES=['common', 'client-api', 'model-java', 'jaxrs', 'ftests']

parser = argparse.ArgumentParser()
parser.add_argument("short_name", help="name of the new add-on, used to name the directory and artifactIds")
parser.add_argument("long_name", help="name used in the <name/> element of the Maven POM")
parser.add_argument("modules", metavar='module', nargs='*', help="list of modules to add to the new add-on. Default: %s" % DEFAULT_MODULES)
args=parser.parse_args()

addonPath=os.path.join(BASE, ADDONS_DIR, args.short_name)

print "Creating new add-on '%s' in %s" % (args.long_name, addonPath)

if os.path.isdir(addonPath):
	print "%s: add-on already exists!" % args.short_name
	exit(1)

templatePath=os.path.join(BASE, TEMPLATE_DIR)

os.makedirs(addonPath)
shutil.copy(os.path.join(templatePath, 'pom.xml'), addonPath)

for module in args.modules:
	print "Creating module %s" % module

	moduleSrc=os.path.join(templatePath, module)
	if not os.path.isdir(moduleSrc):
		print "Invalid module: %s. Skipping." % module
		continue

	moduleDest=os.path.join(addonPath, module)
	shutil.copytree(moduleSrc, moduleDest)

	modulePom = os.path.join(moduleDest, 'pom.xml')
	with open(modulePom) as f:
		pomTemplate= f.read()

	print "Setting up module POM"
	with open(modulePom, 'w') as f:
		f.write(pomTemplate % {'long_name': args.long_name, 'short_name': args.short_name, 'module': module})

addonPom = os.path.join(addonPath, 'pom.xml')
with open(addonPom) as f:
	pomTemplate= f.read()

modulesSection="\n    ".join([MODULE_INCLUSION % {'addon': args.short_name, 'module': m} for m in args.modules])

print "Writing add-on POM"
with open(addonPom, 'w') as f:
	f.write(pomTemplate % {'long_name': args.long_name, 'short_name': args.short_name, 'modules': modulesSection})

rootPom = os.path.join(BASE, 'pom.xml')
with open(rootPom) as f:
	rootPomLines = f.readlines()

for line in rootPomLines:
	match = re.match(r"  <version>([^<]+)</version>.*", line)
	if match:
		version = match.group(1)
		break

deps = [DEP % {'short_name': args.short_name, 'module': m, 'version': version, 'extra': ("\n      <scope>test</scope>" if m == 'ftests' else "")} for m in args.modules]
if 'common' in args.modules:
	deps.append(DEP % {'short_name': args.short_name, 'module': 'common', 'version': version, 'extra': "\n        ".join(["", "<classifier>confset</classifier>", "<type>tar.gz</type>"])})

print "Adding module dependencies to dependencyManagement in root POM"
with open(rootPom,'w') as f:
	for line in rootPomLines:
		if DEP_INSERTION in line:
			f.write("\n".join(deps))
			f.write("\n\n")
		f.write(line)


allAddonsPom = os.path.join(BASE, ADDONS_DIR, 'pom.xml')
with open(allAddonsPom) as f:
	allAddonsLines= f.readlines()

print "Adding new add-on to add-ons parent POM"
with open(allAddonsPom, 'w') as f:
	for line in allAddonsLines:
		if ADDON_INSERTION in line:
			f.write("    <module>%s</module>\n" % args.short_name)
		f.write(line)







