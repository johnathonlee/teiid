/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package com.metamatrix.common.transaction.manager;

import com.metamatrix.common.transaction.TransactionException;
import com.metamatrix.common.transaction.TransactionStatus;
import com.metamatrix.common.transaction.UserTransaction;

public class SimpleUserTransaction implements UserTransaction {
    private int status = TransactionStatus.STATUS_NO_TRANSACTION;
    private Object source = null;

    public SimpleUserTransaction() {
    }

    public SimpleUserTransaction(Object source) {
        this.source = source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    /**
     * Obtain the status of the transaction represented by this object.
     * @return The transaction status.
     */
    public int getStatus() throws TransactionException {
        return this.status;
    }

    /**
     * Create a new transaction and associate it with this object.
     * @throws TransactionNotSupportedException if the current thread is already
     * associated with a transaction and the manager does not support
     * nested system transactions.
     */
    public void begin() throws TransactionException {
        this.status = TransactionStatus.STATUS_ACTIVE;
    }

    /**
     * Modify the value of the timeout value that is associated with the
     * transactions represented by this object.
     * If an application has not called this method, the transaction service
     * uses some default value for the transaction timeout.
     * @param seconds The value of the timeout in seconds. If the value is
     * zero, the transaction service restores the default value.
     * @throws IllegalStateException Thrown if this object is not associated with a transaction
     */
    public void setTransactionTimeout(int seconds) throws TransactionException {
        // do nothing
    }

    /**
     * Modify the transaction associated with this object such that
     * the only possible outcome of the transaction is to roll back the transaction.
     * @throws IllegalStateException Thrown if this object is not
     * associated with a transaction.
     */
    public void setRollbackOnly() throws TransactionException {
        this.status = TransactionStatus.STATUS_MARKED_ROLLBACK;
    }

    /**
     * Complete the transaction associated with this object.
     * When this method completes, the thread becomes associated with no transaction.
     * @throws IllegalStateException Thrown if this object is not
     * associated with a transaction.
     */
    public void commit() throws TransactionException {
        this.status = TransactionStatus.STATUS_COMMITTED;
    }

    /**
     * Roll back the transaction associated with this object.
     * When this method completes, the thread becomes associated with no
     * transaction.
     * @throws IllegalStateException Thrown if this object is not
     * associated with a transaction.
     */
    public void rollback() throws TransactionException {
        this.status = TransactionStatus.STATUS_ROLLEDBACK;
    }

    /**
     * Return the (optional) reference to the object that is considered
     * the source of the transaction represented by this object.
     * This is used, for example, to set the source of all events occuring within this
     * transaction.
     * @return the source object, which may be null
     */
    public Object getSource() throws TransactionException {
        return this.source;
    }

}
