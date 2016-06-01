#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

SRC_EXT="mtsrc"
TGT_EXT="pe"
DATA_DIR=""
WORK_DIR=""
moses_ini=""

while [[ $# > 1 ]]
do
    case "$1" in
        --data_dir)
            DATA_DIR="$2"
            shift 2
            ;;
        --work_dir)
            WORK_DIR="$2"
            shift 2
            ;;
       --moses_ini)
            moses_ini="$2"
            shift 2
            ;;
        *)
            echo "Command line error"
            exit 1
            ;;
    esac
done

src="$DATA_DIR/test.$SRC_EXT"
test_dir="$WORK_DIR/test"
mkdir $test_dir

$MOSES_DIR/bin/moses -config "$moses_ini" -input-file "$src" > "$test_dir/test.ape" 2> "$test_dir/test.err"

