package cn.ponfee.scheduler.core.handle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Split task structure.
 *
 * @author Ponfee
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitTask implements java.io.Serializable {
    private static final long serialVersionUID = 5200874217689134007L;

    private String taskParam;

}
