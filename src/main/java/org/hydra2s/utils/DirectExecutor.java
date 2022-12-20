package org.hydra2s.utils;

//

import java.util.concurrent.Executor;

//
public class DirectExecutor implements Executor {
    public DirectExecutor() {

    }

    @Override
    public void execute(Runnable arg0) {
        arg0.run();
    }
}
