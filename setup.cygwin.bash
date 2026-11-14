#!/bin/bash

# 2 awful sed commands as environment variables
# used to replace c:/foo with /cygdrive/c/foo and vice versa.
# usage is something like this:
# JAVA_HOME_DOS=`echo $JAVA_HOME | $CYG_TO_DOS`; 
DOS_TO_CYG="sed -e s@^@/cygdrive/@ -e s@:@@";
CYG_TO_DOS="sed -e s@/cygdrive/@@ -e s@/@:/@";

# we need to set variables for the cvs/ant 
# java environment running cygwin
#JAVA_HOME=/cygdrive/c/java/jdk1.3.0_02;
#JAVA_HOME=/cygdrive/c/java/j2sdk1.4.1_01;
#JAVA_HOME=/cygdrive/c/java/j2sdk1.4.2_06;
JAVA_HOME=/cygdrive/c/java/jdk1.5.0_02;
#ANT_HOME=/cygdrive/c/java/jakarta-ant-1.5;
ANT_HOME=/cygdrive/c/java/apache-ant-1.6.5;
JUNIT_HOME=/cygdrive/c/java/junit3.7;
LOG4J_HOME=/cygdrive/c/java/jakarta-log4j-1.2.8;

#echo "ANT_HOME set to ${ANT_HOME}";
#echo "JAVA_HOME set to ${JAVA_HOME}";

CVS_RSH=ssh;
CVSEDITOR=pico;

export ANT_HOME;
export JAVA_HOME;
export LOG4J_HOME;
export JUNIT_HOME;
export CVS_RSH;
export CVSEDITOR;

LOG4J_HOME="`echo $LOG4J_HOME | $CYG_TO_DOS`"

# append to path also, if we haven't done so already.
if ((PATH_SET==0)); then {
    PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:${PATH}
    PATH_SET=1;
    export PATH;
    export PATH_SET;
};
else
    echo "path already set.";
fi

echo "--- setup.cygwin.bash done! ---"
