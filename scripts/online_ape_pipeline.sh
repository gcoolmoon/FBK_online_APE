#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

SRC_EXT="mtsrc"
TGT_EXT="pe"

TRAIN="1"
TUNE="1"
TEST="1"
LM="1"
DATA_DIR=""
WORK_DIR=""

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
        *)
            echo "Command line error"
            exit 1
            ;;
    esac
done

align_file="$DATA_DIR/align.mt.pe"
seeds="$WORK_DIR/seeds.out"

if [[ -s "$align_file" ]]
then
    if [ $LM -eq 1 ]
    then
        $script_dir/create_lm.sh --data_dir $DATA_DIR --work_dir $WORK_DIR
    fi
    
    if [ $TRAIN -eq 1 ]
    then
        parent_dir="$(dirname "$WORK_DIR")"
        parent_dir="$(dirname "$parent_dir")"
        global_lm="$parent_dir/global/lm/global.blm"
        local_lm="$WORK_DIR/lm/local.blm"

        $script_dir/train.sh --data_dir $DATA_DIR --work_dir $WORK_DIR --global_lm $global_lm --local_lm $local_lm
        python $script_dir/create_self_rule_pt.py "$DATA_DIR/test.$SRC_EXT" | LC_ALL=C sort | gzip > "$WORK_DIR/train/model/phrase-table-backoff.gz"
        $script_dir/create_moses_ini.sh --work_dir $WORK_DIR --global_lm $global_lm --local_lm $local_lm --feature_weights $WORK_DIR/features.out

        if [[ -s "$seeds" ]]
        then
            cat "$seeds" | parallel --gnu --jobs 5 "mkdir $WORK_DIR/{}"
            cat "$seeds" | parallel --gnu --jobs 5 "$script_dir/train.sh --data_dir $DATA_DIR/{} --work_dir $WORK_DIR/{} --global_lm $global_lm --local_lm $local_lm"
            cat "$seeds" | parallel --gnu --jobs 5 "python $script_dir/create_self_rule_pt.py $DATA_DIR/{}/dev.$SRC_EXT | LC_ALL=C sort | gzip > $WORK_DIR/{}/train/model/phrase-table-backoff.gz"
            cat "$seeds" | parallel --gnu --jobs 5 "$script_dir/create_moses_ini.sh --work_dir $WORK_DIR/{} --global_lm $global_lm --local_lm $local_lm --feature_weights $WORK_DIR/features.out"
        fi
    fi
    
    if [ $TUNE -eq 1 ]
    then
        if [[ -s "$seeds" ]]
        then
            tune_cmd="$MOSES_DIR/scripts/training/mert-moses.pl"

            cat "$seeds" | parallel --gnu --jobs 5 "mkdir $WORK_DIR/{}/tune"

            cat "$seeds" | parallel --gnu --jobs 5 "$script_dir/tune.sh --data_dir $DATA_DIR/{} --work_dir $WORK_DIR/{}"

            # compute average weights
            params=""
            while read line
            do
                params="$params $WORK_DIR/$line/tune/moses.ini"
            done < $seeds

            mkdir "$WORK_DIR/tune"
            python $script_dir/average_weights.py $params > "$WORK_DIR/tune/moses.ini"
        fi  
    fi
    
    ##### TESTING #####
    if [ $TEST -eq 1 ]
    then
        src="$DATA_DIR/test.$SRC_EXT"
        moses_ini="$WORK_DIR/train/model/moses.ini"
    
        if [ $TUNE -eq 1 ]
        then
            if [[ -s "$WORK_DIR/tune/moses.ini" ]]
            then
                moses_ini="$WORK_DIR/tune/moses.ini"
            fi
        fi
        $script_dir/test.sh --data_dir $DATA_DIR --work_dir $WORK_DIR --moses_ini $moses_ini
    fi
fi


