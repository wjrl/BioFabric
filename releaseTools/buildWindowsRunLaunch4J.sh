# /bin/bash

#
# This script runs launch4j, which creates a windows executable out of the
# jar file.
#

EXEHOME=$1
RESHOME=$2
LAUNCH4J_HOME=$3
L4J_WORKING=$EXEHOME/bioFabl4jWorking.xml
VERCOMP=$4
CURRYEAR=$5
SC_HOME=$6
KEY_HOME=$7

echo $EXEHOME
echo $L4J_WORKING
cat $L4J_WORKING

java -jar $LAUNCH4J_HOME/launch4j.jar $L4J_WORKING
