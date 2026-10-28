package vn.iotstar.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.iotstar.exception.RegistrationException;
import vn.iotstar.service.auth.OtpDelivery;
import vn.iotstar.service.auth.SpeedSmsOtpDelivery;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class SpeedSmsConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(OtpConfiguration.class);

    @Test void smsProfileLoadsTheRealAdapterWithoutMakingANetworkRequest() {
        context.withPropertyValues("spring.profiles.active=sms", "SPEEDSMS_ACCESS_TOKEN=test-token").run(app -> {
            assertThat(app).hasNotFailed().hasSingleBean(OtpDelivery.class);
            assertThat(app.getBean(OtpDelivery.class)).isInstanceOf(SpeedSmsOtpDelivery.class);
        });
    }

    @Test void smsIsDisabledByDefault() {
        context.run(app -> {
            assertThat(app).hasNotFailed();
            assertThat(app.getBean(OtpDelivery.class)).isNotInstanceOf(SpeedSmsOtpDelivery.class);
            assertThatThrownBy(() -> app.getBean(OtpDelivery.class).send(UUID.randomUUID(), "+84901234567", "123456", Instant.now().plusSeconds(300)))
                    .isInstanceOf(RegistrationException.class);
        });
    }

    @Test void missingTokenStopsSmsStartup() {
        context.withPropertyValues("spring.profiles.active=sms", "SPEEDSMS_ACCESS_TOKEN=").run(app -> {
            assertThat(app).hasFailed();
            assertThat(app.getStartupFailure()).hasRootCauseMessage("Set SPEEDSMS_ACCESS_TOKEN privately before enabling SMS");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms,otp-local", "otp-local,sms"})
    void refusesToMixRealAndSimulatedOtpRegardlessOfProfileOrder(String profiles) {
        context.withPropertyValues("spring.profiles.active=" + profiles, "SPEEDSMS_ACCESS_TOKEN=test-token").run(app -> {
            assertThat(app).hasFailed();
            assertThat(app.getStartupFailure()).hasRootCauseMessage("Do not combine real SMS delivery with the otp-local profile");
        });
    }
}
