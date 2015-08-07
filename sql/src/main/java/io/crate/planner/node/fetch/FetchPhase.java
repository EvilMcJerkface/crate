/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.planner.node.fetch;

import io.crate.planner.node.ExecutionPhaseBase;
import io.crate.planner.node.ExecutionPhaseVisitor;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class FetchPhase extends ExecutionPhaseBase {

    public static final ExecutionPhaseFactory<FetchPhase> FACTORY = new ExecutionPhaseFactory<FetchPhase>() {
        @Override
        public FetchPhase create() {
            return new FetchPhase();
        }
    };

    private FetchPhase(){};

    public FetchPhase(UUID jobId, int executionPhaseId) {
        this.jobId = jobId;
        this.executionPhaseId = executionPhaseId;
    }

    @Override
    public Type type() {
        return Type.FETCH;
    }

    @Override
    public String name() {
        return "fetch";
    }

    @Override
    public Set<String> executionNodes() {
        return null;
    }

    @Override
    public <C, R> R accept(ExecutionPhaseVisitor<C, R> visitor, C context) {
        return visitor.visitFetchPhase(this, context);
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
