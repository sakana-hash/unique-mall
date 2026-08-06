package io.github.sakana.mockdata.runner;

import io.github.sakana.mockdata.service.MockDataImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后立即执行数据灌入，执行完即退出
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MockDataRunner implements CommandLineRunner {

    private final MockDataImportService importService;

    @Override
    public void run(String... args) {
        importService.run();
    }
}
