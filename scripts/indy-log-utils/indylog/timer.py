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

import os
import sys
import re
import yaml
import time
import math

START='start'
REFS='refs'
END='end'

START_RE='start_regexp'
END_RE = 'end_regexp'

START_TIME='start_time'
END_TIME='end_time'

START_LINE='start_line'
END_LINE='end_line'

ELAPSED='elapsed'

TIMESTAMP_FORMAT='%Y-%m-%d %H:%M:%S.%f'
ELAPSED_FORMAT='%H:%M:%S'

def timerConfigSample():
    sample = []
    sample.append({
        START: '.+ o\.c\.i\.b\.j\.ResourceManagementFilter - START request: (\S+ \S+) .+',
        REFS: '\\1',
        END: '.+ o\.c\.i\.b\.j\.ResourceManagementFilter - END request: %(refs)s .+',
    })

    return sample

def getDatestampFields(line):
    return ' '.join(line.split(' ')[0:2])

def parseTime(line):
    tstamp = getDatestampFields(line)
    return time.mktime(time.strptime(tstamp, TIMESTAMP_FORMAT))

def formatElapsed(seconds):
    return time.strftime(ELAPSED_FORMAT, time.gmtime(seconds))

def findTimings(timer_config, logdir):
    end_matchers = []

    for timing in timer_config:
        timing[START_RE] = re.compile(timing[START])

    done=[]
    lines=0
    errors = 0
    avgTime=None
    sumSqr=0
    avgCount=0
    minTime=None
    maxTime=None
    processed = []
    firstStart = None
    lastEnd = None
    maxConcurrent=0

    fnames = sorted([fname for fname in os.listdir(logdir) if re.match(r'indy(\.\d+).log', fname)], reverse=True)
    if os.path.exists(os.path.join(logdir, 'indy.log')):
        fnames.append('indy.log')

    for fname in fnames:
        processed.append(fname)
        print "Scanning %s (%d current end-matchers in progress. Output contains: %d entries)" % (fname, len(end_matchers), len(done))
        with open(os.path.join(logdir, fname)) as f:
            for line in f:
                line = line.rstrip()
                lines = lines+1
                found = False

                for timing in timer_config:
                    exp = timing[START_RE]
                    m = exp.match(line)
                    if m is not None:
                        raw_refs = exp.sub(timing[REFS], line)
                        end_refs = re.escape(raw_refs)
                        end_exp = timing[END] % {REFS: end_refs}
                        end_re = re.compile(end_exp)

                        matcher = {}
                        matcher[END_RE] = end_re
                        matcher[REFS] = raw_refs

                        startTime = parseTime(line)
                        if firstStart is None:
                            firstStart = getDatestampFields(line)

                        matcher[START_TIME] = startTime
                        end_matchers.append(matcher)

                        concurrent = len(end_matchers)
                        maxConcurrent = concurrent if concurrent > maxConcurrent else maxConcurrent

                        print "Found new START (%d/%d)." % (concurrent, (concurrent + len(done)))
                        # print "END will be:\n'%s'\n\n" % end_exp
                        found = True
                        break

                if found is False:
                    remove_entry=None
                    for idx,entry in enumerate(end_matchers):
                        if entry[END_RE].match(line) is not None:
                            endTime = parseTime(line)
                            lastEnd = getDatestampFields(line)

                            elapsedSeconds=endTime - entry[START_TIME]

                            if elapsedSeconds < 0:
                                print "ERROR: Elapsed time is %d (negative) for: %s" % (elapsedSeconds, line)
                            else:
                                if avgTime is None:
                                    avgTime = elapsedSeconds
                                    minTime = elapsedSeconds
                                    maxTime = elapsedSeconds
                                else:
                                    avgTime = (((avgTime * avgCount) + elapsedSeconds) / (avgCount+1))
                                    minTime = elapsedSeconds if minTime > elapsedSeconds else minTime
                                    maxTime = elapsedSeconds if maxTime < elapsedSeconds else maxTime

                                sumSqr = sumSqr + (elapsedSeconds ** 2 )
                                avgCount = avgCount+1

                            entry[ELAPSED] = formatElapsed(elapsedSeconds)

                            entry.pop(START_TIME, None)
                            entry.pop(START_RE, None)
                            entry.pop(END_RE, None)

                            print "Found END (%d/%d)." % (len(end_matchers), (len(end_matchers) + len(done)))
                            done.append(entry)
                            remove_entry = entry
                            break

                    if remove_entry is not None:
                        end_matchers.remove(remove_entry)

    output={
        '_summary': {
            'processed_logs': processed, 
            'span': {
                'first_start': firstStart,
                'last_end': lastEnd
            },
            'counts': {
                'entries_ended': avgCount,
                'lines_processed': lines,
                'unmatched_starts': len(end_matchers),
                'max_concurrency': maxConcurrent
            },
            'times': {
                'avg': formatElapsed(avgTime), 
                'max': formatElapsed(maxTime),
                'min': formatElapsed(minTime), 
                'std_dev': formatElapsed(math.sqrt((sumSqr/avgCount) - (avgTime**2)))
            }
        },
        'entries': done
    }

    return output

