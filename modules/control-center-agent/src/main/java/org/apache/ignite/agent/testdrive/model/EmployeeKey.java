/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.agent.testdrive.model;

import java.io.Serializable;

/**
 * EmployeeKey definition.
 *
 * Code generated by Apache Ignite Schema Import utility: 08/24/2015.
 */
public class EmployeeKey implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Value for employeeId. */
    private int employeeId;

    /**
     * Empty constructor.
     */
    public EmployeeKey() {
        // No-op.
    }

    /**
     * Full constructor.
     */
    public EmployeeKey(
        int employeeId
    ) {
        this.employeeId = employeeId;
    }

    /**
     * Gets employeeId.
     *
     * @return Value for employeeId.
     */
    public int getEmployeeId() {
        return employeeId;
    }

    /**
     * Sets employeeId.
     *
     * @param employeeId New value for employeeId.
     */
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof EmployeeKey))
            return false;

        EmployeeKey that = (EmployeeKey)o;

        if (employeeId != that.employeeId)
            return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = employeeId;

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "EmployeeKey [employeeId=" + employeeId +
            "]";
    }
}

