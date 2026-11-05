#!/usr/bin/env bash
# Local equivalent of Gradle's single forked test JVM (maxHeap 512MiB). JDK21 needs -Xss4m for one regex only.
set -uo pipefail
ROOT=${1:-/home/user/starcore-public-build}; OUT=${2:-/tmp/claude-0/unit1}; S=/tmp/claude-0/sdk
CL=$ROOT/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
rm -rf $OUT; mkdir -p $OUT/classes
find $ROOT/app/src/test/java -name '*.java' > $OUT/src.txt
javac -nowarn -encoding UTF-8 --release 17 -proc:none -cp $CL:$S/android-mockable.jar:$S/junit.jar:$S/hamcrest.jar -d $OUT/classes @$OUT/src.txt > $OUT/javac.log 2>&1 || { grep error: $OUT/javac.log | head; exit 2; }
TESTS=$(cd $OUT/classes && find . -name '*Test.class' ! -name '*$*' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort)
cd $ROOT/app && java -Xmx512m -Xss4m -cp $OUT/classes:$ROOT/app/src/test/resources:$CL:$S/junit.jar:$S/hamcrest.jar:$S/android-mockable.jar org.junit.runner.JUnitCore $TESTS > $OUT/all.log 2>&1
grep -E "^OK \(|^Tests run:" $OUT/all.log | tail -1
grep -E "^[0-9]+\) " $OUT/all.log | head -40
