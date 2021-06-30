#Move Repos from One Indy to Another

#1. Get repos
./get-repos.sh <sourceUrl>

E.g, ./get-repos.sh http://indy-stage.psi.redhat.com

The result are 3 json files, writen to ./output dir.

#2. Import repos
Specify target Indy url, which json file (from step #1) to import, and a token if need.

python import-repos.py <targetUrl> <output/xyz.json> <optional:token>

E.g, python import-repos.py http://localhost:8080 output/test.json

#Q & A

##1. what if it fails due to network issue?
Just rerun it. The script will pick up what is left and continue.

##2. How much time does it take to import repos?
Depends on how many repos to import. Normally it imports 1 repo per second.
There are thousands of repos on production. It might take hours to finish. Here is advice.
- Run the import-repos.py in parallel.
- Run it two-time. The first run will import most of them. The second run will pick up the rest. Usually you do the first run before the production outage.

