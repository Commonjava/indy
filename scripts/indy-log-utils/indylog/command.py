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

import click
import os
import yaml
import indylog.timer as it

@click.command()
@click.argument('timers-yml', type=click.Path(exists=True))
@click.option('--logdir', '-d', type=click.Path(exists=True), help='Specify the directory containing the logs to analyze (default: current dir)', default=os.getcwd())
@click.option('--filename-prefix', '-F', help='Specify the filename prefix for logs to analyze (default: indy)', default='indy')
@click.option('--output-file', '-O', help='Specify the output file (default: output.log)', type=click.Path(), default='output.yml')
def timer(timers_yml, filename_prefix, logdir, output_file):
    with open(timers_yml) as f:
        timer_config = yaml.safe_load(f)

    output = it.findTimings(timer_config, logdir, filename_prefix)
    with open(output_file, 'w') as f:
        yaml.dump(output, f, default_flow_style=False)

@click.command()
def timerConfigSample():
    sample = it.timerConfigSample()
    print yaml.dump(sample, default_flow_style=False)


