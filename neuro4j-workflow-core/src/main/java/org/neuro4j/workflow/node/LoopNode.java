/**
 * Copyright (c) 2013-2016, Neuro4j
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.neuro4j.workflow.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.neuro4j.workflow.FlowContext;
import org.neuro4j.workflow.WorkflowRequest;
import org.neuro4j.workflow.common.FlowExecutionException;
import org.neuro4j.workflow.loader.f4j.SWFConstants;

public class LoopNode extends WorkflowNode {

    private static final String NEXT_EXIT_RELATION = SWFConstants.NEXT_RELATION_NAME;
    private static final String DO_EXIT_RELATION = "LOOP_EXIT";

    private String iteratorKey = null;
    private String elementKey = null;

    private Transition doExit = null;
    private Transition loopExit = null;

    public LoopNode(String name, String uuid)
    {
        super(name, uuid);
    }

    public void setIteratorKey(String iteratorKey) {
        this.iteratorKey = iteratorKey;
    }

    public void setElementKey(String elementKey) {
        this.elementKey = elementKey;
    }

    @Override
    public final Transition execute(final WorkflowProcessor processor, final WorkflowRequest request)
            throws FlowExecutionException {
        Object object = null;
        FlowContext fctx = request.getLogicContext();
        Iterator<?> iteratorObject = request.getLoopIterator(this.iteratorKey);
        if (iteratorObject == null) {
            object = fctx.get(iteratorKey);
            if (object == null) {
                cleanOnExit(fctx, request);
                return request.getNextWorkflowNode();
            }

            if ((object instanceof Iterable)) {
                iteratorObject = ((Iterable<?>) object).iterator();
            } else if ((object instanceof Collection)) {
                iteratorObject = ((Collection<?>) object).iterator();
            } else if (object.getClass().isArray()) {
                iteratorObject = Arrays.asList((Object[]) object).iterator();
            } else {
                cleanOnExit(fctx, request);
                return request.getNextWorkflowNode();
            }

            request.putLoopIterator(this.iteratorKey, iteratorObject);
        }

        if (iteratorObject.hasNext()) {
            Object value = iteratorObject.next();
            fctx.put(this.elementKey, value);
            request.setNextRelation(doExit);
        } else {
            cleanOnExit(fctx, request);
        }
        return request.getNextWorkflowNode();
    }


    private void cleanOnExit(FlowContext fctx, WorkflowRequest request) throws FlowExecutionException
    {
        request.removeLoopIterator(this.iteratorKey);
        request.setNextRelation(loopExit);

    }

    @Override
    public final void init() throws FlowExecutionException {

        doExit = getExitByName(DO_EXIT_RELATION);

        loopExit = getExitByName(NEXT_EXIT_RELATION);

        return;
    }

    @Override
    public final void validate(final WorkflowProcessor processor, final FlowContext ctx) throws FlowExecutionException {
        if (elementKey == null || iteratorKey == null || doExit == null || loopExit == null)
        {
            throw new FlowExecutionException("LoopBlock uuid: " + getUuid() +": Wrong configuration:  elementKey: " + elementKey + "  iteratorKey: " + iteratorKey + "  doExit: " + doExit + "  loopExit: " + loopExit);
        }

    }

}
