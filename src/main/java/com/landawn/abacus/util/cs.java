/*
 * Copyright (c) 2024, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

// cs -> constant String; ps -> parameter String; fs -> field string. s -> string?
@Beta // Not sure if it's a good idea or not.
@Internal
@SuppressWarnings("java:S1845")
public final class cs { // NOSONAR
    public static final String a = "a";

    public static final String accumulator = "accumulator";
    public static final String action = "action";

    public static final String actionOnError = "actionOnError";
    public static final String actionOnFailure = "actionOnFailure";
    public static final String actionOnSuccess = "actionOnSuccess";
    public static final String aggregateOnColumnNames = "aggregateOnColumnNames";
    public static final String appendable = "appendable";
    public static final String arrayType = "arrayType";
    public static final String arrays = "arrays";

    public static final String atLeast = "atLeast";
    public static final String atMost = "atMost";

    public static final String b = "b";
    // public static final String batchAction = "batchAction";
    public static final String batchSize = "batchSize";
    public static final String bean = "bean";
    public static final String beanClassForColumnType = "beanClassForColumnType";
    public static final String BiConsumer = "BiConsumer";
    // public static final String Function = "Function";
    public static final String BiFunction = "BiFunction";
    public static final String BiPredicate = "BiPredicate";

    public static final String bufferSize = "bufferSize";

    public static final String c = "c";
    public static final String calendar = "calendar";
    public static final String calendar1 = "calendar1";
    public static final String calendar2 = "calendar2";
    public static final String CalendarField = "CalendarField";
    public static final String chunkSize = "chunkSize";

    public static final String cls = "cls";
    public static final String cmd = "cmd";
    public static final String cmp = "cmp";
    public static final String codeConfig = "codeConfig";
    public static final String coll = "coll";
    public static final String collectionSupplier = "collectionSupplier";
    public static final String collector = "collector";

    public static final String columnName = "columnName";
    public static final String columnNames = "columnNames";

    public static final String conditionToBreak = "conditionToBreak";
    public static final String consumer = "consumer";
    public static final String Consumer = "Consumer";
    public static final String consumerForNewStreamWithTerminalAction = "consumerForNewStreamWithTerminalAction";
    public static final String converter = "converter";
    public static final String coreThreadPoolSize = "coreThreadPoolSize";
    public static final String count = "count";
    public static final String creator = "creator";
    public static final String csvHeaders = "csvHeaders";
    public static final String curl = "curl";
    public static final String date = "date";
    public static final String date1 = "date1";
    public static final String date2 = "date2";
    public static final String decimalFormat = "decimalFormat";
    public static final String delay = "delay";
    public static final String delimiter = "delimiter";
    public static final String delimiterRegex = "delimiterRegex";
    public static final String deque = "deque";
    public static final String directory = "directory";
    public static final String downstream = "downstream";
    public static final String downstream1 = "downstream1";
    public static final String downstream2 = "downstream2";
    public static final String downstream3 = "downstream3";
    public static final String downstream4 = "downstream4";
    public static final String downstream5 = "downstream5";
    public static final String downstream6 = "downstream6";
    public static final String downstream7 = "downstream7";
    public static final String duration = "duration";
    public static final String element = "element";
    public static final String emptyAction = "emptyAction";
    // public static final String elementConsumer = "elementConsumer";
    public static final String endExclusive = "endExclusive";
    public static final String entry = "entry";
    public static final String enumClass = "enumClass";
    public static final String equalsFunction = "equalsFunction";
    public static final String exceptionClass = "exceptionClass";
    public static final String exceptionSupplier = "exceptionSupplier";
    public static final String exceptionSupplierIfErrorOccurred = "exceptionSupplierIfErrorOccurred";
    public static final String executor = "executor";
    public static final String expectedSize = "expectedSize";
    public static final String f = "f";
    public static final String file = "file";
    public static final String filter = "filter";
    public static final String finalAction = "finalAction";
    public static final String finisher = "finisher";
    public static final String flatMapper = "flatMapper";
    public static final String flatMapper2 = "flatMapper2";
    public static final String fromIndex = "fromIndex";
    public static final String fromStringFunc = "fromStringFunc";
    public static final String func = "func";
    public static final String function = "function";
    public static final String generator = "generator";
    public static final String handler = "handler";
    public static final String hashFunction = "hashFunction";
    public static final String hasMore = "hasMore";
    public static final String hasNext = "hasNext";
    public static final String httpHeaderFilterForHARRequest = "httpHeaderFilterForHARRequest";
    // public static final String idPropNames = "idPropNames";
    public static final String increment = "increment";
    public static final String index = "index";
    public static final String indexFunc = "indexFunc";
    public static final String initialCapacity = "initialCapacity";
    public static final String iter = "iter";
    public static final String iterator = "iterator";
    public static final String iteratorSupplier = "iteratorSupplier";
    public static final String joiner = "joiner";
    public static final String k = "k";
    public static final String keepAliveTime = "keepAliveTime";
    public static final String keyColumnNames = "keyColumnNames";
    public static final String keyMapper = "keyMapper";
    // public static final String keyExtractor = "keyExtractor";
    public static final String keyType = "keyType";
    public static final String leftKeyMapper = "leftKeyMapper";
    public static final String len = "len";
    public static final String length = "length";
    public static final String limit = "limit";
    public static final String lineIndex = "lineIndex";
    public static final String list = "list";
    public static final String map = "map";
    public static final String mapFactory = "mapFactory";
    public static final String mapInstanceType = "mapInstanceType";
    public static final String mapper = "mapper";
    // public static final String mapSupplier = "mapSupplier";
    public static final String max = "max";
    public static final String maxChunkCount = "maxChunkCount";
    public static final String maxDuration = "maxDuration";
    public static final String maxLen = "maxLen";
    public static final String maxSize = "maxSize";
    public static final String maxThreadNum = "maxThreadNum";
    public static final String maxThreadPoolSize = "maxThreadPoolSize";
    public static final String maxWaitForAddingElementToQuery = "maxWaitForAddingElementToQuery";
    public static final String maxWaitForNextInMillis = "maxWaitForNextInMillis";
    public static final String maxWaitIntervalInMillis = "maxWaitIntervalInMillis";
    public static final String maxWidth = "maxWidth";
    public static final String maxWindowSize = "maxWindowSize";
    public static final String mergeFunction = "mergeFunction";
    public static final String mergeOp = "mergeOp";
    public static final String merger = "merger";
    public static final String minLength = "minLength";
    public static final String minSize = "minSize";
    public static final String mode = "mode";
    public static final String mutex = "mutex";
    public static final String n = "n";
    public static final String name = "name";
    public static final String name1 = "name1";
    public static final String name2 = "name2";
    public static final String name3 = "name3";
    public static final String next = "next";
    public static final String numberToAdvance = "numberToAdvance";
    public static final String numOfParts = "numOfParts";
    public static final String offset = "offset";
    public static final String onComplete = "onComplete";
    public static final String other = "other";
    public static final String otherIfErrorOccurred = "otherIfErrorOccurred";
    public static final String output = "output";
    public static final String outputWriter = "outputWriter";
    public static final String pageSize = "pageSize";
    public static final String pair = "pair";
    public static final String parser = "parser";
    public static final String path = "path";
    public static final String position = "position";
    public static final String predicate = "predicate";
    public static final String Predicate = "Predicate";
    public static final String prefix = "prefix";
    public static final String prefixSuffix = "prefixSuffix";
    // public static final String processThreadNum = "processThreadNum";
    public static final String propName = "propName";
    public static final String ps = "ps";
    public static final String queue = "queue";
    public static final String queueSize = "queueSize";
    public static final String queueToBuffer = "queueToBuffer";
    public static final String rateLimiter = "rateLimiter";
    public static final String ReadableByteChannel = "ReadableByteChannel";
    public static final String reader = "reader";
    public static final String reference = "reference";
    public static final String remappingFunction = "remappingFunction";
    public static final String resultClass = "resultClass";
    public static final String resultHandler = "resultHandler";
    public static final String resultType = "resultType";
    public static final String retryIntervallInMillis = "retryIntervallInMillis";
    public static final String retryTimes = "retryTimes";
    public static final String rightKeyMapper = "rightKeyMapper";
    public static final String rnd = "rnd";
    public static final String rounds = "rounds";
    public static final String rowElementName = "rowElementName";
    public static final String rowMapper = "rowMapper";
    public static final String rowType = "rowType";
    public static final String runtimeExceptionMapper = "runtimeExceptionMapper";
    public static final String scale = "scale";
    public static final String serializer = "serializer";
    public static final String size = "size";
    public static final String source = "source";
    public static final String srcClass = "srcClass";
    public static final String startInclusive = "startInclusive";
    public static final String startTime = "startTime";
    public static final String step = "step";
    public static final String subColl = "subColl";
    public static final String suffix = "suffix";
    public static final String supplier = "supplier";
    public static final String Supplier = "Supplier";
    public static final String supplierForDefaultValue = "supplierForDefaultValue";
    public static final String targetBean = "targetBean";
    public static final String targetClass = "targetClass";
    public static final String targetResource = "targetResource";

    public static final String targetResourceSupplier = "targetResourceSupplier";
    public static final String targetType = "targetType";
    public static final String terminalAction = "terminalAction";
    public static final String timeInMillis = "timeInMillis";
    public static final String toStringFunc = "toStringFunc";
    public static final String toStringFunction = "toStringFunction";
    public static final String totalRetryTimes = "totalRetryTimes";
    public static final String totalSize = "totalSize";
    public static final String totalTimeoutForAll = "totalTimeoutForAll";
    public static final String transfer = "transfer";
    public static final String triple = "triple";
    public static final String TriPredicate = "TriPredicate";
    public static final String type = "type";
    public static final String typeName = "typeName";
    public static final String unit = "unit";
    public static final String url = "url";
    public static final String valueEquivalence = "valueEquivalence";
    public static final String valueMapper = "valueMapper";
    // public static final String valueExtractor = "valueExtractor";
    public static final String valuesToFind = "valuesToFind";
    public static final String valueSupplier = "valueSupplier";
    public static final String valueType = "valueType";
    public static final String where = "where";
    public static final String windowSplitter = "windowSplitter";
    public static final String WritableByteChannel = "WritableByteChannel";

    private cs() {
        // Utility class for constant string values.
    }
}
