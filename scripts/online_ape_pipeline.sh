#!/bin/bash

MOSES_DIR="/hltsrv1/software/moses/moses-20150228_kenlm_cmph_xmlrpc_irstlm_master"
EXTERNAL_BIN_DIR="/hltsrv1/software/mgiza/bin"
MERT_OPTS="--sctype TER --scconfig weights:1.0"
MERT_DIR="$MOSES_DIR/bin/"

IRSTLM="/home/chatterjee/Public/irstlm-5.80.08/bin"

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

align_file=""

if [[ -s $align_file ]]
then
    ##### CREATING LOCAL LANGUAGE MODEL #####
    if [ $LM -eq 1 ]
    then
        tgt="$DATA_DIR/train.$TGT_EXT"
        $IRSTLM/add-start-end.sh < "$tgt" > "$tgt.add-sym"
        tgt="$tgt.add-sym"
        
        mkdir "$WORK_DIR/lm"
        $IRSTLM/tlm -tr="$tgt" -n=3 -lm=wb -bo=yes -o="$WORK_DIR/lm/de.lm"
        $IRSTLM/compile-lm "$WORK_DIR/lm/de.lm" "$WORK_DIR/lm/de.blm"
        rm "$WORK_DIR/lm/de.lm"
    fi
    
    ##### TRAINING #####
    if [ $TRAIN -eq 1 ]
    then
        src="$DATA_DIR/train.$SRC_EXT"
        tgt="$DATA_DIR/train.$TGT_EXT"
        align_file="$DATA_DIR/align.tgt.pe"
    
        mkdir "$WORK_DIR/train"
        mkdir "$WORK_DIR/train/model"
    
        TRAIN_MODEL_OPTS="-alignment grow-diag-final-and -reordering msd-bidirectional-fe -alignment-file $align_file -first-step 4 -last-step 8 -cores 2 -mgiza -mgiza-cpus 2 -parallel"
        $MOSES_DIR/scripts/training/train-model.perl -external-bin-dir $EXTERNAL_BIN_DIR -root-dir "$WORK_DIR/train" -corpus "$DATA_DIR/train" -e "$TGT_EXT" -f "$SRC_EXT" $TRAIN_MODEL_OPTS > $WORK_DIR/train.out 2> $WORK_DIR/train.err
    
        # create self rule phrase table
        backoff_pt=""$WORK_DIR/train"/model/phrase-table-backoff.gz"
        dev_src="$DATA_DIR/dev_top1.$SRC_EXT"
        test_src="$DATA_DIR/test.$SRC_EXT"
    
        if [ $TUNE -eq 1 ]
        then
            python create_self_rule_pt.py "$dev_src" "$test_src" | LC_ALL=C sort | gzip > "$backoff_pt"
        else
            python create_self_rule_pt.py "$test_src" | LC_ALL=C sort | gzip > "$backoff_pt"
        fi
    fi
    
    ##### TUNNING #####
    if [ $TUNE -eq 1 ]
    then
        src="$DATA_DIR/dev.$SRC_EXT"
        tgt="$DATA_DIR/dev.$TGT_EXT"
        moses_ini="$WORK_DIR/moses.ini"
    
        mkdir "$WORK_DIR/tune"
    
        # tuning with MERT
        $MOSES_DIR/scripts/training/mert-moses.pl "$src" "$tgt" "$MOSES_DIR/bin/moses" "$moses_ini" --mertargs="$MERT_OPTS" --working-dir "$WORK_DIR/tune" --mertdir "$MERT_DIR" > "$WORK_DIR/tune/tune.out" 2> "$WORK_DIR/tune/tune.err" 
        # tuning with MIRA
        # $MOSES_DIR/scripts/training/mert-moses.pl "$src" "$tgt" "$MOSES_DIR/bin/moses" "$moses_ini" --working-dir "$WORK_DIR/tune" --mertdir "$MERT_DIR" --batch-mira --return-best-dev > "$WORK_DIR/tune/tune.out" 2> "$WORK_DIR/tune/tune.err"
    fi
    
    ##### TESTING #####
    if [ $TEST -eq 1 ]
    then
        src="$DATA_DIR/test.$SRC_EXT"
        moses_ini="$WORK_DIR/moses.ini"
    
        mkdir "$WORK_DIR/test"
    
        if [ $TUNE -eq 1 ]
        then
            moses_ini="$WORK_DIR/tune/moses.ini"
        fi
        "$MOSES_DIR/bin/moses" -config "$moses_ini" -input-file "$src" > "$WORK_DIR/test/test.ape" 2> "$WORK_DIR/test/test.err"
    fi
else
    src="$DATA_DIR/test.$SRC_EXT"
    mkdir "$WORK_DIR/test"
    cat $src | sed 's/###[^#| ][^#| ]*//g' > "$WORK_DIR/test/test.ape"
fi


