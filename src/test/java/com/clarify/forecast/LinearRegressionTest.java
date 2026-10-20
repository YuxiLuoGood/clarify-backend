package com.clarify.forecast;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LinearRegressionTest {

    @Test
    void fitsAPerfectLine() {
        LinearRegression model = LinearRegression.fit(new double[]{800, 900, 1000});

        assertThat(model.slope()).isCloseTo(100, within(1e-9));
        assertThat(model.intercept()).isCloseTo(700, within(1e-9));
        assertThat(model.predict(4)).isCloseTo(1100, within(1e-9));
    }

    @Test
    void flatSeriesHasZeroSlope() {
        LinearRegression model = LinearRegression.fit(new double[]{250, 250, 250, 250});

        assertThat(model.slope()).isCloseTo(0, within(1e-9));
        assertThat(model.predict(10)).isCloseTo(250, within(1e-9));
    }

    @Test
    void needsAtLeastTwoPoints() {
        assertThatThrownBy(() -> LinearRegression.fit(new double[]{42}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
