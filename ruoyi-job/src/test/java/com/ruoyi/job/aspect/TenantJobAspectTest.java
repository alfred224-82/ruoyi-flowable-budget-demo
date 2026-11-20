package com.ruoyi.job.aspect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * TenantJobAspect 单元测试（T15.21）
 * <p>
 * 测试核心逻辑：{@code matchVersion(edition, versions)} 版本/档位归一化匹配。
 * 该私有方法同时用于 edition（{@code versions}）与 version_type（{@code versionTypes}）两个维度的过滤，
 * 是租户级定时任务正确执行的关键路径。
 * <p>
 * 测试覆盖：
 * <ul>
 *   <li>空 allowed = 不过滤（全命中）</li>
 *   <li>精确匹配（大小写一致）</li>
 *   <li>大小写不敏感匹配</li>
 *   <li>含前后空格归一化</li>
 *   <li>多值任一匹配</li>
 *   <li>不匹配返回 false</li>
 *   <li>edition 为 null/空时返回 false</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@DisplayName("T15.21 TenantJobAspect 版本匹配单元测试")
public class TenantJobAspectTest {

    /**
     * 通过反射调用私有方法 matchVersion(String edition, String[] versions)
     */
    private boolean invokeMatchVersion(TenantJobAspect aspect, String edition, String[] versions) throws Exception {
        Method method = TenantJobAspect.class.getDeclaredMethod("matchVersion", String.class, String[].class);
        method.setAccessible(true);
        return (boolean) method.invoke(aspect, edition, versions);
    }

    /**
     * 创建一个仅用于 matchVersion 测试的实例（不注入 SysTenantMapper，因为 matchVersion 不依赖它）
     */
    private TenantJobAspect createAspect() {
        // matchVersion 是纯逻辑方法，不依赖注入的 mapper，传 null 即可
        return new TenantJobAspect(null);
    }

    @Test
    @DisplayName("allowed 为空数组 = 不过滤，全命中")
    void testEmptyAllowed() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertTrue(invokeMatchVersion(aspect, "MICRO", new String[]{}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro_pro", null));
    }

    @Test
    @DisplayName("精确匹配（大小写一致）")
    void testExactMatch() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro", new String[]{"micro"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "MICRO", new String[]{"MICRO"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro_pro", new String[]{"micro_pro"}));
    }

    @Test
    @DisplayName("大小写不敏感匹配")
    void testCaseInsensitive() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertTrue(invokeMatchVersion(aspect, "MICRO", new String[]{"micro"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro", new String[]{"MICRO"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "Micro_Pro", new String[]{"micro_pro"}));
    }

    @Test
    @DisplayName("含前后空格归一化匹配")
    void testTrimWhitespace() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertTrue(invokeMatchVersion(aspect, " micro ", new String[]{"micro"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro", new String[]{" micro "}));
        Assertions.assertTrue(invokeMatchVersion(aspect, " micro_pro ", new String[]{"micro_pro"}));
    }

    @Test
    @DisplayName("多值任一匹配")
    void testMultipleValues() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertTrue(invokeMatchVersion(aspect, "micro_pro", new String[]{"micro_lite", "micro_pro"}));
        Assertions.assertTrue(invokeMatchVersion(aspect, "basic", new String[]{"basic", "advanced", "pro"}));
    }

    @Test
    @DisplayName("不匹配返回 false")
    void testNoMatch() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertFalse(invokeMatchVersion(aspect, "micro_lite", new String[]{"micro_pro"}));
        Assertions.assertFalse(invokeMatchVersion(aspect, "basic", new String[]{"micro_lite", "micro_pro"}));
    }

    @Test
    @DisplayName("edition 为 null 时返回 false")
    void testNullEdition() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertFalse(invokeMatchVersion(aspect, null, new String[]{"micro"}));
    }

    @Test
    @DisplayName("edition 为空字符串时返回 false")
    void testEmptyEdition() throws Exception {
        TenantJobAspect aspect = createAspect();
        Assertions.assertFalse(invokeMatchVersion(aspect, "", new String[]{"micro"}));
        Assertions.assertFalse(invokeMatchVersion(aspect, "  ", new String[]{"micro"}));
    }
}
