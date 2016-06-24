/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.metadata.table;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.crate.analyze.WhereClause;
import io.crate.analyze.expressions.ExpressionAnalysisContext;
import io.crate.analyze.expressions.ExpressionAnalyzer;
import io.crate.analyze.expressions.TableReferenceResolver;
import io.crate.analyze.symbol.Reference;
import io.crate.metadata.*;
import io.crate.metadata.doc.DocSysColumns;
import io.crate.metadata.doc.DocTableInfo;
import io.crate.sql.parser.SqlParser;
import io.crate.sql.tree.Expression;
import io.crate.types.DataType;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.settings.Settings;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class TestingTableInfo extends DocTableInfo {

    private Routing routing;

    public TestingTableInfo(TableIdent ident,
                            List<ReferenceInfo> columns,
                            List<ReferenceInfo> partitionedByColumns,
                            List<GeneratedReferenceInfo> generatedColumns,
                            ImmutableMap<ColumnIdent, IndexReferenceInfo> indexColumns,
                            ImmutableMap<ColumnIdent, ReferenceInfo> references,
                            List<ColumnIdent> primaryKeys,
                            ColumnIdent clusteredBy,
                            boolean isAlias,
                            boolean hasAutoGeneratedPrimaryKey,
                            String[] concreteIndices,
                            int numberOfShards,
                            BytesRef numberOfReplicas,
                            ImmutableMap<String, Object> tableParameters,
                            List<ColumnIdent> partitionedBy,
                            List<PartitionName> partitions,
                            ColumnPolicy columnPolicy, Routing routing) {
        super(ident, columns, partitionedByColumns, generatedColumns, indexColumns, references,
            ImmutableMap.<ColumnIdent, String>of(), primaryKeys, clusteredBy, isAlias,
            hasAutoGeneratedPrimaryKey, concreteIndices, null, new IndexNameExpressionResolver(Settings.EMPTY),
            numberOfShards, numberOfReplicas, tableParameters, partitionedBy, partitions, columnPolicy,
            Operation.ALL, null);
        this.routing = routing;
    }

    @Override
    public Routing getRouting(WhereClause whereClause, @Nullable String preference) {
        return routing;
    }

    public static Builder builder(TableIdent ident, Routing routing) {
        return new Builder(ident, routing);
    }

    public static class Builder {

        private final ImmutableList.Builder<ReferenceInfo> columns = ImmutableList.builder();
        private final ImmutableMap.Builder<ColumnIdent, ReferenceInfo> references = ImmutableMap.builder();
        private final ImmutableList.Builder<ReferenceInfo> partitionedByColumns = ImmutableList.builder();
        private final ImmutableList.Builder<GeneratedReferenceInfo> generatedColumns = ImmutableList.builder();
        private final ImmutableList.Builder<ColumnIdent> primaryKey = ImmutableList.builder();
        private final ImmutableList.Builder<ColumnIdent> notNullColumns = ImmutableList.builder();
        private final ImmutableList.Builder<ColumnIdent> partitionedBy = ImmutableList.builder();
        private final ImmutableList.Builder<PartitionName> partitions = ImmutableList.builder();
        private final ImmutableMap.Builder<ColumnIdent, IndexReferenceInfo> indexColumns = ImmutableMap.builder();
        private ColumnIdent clusteredBy;

        private final int numberOfShards = 1;
        private final BytesRef numberOfReplicas = new BytesRef("0");

        private final TableIdent ident;
        private final Routing routing;
        private boolean isAlias = false;
        private ColumnPolicy columnPolicy = ColumnPolicy.DYNAMIC;

        public Builder(TableIdent ident, Routing routing) {
            this.routing = routing;
            this.ident = ident;
        }

        public DocTableInfo build() {
            return build(mock(Functions.class));
        }

        public DocTableInfo build(Functions functions) {
            addDocSysColumns();
            ImmutableList<ColumnIdent> pk = primaryKey.build();
            ImmutableList<PartitionName> partitionsList = partitions.build();
            String[] concreteIndices;
            if (partitionsList.isEmpty()) {
                concreteIndices = new String[]{ident.indexName()};
            } else {
                concreteIndices = Lists.transform(partitionsList, new Function<PartitionName, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable PartitionName input) {
                        assert input != null;
                        return input.asIndexName();
                    }
                }).toArray(new String[partitionsList.size()]);
            }

            initializeGeneratedExpressions(functions, references.build().values());

            return new TestingTableInfo(
                    ident,
                    columns.build(),
                    partitionedByColumns.build(),
                    generatedColumns.build(),
                    indexColumns.build(),
                    references.build(),
                    pk,
                    clusteredBy,
                    isAlias,
                    pk.isEmpty(),
                    concreteIndices,
                    numberOfShards,
                    numberOfReplicas,
                    null, // tableParameters
                    partitionedBy.build(),
                    partitionsList,
                    columnPolicy,
                    routing
            );
        }

        private ReferenceInfo genInfo(ColumnIdent columnIdent, DataType type) {
            return new ReferenceInfo(
                    new ReferenceIdent(ident, columnIdent.name(), columnIdent.path()),
                    RowGranularity.DOC, type
            );
        }

        private void addDocSysColumns() {
            for (Map.Entry<ColumnIdent, DataType> entry : DocSysColumns.COLUMN_IDENTS.entrySet()) {
                references.put(
                        entry.getKey(),
                        genInfo(entry.getKey(), entry.getValue())
                );
            }
        }

        public Builder add(String column, DataType type) {
            return add(column, type, null);
        }
        public Builder add(String column, DataType type, List<String> path) {
            return add(column, type, path, ColumnPolicy.DYNAMIC);
        }
        public Builder add(String column, DataType type, List<String> path, ColumnPolicy columnPolicy) {
            return add(column, type, path, columnPolicy, ReferenceInfo.IndexType.NOT_ANALYZED, false, true);
        }
        public Builder add(String column, DataType type, List<String> path, ReferenceInfo.IndexType indexType) {
            return add(column, type, path, ColumnPolicy.DYNAMIC, indexType, false, true);
        }
        public Builder add(String column, DataType type, List<String> path,
                           boolean partitionBy) {
            return add(column, type, path, ColumnPolicy.DYNAMIC,
                    ReferenceInfo.IndexType.NOT_ANALYZED, partitionBy, true);
        }

        public Builder add(String column, DataType type, List<String> path,
                           boolean partitionBy, boolean nullable) {
            return add(column, type, path, ColumnPolicy.DYNAMIC,
                ReferenceInfo.IndexType.NOT_ANALYZED, partitionBy, nullable);
        }

        public Builder add(String column, DataType type, List<String> path,
                           ColumnPolicy columnPolicy, ReferenceInfo.IndexType indexType,
                           boolean partitionBy,
                           boolean nullable) {
            RowGranularity rowGranularity = RowGranularity.DOC;
            if (partitionBy) {
                rowGranularity = RowGranularity.PARTITION;
            }
            ReferenceInfo info = new ReferenceInfo(new ReferenceIdent(ident, column, path),
                    rowGranularity, type, columnPolicy, indexType, nullable);
            if (info.ident().isColumn()) {
                columns.add(info);
            }
            references.put(info.ident().columnIdent(), info);
            if (partitionBy) {
                partitionedByColumns.add(info);
                partitionedBy.add(info.ident().columnIdent());
            }
            return this;
        }

        public Builder addGeneratedColumn(String column, DataType type, String expression, boolean partitionBy) {
            return addGeneratedColumn(column, type, expression, partitionBy, true);
        }

        public Builder addGeneratedColumn(String column, DataType type, String expression,
                                          boolean partitionBy, boolean nullable) {
            RowGranularity rowGranularity = RowGranularity.DOC;
            if (partitionBy) {
                rowGranularity = RowGranularity.PARTITION;
            }
            GeneratedReferenceInfo info = new GeneratedReferenceInfo(new ReferenceIdent(ident, column),
                rowGranularity, type, ColumnPolicy.DYNAMIC, ReferenceInfo.IndexType.NOT_ANALYZED, expression, nullable);

            generatedColumns.add(info);
            if (info.ident().isColumn()) {
                columns.add(info);
            }
            references.put(info.ident().columnIdent(), info);
            if (partitionBy) {
                partitionedByColumns.add(info);
                partitionedBy.add(info.ident().columnIdent());
            }
            return this;
        }

        public Builder addIndex(ColumnIdent columnIdent, ReferenceInfo.IndexType indexType) {
            IndexReferenceInfo info = new IndexReferenceInfo(
                    new ReferenceIdent(ident, columnIdent),
                    indexType,
                    Collections.<ReferenceInfo>emptyList(),
                    null);
            indexColumns.put(columnIdent, info);
            return this;
        }

        public Builder addPrimaryKey(String column) {
            primaryKey.add(ColumnIdent.fromPath(column));
            return this;
        }

        public Builder clusteredBy(String clusteredBy) {
            this.clusteredBy = ColumnIdent.fromPath(clusteredBy);
            return this;
        }

        public Builder isAlias(boolean isAlias) {
            this.isAlias = isAlias;
            return this;
        }

        public Builder addPartitions(String... partitionNames) {
            for (String partitionName : partitionNames) {
                PartitionName partition = PartitionName.fromIndexOrTemplate(partitionName);
                partitions.add(partition);
            }
            return this;
        }

        private void initializeGeneratedExpressions(Functions functions, Collection<ReferenceInfo> columns) {
            TableReferenceResolver tableReferenceResolver = new TableReferenceResolver(columns);
            ExpressionAnalyzer expressionAnalyzer = new ExpressionAnalyzer(
                    functions, null, null, tableReferenceResolver, null);
            for (GeneratedReferenceInfo generatedReferenceInfo : generatedColumns.build()) {
                Expression expression = SqlParser.createExpression(generatedReferenceInfo.formattedGeneratedExpression());
                ExpressionAnalysisContext context = new ExpressionAnalysisContext();
                generatedReferenceInfo.generatedExpression(expressionAnalyzer.convert(expression, context));
                generatedReferenceInfo.referencedReferenceInfos(ImmutableList.copyOf(Lists.transform(tableReferenceResolver.references(), new Function<Reference, ReferenceInfo>() {
                    @Nullable
                    @Override
                    public ReferenceInfo apply(@Nullable Reference input) {
                        if (input == null) {
                            return null;
                        }
                        return input.info();
                    }
                })));
                tableReferenceResolver.references().clear();
            }
        }

    }
}
