import os
import sys
import re
import yaml
import time
import math
from dateutil import tz

UTC=tz.tzutc()
LOCAL=tz.tzlocal()

START='start'
REFS='refs'
END='end'
FROM='from'

START_RE='start_regexp'
END_RE = 'end_regexp'

START_TIME='start_time'
END_TIME='end_time'

START_LINE='start_line'
END_LINE='end_line'

ELAPSED='elapsed'
CONCURRENCY='concurrent_requests'
CONCURRENCY_FROM = 'concurrent_requests_from'

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

def formatTime(seconds):
    return time.strftime(TIMESTAMP_FORMAT[:-3], time.gmtime(seconds))

def findTimings(timer_config, logdir, filename_prefix):
    expression = "%s(\.\d+).log" % filename_prefix
    basefile = "%s.log" % filename_prefix

    end_matchers = []
    end_matchers_from = {}

    for timing in timer_config:
        timing[START_RE] = re.compile(timing[START])
        if timing.get(FROM) is not None:
            timing[FROM] = re.compile(timing[FROM])

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

    fnames = sorted([fname for fname in os.listdir(logdir) if re.match(expression, fname)], reverse=True)
    if os.path.exists(os.path.join(logdir, basefile)):
        fnames.append(basefile)

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

                        fromIP = 'unknown'
                        if timing.get(FROM) is not None:
                            fromMatch = timing[FROM].search(line)
                            if fromMatch is not None:
                                fromIP = fromMatch.group(1)

                        startTime = parseTime(line)
                        if firstStart is None:
                            firstStart = getDatestampFields(line)

                        matcher[START_TIME] = startTime
                        matcher[FROM] = fromIP
                        end_matchers.append(matcher)

                        matchers = end_matchers_from.get(fromIP) or []
                        matchers.append(matcher)
                        end_matchers_from[fromIP] = matchers

                        concurrent = len(end_matchers)
                        concurrent_from = len(matchers)
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

                            entry[START_TIME] = formatTime(entry[START_TIME])
                            entry[END_TIME] = formatTime(endTime)
                            entry[ELAPSED] = formatElapsed(elapsedSeconds)
                            entry[CONCURRENCY] = len(end_matchers)

                            matchers_from = end_matchers_from[entry[FROM]]
                            entry[CONCURRENCY_FROM] = len(matchers_from)

                            entry.pop(START_RE, None)
                            entry.pop(END_RE, None)

                            print "Found END (%d/%d)." % (len(end_matchers), (len(end_matchers) + len(done)))
                            done.append(entry)
                            remove_entry = entry
                            break

                    if remove_entry is not None:
                        end_matchers.remove(remove_entry)

                        matchers_from = end_matchers_from[entry[FROM]]
                        matchers_from.remove(remove_entry)


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

