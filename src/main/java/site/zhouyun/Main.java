package site.zhouyun;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1); // 10 为线程数量
        //  定时执行任务
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            DDNS.task();
        }, 1, DDNS.period, TimeUnit.SECONDS); // 1s 后开始执行，每 10s 执行一次

    }
}
