package ideal.sylph.api.etl;

import ideal.sylph.api.PipelinePlugin;

import java.util.Map;

public interface Sink<T>
        extends PipelinePlugin
{
    /**
     * 初始化(driver阶段执行) 需要注意序列化问题
     */
    default void driverInit(Map<String, Object> optionMap) {}

    void run(final T stream);
}
