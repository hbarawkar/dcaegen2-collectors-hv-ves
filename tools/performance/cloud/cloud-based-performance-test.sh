#!/usr/bin/env bash
# ============LICENSE_START=======================================================
# dcaegen2-collectors-veshv
# ================================================================================
# Copyright (C) 2019 NOKIA
# ================================================================================
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
# ============LICENSE_END=========================================================

SCRIPT_DIRECTORY="$(pwd "$0")"
CONTAINERS_COUNT=1
PROPERTIES_FILE=${SCRIPT_DIRECTORY}/test.properties
CONFIG_MAP_NAME=performance-test-config
PRODUCER_APPS_LABEL=hv-collector-producer
CONSUMER_APPS_LABEL=hv-collector-kafka-consumer
PROMETHEUS_CONF_LABEL=prometheus-server-conf
PROMETHEUS_APPS_LABEL=hv-collector-prometheus
ONAP_NAMESPACE=onap
MAXIMUM_BACK_OFF_CHECK_ITERATIONS=30
CHECK_NUMBER=0
NAME_REASON_PATTERN="custom-columns=NAME:.metadata.name,REASON:.status.containerStatuses[].state.waiting.reason"

function clean() {
    echo "Cleaning up environment"

    echo "Attempting to delete test parameters ConfigMap"
    kubectl delete configmap ${CONFIG_MAP_NAME} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete prometheus ConfigMap"
    kubectl delete configmap -l name=${PROMETHEUS_CONF_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete prometheus deployment and service"
    kubectl delete service,deployments -l app=${PROMETHEUS_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete consumer deployments"
    kubectl delete deployments -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Attempting to delete producer pods"
    kubectl delete pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE}

    echo "Environment clean up finished!"
}

function create_producers() {
    set -e
    for i in $(seq 1 ${CONTAINERS_COUNT});
    do
        echo "Creating ${i}/${CONTAINERS_COUNT} producer"
        kubectl create -f producer-pod.yaml -n ${ONAP_NAMESPACE}
    done
    echo "Producers created"
    set +e
}

function usage() {
    echo ""
    echo "Run cloud based HV-VES performance test"
    echo "Usage $0 setup|start|clean|help"
    echo "  setup    : set up ConfigMap and consumers"
    echo "  start    : create producers - start the performance test"
    echo "    Optional parameters:"
    echo "      --containers      : number of producer containers to create (1)"
    echo "      --properties-file : path to file with benchmark properties (./test.properties)"
    echo "  clean    : remove ConfigMap, HV-VES consumers and producers"
    echo "  help     : print usage"
    echo "Example invocations:"
    echo "./cloud-based-performance-test.sh setup"
    echo "./cloud-based-performance-test.sh start"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh start --containers 10"
    echo "./cloud-based-performance-test.sh clean"
    exit 1
}

function setup_environment() {
    echo "Setting up environment"
    echo "Creating test properties ConfigMap from: $PROPERTIES_FILE"
    kubectl create configmap ${CONFIG_MAP_NAME} --from-env-file=${PROPERTIES_FILE} -n ${ONAP_NAMESPACE}

    echo "Creating consumer deployment"
    kubectl apply -f consumer-deployment.yaml

    echo "Creating ConfigMap for prometheus deployment"
    kubectl apply -f prometheus-config-map.yaml

    echo "Creating prometheus deployment"
    kubectl apply -f prometheus-deployment.yaml

    echo "Waiting for consumers to be running."
    while [[ $(kubectl get pods -l app=${CONSUMER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "unhealthy\|starting") -ne 0 ]] ; do
        sleep 1
    done
    echo "Setting up environment finished!"
}

function start_performance_test() {
    echo "Starting cloud based performance tests"
    echo "________________________________________"
    echo "Test configuration:"
    echo "Producer containers count: ${CONTAINERS_COUNT}"
    echo "Properties file path: ${PROPERTIES_FILE}"
    echo "________________________________________"

    create_producers

    echo "Waiting for producers completion"
    while :; do
        COMPLETED_PRODUCERS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} | grep -c "Completed")
        IMAGE_PULL_BACK_OFFS=$(kubectl get pods -l app=${PRODUCER_APPS_LABEL} -n ${ONAP_NAMESPACE} -o ${NAME_REASON_PATTERN} | grep -c "ImagePullBackOff \| ErrImagePull")

        if [[ ${IMAGE_PULL_BACK_OFFS} -gt 0 ]]; then
            CHECK_NUMBER=$((CHECK_NUMBER + 1))
            if [[ ${CHECK_NUMBER} -gt ${MAXIMUM_BACK_OFF_CHECK_ITERATIONS} ]]; then
                echo "Error: Image pull problem"
                exit 1
            fi
        fi
        
        [[ ${COMPLETED_PRODUCERS} -eq ${CONTAINERS_COUNT} || ${CHECK_NUMBER} -gt ${MAXIMUM_BACK_OFF_CHECK_ITERATIONS} ]] && break
        sleep 1
    done

    echo "Performance test finished"
    exit 0
}

cd ${SCRIPT_DIRECTORY}

if [[ $# -eq 0 ]]; then
    usage
else
    for arg in ${@}
    do
        case ${arg} in
            setup)
            setup_environment
            ;;
            start)
            shift 1
            while [[ $(($#)) -gt 0 ]]; do
                case "${1}" in
                    --containers)
                        CONTAINERS_COUNT=${2}
                        ;;
                    --properties-file)
                        PROPERTIES_FILE=${2}
                        ;;
                    *)
                        echo "Unknown option: ${1}"
                        usage
                        ;;
                esac
                shift 2
            done
            start_performance_test
            ;;
            clean)
            clean
            ;;
            help)
            usage
            ;;
            *)
            echo "Unknown action: ${arg}" >&2
            usage
            ;;
        esac
    done
fi