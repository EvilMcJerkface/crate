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

package io.crate.planner.node;

import io.crate.Streamer;
import io.crate.planner.node.dql.CollectNode;
import io.crate.planner.node.dql.MergeNode;
import io.crate.planner.node.dql.join.NestedLoopNode;
import io.crate.types.DataTypes;

/**
 * get input and output {@link io.crate.Streamer}s for {@link io.crate.planner.node.PlanNode}s
 */
public class StreamerVisitor {

    private static final ExecutionNodeStreamerVisitor EXECUTION_NODE_STREAMER_VISITOR = new ExecutionNodeStreamerVisitor();

    private StreamerVisitor() {}

    public static Streamer<?>[] streamerFromOutputs(ExecutionNode executionNode) {
        return EXECUTION_NODE_STREAMER_VISITOR.process(executionNode, null);
    }

    private static class ExecutionNodeStreamerVisitor extends ExecutionNodeVisitor<Void, Streamer<?>[]> {

        @Override
        public Streamer<?>[] visitMergeNode(MergeNode node, Void context) {
            return DataTypes.getStreamer(node.outputTypes());
        }

        @Override
        public Streamer<?>[] visitCollectNode(CollectNode node, Void context) {
            return DataTypes.getStreamer(node.outputTypes());
        }

        @Override
        public Streamer<?>[] visitNestedLoopNode(NestedLoopNode node, Void context) {
            return DataTypes.getStreamer(node.outputTypes());
        }

        @Override
        protected Streamer<?>[] visitExecutionNode(ExecutionNode node, Void context) {
            throw new UnsupportedOperationException(String.format("Got unsupported ExecutionNode %s", node.getClass().getName()));
        }
    }
}

