#
# Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import indylog.counter as ic

@click.command()
@click.option('--filename-prefix', '-F', help='Specify the filename prefix for logs to analyze (default: indy)', default='indy')
@click.option('--logdir', '-d', type=click.Path(exists=True), help='Specify the directory containing the logs to analyze (default: current dir)', default=os.getcwd())
@click.option('--output-file', '-O', help='Specify the output file (default: output.yml)', type=click.Path(), default='output.yml')
@click.option('--level-group', '-L', help='Specify the regular expression group number containing the log level (default: 1)', type=click.INT, default=1)
@click.option('--class-group', '-C', help='Specify the regular expression group number containing the class name (default: 3)', type=click.INT, default=3)
@click.option('--logline-expr', '-E', help='Specify the expression identifying a log line (default: [^\[]+(\[[^\]]+\])\s+(\[[^\]]+\])\s+(\S+).+)', default='[^\[]+(\[[^\]]+\])\s+(\[[^\]]+\])\s+(\S+).+')
def counter(filename_prefix, logdir, output_file, level_group, class_group, logline_expr):
    output = ic.countLogs(logdir, filename_prefix, logline_expr, level_group, class_group)
    with open(output_file, 'w') as f:
        yaml.dump(output, f, default_flow_style=False)

@click.command()
@click.argument('timers-yml', type=click.Path(exists=True))
@click.option('--logdir', '-d', type=click.Path(exists=True), help='Specify the directory containing the logs to analyze (default: current dir)', default=os.getcwd())
@click.option('--filename-prefix', '-F', help='Specify the filename prefix for logs to analyze (default: indy)', default='indy')
@click.option('--output-file', '-O', help='Specify the output file (default: output.yml)', type=click.Path(), default='output.yml')
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


