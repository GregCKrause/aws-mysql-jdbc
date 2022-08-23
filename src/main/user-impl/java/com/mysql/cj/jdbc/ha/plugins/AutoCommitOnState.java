// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License, version 2.0
// (GPLv2), as published by the Free Software Foundation, with the
// following additional permissions:
//
// This program is distributed with certain software that is licensed
// under separate terms, as designated in a particular file or component
// or in the license documentation. Without limiting your rights under
// the GPLv2, the authors of this program hereby grant you an additional
// permission to link the program and your derivative works with the
// separately licensed software that they have included with the program.
//
// Without limiting the foregoing grant of rights under the GPLv2 and
// additional permission as to separately licensed software, this
// program is also subject to the Universal FOSS Exception, version 1.0,
// a copy of which can be found along with its FAQ at
// http://oss.oracle.com/licenses/universal-foss-exception.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
// See the GNU General Public License, version 2.0, for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see
// http://www.gnu.org/licenses/gpl-2.0.html.

package com.mysql.cj.jdbc.ha.plugins;

import com.mysql.cj.jdbc.JdbcConnection;

public class AutoCommitOnState implements IState {

    private ConnectionMethodAnalyzer analyzer = new ConnectionMethodAnalyzer();

    @Override
    public IState getNextState(JdbcConnection currentConnection, String methodName, Object[] args) {
        // execute("COMMIT")/execute("ROLLBACK") will not throw an error, but the driver will throw an error if
        // commit()/rollback() are called
        if (analyzer.isExecuteDml(methodName, args) || analyzer.isExecuteClosingTransaction(methodName, args)) {
            return ReadWriteSplittingStateMachine.AUTOCOMMIT_ON_TRANSACTION_BOUNDARY_STATE;
        }

        if (analyzer.isExecuteStartingTransaction(methodName, args)) {
            return ReadWriteSplittingStateMachine.AUTOCOMMIT_ON_TRANSACTION_STATE;
        }

        if (analyzer.isSetAutoCommitFalse(methodName, args)) {
            return ReadWriteSplittingStateMachine.AUTOCOMMIT_OFF_STATE;
        }

        if ("setReadOnly".equals(methodName) && args != null && args.length > 0) {
            Boolean readOnly = (Boolean) args[0];
            return Boolean.TRUE.equals(readOnly) ?
                    ReadWriteSplittingStateMachine.AUTOCOMMIT_ON_STATE :
                    ReadWriteSplittingStateMachine.READ_WRITE_STATE;
        }

        return ReadWriteSplittingStateMachine.AUTOCOMMIT_ON_STATE;
    }

    @Override
    public IState getNextState(Exception e) {
        return ReadWriteSplittingStateMachine.AUTOCOMMIT_ON_STATE;
    }

    @Override
    public boolean isTransactionBoundary() {
        return false;
    }
}
