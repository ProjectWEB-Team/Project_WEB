package vn.iotstar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadinessControllerTest {
    @Test void healthChecksTheDatabaseAndReportsTheArtifactCommitWithoutLeakingErrors() {
        var jdbc = mock(JdbcTemplate.class); var properties = new Properties(); properties.setProperty("commit", "123abc");
        var beans = new StaticListableBeanFactory(); beans.addBean("build", new BuildProperties(properties));
        var controller = new ReadinessController(jdbc, beans.getBeanProvider(BuildProperties.class));
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        assertThat(controller.readiness().getBody()).containsEntry("status", "UP").containsEntry("commit", "123abc");
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(new IllegalStateException("private DB connection"));
        assertThat(controller.readiness().getStatusCode().value()).isEqualTo(503);
        assertThat(controller.readiness().getBody()).containsOnlyKeys("status");
    }
}
