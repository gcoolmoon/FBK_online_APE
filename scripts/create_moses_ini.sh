#!/bin/bash

script_dir=$(dirname $0)

source $script_dir/conf_env

global_lm=""
local_lm=""
feature_weights=""
moses_ini=""
model_path=""

while [[ $# > 1 ]]
do
    case "$1" in
        --global_lm)
            global_lm="$2"
            shift 2
            ;;
        --local_lm)
            local_lm="$2"
            shift 2
            ;;
        --feature_weights)
            feature_weights="$2"
            shift 2
            ;;
        --moses_ini)
            moses_ini="$2"
            shift 2
            ;;
        --model_path)
            model_path="$2"
            shift 2
            ;;
        *)
            echo "Command line error"
            exit 1
            ;;
    esac
done

echo "[input-factors]" > $moses_ini
echo "0" >> $moses_ini
echo "" >> $moses_ini
echo "[mapping]" >> $moses_ini
echo "0 T 0" >> $moses_ini
echo "1 T 1" >> $moses_ini
echo "" >> $moses_ini
echo "[distortion-limit]" >> $moses_ini
echo "6" >> $moses_ini
echo "" >> $moses_ini
echo "[feature]" >> $moses_ini
echo "UnknownWordPenalty" >> $moses_ini
echo "WordPenalty" >> $moses_ini
echo "PhrasePenalty" >> $moses_ini
echo "PhraseDictionaryMemory name=TranslationModel0 num-features=4 path=$model_path/phrase-table.gz input-factor=0 output-factor=0 table-limit=20" >> $moses_ini
echo "PhraseDictionaryMemory name=TranslationModel1 num-features=4 path=$model_path/phrase-table-backoff.gz input-factor=0 output-factor=0 table-limit=20" >> $moses_ini
echo "LexicalReordering name=LexicalReordering0 num-features=6 type=wbe-msd-bidirectional-fe-allff input-factor=0 output-factor=0 path=$model_path/reordering-table.wbe-msd-bidirectional-fe.gz" >> $moses_ini
echo "Distortion" >> $moses_ini
echo "IRSTLM name=LM0 factor=0 path=$global_lm order=3" >> $moses_ini
echo "IRSTLM name=LM1 factor=0 path=$local_lm order=3" >> $moses_ini
echo "" >> $moses_ini
echo "[decoding-graph-backoff]" >> $moses_ini
echo "0" >> $moses_ini
echo "1" >> $moses_ini
echo "" >> $moses_ini
echo "[weight]" >> $moses_ini
cat $feature_weights >> $moses_ini

