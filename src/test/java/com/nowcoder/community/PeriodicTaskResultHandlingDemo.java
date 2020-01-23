package com.nowcoder.community;

import java.util.Random;
import java.util.concurrent.*;

public class PeriodicTaskResultHandlingDemo {
    final static ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) throws InterruptedException {
        final String host = args[0];
        final AsyncTask<Integer> asyncTask = new AsyncTask<Integer>(ses) {

            final Random rnd = new Random();
            final String targetHost = host;

            @Override
            public Integer call() throws Exception {
                return doCall();
            }

            private Integer doCall() throws Exception{
                Thread.sleep(2000);
                Integer r = Integer.valueOf(rnd.nextInt(4));
                return r;
            }

            @Override
            protected void onResult(Integer result) {
                doSomething(result);
            }

            private void doSomething(Integer r){
                System.out.println(r);
            }
        };

        ses.scheduleAtFixedRate(asyncTask,0,3, TimeUnit.SECONDS);
    }





    abstract static class AsyncTask<V> implements Runnable, Callable<V> {
        protected final Executor executor;

        public AsyncTask(Executor executor){
            this.executor = executor;
        }

        public AsyncTask(){
            this(new Executor() {
                @Override
                public void execute(Runnable command) {
                    command.run();
                }
            });
        }

        // run()成为入口
        @Override
        public void run() {
            Exception exp = null;
            V r = null;
            try{
                // 执行task
                r = call();
            } catch (Exception e){
                exp = e;
            }
            final V result = r;
            // 异步处理结果
            if (null == exp){
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onResult(result);
                    }
                });
            } else {
                final Exception exceptionCaught = exp;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onError(exceptionCaught);
                    }
                });
            }
        }
        protected abstract void onResult(V result);

        protected void onError(Exception e){
            e.printStackTrace();;
        }
    }
}

