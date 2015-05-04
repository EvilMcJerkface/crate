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

package io.crate.planner.node.dml;

import io.crate.planner.Plan;
import io.crate.planner.PlanAndPlannedAnalyzedRelation;
import io.crate.planner.PlanVisitor;
import io.crate.planner.node.dql.DQLPlanNode;
import io.crate.planner.projection.Projection;

import java.util.List;

public class BulkDeletePlan extends PlanAndPlannedAnalyzedRelation {

    private List<Plan> childPlans;

    public BulkDeletePlan(List<Plan> childPlans) {
        this.childPlans = childPlans;
    }

    public List<Plan> childPlans() {
        return childPlans;
    }

    @Override
    public <C, R> R accept(PlanVisitor<C, R> visitor, C context) {
        return visitor.visitBulkDeletePlan(this, context);
    }

    @Override
    public void addProjection(Projection projection) {
        throw new UnsupportedOperationException("Cannot add projection to a BulkDeletePlan");
    }

    @Override
    public boolean resultIsDistributed() {
        return false;
    }

    @Override
    public DQLPlanNode resultNode() {
        throw new UnsupportedOperationException("BulkDeletePlan doesn't have a resultNode");
    }
}
