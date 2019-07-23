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

import os
import requests
import time
import mb.util

def find_vagrant_dir(vagrant_dir):
    if vagrant_dir is not None:
        vagrant_dir = os.path.abspath(vagrant_dir)
    else:
        vagrant_dir = os.path.abspath(os.getcwd())
        while vagrant_dir is not None:
            if os.path.isdir(os.path.join(vagrant_dir, '.vagrant')):
                break
            vagrant_dir = os.path.dirname(vagrant_dir)

    if vagrant_dir is None:
        print "Cannot find vagrant root directory (containing .vagrant subdirectory). Quitting."
        exit(3)

    return vagrant_dir

def init_ssh_config(vagrant_dir):
    old_dir = os.getcwd()
    try:
        os.chdir(vagrant_dir)
        mb.util.run_cmd("vagrant ssh-config > .vagrant/ssh-config", fail=True)
    finally:
        os.chdir(old_dir)

def wait_for_indy(indy_url):
    max_tries=30
    ready = False
    for t in range(max_tries):
        print "Attempting to contact Indy..."
        try:
            resp = requests.head(indy_url, timeout=1)
            if resp.status_code == 200:
                ready = True
                break
            else:
                print "...Indy isn't ready yet."
        except (requests.exceptions.Timeout, requests.exceptions.ConnectionError):
            print "...Indy isn't ready yet."
            # nop

        print "Sleeping 10s before next attempt"
        time.sleep(10)

    if not ready:
        raise Exception("Indy isn't responding on: %s. Aborting test run." % indy_url)

def run_vagrant_commands(section, indy_url):
    cmd_sections = section.get('run')
    if cmd_sections is not None:
        for cmd_section in cmd_sections:
            host = cmd_section['host']
            for cmd in cmd_section['commands']:
                mb.util.run_cmd("vagrant ssh -c '%s' %s" % (cmd, host), fail=True)
            if cmd_section.get('wait-for-indy') is True:
                wait_for_indy(indy_url)

def run_vagrant_copy_ops(section, project_dir, output_dir):
    copy_ops = section.get('copy')
    if copy_ops is not None:
        for src in copy_ops:
            dest = copy_ops[src]
            mb.util.run_cmd(("scp -q -r -F .vagrant/ssh-config %s %s" % (src, dest)).format(project_dir=project_dir, output_dir=output_dir), fail=True)

def vagrant_env(build_config, env, indy_url, vagrant_dir, project_dir, output_dir):
    vagrant = build_config.get('vagrant')
    if vagrant is not None:
        cwd = os.getcwd()
        try:
            if vagrant_dir is not None:
                print "Switching to vagrant directory: %s" % vagrant_dir
                os.chdir(vagrant_dir)

            section = vagrant.get(env)
            if section is not None:
                run_vagrant_copy_ops(section, project_dir, output_dir)
                run_vagrant_commands(section, indy_url)
        finally:
            os.chdir(cwd)

