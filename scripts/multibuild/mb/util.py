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
import sys
from datetime import datetime as dt

def run_cmd(cmd, fail=True):
    """Run the specified command. If fail == True, and a non-zero exit value 
       is returned from the process, raise an exception
    """
    print cmd
    ret = os.system(cmd)
    if ret != 0:
        print "Error running command: %s (return value: %s)" % (cmd, ret)
        if fail:
            raise Exception("Failed to run: '%s' (return value: %s)" % (cmd, ret))


def setup_builddir(builds_dir, projectdir, branch, tid_base, idx):
    if os.path.isdir(builds_dir) is False:
        os.makedirs(builds_dir)

    builddir="%s/%s-%s-%s" % (builds_dir, tid_base, dt.now().strftime("%Y%m%dT%H%M%S"), idx)

    run_cmd("git clone -l -b %s file://%s %s" % (branch, projectdir, builddir))
    
    return os.path.join(os.getcwd(), builddir)
