package io.github.sakana.order.service;

import io.github.sakana.api.client.StockClient;
import io.github.sakana.api.pojo.dto.StockReleaseRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StockReleaseCompensationService {

    @Autowired
    private StockClient stockClient;

    // TODO 将释放失败持久化为补偿任务并通过定时任务可靠重试，避免服务重启后丢失待释放库存。
    public void release(Long orderId, RuntimeException originalFailure) {
        try {
            boolean released = stockClient.release(
                    StockReleaseRequestDTO.builder().orderId(orderId).build()
            );
            if (!released) {
                RuntimeException releaseFailure = new RuntimeException("库存释放接口返回失败");
                originalFailure.addSuppressed(releaseFailure);
                log.error("库存释放失败, orderId={}", orderId, releaseFailure);
            }
        } catch (RuntimeException releaseFailure) {
            originalFailure.addSuppressed(releaseFailure);
            log.error("库存释放异常, orderId={}", orderId, releaseFailure);
        }
    }
}
