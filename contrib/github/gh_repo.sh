#!/bin/bash
#
# Simple script to manage Github forked repositories
#
# Notes on usage targets:
#
# clone   : a regular git clone of the upstream repo
# sync    : syncs a forked repo with upstream repo
# build   : executes a maven build
# deploy  : deploys the built war to the deployment dir
# scratch : removes the local maven repo and executes targets 
#           sync, build and deploy 
# info    : provides repo information for trouble-shooting
#
# Notes on workflow:
#
# Copy this script into your desired workspace and execute from there to 
# retrive the required repos.  The "scratch" target should be executed when 
# running the script for the first time.  It should be sufficient to run the 
# "sync", "build" and "deploy" targets afterwards.  The "clone" and "info"
# targets are useful for verfication and trouble-shooting.
#
# Warning: 
#
# Running a "scratch" build will delete your local maven repo along with any 
# other nominated repos defined in the $REPO varible so use with care!!!
 
# Environment varibles
REPOS="atlas galley cartographer aprox"
UPSTREAM=`git config -l | grep upstream`
PWD=`pwd`
APROX_WAR=${APROX_WAR:=$PWD/aprox/wars/savant/target/aprox.war}
DEPLOY_DIR=${DEPLOY_DIR:=/var/lib/jboss-as/standalone/deployments/}
WORKSPACE=${WORKSPACE:=$HOME/workspace}
USER=$USER # Define GitHub username here.
MAINTAINER=${MAINTAINER:=jdcasey}

# Debugging (comment out if not required)
set -x

# Usage targets
if [ $# -lt 1 ]
then
        echo "Usage : $0 {clone|sync|build|deploy|scratch|info}"
        exit
fi

case "$1" in

clone) echo "Cloning repos"
       for repo in $REPOS
           do git clone https://github.com/$MAINTAINER/$repo.git
       done
   ;;
sync)  echo "Syncing repos"

function repo_sync {
    cd $repo
    TAG=`git describe --abbrev=0 --tags`
    git fetch upstream
    git checkout master
    git merge upstream/master
    git push -f origin master --tags
    cd -
    echo
} 	

# Update repos
for repo in $REPOS
    do
	if [ -d $repo ]; then
	    echo -e "Updating $repo"
	    cd $repo
	    git config credential.helper store
	    git pull
	    cd -
	    echo
	else
	    echo -e "Cloning $repo"
	    git clone https://github.com/$USER/$repo.git
	fi 
done

# Sync with upstream
    echo -e "Syncronising $repo"
	UPSTREAM=`git config -l | grep upstream`
	if [ -z "$UPSTREAM" ]; then
	    echo "Initialising Upstream"
	    git remote add upstream https://github.com/$MAINTAINER/$repo.git
	    repo_sync
	else
	    repo_sync
	fi
    ;;
build)  echo  "Building sources"
	# Build sources
	for repo in $REPOS
	    do
		echo -e "Building $repo"
		cd $repo
		#mvn clean install 
		mvn -Dmaven.test.skip=true clean install
		cd -
	done

    ;;
deploy)	 if [ -f "$APROX_WAR $DEPLOY_DIR/aprox.war" ]; then
              echo "Deploying war"
	      sudo systemctl stop jboss-as.service
	      sudo rm -rf $DEPLOY_DIR/aprox*
              sudo cp -p $APROX_WAR $DEPLOY_DIR
              sudo systemctl start jboss-as.service
         else
                    echo "War not found...See build log"
         fi

    ;;
scratch) echo  "Deploying from scratch"
        set -x
	for repo in $REPOS
	    do rm -rf $WORKSPACE/$repo
	done

	rm -rf ~/.m2
	$0 sync
        $0 build
	$0 deploy

   ;;
info) echo "Retrieving repo info"
      for repo in $REPOS
          do
              echo -e "Repo info for $repo"
              cd $WORKSPACE/$repo
              git remote -v
              git branch
              git tag
              git rev-parse HEAD
              cd -
        done
  ;; 
*) echo "Usage : $0 {clone|sync|build|deploy|scratch|info}"
   ;;
esac

