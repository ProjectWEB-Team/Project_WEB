package vn.iotstar.service.auth;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VietnamPhoneNormalizerTest {
    @Test void localAndInternationalFormatsResolveToOnePhoneKey() {
        for (String value : new String[]{"0901234567", "+84 901 234 567", "84901234567"})
            assertThat(VietnamPhoneNormalizer.normalize(value)).isEqualTo("+84901234567");
    }
    @Test void rejectsNonPhoneCharactersAndInvalidLengths() {
        for (String value : new String[]{"abc0901234567", "090123", "+84901234567xyz", "0101234567", "++84901234567"})
            assertThatThrownBy(() -> VietnamPhoneNormalizer.normalize(value)).isInstanceOf(RuntimeException.class);
    }
}
