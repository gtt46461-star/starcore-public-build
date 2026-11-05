#!/usr/bin/env bash
# NON-AUTHORITATIVE local equivalent of :app:testDebugUnitTest (mockable android jar, returnDefaultValues=true).
set -uo pipefail
ROOT=${1:-/home/user/starcore-public-build}; OUT=${2:-/tmp/claude-0/unit}; S=/tmp/claude-0/sdk
CL=$ROOT/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
rm -rf $OUT; mkdir -p $OUT/classes
find $ROOT/app/src/test/java -name '*.java' > $OUT/src.txt
javac -nowarn -encoding UTF-8 --release 17 -proc:none -cp $CL:$S/android-mockable.jar:$S/junit.jar:$S/hamcrest.jar -d $OUT/classes @$OUT/src.txt > $OUT/javac.log 2>&1 || { grep error: $OUT/javac.log | head -30; exit 2; }
CP=$OUT/classes:$ROOT/app/src/test/resources:$CL:$S/junit.jar:$S/hamcrest.jar:$S/android-mockable.jar
TESTS=$(cd $OUT/classes && find . -name '*Test.class' ! -name '*$*' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort)
cd $ROOT/app
pass=0; fail=0; : > $OUT/failed.txt
for t in $TESTS; do
  if timeout 900 java -Xmx3g -Xss8m -cp $CP org.junit.runner.JUnitCore $t > $OUT/$t.log 2>&1; then pass=$((pass+1)); else fail=$((fail+1)); echo $t >> $OUT/failed.txt; fi
done
total=$(grep -hE "^OK \(|^Tests run:" $OUT/*.log | sed -E 's/OK \(([0-9]+) tests?\)/\1 0/; s/Tests run: ([0-9]+),  Failures: ([0-9]+)/\1 \2/' | awk '{t+=$1;f+=$2} END{print "tests="t" failures="f}')
echo "classes_pass=$pass classes_fail=$fail $total"
