import os
import sys
import re

def countLogs(logdir, filename_prefix, logline_expr, level_group, class_group):
    expression = "%s(\.\d+).log" % filename_prefix
    basefile = "%s.log" % filename_prefix

    fnames = sorted([fname for fname in os.listdir(logdir) if re.match(expression, fname)], reverse=True)
    if os.path.exists(os.path.join(logdir, basefile)):
        fnames.append(basefile)

    totalLines = 0
    counts = {}

    line_re=re.compile(logline_expr)
    last_counter=None
    last_level=None
    for fname in fnames:
        print "Scanning %s" % fname
        with open(os.path.join(logdir, fname)) as f:
            for line in f:
                line = line.rstrip()
                matcher = line_re.match(line)
                if matcher is None:
                    if last_counter is not None and last_level is not None:
                        last_level['lines'] = last_level['lines']+1
                        last_counter['total'] = last_counter['total']+1
                    else:
                        continue
                else:
                    cname = matcher.group(class_group)
                    level = matcher.group(level_group)[1:-1].rstrip()
                    # print "Class: %s, Level: %s" % (cname, level)

                    totalLines = totalLines + 1

                    cinfo = counts.get(cname)
                    if cinfo is None:
                        cinfo = {'class': cname}
                        counts[cname] = cinfo

                    last_counter = cinfo

                    cinfo['total'] = (cinfo.get('total') or 0) + 1

                    clevel = cinfo.get(level)
                    if clevel is None:
                        clevel = {}
                        cinfo[level] = clevel
                    last_level = clevel

                    clevel['starts'] = (clevel.get('starts') or 0) + 1
                    clevel['lines'] = (clevel.get('lines') or 0) + 1
                    counts[cname] = cinfo

    sorted_counts = []
    sort_result = sorted(counts.items(), reverse=True, key=lambda (k,v): v['total'])
    for item in sort_result:
        sorted_counts.append(item[1])

    return sorted_counts


