/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.core.param.worker;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configure worker param
 *
 * @author Ponfee
 */
@Getter
@Setter
@NoArgsConstructor
public class ConfigureWorkerParam extends AuthenticationParam {
    private static final long serialVersionUID = 1023942345935168778L;

    /**
     * Action type
     */
    private Action action;

    /**
     * Action data
     */
    private String data;

    public ConfigureWorkerParam(String supervisorToken) {
        super.setSupervisorToken(supervisorToken);
    }

    public enum Action {
        /**
         * Modify worker thread pool maximum pool size
         */
        MODIFY_MAXIMUM_POOL_SIZE {
            @Override
            public <T> T parse(String data) {
                return (T) new Integer(data);
            }
        },

        /**
         * Remove(deregister) worker and clear worker thread pool task queue
         */
        REMOVE_WORKER_AND_CLEAR_TASK_QUEUE {
            @Override
            public <T> T parse(String data) {
                throw new UnsupportedOperationException();
            }
        },

        /**
         * Remove(deregister) worker
         */
        REMOVE_WORKER {
            @Override
            public <T> T parse(String data) {
                throw new UnsupportedOperationException();
            }
        },

        /**
         * Add(register) worker
         */
        ADD_WORKER {
            @Override
            public <T> T parse(String data) {
                return (T) data;
            }
        },

        ;

        public abstract <T> T parse(String data);
    }

}
