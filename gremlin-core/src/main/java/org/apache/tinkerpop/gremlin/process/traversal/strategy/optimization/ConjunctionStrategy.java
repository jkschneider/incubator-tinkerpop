/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization;

import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.AndStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.ConjunctionStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.OrStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.StartStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.EmptyStep;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class ConjunctionStrategy extends AbstractTraversalStrategy {

    private static final ConjunctionStrategy INSTANCE = new ConjunctionStrategy();

    private ConjunctionStrategy() {
    }

    @Override
    public void apply(final Traversal.Admin<?, ?> traversal) {
        processConjunctionMarker(AndStep.AndMarker.class, traversal);
        processConjunctionMarker(OrStep.OrMarker.class, traversal);
    }

    private static final boolean legalCurrentStep(final Step<?, ?> step) {
        return !(step instanceof EmptyStep || step instanceof OrStep.OrMarker || step instanceof AndStep.AndMarker || step instanceof StartStep);
    }

    private static final void processConjunctionMarker(final Class<? extends ConjunctionStep.ConjunctionMarker> markerClass, final Traversal.Admin<?, ?> traversal) {
        TraversalHelper.getStepsOfClass(markerClass, traversal).forEach(markerStep -> {
            Step<?, ?> currentStep = markerStep.getNextStep();
            final Traversal.Admin<?, ?> rightTraversal = __.start().asAdmin();
            while (legalCurrentStep(currentStep)) {
                final Step<?, ?> nextStep = currentStep.getNextStep();
                rightTraversal.addStep(currentStep);
                traversal.removeStep(currentStep);
                currentStep = nextStep;
            }

            currentStep = markerStep.getPreviousStep();
            final Traversal.Admin<?, ?> leftTraversal = __.start().asAdmin();
            while (legalCurrentStep(currentStep)) {
                final Step<?, ?> previousStep = currentStep.getPreviousStep();
                leftTraversal.addStep(0, currentStep);
                traversal.removeStep(currentStep);
                currentStep = previousStep;
            }
            TraversalHelper.replaceStep(markerStep,
                    markerClass.equals(AndStep.AndMarker.class) ?
                            new AndStep<Object>(traversal, (Traversal.Admin) leftTraversal, (Traversal.Admin) rightTraversal) :
                            new OrStep<Object>(traversal, (Traversal.Admin) leftTraversal, (Traversal.Admin) rightTraversal),
                    traversal);
        });
    }

    public static ConjunctionStrategy instance() {
        return INSTANCE;
    }
}
