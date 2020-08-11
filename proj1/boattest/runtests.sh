set -e

###### functions ######

function test-boats {
    # modify, compile, run, log, and test.
    # call with two arguments: ADULTS then CHILDREN
    # make sure CHILDREN is at least two
    echo "Testing Boats with $2 children, $1 adult(s)"
    sed -e s/{A}/$1/ -e s/{C}/$2/ resources/modboattemplate.vim > resources/modboat.vim
    vim -e -s < resources/modboat.vim resources/Boat.java
    javac -classpath .. resources/Boat.java
    mv resources/*.class ../nachos/threads/.

    cd ..
    ../bin/nachos > boattest/simlogs/$1-$2.log
    cd boattest

    python boattest.py simlogs/$1-$2.log > testlogs/$1-$2.log
    head -n 1 testlogs/$1-$2.log

    echo
}

###### main code ######

cp ../../threads/ThreadedKernel.java resources/.
echo "Modifying and compiling ThreadedKernel.java..."
vim -e -s < resources/modkernel.vim resources/ThreadedKernel.java
set +e
if ! javac -classpath .. resources/ThreadedKernel.java 2> /dev/null ;
then
    echo Compilation failed. Did you run \"make\"?
    exit 1
fi
set -e

mv resources/*.class ../nachos/threads/.
echo

mkdir -p testlogs
mkdir -p simlogs

cp ../../threads/Boat.java resources/.

test-boats 2 2
test-boats 10 2
test-boats 2 10
test-boats 0 10
test-boats 5 7
test-boats 7 5
test-boats 19 19
test-boats 63 14
test-boats 14 63
test-boats 2 100
test-boats 100 2
test-boats 100 100

echo "All tests done. More details are in the testlogs directory."
