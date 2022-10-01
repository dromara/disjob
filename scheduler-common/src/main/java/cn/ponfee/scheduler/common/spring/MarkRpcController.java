package cn.ponfee.scheduler.common.spring;

import org.springframework.web.bind.annotation.RestController;

/**
 * Mark this subclass is a spring mvc controller and with rpc({@code LocalizedMethodArguments}) trait
 *
 * @author Ponfee
 */
@RestController
@LocalizedMethodArguments
public interface MarkRpcController {
}
