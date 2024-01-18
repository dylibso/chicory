#! /bin/bash
# set -euxo pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

rm -rf ${SCRIPT_DIR}/pom.txt ${SCRIPT_DIR}/pom_sorted.txt ${SCRIPT_DIR}/testsuite.txt ${SCRIPT_DIR}/testsuite_sorted.txt

IFS=', ' read -r -a pom <<< "$(yq -p=xml '.project.build.plugins.plugin.configuration.wastToProcess' ${SCRIPT_DIR}/../runtime/pom.xml | tr '\n' ' ')"

for element in "${pom[@]}"
do
    echo "$element" | xargs >> ${SCRIPT_DIR}/pom.txt
done

IFS=', ' read -r -a pom <<< "$(yq -p=xml '.project.build.plugins.plugin.configuration.orderedWastToProcess' ${SCRIPT_DIR}/../runtime/pom.xml | tr '\n' ' ')"

for element in "${pom[@]}"
do
    echo "$element" | xargs >> ${SCRIPT_DIR}/pom.txt
done

{
    cd ${SCRIPT_DIR}/../testsuite
    IFS=$'\n' read -rd '' -a testsuite <<< "$(ls -a *.wast)"
}

for element in "${testsuite[@]}"
do
    echo "$element" | xargs >> ${SCRIPT_DIR}/testsuite.txt
done

sort -n ${SCRIPT_DIR}/pom.txt > ${SCRIPT_DIR}/pom_sorted.txt
sort -n ${SCRIPT_DIR}/testsuite.txt > ${SCRIPT_DIR}/testsuite_sorted.txt

diff --color ${SCRIPT_DIR}/pom_sorted.txt ${SCRIPT_DIR}/testsuite_sorted.txt

rm -rf ${SCRIPT_DIR}/pom.txt ${SCRIPT_DIR}/pom_sorted.txt ${SCRIPT_DIR}/testsuite.txt ${SCRIPT_DIR}/testsuite_sorted.txt
