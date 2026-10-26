package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import vn.iotstar.entity.ShipmentStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves the Workflow section 101 demo flags are enabled by the demo profile only (D-08, D-09). */
class DemoProfilePropertiesTest {

    @Test
    void demoProfileEnablesBothFlagsAndTheShortenedTimers() throws IOException {
        LogisticsProperties properties = bind("application-demo.properties");

        assertThat(properties.getDemo().isFastTrackRouteBypass()).isTrue();
        assertThat(properties.getDemo().isAutoLinehaulTransitSimulation()).isTrue();
        assertThat(properties.getTimeout().getDisposedGraceMinutes()).isEqualTo(2);
        assertThat(properties.getTimeout().getDisposedGraceDays()).isZero();
        assertThat(properties.getDelivery().isAttemptDayLock()).isFalse();
    }

    @Test
    void productionPropertiesKeepTheStrictDefaults() throws IOException {
        LogisticsProperties properties = bind("application.properties");

        assertThat(properties.getDemo().isFastTrackRouteBypass()).isFalse();
        assertThat(properties.getDemo().isAutoLinehaulTransitSimulation()).isFalse();
        assertThat(properties.getDelivery().isAttemptDayLock()).isTrue();
        assertThat(properties.getTimeout().getDisposedGraceDays()).isEqualTo(7);
    }

    @Test
    void codeDefaultsAreOff() {
        LogisticsProperties.Demo demo = new LogisticsProperties().getDemo();
        assertThat(demo.isFastTrackRouteBypass()).isFalse();
        assertThat(demo.isAutoLinehaulTransitSimulation()).isFalse();
    }

    @Test
    void noOtherClasspathPropertiesFileSetsADemoKey() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath*:application*.properties");
        assertThat(resources).isNotEmpty();
        for (Resource resource : resources) {
            if ("application-demo.properties".equals(resource.getFilename())) continue;
            assertThat(PropertiesLoaderUtils.loadProperties(resource).stringPropertyNames())
                    .as(resource.getDescription())
                    .noneMatch(key -> key.startsWith("uteexpress.demo."));
        }
    }

    @Test
    void flagsDoNotAddShipmentStatuses() {
        // Same contract as CanonicalDomainContractTest; repeated here so this suite alone shows the invariant.
        assertThat(ShipmentStatus.values()).hasSize(13);
    }

    private static LogisticsProperties bind(String file) throws IOException {
        LogisticsProperties properties = new LogisticsProperties();
        new Binder(ConfigurationPropertySources.from(new PropertiesPropertySourceLoader().load(file, new ClassPathResource(file))))
                .bind("uteexpress", Bindable.ofInstance(properties));
        return properties;
    }
}
