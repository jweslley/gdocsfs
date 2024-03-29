#!/bin/sh
# ----------------------------------------------------------------------------
# Copyright (C) 2008 Jonhnny Weslley.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Locates the JAVA_HOME
# ----------------------------------------------------------------------------

test_jvm() {

    target=$1
    if [ -z "$target" ]; then
        return
    fi
  
    if [ -d "$target" ]; then
        java_exc="$target/bin/java"
    else
        java_exc=$target
    fi
  
    exec_name=`basename $java_exc`
    if  [ ! "$exec_name" = "java" ] || [ ! -f "$java_exc" ] || [ ! -x "$java_exc" ]; then
        return
    fi
  
    version_output=`$java_exc -version 2>&1`
  
    is_gcj=`expr "$version_output" : '.*gcj'`
    if [ "$is_gcj" = "0" ]; then
        java_version=`expr "$version_output" : '.*"\(.*\)".*'`
        ver_major=`expr "$java_version" : '\([0-9][0-9]*\)\..*'`
        ver_minor=`expr "$java_version" : '[0-9][0-9]*\.\([0-9][0-9]*\)\..*'`
        ver_micro=`expr "$java_version" : '[0-9][0-9]*\.[0-9][0-9]*\.\([0-9][0-9]*\).*'`
        ver_patch=`expr "$java_version" : '.*_\(.*\)'`
    fi

    if [ "$ver_patch" = "" ]; then
        ver_patch=0
    fi

    if [ "$ver_major" = "" ]; then
        return;
    fi
    if [ "$ver_major" -lt "1" ]; then
        return;
    elif [ "$ver_major" -eq "1" ]; then
        if [ "$ver_minor" -lt "5" ]; then
            return;
        fi
    fi

    APP_JAVA=$java_exc
}

APP_JAVA=""

test_jvm $JAVA_HOME

common_jvm_locations="/usr/bin/java* /usr/bin/jdk* /usr/bin/jre* /usr/bin/j2*re* /usr/bin/j2sdk* /usr/java* /usr/jdk* /usr/jre* /usr/j2*re* /usr/j2sdk* /usr/java/j2*re* /usr/java/j2sdk* /opt/java* /usr/java/jdk* /usr/java/jre* /usr/lib/java/jre /usr/local/java* /usr/local/jdk* /usr/local/jre* /usr/local/j2*re* /usr/local/j2sdk* /usr/jdk/java* /usr/jdk/jdk* /usr/jdk/jre* /usr/jdk/j2*re* /usr/jdk/j2sdk* /usr/lib/java* /usr/lib/jdk* /usr/lib/jre* /usr/lib/j2*re* /usr/lib/j2sdk*"

for current_location in $common_jvm_locations; do
    if [ -z "$APP_JAVA" ]; then
        test_jvm $current_location
    fi
done

if [ -z "$APP_JAVA" ]; then
    exit 1
fi

javadir=`dirname $APP_JAVA`
JAVA_HOME=`cd $javadir/.. && pwd`
export JAVA_HOME
