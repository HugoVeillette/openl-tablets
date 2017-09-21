package org.openl.rules.core.ce;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.rules.vm.ce.SimpleRulesRuntimeEnvMT;
import org.openl.vm.IRuntimeEnv;

public final class ServiceMT {

    private ForkJoinPool forkJoinPool = new ForkJoinPool();

    private static class ServiceMTHolder {
        private static final ServiceMT INSTANCE = new ServiceMT();
    }

    public static ServiceMT getInstance() {
        return ServiceMTHolder.INSTANCE;
    }

    public boolean isParallelismReasonableNow() {
        return forkJoinPool.getActiveThreadCount() <= forkJoinPool.getParallelism();
    }
    
    public void execute(IRuntimeEnv env, Runnable runnable) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        RunnableRecursiveAction action = new RunnableRecursiveAction(runnable, simpleRulesRuntimeEnv);
        simpleRulesRuntimeEnv.pushAction(action);
        if (env instanceof SimpleRulesRuntimeEnvMT) {
            action.fork();
            return;
        }
        forkJoinPool.execute(action);
        return;
    }
    
    public void execute(ForkJoinTask<?> task) {
        forkJoinPool.execute(task);
    }

    public void executeAll(ForkJoinTask<?>... tasks) {
        for (ForkJoinTask<?> task : tasks) {
            forkJoinPool.execute(task);
        }
    }

    public void join(IRuntimeEnv env) {
        SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = extractSimpleRulesRuntimeEnv(env);
        try {
            while (simpleRulesRuntimeEnv.joinActionIfExists()) {
            }
        } finally {
            while (simpleRulesRuntimeEnv.cancelActionIfExists()) {
            }
        }
    }

    private SimpleRulesRuntimeEnv extractSimpleRulesRuntimeEnv(IRuntimeEnv env) {
        if (env instanceof TBasicContextHolderEnv) {
            IRuntimeEnv env1 = ((TBasicContextHolderEnv) env).getEnv();
            if (env1 instanceof TBasicContextHolderEnv) {
                return extractSimpleRulesRuntimeEnv(env1);
            } else {
                return (SimpleRulesRuntimeEnv) env1;
            }
        } else {
            return (SimpleRulesRuntimeEnv) env;
        }
    }

    private static class RunnableRecursiveAction extends RecursiveAction {
        private static final long serialVersionUID = -6827837658658403954L;
        private Runnable runnable;
        private SimpleRulesRuntimeEnv env;

        private RunnableRecursiveAction(Runnable runnable, SimpleRulesRuntimeEnv env) {
            this.runnable = runnable;
            this.env = env;
        }

        @Override
        protected void compute() {
            runnable.run((SimpleRulesRuntimeEnv) env.clone());
        }
    }
}
